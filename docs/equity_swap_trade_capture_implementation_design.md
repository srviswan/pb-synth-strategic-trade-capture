# Equity Swap Trade Capture — Implementation Design (GCAM → SwapBlotter)

This document is an implementation-oriented architecture artefact for the **equity swap lifecycle** slice of strategic trade capture. It aligns the layered model in [trade_capture_service_design.md](./trade_capture_service_design.md) with two concrete inbound JSON message kinds ([fund-allocation-input.schema.json](./fund-allocation-input.schema.json), [swap-allocation-input.schema.json](./swap-allocation-input.schema.json)) and the egress contract in [swap-dataproduct-openapi.yaml](./api/swap-dataproduct-openapi.yaml). **No executable code** is specified here; this is the blueprint for subsequent specifications, services, and tests.

---

## 1. Purpose and scope

**In scope**

- Ingestion of **raw hedge trades** allocated to an **account**, expressed as GCAM messages at **two allocation levels**: **fund allocation** (`fund-allocation`) or **swap allocation** (`swap-allocation`), discriminated by `messageKind`.
- **Enrichment** from **Security reference data**, **Account reference data**, and **Book reference data** (and related party/entity sources as needed).
- Passage through a **rules engine** (economic and non-economic rules) that transforms the enriched raw allocation into **`SwapContract`** and **`SwapTransaction`**, from which **`SwapPosition`** rows are materialised.
- Encapsulation in a **deduplicated graph**: **`SwapBlotter`** (`swapContracts[]`, each contract with `positions[]`, each position with `transactions[]`), and **async egress** via **`BusinessEvent`** (`payload` carries **`SwapBlotter`** for full capture; one or more contracts per event).

**Out of scope (for this artefact)**

- CDM-internal **`TradeLot`** / protobuf-only serialisation details where they differ from this **OpenAPI `SwapBlotter`** (lifecycle handoff) shape; map between internal and egress at the anti-corruption boundary.
- Concrete technology choices for each integration (beyond alignment with the existing design: Solace, REST, idempotency stores, and so on).

---

## 2. Business context — raw hedge, two-level allocation, egress shape

**Starting point:** the platform receives a **raw hedge trade** that has been **allocated to an account**. The allocation detail arrives at one of **two levels**:

| Allocation level | `messageKind` | Meaning |
|------------------|---------------|---------|
| **Swap allocation** | `swap-allocation` | Contract-level terms and economics for the equity swap (the “swap” slice of the hedge allocation). Feeds primarily **`SwapContract`**. |
| **Fund allocation** | `fund-allocation` | Fund / book line against the swap (quantity, price, settlement, commissions, etc.). Feeds primarily **`SwapTransaction`** and, via rules, **`SwapPosition`**. |

**Pipeline (logical):**

1. **Ingress** — validate message against the appropriate JSON Schema; correlate to contract and transaction identity (see §4).
2. **Enrichment** — join to **Security** reference data (e.g. instrument, RIC), **Account** reference data (account, limits, attributes), and **Book** reference data (book entity, routing, normalised book keys used in partitioning).
3. **Rules engine** — **economic** rules (notionals, quantities, calendars, financing, rounding) and **non-economic** rules (legal entity, documentation, break rights, regulatory flags) transform the enriched raw hedge into a normalised **`SwapContract`** and one or more **`SwapTransaction`** instances.
4. **Position materialisation** — allocation and book/account rules derive **`SwapPosition`** rows (notional, quantity, identifiers) tied to the contract.
5. **Transaction placement** — each **`SwapTransaction`** is attached under **exactly one** parent **`SwapPosition`** (1:M from position to transaction).
6. **Encapsulation** — assemble **`SwapBlotter`**: `swapContracts[]` (length ≥ 1); each contract holds only `positions[]`, and each position holds only `transactions[]` (see OpenAPI `components.schemas.SwapBlotter`). No parallel contract-level `transactions` array.
7. **Envelope (async)** — publish **`BusinessEvent`** whose **`payload`** SHOULD be **`SwapBlotter`** for full capture; metadata on the envelope carries `eventId`, `eventType`, correlation, Solace routing, etc.

**Synchronous APIs** may still expose resource-oriented paths (`/swapContracts`, positions, transactions) as in the OpenAPI; list or detail responses SHOULD follow the same **deduplicated** nesting when returning embedded graphs. **Downstream async consumers** treat **`BusinessEvent`** as the **only** outer wrapper: the business tree is inside `payload`, not duplicated beside the envelope.

### 2.1 Canonical graph and deduplication rules

| Relationship | Cardinality | Rule |
|--------------|-------------|------|
| **BusinessEvent** → **SwapBlotter** | 1:1 in `payload` | Envelope carries one blotter per message for full capture. |
| **SwapBlotter** → **SwapContract** | 1:M (`swapContracts[]`) | One blotter may carry one or many provisional or existing contracts (e.g. batch or linked deals). |
| **SwapContract** → **SwapPosition** | 1:M (`positions[]`) | Only child collection on the contract. |
| **SwapPosition** → **SwapTransaction** | 1:M (`transactions[]`) | Sole location for transaction rows in the full-capture payload. |

**Deduplication (normative):**

- **MUST NOT** emit a `transactions` array on **`SwapContract`**.
- **MUST NOT** emit the same **`SwapTransaction`** (same `tradingReportId`) in more than one position.
- **SHOULD** set **`SwapTransaction.positionId`** to the parent `positionId` when the transaction appears in granular `oneOf` payloads; when nested under **`SwapPosition.transactions`**, it **SHOULD** match the enclosing position.
- **List vs detail:** `GET /swapContracts` **MAY** omit `positions` (or empty `positions`) for performance; when `positions` are present, they **MUST** use the nested `transactions` shape, not a separate contract-level transaction list.

### 2.2 Lifecycle handoff and provisional identifiers (first trade)

**Trade capture** does **not** own the golden **SwapContract** store: it produces a **`SwapBlotter`** for the **swap lifecycle service**, which **creates or updates** the canonical contract and may **allocate `ContractIdentifier`** when none exists yet.

| Situation | `tradeIdString` / GCAM | `SwapContract.ContractIdentifier` on blotter | Correlation |
|-----------|-------------------------|-----------------------------------------------|-------------|
| Subsequent trades | Usually present | Set to known id | Normal idempotency on contract id + transaction ids |
| **First trade** | **May be absent** | **Omit** until lifecycle assigns | **MUST** populate **`SwapBlotter.captureCorrelationId`** (and optionally `blotterId`); lifecycle returns or publishes the assigned **`ContractIdentifier`** for downstream correlation |

**Implementation notes:**

- Partition and idempotency keys that today assume `tradeIdString` **MUST** fall back to `captureCorrelationId` + security/book (or another stable intake key) until a contract id exists.
- **`SwapPosition.contractIdentifier`** may be omitted when nested under a provisional contract; bind positions and transactions by parent **`SwapContract`** node in the blotter tree.
- Align **`BusinessEvent.correlationId`** with **`SwapBlotter.captureCorrelationId`** when both are present.

---

## 3. Alignment with platform architecture

The following table binds the **generic** layers from the strategic design to **this program’s** responsibilities. It does not replace the parent document; it specialises it.

| Layer (from strategic design) | Equity swap GCAM implementation |
|------------------------------|----------------------------------|
| **API / messaging ingress** | Accept wrapped JSON validated against the two JSON Schemas; optional protobuf envelope later. Discriminate routing by `messageKind`. Expose internal idempotency and correlation headers consistent with the parent design. |
| **Trade capture orchestration** | `TradeCaptureService` (or equivalent) branches: swap-allocation pipeline vs fund-allocation pipeline; merges results into **`SwapBlotter`** (`swapContracts[]`). Typical message yields one contract; multi-contract events are allowed when rules batch or link identifiers. |
| **Enrichment** | **Security** reference data: instrument / RIC / identifiers from `securityId`. **Account** reference data: account attributes, credit context. **Book** reference data: book entity, normalised book keys, desk alignment. Party SDS IDs and display names when inbound payloads are incomplete. |
| **Rules engine** | **Economic** rules: map raw hedge fields into contract economics and transaction economics (notionals, quantities, calendars, financing). **Non-economic** rules: legal entity, documentation, break rights, regulatory flags. **Workflow** rules: STP vs pending approval. Output: **`SwapContract`** rows, **`SwapPosition`** rows, **`SwapTransaction`** rows placed under the correct position (1:M). |
| **Validation** | JSON Schema validation at boundary; OpenAPI-shaped validation for emitted aggregates; business rules (open book, credit, settlement date vs calendar). |
| **State management** | CDM-aligned position/trade state remains the internal source of truth; **published snapshot** is **`SwapBlotter`** (same tree inside **`BusinessEvent.payload`** when async). Transitions must be consistent with partition sequencing per contract. |
| **Output generation** | Build **`SwapBlotter`** with deduplicated nesting. For **async** egress, set **`BusinessEvent.payload`** to that object. Serialise for REST, Solace, or both per deployment. |

**Canonical egress aggregate:** **`SwapBlotter`** (`components.schemas.SwapBlotter` in [swap-dataproduct-openapi.yaml](./api/swap-dataproduct-openapi.yaml)) — **consumed by the swap lifecycle service** to create/update the persisted swap contract. **Canonical async wrapper:** **`BusinessEvent`** with `payload` = **`SwapBlotter`** for full capture; other subscribers may use protobuf or parallel bindings as needed.

---

## 4. Inbound message model

### 4.1 Discrimination

Both schemas set a fixed `messageKind`:

- `fund-allocation` → fund allocation line.
- `swap-allocation` → swap-level allocation / contract attributes.

`additionalProperties: true` on both schemas allows forward-compatible fields; the platform should persist unknown fields in **metadata** for audit and later mapping, but only promote fields to the OpenAPI model when governed by rules or schema updates.

### 4.2 Identity and correlation (implementation requirements)

| Concept | Source fields | Usage |
|--------|----------------|--------|
| Contract identity | `tradeIdString` (when present) | Maps to `SwapContract.ContractIdentifier` after lifecycle confirmation; **omit on blotter** for first trade until lifecycle assigns. |
| Pre-lifecycle correlation | Intake idempotency key, message id | Populates **`SwapBlotter.captureCorrelationId`** (and envelope **`BusinessEvent.correlationId`**) when **`ContractIdentifier`** is unknown. |
| Transaction identity | `transactionId` (fund path) | Maps to `SwapTransaction.tradingReportId` (required in OpenAPI). |
| Allocation link | `fundAllocationId` | Maps to `SwapTransaction.allocationId`. |
| Security for enrichment | `securityId` | Lookup key; fund schema notes optional alignment to `ricCode` on contract when enrichment supplies it. |

These fields drive **idempotency** and **partition** strategy (see §6).

---

## 5. Layer-by-layer processing (logical)

### 5.1 Ingress validation

1. Parse body; verify `messageKind` matches one of the two allowed values.
2. Validate against the appropriate JSON Schema (`fund-allocation-input` vs `swap-allocation-input`).
3. Attach correlation ID (HTTP header or message metadata) for tracing through enrichment and rules.

### 5.2 Enrichment

- **Security reference data:** Resolve `securityId` to instrument / RIC and contract-level identifiers required by downstream consumers.
- **Account reference data:** Validate and enrich account context for the allocated hedge (limits, classification, STP eligibility signals).
- **Book reference data:** Normalise `book` (fund path) and align with desk / entity; resolve `legalEntity` and party fields (swap path) against book and party reference stores.
- Populate missing party attributes when only IDs or only names are present.

### 5.3 Rules application

- **Economic rules — swap-allocation:** Normalise break-fee and calendar strings; default financing and day-count behaviour by product type; map GCAM-specific flags into **`SwapContract`** fields (see §7.2 for OpenAPI gaps).
- **Economic rules — fund-allocation:** Derive `actionbuysell`, settlement, and commission semantics; align quantity/price with contract currency and rounding rules; emit **`SwapTransaction`** rows under the target **`SwapPosition`** (create position if absent per position-derivation rules).
- **Non-economic rules:** Legal entity, documentation, break rights, regulatory flags on **`SwapContract`** (and transaction-level flags where applicable).
- **Position rules:** From allocations + book/account context, materialise **`SwapPosition`** (identifiers, notionals, quantities); attach new fund allocations as **`transactions`** children of that position only.
- **Workflow:** Determine whether the resulting event may proceed to STP publication or requires approval (mirrors parent design).

### 5.4 Validation

- Cross-check fund allocation against existing contract: contract must exist (or rules define create-on-first-fund behaviour — **decision point**, see §8).
- Validate dates, numeric ranges, and required OpenAPI fields before emission.
- State transition checks against current CDM-backed snapshot for the partition.

### 5.5 Output assembly

- **Swap-allocation:** Upsert **`SwapContract`** inside **`swapContracts`**; refresh contract-level fields; update or create **`positions`** as needed. **Do not** attach a contract-level `transactions` array.
- **Fund-allocation:** Resolve or create the parent **`SwapPosition`**; append or upsert **`SwapTransaction`** only inside that position’s **`transactions`** array.
- **Encapsulate:** Build **`SwapBlotter`** with `swapContracts[]` (≥ 1), each obeying Contract → Position → Transaction nesting.
- **Async envelope:** Materialise **`BusinessEvent`** with `payload` = **`SwapBlotter`** for full capture. Narrower `payload` shapes (`SwapContract`, `SwapPosition`, `SwapTransaction` alone) remain valid for partial lifecycle events per OpenAPI `oneOf`.

---

## 6. Idempotency, partitioning, and ordering

### 6.1 Partition key

The strategic design uses `{accountId}_{bookId}_{securityId}`. For GCAM messages:

- **Minimum:** `{contractIdentifier}_{securityId}` or `{book}_{securityId}_{contractIdentifier}` once book and security are normalised post-enrichment.
- **Recommendation:** After enrichment, compute partition as `{normalisedBookId}_{normalisedSecurityId}_{ContractIdentifier}` to align with account/book + security sequencing while keeping contract scope explicit.
- **First trade (no `ContractIdentifier` yet):** use `{normalisedBookId}_{normalisedSecurityId}_{captureCorrelationId}` (or another stable intake key) until the lifecycle service assigns **`ContractIdentifier`**; reconcile or migrate partition state when the id arrives.

### 6.2 Idempotency

| Path | Suggested key | Notes |
|------|----------------|-------|
| Fund allocation | `transactionId` + `fundAllocationId` (composite) | Prevents duplicate allocation lines; align with parent `tradeId` / idempotency header patterns. |
| Swap allocation | `tradeIdString` + message hash or version timestamp | Contract upserts may be repeated; define idempotent merge semantics (replace vs patch). |

### 6.3 Ordering

For a **new** swap, **swap-allocation** should precede **fund-allocation** in the happy path so contract economic terms exist before allocation lines. The implementation should define behaviour for **out-of-order arrival** (queue, hold fund messages until contract materialises, or reject with a recoverable error).

---

## 7. Field mapping to OpenAPI

Mappings in this section populate **`SwapContract`**, **`SwapPosition`**, and **`SwapTransaction`** (transactions always under a position), composed into **`SwapBlotter`** and, when published async, placed in **`BusinessEvent.payload`**.

### 7.1 Fund allocation → `SwapTransaction` (and related)

The fund schema already carries `x-oas-property` hints. Implementation should treat these as the **authoritative GCAM → API field map** for the transaction slice:

| GCAM field (schema) | OpenAPI property |
|---------------------|------------------|
| `transactionId` | `tradingReportId` |
| `fundAllocationId` | `allocationId` |
| `tradeIdString` | Context: `ContractIdentifier` / path |
| `ApprovingTrader` | `approvingTraderName` |
| `book` | `books` |
| `brokerSDSID` | `brokerSdsId` |
| `clientRefId` | `ClientRefId` |
| `clientUnwindIndicator` | `ClientUnwindIndicator` |
| `initialCollateralPct` | `collateralPercentage` |
| `commission` | `Commission` |
| `commissionRate` | `CommissionRate` |
| `commissionRateType` | `commissionRateType` |
| `corporateActionType` | `CorporationActionType` |
| `creditApproval` / `creditApprovalName` / `creditApprovalNumber` | Same-named OpenAPI fields |
| `settlementDate` | `dateOfSettlement` |
| `quantity` | `tradeQuantity` |
| `price` | `tradeNetPrice` |
| `buySell` | `actionbuysell` |
| `activeTrade` | `activeTrading` |

**Contract-level:** `securityId` may contribute to `ricCode` on `SwapContract` after enrichment (per schema description).

### 7.2 Swap allocation → `SwapContract`

Use `x-oas-property` where present. Selected mappings:

| GCAM field | OpenAPI property |
|------------|------------------|
| `tradeIdString` | `ContractIdentifier` |
| `interestRateType` | `Financing` |
| `interestFixedNotional` | `fixedInterestNotional` |
| `barcapBreakRights` | `barclaysBreakableRights` |
| `clientBreakRights` | `clientBreakableRights` |
| `legalEntity` | `bookEntity` |
| `equityLegPaymentGap` | `equityLegPaymentGap` |
| `equityPaymentCalendar` | `equityPaymentBusinessCalendar` |
| `equityPaymentConvention` | `equityPaymentConvention` |
| `interestPaymentCalendar` | `fundingPaymentBusinessCalendar` |
| `interestResetCalendar` | `fundingResetBusinessCalendar` |
| `maturityDate` | `swapMaturityDate` |
| `swapCurrency` | `swapCurrency` |
| `longShortInd` | `SwapLongShort` |
| `docAssetType` | `SwapAssetType` |
| `counterParty` / `counterPartyId` | `tradeParty1Name` / `tradeParty1SdsId` |
| `party` / `partyId` | `tradeParty2Name` / `tradeParty2SdsId` |
| `traderId` | `traderName` |

Set **`execSource`** (e.g. `GCAM`) and **`executionDateTime`** via rules or message metadata when not in the inbound schema.

### 7.3 OpenAPI gap register (swap-allocation)

Several inbound fields map to **`x-oas-property` values that are not yet defined** on `SwapContract` in `swap-dataproduct-openapi.yaml`. Track these as **schema extensions** or **metadata** until the OpenAPI is revised:

- `accrueInterestOnPartialUnwinds`, `addCommissionToPrice`
- `barclaysBreakableRightsNoOfDays`, `barclaysBreakFee`, `barclaysBreakFeeType`
- `barclaysRepresentativeBrid`, `barclaysRepresentativeName`
- `clientBreakableRightsNoOfDays`, `clientBreakFee`, `clientBreakFeeType`
- `confirmationType`, `ConfirmationType`
- `equityMarketSettlementDays`

**Architectural rule:** do not drop these fields; carry them in a versioned **extension bag** on the internal model and, when publishing async, on the relevant **`SwapContract`** (or companion metadata inside **`BusinessEvent.payload`** alongside **`SwapBlotter`**) until the egress schema absorbs them.

### 7.4 `SwapPosition`

The OpenAPI requires `positionId` and `contractIdentifier`. GCAM inbound schemas do not specify position identifiers directly. Implementation must define:

- How **`positionId`** is generated (rule-based: one position per book per contract, per fund, etc.).
- How **`positionNotional`** / **`positionQuantity`** relate to rolled-up fund allocations.

This belongs in the **rules** catalogue and should be documented in a follow-on **position derivation** supplement.

---

## 8. Decision log (to close before build)

| ID | Topic | Options | Recommendation |
|----|--------|---------|----------------|
| D1 | First message for a new deal | Require swap-allocation first vs create stub contract on first fund-allocation | Prefer swap-allocation first; buffer or DLQ fund-allocation until contract exists unless product mandates otherwise. |
| D2 | REST vs messaging as system of record | REST paths in OpenAPI vs async only | Use REST for synchronous confirmation where needed; for async lifecycle, publish **`BusinessEvent`** (envelope) with **`payload`** = **`SwapBlotter`** when publishing full capture outcomes (per OpenAPI `publishBusinessEvent`). |
| D3 | Contract embed vs normalised store | Large `SwapContract` with embedded arrays vs separate resources | Align runtime persistence with API resource model (`/swapContracts`, `.../positions`, `.../transactions`) for traceability. |
| D4 | OpenAPI gap fields | Extend OAS vs internal-only metadata | Plan OAS minor version bump; until then, `payload` extensions or `solaceHeaders` for opaque carry-forward. |

---

## 9. Traceability

| Artefact | Role |
|----------|------|
| [trade_capture_service_design.md](./trade_capture_service_design.md) | Platform layers, idempotency, state, scalability |
| [fund-allocation-input.schema.json](./fund-allocation-input.schema.json) | Inbound fund line structure and transaction field hints |
| [swap-allocation-input.schema.json](./swap-allocation-input.schema.json) | Inbound contract structure and contract field hints |
| [swap-dataproduct-openapi.yaml](./api/swap-dataproduct-openapi.yaml) | Deduplicated graph: **`SwapBlotter`**, **`BusinessEvent`**; components **`SwapContract`**, **`SwapPosition`**, **`SwapTransaction`** |

---

## 10. Next artefacts (still not code)

1. **OpenAPI delta**: add missing `SwapContract` properties or document explicit extension object.
2. **Position derivation spec**: rules for `SwapPosition` from fund allocations.
3. **Message ordering and recovery**: state machine for out-of-order GCAM events.
4. **Test vectors**: golden JSON for each `messageKind` → expected **`SwapBlotter`** (nested positions/transactions) and wrapped **`BusinessEvent`** (`payload` = aggregate).
5. **Correlation matrix**: Splunk/dashboard IDs for metrics listed in the strategic design, scoped to GCAM ingress.

---

*Document version: 1.3 — aligns with repository `pb-synth-strategic-trade-capture` and swap-dataproduct OpenAPI v1.3.0 (`SwapBlotter` → lifecycle service, Contract → Position → Transaction, **BusinessEvent** envelope, provisional ids on first trade).*
