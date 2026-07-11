# ops-sample-service

`ops-sample-service` is a small Spring Boot workload for the `lab-full-min` WEB/WAS/DB operations lab and the later `lab-full-ops` storage consistency validation path.

This repository is an operations portfolio, not an application development portfolio. The service is intentionally small, but it is not an empty health-check app. It exists to create deterministic operating evidence for a VM-based multi-tier system.

The fixed project theme is:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

The app must therefore remain a workload used by the operating environment. It must not become the center of the project.

## Role in the project

OpenKoda remains Phase 0 smoke-test evidence for running a third-party workload. This service is added because it gives the lab controllable behavior that can be used to verify incidents, logs, storage consistency, and recovery procedures.

It provides deterministic evidence for:

- Nginx upstream routing
- WAS process health
- DB-dependent readiness
- app node identity
- HTTP request logging
- DB read/write traffic
- DB-backed state changes
- DB metadata plus NFS file-object consistency checks in `lab-full-ops`
- later `app-01` failure and `app-02` continuation drills
- later PostgreSQL failure and recovery drills
- later file storage failure, backup, and restore drills

## Runtime role

In the target architecture, this service runs on:

```text
app-01
app-02
```

Nginx routes traffic to app nodes on port `8080`. PostgreSQL runs on `db-primary-01`. In `lab-full-ops`, app nodes can also mount the NFS export from `nfs-01` at `/mnt/ops-sample/files`.

The WEB/WAS/DB evidence flow is:

```text
HTTP request -> Nginx -> app-01/app-02 -> PostgreSQL
```

The storage consistency evidence flow is:

```text
HTTP request -> Nginx -> app-01 -> PostgreSQL metadata
                                  -> NFS-mounted evidence file object
```

## Data model

The service creates one table on the first DB-backed work-order request:

```text
ops_work_orders
```

It inserts seed records when the table is empty. These records are deliberately tied to operating scenarios:

- Nginx upstream validation
- DB readiness validation
- app node identity evidence
- app-01 failure drill
- PostgreSQL restart/recovery drill

For `lab-full-ops`, the service also creates this table when the first evidence-file request succeeds:

```text
ops_work_order_evidence_files
```

That table links work-order records to generated evidence file objects stored under the configured mounted file path. This gives the lab a DB/file consistency target without turning the repository into a product file-management project.

## Build

```bash
cd apps/ops-sample-service
mvn clean package
```

The jar is created under:

```text
target/ops-sample-service-0.1.0.jar
```

If local Maven is unavailable, use the existing GitHub Actions workflow artifact path documented in the deployment runbook instead of treating local Maven as required.

## Run without DB

The service can start without DB environment variables. This is intentional because DB unavailability should be visible through readiness checks, not as an app startup failure.

```bash
java -jar target/ops-sample-service-0.1.0.jar
```

Expected behavior:

```text
GET  /healthz                                             -> 200
GET  /node                                                -> 200
GET  /readyz                                              -> 503 when DB env is missing
GET  /db/time                                             -> 503 when DB env is missing
GET  /api/work-orders                                     -> 503 when DB env is missing
GET  /api/work-orders/summary                             -> 503 when DB env is missing
POST /api/work-orders/{id}/evidence-files                 -> 503 when DB env is missing
GET  /api/work-orders/{id}/evidence-files                 -> 503 when DB env is missing
GET  /api/work-orders/{id}/evidence-files/{id}/consistency -> 503 when DB env is missing
```

## Run with DB and file storage path

```bash
export OPS_DB_URL='jdbc:postgresql://<db-primary-private-ip>:5432/opsdb'
export OPS_DB_USERNAME='ops_user'
export OPS_DB_PASSWORD='<password>'
export OPS_EVIDENCE_FILE_ROOT='/mnt/ops-sample/files'
export OPS_NODE_ROLE='app'
export OPS_NODE_TIER='private-was'
export OPS_ENVIRONMENT='lab-full-ops'
export APP_VERSION='0.1.0'

java -jar target/ops-sample-service-0.1.0.jar
```

The app does not create or mount NFS. It only uses the file path provided by the operating environment. In `lab-full-ops`, that path should match the app-side NFS mount configured by Ansible.

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

This endpoint is used to prove which app instance served the request.

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

### `POST /api/work-orders/{id}/evidence-files`

Creates a small generated evidence file for an existing work order.

```bash
curl -s -X POST http://localhost:8080/api/work-orders/1/evidence-files
```

Expected behavior:

```text
1. Verify the work order exists in PostgreSQL.
2. Write a generated evidence file under OPS_EVIDENCE_FILE_ROOT.
3. Calculate file size and SHA-256.
4. Store file metadata in PostgreSQL.
5. Return metadata and immediate consistency result.
```

This is not a general upload endpoint. The service does not accept arbitrary user file content or file names.

### `GET /api/work-orders/{id}/evidence-files`

Lists evidence-file metadata for a work order.

```bash
curl -s http://localhost:8080/api/work-orders/1/evidence-files
```

### `GET /api/work-orders/{id}/evidence-files/{evidenceId}/consistency`

Compares the PostgreSQL metadata row with the file object on the mounted path.

```bash
curl -s http://localhost:8080/api/work-orders/1/evidence-files/1/consistency
```

The consistency result reports:

```text
fileExists
regularFile
readable
expectedSizeBytes
actualSizeBytes
sizeMatches
expectedSha256
actualSha256
checksumMatches
consistent
```

This endpoint is intended for storage failure, backup, and restore validation.

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

DB-backed work-order and evidence-file responses also include:

```text
operation
durationMs
```

These fields are evidence aids for Nginx upstream, app failover, DB latency, and DB/file consistency observations.

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

### DB/file consistency flow

Run after `nfs-01` export and app mount baselines are applied in a batched runtime validation window.

```bash
curl -s http://<nginx-public-ip>/api/work-orders/1
curl -s -X POST http://<nginx-public-ip>/api/work-orders/1/evidence-files
curl -s http://<nginx-public-ip>/api/work-orders/1/evidence-files
curl -s http://<nginx-public-ip>/api/work-orders/1/evidence-files/<evidenceId>/consistency
```

Expected evidence:

- work-order metadata exists in PostgreSQL
- evidence-file metadata exists in PostgreSQL
- file object exists on the mounted path
- file size and SHA-256 match metadata
- response includes node identity for the app node that handled the request

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
| `OPS_DB_URL` | For DB checks, work orders, and evidence metadata | PostgreSQL JDBC URL. |
| `OPS_DB_USERNAME` | For DB checks, work orders, and evidence metadata | PostgreSQL username. |
| `OPS_DB_PASSWORD` | For DB checks, work orders, and evidence metadata | PostgreSQL password. |
| `OPS_EVIDENCE_FILE_ROOT` | For evidence file objects | Mounted evidence file root. Defaults to `/mnt/ops-sample/files`. |
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
- general file upload/download behavior
- arbitrary user-provided file content
- PostgreSQL replication
- backup and restore automation
- deployment automation
- Nginx configuration
- incident drill automation

Those belong to later infrastructure and operations issues.
