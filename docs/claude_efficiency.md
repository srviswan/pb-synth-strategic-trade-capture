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
