# Claude efficiency — using this repository with AI assistants

This note helps **Claude Code**, **Cursor**, and similar tools load the right context quickly. Keep it **short**; deep truth lives in the linked design docs and OpenAPI files.

## Repository map

| Area | Path | Notes |
|------|------|--------|
| Strategic / capture design | `docs/*.md` | Architecture and product behavior |
| HTTP contracts | `docs/api/*.yaml` | Versioned OpenAPI; align Java DTOs when changing |
| JSON schemas (examples) | `docs/*.schema.json` | Allocation inputs, etc. |
| Reference Data (Java) | `reference-data/` | Maven parent + server, client SDK, sample |

Root layout: **`docs/`** is specification-heavy; **`reference-data/`** is the runnable Reference Data service and shared client. See also `reference-data/README.md` for module-level build instructions.

## Authoritative docs (read these before large changes)

- **Trade Capture** — `docs/trade_capture_service_design.md`
- **Equity swap / blotter / lifecycle handoff** — `docs/equity_swap_trade_capture_implementation_design.md`
- **Reference Data platform, SDK, vs market data** — `docs/reference_data_service_design.md`

## API artifacts

- Swap data product — `docs/api/swap-dataproduct-openapi.yaml`
- Reference Data Service — `docs/api/reference-data-service-openapi.yaml`

When you change YAML paths or models, update hand-written client DTOs and controllers under `reference-data/` unless codegen is introduced.

## Build and verify (Reference Data)

From repository root:

```bash
cd reference-data && mvn verify
```

Stack: Java 17, Spring Boot 3.3.x (see `reference-data/pom.xml`). Server uses **separate H2 databases** per module (security, account, book) with Flyway migrations.

**Account ingestion from an external API** (optional): configure `refdata.account.sync.*` and `refdata.account.source.*` on the server; see `docs/reference_data_service_design.md` §3.1. Integration tests that do not opt in set `refdata.account.sync.enabled=false`.

## Conventions worth preserving

- **Reference Data** exposes resources under a versioned base (e.g. `/api/v1/...`); batch endpoints reduce N+1 from domain services.
- **Market data** (prices, curves, feeds) is **out of scope** for the Reference Data client; keep that boundary clear in new code.
- **Trade capture enrichment** should use the **`ReferenceDataClient`** facade pattern (see `reference-data/trade-capture-sample/`) rather than ad-hoc HTTP per resource.
- Prefer **linking** long design documents here instead of duplicating them in chat or in this file.

## Optional: repo-root brief for tools

Some tools auto-read **`CLAUDE.md`** or **`AGENTS.md`** at the repository root. This file is the canonical **docs/** copy; you may add a one-line root file that points here if your workflow benefits from it.

## What not to do

- Do not paste multi-hundred-line design sections into prompts when a file path suffices—ask the assistant to read the linked doc.
- Do not drift OpenAPI and Java types silently; treat contract and implementation as one change when feasible.

===============
You don’t need a generic “AI prompt library”—you need a **domain-specific, production-grade skill system** aligned to your equity swap lifecycle, CDM mapping, and event-driven architecture. Below is a **complete, opinionated skill library** tailored to your system.

---

# 0. Design Principles (Non-negotiable)

1. **Deterministic outputs** → JSON schemas only
2. **Domain anchoring** → always inject swap lifecycle context
3. **Composable skills** → outputs feed downstream skills
4. **Event-first modeling** → everything resolves to events
5. **No hidden inference** → unknowns explicitly flagged

---

# 1. Skill Library Structure

```id="9g3x2a"
/claude-skills/
  /core/
    context.md
    output-contracts.md

  /discovery/
    extract-flow.md
    dependency-map.md
    data-lineage.md

  /domain/
    map-to-cdm.md
    lifecycle-normalizer.md
    economic-state-builder.md

  /architecture/
    service-decomposition.md
    event-modeling.md
    api-contract.md
    saga-design.md

  /data/
    canonical-model.md
    ods-design.md
    cqrs-read-model.md

  /quality/
    cucumber-tests.md
    edge-case-generator.md
    reconciliation-checks.md

  /ops/
    idempotency-strategy.md
    replay-strategy.md
    observability.md
```

---

# 2. Core Context (Injected into EVERY skill)

### `core/context.md`

```id="0a6l3d"
System: Equity Swap Lifecycle Management

Products:
- Single Stock Swap
- Index Swap
- Basket Swap
- Custom Index Swap

Lifecycle Events:
- New Trade
- Amend (Top-up)
- Partial/Full Unwind
- Reset Event
- Corporate Action (dividends, splits)
- Termination

Architecture:
- Event-driven
- CQRS (OLTP + Read Models)
- Real-time ODS
- 15-second regulatory SLA

Standards:
- FpML 5.x (trade representation)
- CDM (event + state model)

Constraints:
- Event versioning with selective replay
- Lot-level P&L tracking
- Separate pricing service
```

---

# 3. Discovery Skills (Reverse Engineering Legacy)

## 3.1 `extract-flow.md`

```id="dznk6q"
TASK: Extract complete execution flow.

INPUT:
- Code files
- Entry trigger (API, batch, message)

OUTPUT:
{
  "entry_points": [],
  "call_chain": [],
  "domain_objects": [],
  "external_interactions": {
    "db": [],
    "messaging": [],
    "files": []
  },
  "side_effects": [],
  "sequence_flow": []
}

RULES:
- Do not infer missing logic
- Mark unknowns explicitly
```

---

## 3.2 `dependency-map.md`

```id="vybd07"
TASK: Identify module and class dependencies.

OUTPUT:
{
  "modules": [],
  "dependencies": [
    {
      "from": "",
      "to": "",
      "type": "sync|async|db"
    }
  ]
}
```

---

## 3.3 `data-lineage.md`

```id="fl4n3t"
TASK: Trace data movement across system.

OUTPUT:
{
  "source": "",
  "transformations": [],
  "destinations": [],
  "data_mutations": []
}
```

---

# 4. Domain Skills (Your Core Differentiator)

## 4.1 `map-to-cdm.md`

```id="2h4l5g"
TASK: Map legacy model to CDM.

OUTPUT:
{
  "mappings": [
    {
      "legacy": "",
      "cdm_equivalent": "",
      "confidence": "high|medium|low"
    }
  ],
  "gaps": [],
  "extensions_required": []
}
```

---

## 4.2 `lifecycle-normalizer.md`

```id="0rxb5h"
TASK: Normalize business flow into lifecycle events.

OUTPUT:
{
  "normalized_events": [
    {
      "event_type": "",
      "trigger": "",
      "pre_conditions": [],
      "post_conditions": []
    }
  ]
}
```

---

## 4.3 `economic-state-builder.md`

```id="5b5h1q"
TASK: Build economic state from events.

OUTPUT:
{
  "trade_state": {},
  "positions": [],
  "cashflows": [],
  "accruals": [],
  "realized_pnl": [],
  "unrealized_pnl": []
}
```

---

# 5. Architecture Skills (Where You’ll Spend Most Time)

## 5.1 `service-decomposition.md`

```id="tx6x03"
TASK: Decompose into microservices.

OUTPUT:
{
  "services": [
    {
      "name": "",
      "responsibility": "",
      "owned_entities": [],
      "apis": [],
      "events_produced": [],
      "events_consumed": []
    }
  ],
  "anti_corruption_layers": []
}
```

---

## 5.2 `event-modeling.md`

```id="2t32c3"
TASK: Convert lifecycle into event model.

OUTPUT:
{
  "events": [
    {
      "event_name": "",
      "payload": {},
      "versioning_strategy": "",
      "idempotency_key": ""
    }
  ],
  "ordering_rules": [],
  "replay_strategy": ""
}
```

---

## 5.3 `api-contract.md`

```id="c9a1u1"
TASK: Generate OpenAPI contract.

OUTPUT:
{
  "openapi_spec": {}
}
```

---

## 5.4 `saga-design.md`

```id="d7b0qg"
TASK: Define saga orchestration.

OUTPUT:
{
  "saga_name": "",
  "steps": [],
  "compensation_actions": [],
  "failure_modes": []
}
```

---

# 6. Data Skills (Critical for Your ODS + CQRS)

## 6.1 `canonical-model.md`

```id="ljq9b2"
TASK: Define canonical data model.

OUTPUT:
{
  "entities": [],
  "relationships": [],
  "versioning": ""
}
```

---

## 6.2 `ods-design.md`

```id="p8m4fj"
TASK: Design operational data store.

OUTPUT:
{
  "tables": [],
  "indexes": [],
  "partitioning": [],
  "event_store": {}
}
```

---

## 6.3 `cqrs-read-model.md`

```id="n6k9xu"
TASK: Define read models.

OUTPUT:
{
  "views": [],
  "materialization_strategy": "",
  "latency_targets": ""
}
```

---

# 7. Quality Skills

## 7.1 `cucumber-tests.md`

```id="x0n4kp"
TASK: Generate BDD scenarios.

OUTPUT:
{
  "features": [
    {
      "name": "",
      "scenarios": []
    }
  ]
}
```

---

## 7.2 `edge-case-generator.md`

```id="0r8n8v"
TASK: Identify edge cases.

OUTPUT:
{
  "edge_cases": [],
  "failure_scenarios": []
}
```

---

## 7.3 `reconciliation-checks.md`

```id="t9w3lf"
TASK: Define reconciliation logic.

OUTPUT:
{
  "checks": [],
  "tolerances": [],
  "break_conditions": []
}
```

---

# 8. Ops Skills (Where Most Systems Fail)

## 8.1 `idempotency-strategy.md`

```id="6h4wzt"
TASK: Define idempotency approach.

OUTPUT:
{
  "idempotency_keys": [],
  "dedup_strategy": "",
  "conflict_resolution": ""
}
```

---

## 8.2 `replay-strategy.md`

```id="9p4u5g"
TASK: Define replay logic.

OUTPUT:
{
  "replay_scope": "",
  "version_override": "",
  "downstream_impact": []
}
```

---

## 8.3 `observability.md`

```id="g9j6bb"
TASK: Define observability.

OUTPUT:
{
  "metrics": [],
  "logs": [],
  "alerts": []
}
```

---

# 9. Skill Composition (Your Real Power)

### Example Pipeline (Force Reset)

```id="8j92nn"
1. extract-flow
2. lifecycle-normalizer
3. event-modeling
4. service-decomposition
5. ods-design
6. cucumber-tests
7. replay-strategy
```

Each output feeds the next → **end-to-end transformation**

---

# 10. CLI Wrapper (Recommended)

```bash id="wj5hij"
./run-skill.sh extract-flow TradeResetService.java
./run-skill.sh event-modeling reset-flow.json
```

---

# 11. Guardrails You MUST Enforce

Add to every skill:

```id="nkl4zt"
- Do not hallucinate missing logic
- Mark UNKNOWN explicitly
- Follow JSON schema strictly
- No narrative outside output
```

---

# 12. What This Gives You

* Repeatable modernization pipeline
* Deterministic outputs for architecture decisions
* CDM-aligned transformation
* Event-sourced design consistency
* Faster decomposition of legacy monolith

---
================
Good—this is where the skill library becomes concrete. I’ll run a **Force Reset (trade/basket level)** flow through the full pipeline, using realistic assumptions aligned to your system (event-driven, CDM-aligned, lot-based, replay-capable). Where your exact implementation may differ, I’ll explicitly mark **UNKNOWN**.

---

# 0. INPUT (What we assume from your system)

```json
{
  "use_case": "Force Reset",
  "trigger": "Manual or scheduled reset of swap position",
  "scope": "Trade | Basket | Lot",
  "expected_outcome": "Realize P&L, reset cost basis, continue lifecycle"
}
```

---

# 1. extract-flow

```json
{
  "entry_points": [
    "ResetController.resetTrade()",
    "BatchResetJob.execute()",
    "ResetMessageListener.onMessage()"
  ],
  "call_chain": [
    "ResetController -> ResetService",
    "ResetService -> PositionService",
    "ResetService -> PricingService (MTM fetch)",
    "ResetService -> CashflowService",
    "ResetService -> TradeRepository",
    "ResetService -> EventPublisher"
  ],
  "domain_objects": [
    "Trade",
    "EquityLeg",
    "InterestLeg",
    "PositionLot",
    "ResetEvent",
    "Cashflow"
  ],
  "external_interactions": {
    "db": [
      "trade_table",
      "position_lot_table",
      "cashflow_table"
    ],
    "messaging": [
      "swap.reset.events"
    ],
    "files": []
  },
  "side_effects": [
    "Update lot cost basis",
    "Insert realized cashflows",
    "Persist reset event",
    "Publish downstream event"
  ],
  "sequence_flow": [
    "Receive reset request",
    "Fetch trade + positions",
    "Fetch latest MTM price",
    "Calculate P&L per lot",
    "Generate cashflows",
    "Reset cost basis",
    "Persist changes",
    "Publish reset event"
  ]
}
```

---

# 2. lifecycle-normalizer

```json
{
  "normalized_events": [
    {
      "event_type": "RESET_REQUESTED",
      "trigger": "User or scheduler",
      "pre_conditions": [
        "Trade is ACTIVE"
      ],
      "post_conditions": [
        "Reset process initiated"
      ]
    },
    {
      "event_type": "RESET_CALCULATED",
      "trigger": "MTM + position evaluation",
      "pre_conditions": [
        "Positions available",
        "Pricing available"
      ],
      "post_conditions": [
        "P&L computed"
      ]
    },
    {
      "event_type": "RESET_APPLIED",
      "trigger": "State mutation",
      "pre_conditions": [
        "P&L computed"
      ],
      "post_conditions": [
        "Cost basis reset",
        "Cashflows created"
      ]
    },
    {
      "event_type": "RESET_CONFIRMED",
      "trigger": "Persistence + publish",
      "pre_conditions": [
        "DB commit successful"
      ],
      "post_conditions": [
        "Event visible downstream"
      ]
    }
  ]
}
```

---

# 3. event-modeling

```json
{
  "events": [
    {
      "event_name": "SwapResetRequested",
      "payload": {
        "trade_id": "string",
        "scope": "TRADE|BASKET|LOT",
        "effective_date": "date"
      },
      "versioning_strategy": "append-only",
      "idempotency_key": "trade_id + effective_date"
    },
    {
      "event_name": "SwapResetCalculated",
      "payload": {
        "trade_id": "string",
        "lot_results": [
          {
            "lot_id": "string",
            "realized_pnl": "number",
            "mtm_price": "number"
          }
        ]
      },
      "versioning_strategy": "recomputable",
      "idempotency_key": "trade_id + calc_hash"
    },
    {
      "event_name": "SwapResetApplied",
      "payload": {
        "trade_id": "string",
        "new_cost_basis": "number",
        "cashflows": []
      },
      "versioning_strategy": "immutable",
      "idempotency_key": "trade_id + version"
    }
  ],
  "ordering_rules": [
    "SwapResetRequested -> SwapResetCalculated -> SwapResetApplied"
  ],
  "replay_strategy": "Selective replay by event version; override ResetCalculated without re-triggering downstream if ResetApplied unchanged"
}
```

---

# 4. service-decomposition

```json
{
  "services": [
    {
      "name": "reset-service",
      "responsibility": "Orchestrate reset lifecycle",
      "owned_entities": ["reset_event"],
      "apis": ["POST /resets"],
      "events_produced": [
        "SwapResetRequested"
      ],
      "events_consumed": []
    },
    {
      "name": "valuation-service",
      "responsibility": "Provide MTM pricing",
      "owned_entities": [],
      "apis": ["GET /mtm"],
      "events_produced": [],
      "events_consumed": ["SwapResetRequested"]
    },
    {
      "name": "position-service",
      "responsibility": "Manage lot-level positions",
      "owned_entities": ["position_lot"],
      "apis": [],
      "events_produced": [
        "SwapResetCalculated"
      ],
      "events_consumed": ["SwapResetRequested"]
    },
    {
      "name": "cashflow-service",
      "responsibility": "Generate realized cashflows",
      "owned_entities": ["cashflow"],
      "apis": [],
      "events_produced": [
        "SwapResetApplied"
      ],
      "events_consumed": ["SwapResetCalculated"]
    }
  ],
  "anti_corruption_layers": [
    "Legacy ResetService adapter"
  ]
}
```

---

# 5. ods-design

```json
{
  "tables": [
    "swap_event_store",
    "position_lot",
    "cashflow",
    "reset_audit"
  ],
  "indexes": [
    "trade_id + event_version",
    "lot_id",
    "business_date"
  ],
  "partitioning": [
    "monthly on business_date"
  ],
  "event_store": {
    "model": "append-only",
    "versioning": "per trade stream",
    "replay_support": true
  }
}
```

---

# 6. cqrs-read-model

```json
{
  "views": [
    "v_trade_position",
    "v_trade_pnl",
    "v_reset_history"
  ],
  "materialization_strategy": "event-driven projection",
  "latency_targets": "sub-5 seconds"
}
```

---

# 7. cucumber-tests

```json
{
  "features": [
    {
      "name": "Force Reset",
      "scenarios": [
        "Given an active swap with open lots When a reset is triggered Then P&L is realized and cost basis reset",
        "Given missing MTM price When reset triggered Then process fails with error",
        "Given partial lot reset When reset applied Then only selected lots updated"
      ]
    }
  ]
}
```

---

# 8. replay-strategy

```json
{
  "replay_scope": "trade-level selective",
  "version_override": "Allow override of SwapResetCalculated event",
  "downstream_impact": [
    "Do not re-trigger cashflow if SwapResetApplied unchanged",
    "Recompute read models only"
  ]
}
```

---

# 9. idempotency-strategy

```json
{
  "idempotency_keys": [
    "trade_id + effective_date"
  ],
  "dedup_strategy": "event store uniqueness constraint",
  "conflict_resolution": "reject duplicate reset requests"
}
```

---

# 10. observability

```json
{
  "metrics": [
    "reset_processing_time",
    "reset_failure_rate",
    "event_lag"
  ],
  "logs": [
    "reset_request_log",
    "pnl_calculation_log"
  ],
  "alerts": [
    "reset SLA breach > 15s",
    "missing MTM price"
  ]
}
```

---

# What This Actually Demonstrates

This is not just documentation—you now have:

### 1. A canonical event model for reset

### 2. Service boundaries aligned to your architecture

### 3. ODS + CQRS design tied to events

### 4. Replay + idempotency baked in

### 5. Test scenarios derived from lifecycle

---

# Where You Should Challenge This (Important)

Before implementing, scrutinize:

* Should **ResetCalculated** be persisted or derived?
* Do you want **cashflow generation coupled or separate saga?**
* How do you handle **interest leg vs equity leg reset misalignment?**
* Do you need **lot-level event streams instead of trade-level?**

---
============================
Partial unwind is materially more complex than reset because you’re **closing specific lots (fully or partially), reallocating remaining exposure, and generating realized + residual state**—all while preserving event ordering and replay semantics. I’ll run it through the same pipeline with strict, structured outputs.

---

# 0. INPUT

```json
{
  "use_case": "Partial Unwind",
  "trigger": "User initiates unwind of portion of swap",
  "scope": "Trade | Basket | Lot",
  "parameters": {
    "unwind_quantity": "number",
    "selection_method": "FIFO|LIFO|SpecificLot",
    "effective_date": "date"
  },
  "expected_outcome": "Reduce exposure, realize P&L, adjust remaining lots"
}
```

---

# 1. extract-flow

```json
{
  "entry_points": [
    "UnwindController.unwindTrade()",
    "UnwindService.executePartialUnwind()",
    "UnwindMessageListener.onMessage()"
  ],
  "call_chain": [
    "Controller -> UnwindService",
    "UnwindService -> PositionService",
    "UnwindService -> AllocationEngine",
    "UnwindService -> PricingService",
    "UnwindService -> CashflowService",
    "UnwindService -> TradeRepository",
    "UnwindService -> EventPublisher"
  ],
  "domain_objects": [
    "Trade",
    "PositionLot",
    "UnwindInstruction",
    "AllocationResult",
    "Cashflow"
  ],
  "external_interactions": {
    "db": [
      "position_lot",
      "cashflow",
      "trade"
    ],
    "messaging": [
      "swap.unwind.events"
    ],
    "files": []
  },
  "side_effects": [
    "Close or reduce lots",
    "Create new residual lots",
    "Generate realized P&L cashflows",
    "Update trade notional",
    "Publish unwind events"
  ],
  "sequence_flow": [
    "Receive unwind request",
    "Fetch eligible lots",
    "Run allocation (FIFO/LIFO/etc)",
    "Fetch MTM prices",
    "Compute realized P&L",
    "Split lots (closed vs residual)",
    "Persist changes",
    "Emit unwind events"
  ]
}
```

---

# 2. lifecycle-normalizer

```json
{
  "normalized_events": [
    {
      "event_type": "UNWIND_REQUESTED",
      "trigger": "User/API",
      "pre_conditions": ["Trade ACTIVE", "Sufficient quantity"],
      "post_conditions": ["Unwind initiated"]
    },
    {
      "event_type": "UNWIND_ALLOCATED",
      "trigger": "Allocation engine",
      "pre_conditions": ["Lots available"],
      "post_conditions": ["Lots selected for unwind"]
    },
    {
      "event_type": "UNWIND_CALCULATED",
      "trigger": "Pricing + P&L calc",
      "pre_conditions": ["Allocation complete"],
      "post_conditions": ["Realized P&L computed"]
    },
    {
      "event_type": "UNWIND_APPLIED",
      "trigger": "State mutation",
      "pre_conditions": ["P&L computed"],
      "post_conditions": [
        "Lots closed/reduced",
        "Residual lots created",
        "Cashflows generated"
      ]
    }
  ]
}
```

---

# 3. event-modeling

```json
{
  "events": [
    {
      "event_name": "SwapUnwindRequested",
      "payload": {
        "trade_id": "string",
        "quantity": "number",
        "selection_method": "FIFO|LIFO|SpecificLot",
        "effective_date": "date"
      },
      "versioning_strategy": "append-only",
      "idempotency_key": "trade_id + effective_date + quantity"
    },
    {
      "event_name": "SwapUnwindAllocated",
      "payload": {
        "trade_id": "string",
        "allocations": [
          {
            "lot_id": "string",
            "unwind_qty": "number"
          }
        ]
      },
      "versioning_strategy": "recomputable",
      "idempotency_key": "trade_id + allocation_hash"
    },
    {
      "event_name": "SwapUnwindCalculated",
      "payload": {
        "trade_id": "string",
        "results": [
          {
            "lot_id": "string",
            "unwind_qty": "number",
            "realized_pnl": "number",
            "mtm_price": "number"
          }
        ]
      },
      "versioning_strategy": "recomputable",
      "idempotency_key": "trade_id + calc_hash"
    },
    {
      "event_name": "SwapUnwindApplied",
      "payload": {
        "trade_id": "string",
        "closed_lots": [],
        "residual_lots": [],
        "cashflows": []
      },
      "versioning_strategy": "immutable",
      "idempotency_key": "trade_id + version"
    }
  ],
  "ordering_rules": [
    "Requested -> Allocated -> Calculated -> Applied"
  ],
  "replay_strategy": "Allow override of Allocated/Calculated; Applied is terminal unless compensating unwind issued"
}
```

---

# 4. service-decomposition

```json
{
  "services": [
    {
      "name": "unwind-service",
      "responsibility": "Initiate unwind",
      "owned_entities": ["unwind_request"],
      "apis": ["POST /unwinds"],
      "events_produced": ["SwapUnwindRequested"],
      "events_consumed": []
    },
    {
      "name": "allocation-service",
      "responsibility": "Lot selection (FIFO/LIFO)",
      "owned_entities": [],
      "events_produced": ["SwapUnwindAllocated"],
      "events_consumed": ["SwapUnwindRequested"]
    },
    {
      "name": "valuation-service",
      "responsibility": "MTM pricing",
      "events_consumed": ["SwapUnwindAllocated"],
      "events_produced": []
    },
    {
      "name": "position-service",
      "responsibility": "Lot mutation",
      "owned_entities": ["position_lot"],
      "events_consumed": ["SwapUnwindCalculated"],
      "events_produced": ["SwapUnwindApplied"]
    },
    {
      "name": "cashflow-service",
      "responsibility": "P&L realization",
      "owned_entities": ["cashflow"],
      "events_consumed": ["SwapUnwindCalculated"],
      "events_produced": []
    }
  ]
}
```

---

# 5. ods-design

```json
{
  "tables": [
    "swap_event_store",
    "position_lot",
    "cashflow",
    "unwind_audit"
  ],
  "indexes": [
    "trade_id",
    "lot_id",
    "event_version"
  ],
  "partitioning": ["monthly by business_date"],
  "event_store": {
    "append_only": true,
    "stream_per_trade": true
  }
}
```

---

# 6. cqrs-read-model

```json
{
  "views": [
    "v_trade_position",
    "v_unwind_history",
    "v_realized_pnl"
  ],
  "materialization_strategy": "event-driven projections",
  "latency_targets": "sub-5 seconds"
}
```

---

# 7. cucumber-tests

```json
{
  "features": [
    {
      "name": "Partial Unwind",
      "scenarios": [
        "Given a trade with 3 lots When unwind 50% FIFO Then earliest lots reduced first",
        "Given insufficient quantity When unwind requested Then error returned",
        "Given specific lot unwind When executed Then only selected lot reduced",
        "Given partial lot unwind Then residual lot created with correct cost basis"
      ]
    }
  ]
}
```

---

# 8. replay-strategy

```json
{
  "replay_scope": "trade-level",
  "version_override": "Allow recalculation before Applied",
  "downstream_impact": [
    "Rebuild read models",
    "Do not duplicate cashflows if Applied unchanged"
  ]
}
```

---

# 9. idempotency-strategy

```json
{
  "idempotency_keys": [
    "trade_id + effective_date + quantity"
  ],
  "dedup_strategy": "event uniqueness constraint",
  "conflict_resolution": "reject duplicate unwind request"
}
```

---

# 10. observability

```json
{
  "metrics": [
    "unwind_latency",
    "allocation_time",
    "pnl_calc_time"
  ],
  "logs": [
    "allocation_log",
    "lot_split_log"
  ],
  "alerts": [
    "unwind failure",
    "allocation mismatch"
  ]
}
```

---

# Critical Architectural Observations (You Should Not Skip)

### 1. Allocation is a First-Class Service

This is where most systems break:

* deterministic ordering (book + stock id)
* reproducibility for replay
* auditability

---

### 2. Lot Splitting is Non-Trivial

You must handle:

* full close vs partial close
* residual lot creation
* cost basis recalculation

---

### 3. Event Granularity Decision

You need to decide:

**Option A (current above):**

* trade-level events with lot arrays

**Option B (more scalable):**

* **lot-level event streams**

Given your scale → **Option B is superior**

---

### 4. Replay Complexity

Partial unwind replay is harder than reset because:

* allocation must be identical
* lot ordering must be stable
* rounding issues propagate

---
========================================

