# ops-sample-service

`ops-sample-service` is a small Spring Boot workload for the `lab-full-min` WEB/WAS/DB operations lab.

It is not the subject of the portfolio. The service exists to create deterministic operating evidence for:

- Nginx upstream routing
- WAS process health
- DB-dependent readiness
- app node identity
- DB read/write traffic
- DB-backed state changes
- later `app-01` failure and `app-02` continuation drills

## Runtime role

In the target architecture, this service runs on:

```text
app-01
app-02
```

Nginx routes traffic to both app nodes on port `8080`. PostgreSQL runs on `db-primary-01`.

## Why this app exists

The repository is an operations portfolio, not an application development portfolio. A large third-party application can make the project look realistic, but it can also hide the exact operating behavior that must be tested.

This service is intentionally small, but it is not empty. It provides DB-backed work order data so the lab can prove:

```text
HTTP request -> Nginx -> app-01/app-02 -> PostgreSQL
```

The app supports enough data behavior for operational validation:

- initial sample work orders
- read traffic
- write traffic
- status update traffic
- DB readiness failure when PostgreSQL is down
- data continuity when traffic moves from one app node to another

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

## Operational evidence examples

### Nginx upstream routing

```bash
for i in {1..10}; do curl -s http://<nginx-public-ip>/node; echo; done
```

Expected evidence:

- responses come from app nodes behind Nginx
- hostname/localAddress changes between app instances when both are healthy

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

- authentication
- UI screens
- complex business workflow
- OpenKoda customization
- deployment automation
- Nginx configuration
- incident drill automation

Those belong to later infrastructure and operations issues.
