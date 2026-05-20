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
# Build and start all services (postgres + projects-api + user-api)
docker compose up -d --build

# Health checks
curl http://localhost:8080/api/v1/actuator/health
curl http://localhost:8081/api/v1/actuator/health
```
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

| Method | Path            | Auth       | Description         |
|--------|-----------------|------------|---------------------|
| POST   | /users          | Public     | Register user       |
| GET    | /users/{id}     | Required   | Get user by UUID    |
| PUT    | /users/{id}     | Required   | Update user         |
| DELETE | /users/{id}     | ADMIN only | Delete user         |

### Project links

| Method | Path                              | Auth     | Description                  |
|--------|-----------------------------------|----------|------------------------------|
| GET    | /users/{id}/projects              | Required | List user's linked projects  |
| POST   | /users/{userId}/projects/{projId} | Required | Link project to user         |
| DELETE | /users/{userId}/projects/{projId} | Required | Unlink project from user     |

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
Omit to use the project's own description from the catalog.

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

### Correlation

Every request gets a `traceId` (internal) and `correlationId` (propagated across
services via `X-Correlation-Id`). Pass your own:

```bash
curl -H 'X-Correlation-Id: my-request-id' ...
```

Both IDs appear in every log line and in error responses:
```
2025-01-01 10:00:00 [main] INFO UserService traceId=abc123 correlationId=my-request-id - link_project_created userId=... projectId=...
```

### Metrics (Prometheus)

```
GET /api/v1/actuator/prometheus
```

Custom metrics:
- `user_project_link_created_total` — links created
- `external_project_lookup_duration_seconds` — time spent calling projects-api
- `external_project_lookup_failure_total` — upstream failures

### Circuit breaker

projects-api calls are protected by Resilience4j:
- **3 retries** with 500 ms wait on integration errors
- **Circuit opens** after 50% failures in a 10-call window
- **Half-open** after 30 s, 3 probe calls to decide recovery

```bash
GET /api/v1/actuator/health   # shows circuitbreaker state
```

---

## Running locally (without Docker)

```bash
# Start postgres only
docker compose up -d postgres

# Start projects-api (separate terminal)
cd ../projects-api && mvn spring-boot:run

# Start user-api
mvn spring-boot:run
```

---

## Environment variables

| Variable                | Default                                    | Description              |
|-------------------------|--------------------------------------------|--------------------------|
| `DB_URL`                | `jdbc:postgresql://localhost:5432/userapi` | JDBC URL                 |
| `DB_USERNAME`           | `postgres`                                 | DB user                  |
| `DB_PASSWORD`           | `postgres`                                 | DB password              |
| `JWT_SECRET`            | *(insecure default)*                       | **Change in production** |
| `JWT_EXPIRATION_MS`     | `86400000` (24 h)                          | Token TTL                |
| `PROJECTS_API_BASE_URL` | `http://localhost:8081/api/v1`             | projects-api URL         |
| `SERVER_PORT`           | `8080`                                     | HTTP port                |

---

## Development

```bash
mvn test                          # run tests
mvn -ntp package -DskipTests      # build fat jar
```

Swagger UI: http://localhost:8080/api/v1/swagger-ui/index.html
