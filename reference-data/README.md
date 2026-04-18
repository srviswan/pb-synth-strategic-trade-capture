# Reference Data — service and client SDK

Maven multi-module build:

| Module | Description |
|--------|-------------|
| `reference-data-server` | Spring Boot API on port **8090** by default; **three H2 databases** (security, account, book) with Flyway; optional **scheduled account sync** from an external REST JSON feed. |
| `reference-data-client` | `ReferenceDataClient` facade, Caffeine cache, Resilience4j circuit breaker, Boot auto-configuration. |
| `trade-capture-sample` | Pilot: integration test + `TradeCaptureEnrichmentSample` showing domain wiring. |

## Run the server

```bash
cd reference-data
mvn -pl reference-data-server spring-boot:run
```

## Use the client in a Spring Boot app

```xml
<dependency>
  <groupId>com.pbsynth</groupId>
  <artifactId>reference-data-client</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

```yaml
reference-data:
  client:
    base-url: http://localhost:8090
    cache-enabled: true
    cache-ttl-seconds: 300
```

Inject `ReferenceDataClient`.

## Account sync from an external API (server)

When `refdata.account.sync.enabled` is `true`, the server calls `GET {base-url}{path}` on a cron (default: every hour) and **MERGE**-upserts into the account database.

Example `application.yml` fragment:

```yaml
refdata:
  account:
    sync:
      enabled: true
      cron: "0 0 * * * *"
    source:
      base-url: https://masters.example.com
      path: /v1/accounts/export
      connect-timeout-ms: 5000
      read-timeout-ms: 15000
```

Example JSON body (array):

```json
[
  {
    "account_id": "ACC-001",
    "name": "Pension Fund Alpha",
    "classification": "INSTITUTIONAL",
    "credit_tier": "TIER_1",
    "stp_eligible": true
  }
]
```

Provide a custom `AccountSourceClient` bean if the feed is not a plain JSON array or needs bespoke auth.

## Build and test

```bash
mvn verify
```

## Contract

OpenAPI: [docs/api/reference-data-service-openapi.yaml](../docs/api/reference-data-service-openapi.yaml)

Architecture: [docs/reference_data_service_design.md](../docs/reference_data_service_design.md)
