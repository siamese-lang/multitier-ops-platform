# ops-sample-service

`ops-sample-service` is a small Spring Boot workload for the `lab-full-min` WEB/WAS/DB operations lab.

This repository is an operations portfolio, not an application development portfolio. The service is intentionally small, but it is not an empty health-check app. It exists to create deterministic operating evidence for a VM-based multi-tier system.

The fixed project theme is:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

The app must therefore remain a workload used by the operating environment. It must not become the center of the project.

## Role in the project

OpenKoda remains Phase 0 smoke-test evidence for running a third-party workload. This service is added for Phase 1 because it gives the lab controllable WEB/WAS/DB behavior that can be used to verify incidents, logs, and recovery procedures.

It provides deterministic evidence for:

- Nginx upstream routing
- WAS process health
- DB-dependent readiness
- app node identity
- HTTP request logging
- DB read/write traffic
- DB-backed state changes
- later `app-01` failure and `app-02` continuation drills
- later PostgreSQL failure and recovery drills

## Runtime role

In the target architecture, this service runs on:

```text
app-01
app-02
```

Nginx routes traffic to both app nodes on port `8080`. PostgreSQL runs on `db-primary-01`.

The evidence flow is:

```text
HTTP request -> Nginx -> app-01/app-02 -> PostgreSQL
```

## Data model

The service creates one table on the first DB-backed request:

```text
ops_work_orders
```

It inserts seed records when the table is empty. These records are deliberately tied to operating scenarios:

- Nginx upstream validation
- DB readiness validation
- app node identity evidence
- app-01 failure drill
- PostgreSQL restart/recovery drill

This gives the lab real data for read/write/state-change tests without turning the repository into a business application project.

## Build

```bash
cd apps/ops-sample-service
mvn clean package
```

The jar is created under:

```text
target/ops-sample-service-0.1.0.jar
```

## Run without DB

The service can start without DB environment variables. This is intentional because DB unavailability should be visible through readiness checks, not as an app startup failure.

```bash
java -jar target/ops-sample-service-0.1.0.jar
```

Expected behavior:

```text
GET /healthz                 -> 200
GET /node                    -> 200
GET /readyz                  -> 503 when DB env is missing
GET /db/time                 -> 503 when DB env is missing
GET /api/work-orders         -> 503 when DB env is missing
GET /api/work-orders/summary -> 503 when DB env is missing
```

## Run with DB

```bash
export OPS_DB_URL='jdbc:postgresql://<db-primary-private-ip>:5432/opsdb'
export OPS_DB_USERNAME='ops_user'
export OPS_DB_PASSWORD='<password>'
export OPS_NODE_ROLE='app'
export OPS_NODE_TIER='private-was'
export OPS_ENVIRONMENT='lab-full-min'
export APP_VERSION='0.1.0'

java -jar target/ops-sample-service-0.1.0.jar
```

The first DB-backed request creates the `ops_work_orders` table if it does not exist and inserts sample data when the table is empty. This keeps app startup independent from DB availability while still giving the lab real data to operate.

## Endpoints

### `GET /healthz`

Process-level health check. It does not require DB connectivity.

```bash
curl -s http://localhost:8080/healthz
```

### `GET /readyz`

Readiness check. It checks PostgreSQL connectivity with a simple query.

Expected behavior:

- returns `200` when the DB connection works
- returns `503` when DB configuration is missing or DB is unavailable

```bash
curl -i http://localhost:8080/readyz
```

### `GET /node`

Node identity endpoint. It returns hostname, local address, role, tier, environment, and app version.

```bash
curl -s http://localhost:8080/node
```

This endpoint is used to prove whether Nginx routed a request to `app-01` or `app-02`.

### `GET /db/time`

Runs `select now()` against PostgreSQL and returns the DB time.

```bash
curl -i http://localhost:8080/db/time
```

### `GET /api/work-orders`

Lists DB-backed work orders. The first successful call creates the table and seed data when needed.

```bash
curl -s http://localhost:8080/api/work-orders
curl -s 'http://localhost:8080/api/work-orders?status=OPEN'
```

### `GET /api/work-orders/{id}`

Reads one work order.

```bash
curl -s http://localhost:8080/api/work-orders/1
```

### `POST /api/work-orders`

Creates a work order and stores it in PostgreSQL.

```bash
curl -s -X POST http://localhost:8080/api/work-orders \
  -H 'Content-Type: application/json' \
  -d '{"title":"Validate app-02 after app-01 stop","priority":"HIGH","assignee":"ops-admin","description":"Created during an incident drill."}'
```

### `PATCH /api/work-orders/{id}/status`

Updates work order status.

Allowed statuses:

```text
OPEN
IN_PROGRESS
DONE
CANCELLED
```

```bash
curl -s -X PATCH http://localhost:8080/api/work-orders/1/status \
  -H 'Content-Type: application/json' \
  -d '{"status":"DONE"}'
```

### `GET /api/work-orders/summary`

Returns count by status.

```bash
curl -s http://localhost:8080/api/work-orders/summary
```

## Response evidence

Every response includes node identity so the operator can prove which app node handled the request.

Example fields:

```text
node.hostname
node.localAddress
node.role
node.tier
node.environment
node.version
```

DB-backed work-order responses also include:

```text
operation
durationMs
```

These fields are intentionally simple. They are evidence aids for Nginx upstream, app failover, and DB latency observations.

## Request log evidence

Every HTTP request is logged with fields suitable for incident evidence:

```text
event=http_request requestId=<id> method=<method> path=<path> status=<status> durationMs=<ms> remoteAddr=<ip> node=<hostname> role=<role> tier=<tier>
```

The service also returns an `X-Request-Id` response header. A caller can provide `X-Request-Id` to correlate curl output, Nginx access logs, and app logs.

Example:

```bash
curl -i -H 'X-Request-Id: incident-app-01-001' http://<nginx-public-ip>/api/work-orders/summary
```

## Operational evidence examples

### Nginx upstream routing

```bash
for i in {1..10}; do curl -s http://<nginx-public-ip>/node; echo; done
```

Expected evidence:

- responses come from app nodes behind Nginx
- hostname/localAddress changes between app instances when both are healthy
- Nginx access log and app request log share request timing evidence

### DB-backed data flow

```bash
curl -s http://<nginx-public-ip>/api/work-orders/summary
curl -s http://<nginx-public-ip>/api/work-orders
```

Expected evidence:

- app reaches PostgreSQL
- seeded records are returned
- the same records are visible regardless of which app node served the request

### App node failure drill

```bash
# stop app service on app-01 later with Ansible/systemd
for i in {1..10}; do curl -s http://<nginx-public-ip>/node; echo; done
curl -s http://<nginx-public-ip>/api/work-orders/summary
```

Expected evidence:

- Nginx continues routing to the remaining healthy app node
- DB-backed records remain available
- app logs show only the surviving node receiving requests after failover

### DB failure drill

```bash
# stop PostgreSQL later with Ansible/systemd
curl -i http://<nginx-public-ip>/healthz
curl -i http://<nginx-public-ip>/readyz
curl -i http://<nginx-public-ip>/api/work-orders
```

Expected evidence:

- `/healthz` can still return `200`
- `/readyz` returns `503`
- DB-backed API returns `503`
- app request logs show DB-backed endpoints failing while process health remains up
- after PostgreSQL recovery, readiness and data APIs return to normal

## Environment variables

| Variable | Required | Purpose |
| --- | --- | --- |
| `SERVER_PORT` | No | HTTP port. Defaults to `8080`. |
| `OPS_DB_URL` | For DB checks and work orders | PostgreSQL JDBC URL. |
| `OPS_DB_USERNAME` | For DB checks and work orders | PostgreSQL username. |
| `OPS_DB_PASSWORD` | For DB checks and work orders | PostgreSQL password. |
| `OPS_NODE_ROLE` | No | Logical role. Defaults to `app`. |
| `OPS_NODE_TIER` | No | Logical tier. Defaults to `private-was`. |
| `OPS_ENVIRONMENT` | No | Environment name. Defaults to `lab-full-min`. |
| `APP_VERSION` | No | Version string returned by responses. |

## Out of scope

This app does not include:

- UI development
- authentication
- OpenKoda customization
- complex business features
- file storage
- PostgreSQL replication
- backup and restore automation
- deployment automation
- Nginx configuration
- incident drill automation

Those belong to later infrastructure and operations issues.
