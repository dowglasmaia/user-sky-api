# user-api

Enterprise-ready REST API for user management and external project association,
built with **Java 21 + Spring Boot 3.3**.

## Architecture

```
┌──────────────┐  HTTP/JWT  ┌────────────────────────┐
│    Client    │──────────►│       user-api :8080    │
└──────────────┘            │  Spring Boot 3 / JPA    │
                            └───────────┬─────────────┘
                                        │ WebClient + Resilience4j
                                        │ X-Correlation-Id header
                                        ▼
                            ┌────────────────────────┐
                            │    projects-api :8081   │
                            │  (external catalog)     │
                            └────────────────────────┘
                                        │
                            ┌────────────────────────┐
                            │   PostgreSQL :5432      │
                            └────────────────────────┘
```

**Key design decisions:**

- Projects live in `projects-api` — user-api never owns project data.
- On link creation, user-api stores a **snapshot** (name + description) locally so
  `GET /users/{id}/projects` never needs a network call.
- All IDs are **UUID** — safe for public exposure, distribution-ready.
- Optimistic locking (`@Version`) on User and UserProjectLink prevents silent overwrites.

---

## Quick start

```bash
# Build and start the full stack (API + database + full observability)
docker compose up -d --build

# Health checks
curl http://localhost:8080/api/v1/actuator/health
curl http://localhost:8081/api/v1/actuator/health
```

### Default credentials

| Service  | URL                       | Login           |
|----------|---------------------------|-----------------|
| API      | http://localhost:8080     | see table below |
| Grafana  | http://localhost:3000     | admin / admin   |
| Prometheus | http://localhost:9090   | —               |
| Jaeger   | http://localhost:16686    | —               |

| Email                | Password   | Role       |
|----------------------|------------|------------|
| admin@userapi.com    | Admin@1234 | ROLE_ADMIN |

---

## API overview

### Authentication

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@userapi.com","password":"Admin@1234"}'
# → {"token":"<JWT>"}
```

Use the token in subsequent requests:
```
Authorization: Bearer <JWT>
```

### Users

| Method | Path            | Auth       | Description               |
|--------|-----------------|------------|---------------------------|
| GET    | /users          | Required   | List users (paginated)    |
| POST   | /users          | Public     | Register user             |
| GET    | /users/{id}     | Required   | Get user by UUID          |
| PUT    | /users/{id}     | Required   | Update user               |
| DELETE | /users/{id}     | ADMIN only | Delete user               |

```bash
# List users (page 0, 20 per page, sorted by name)
curl -H 'Authorization: Bearer <JWT>' \
  'http://localhost:8080/api/v1/users?page=0&size=20'
```

### Project links

| Method | Path                              | Auth     | Description                  |
|--------|-----------------------------------|----------|------------------------------|
| GET    | /users/{id}/projects              | Required | User + linked projects       |
| POST   | /users/{userId}/projects/{projId} | Required | Link project to user         |
| DELETE | /users/{userId}/projects/{projId} | Required | Unlink project from user     |

```bash
# Get user with their projects
curl -H 'Authorization: Bearer <JWT>' \
  http://localhost:8080/api/v1/users/<uuid>/projects
# → {"user":"Alice","projects":[{"id":"...","name":"Atlas","description":"..."}]}
```

#### Link flow

```
POST /api/v1/users/{userId}/projects/{projectId}

1. Validate user exists            → 404 if not
2. Resolve project via projects-api → 404 / 502 if unavailable
3. Check for duplicate link        → 409 if already linked
4. Persist link + snapshot
```

Optional body:
```json
{ "description": "custom note" }
```

---

## Projects catalog (projects-api)

```bash
# List all 10 available projects
curl http://localhost:8081/api/v1/projects

# Get one project by UUID
curl http://localhost:8081/api/v1/projects/11111111-1111-1111-1111-111111111111
```

---

## Observability

The full observability stack runs automatically via Docker Compose. No extra setup needed.

### Services at a glance

| Tool       | URL                        | Purpose                       |
|------------|----------------------------|-------------------------------|
| Grafana    | http://localhost:3000      | Dashboards (metrics + logs)   |
| Prometheus | http://localhost:9090      | Metrics scraping & querying   |
| Jaeger     | http://localhost:16686     | Distributed trace explorer    |
| Loki       | http://localhost:3100      | Log storage (via Promtail)    |
| Actuator   | http://localhost:8080/api/v1/actuator | Health, metrics, info |

---

### Metrics

Prometheus scrapes `/api/v1/actuator/prometheus` every 15 s.

```bash
# Raw metrics endpoint
curl http://localhost:8080/api/v1/actuator/prometheus

# Example: current request rate
curl -s http://localhost:9090/api/v1/query \
  --data-urlencode 'query=sum(rate(http_server_requests_seconds_count{job="user-api"}[5m]))'
```

**Key metrics exposed:**

| Metric | Description |
|--------|-------------|
| `http_server_requests_seconds_*` | HTTP request count, latency histogram |
| `jvm_memory_used_bytes` | JVM heap/non-heap usage |
| `jvm_threads_live_threads` | Active thread count |
| `process_cpu_usage` | Process CPU % |
| `hikaricp_connections_active` | Active DB connections |
| `resilience4j_circuitbreaker_state` | Circuit breaker state |
| `user_project_link_created_total` | Business metric: links created |
| `external_project_lookup_duration_seconds` | Gateway latency |

---

### Distributed Tracing (Jaeger)

Every HTTP request generates a trace exported to Jaeger via OpenTelemetry OTLP.

```
user-api  →  OTLP HTTP (port 4318)  →  Jaeger
```

**How to trace a request:**

1. Make any API call with a custom correlation ID:
```bash
curl -H 'X-Correlation-Id: debug-001' \
     -H 'Authorization: Bearer <JWT>' \
     http://localhost:8080/api/v1/users
```

2. Open **Jaeger UI** → http://localhost:16686
3. Select service `user-api` → click **Find Traces**
4. Click any trace to see spans, timing, and metadata

The `traceId` and `spanId` appear in every log line, making it easy to correlate
logs in Grafana with traces in Jaeger.

---

### Logs (Loki + Promtail)

Promtail reads container logs via the Docker socket and ships them to Loki.
JSON-structured logs (`SPRING_PROFILES_ACTIVE=docker`) allow field-level filtering.

**Every log line contains:**

```json
{
  "@timestamp": "2025-01-01T10:00:00Z",
  "level": "INFO",
  "logger_name": "c.l.u.service.UserService",
  "message": "create_user_completed id=... email=...",
  "traceId": "4bf92f3577b34da6",
  "spanId": "00f067aa0ba902b7",
  "correlationId": "debug-001"
}
```

**Query logs in Grafana:**

1. Open Grafana → http://localhost:3000
2. Go to **Explore** → select **Loki** datasource
3. Run LogQL queries:

```logql
# All user-api logs
{service="user-api"}

# Only errors
{service="user-api"} | json | level="ERROR"

# Trace a specific request end-to-end
{service="user-api"} | json | traceId="<traceId>"

# Logs for a specific correlation ID
{service="user-api"} | json | correlationId="debug-001"

# Filter by operation
{service="user-api"} |= "link_project_created"
```

---

### Grafana Dashboard

A pre-provisioned dashboard is loaded automatically on first start.

**Access:** http://localhost:3000 → Dashboards → **User API — Observability**

Panels included:

| Panel | What it shows |
|-------|---------------|
| Request Rate | Requests/s (all status codes) |
| Error Rate (5xx) | Server errors per second |
| P99 Latency | 99th percentile response time |
| JVM Heap Used | Current heap memory |
| HTTP Request Rate by Status | Rate split by 2xx/4xx/5xx |
| HTTP Latency P50/P95/P99 | Percentile curves over time |
| JVM Heap Memory | Used vs max over time |
| CPU Usage | Process + system CPU |
| JVM Threads | Live + daemon thread count |
| HikariCP Connections | Active / idle / max pool |
| Application Logs | Live Loki log stream |

---

### Correlation across tools

Every request carries:
- `traceId` — generated by Micrometer Tracing (OTel-compatible)
- `spanId` — current span within the trace
- `correlationId` — from `X-Correlation-Id` header (generated if absent)

All three appear in logs, traces, and error responses:

```
2025-01-01 10:00:00 [main] INFO  UserService traceId=4bf92f3577b34da6 spanId=00f067aa0ba902b7 correlationId=debug-001 - link_project_created userId=... projectId=...
```

**Workflow for troubleshooting:**
1. Capture the `traceId` from an error log or response header
2. Search Jaeger for that trace → see full request timeline
3. Search Loki with `| json | traceId="<id>"` → see every log line in that trace

---

### Circuit breaker (Resilience4j)

projects-api calls are protected by Resilience4j:
- **3 retries** with 500 ms wait on integration errors
- **Circuit opens** after 50% failures in a 10-call window
- **Half-open** after 30 s, 3 probe calls to decide recovery

```bash
# Check circuit breaker state
curl http://localhost:8080/api/v1/actuator/health | jq .components.circuitBreakers
```

---

## Running locally (without Docker)

```bash
# Start only infrastructure (postgres + projects-api)
docker compose up -d postgres projects-api

# Run user-api locally (human-readable logs, no JSON)
mvn spring-boot:run
```

To start the observability stack separately (without rebuilding user-api):
```bash
docker compose up -d jaeger prometheus loki promtail grafana
```

---

## Environment variables

| Variable                          | Default                                    | Description                    |
|-----------------------------------|--------------------------------------------|--------------------------------|
| `DB_URL`                          | `jdbc:postgresql://localhost:5432/userapi` | JDBC URL                       |
| `DB_USERNAME`                     | `postgres`                                 | DB user                        |
| `DB_PASSWORD`                     | `postgres`                                 | DB password                    |
| `JWT_SECRET`                      | *(insecure default)*                       | **Change in production**       |
| `JWT_EXPIRATION_MS`               | `86400000` (24 h)                          | Token TTL                      |
| `PROJECTS_API_BASE_URL`           | `http://localhost:8081/api/v1`             | projects-api URL               |
| `OTEL_EXPORTER_OTLP_ENDPOINT`     | `http://localhost:4318/v1/traces`          | Jaeger OTLP endpoint           |
| `SERVER_PORT`                     | `8080`                                     | HTTP port                      |
| `SPRING_PROFILES_ACTIVE`          | *(none)*                                   | Set `docker` for JSON logging  |

---

## Development

```bash
mvn test                          # run tests
mvn -ntp package -DskipTests      # build fat jar
```

Swagger UI: http://localhost:8080/api/v1/swagger-ui/index.html
