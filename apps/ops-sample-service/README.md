# ops-sample-service

`ops-sample-service` is the lightweight operated service used by this repository's WEB/WAS/DB/Storage/Backup/Observability operations portfolio.

It is not intended to become a commercial ITSM clone. Its purpose is to be clear enough to answer “what service did you operate?” while still keeping the project focused on WEB/WAS operations, incident diagnosis, and recovery validation.

Service identity:

```text
운영 작업 요청과 증빙 파일을 관리하는 경량 웹 업무 서비스
```

The fixed project theme remains:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

## Role in the project

This service gives the lab controllable behavior that can be used to verify incidents, logs, storage consistency, backup artifacts, and restore procedures.

It provides deterministic evidence for:

- Nginx upstream routing
- WAS process health
- DB-dependent readiness
- app node identity
- HTTP request logging with request IDs
- DB-backed work-order state
- web-based work-order list/detail/create/status-change workflow
- work-order event history
- operation audit logs
- DB metadata plus NFS file-object consistency checks in `lab-full-ops`
- app node failure and continuation drills
- PostgreSQL failure and recovery drills
- file storage failure, backup, and restore drills

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

The service creates and maintains these tables on the first DB-backed request:

```text
ops_work_orders
ops_work_order_events
ops_operation_audit_logs
```

For `lab-full-ops`, evidence-file requests also create:

```text
ops_work_order_evidence_files
```

Table roles:

| Table | Purpose |
|---|---|
| `ops_work_orders` | Work request records used by WEB/WAS/DB operating scenarios |
| `ops_work_order_events` | Status-change and creation history for each work order |
| `ops_operation_audit_logs` | Operator-visible audit trail for create/status-change actions |
| `ops_work_order_evidence_files` | DB metadata for generated evidence files stored on the mounted file path |

This gives the lab a small but explainable service domain without turning the repository into an application-development portfolio.

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
GET  /work-orders/new                                     -> 200, renders create form without DB
GET  /work-orders                                         -> renders DB-unavailable web page when DB env is missing
GET  /api/work-orders                                     -> 503 when DB env is missing
GET  /api/work-orders/summary                             -> 503 when DB env is missing
GET  /api/work-orders/{id}/events                         -> 503 when DB env is missing
GET  /api/audit-logs                                      -> 503 when DB env is missing
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

## Web workflow pages

The service includes a small server-rendered HTML workflow so that it can be explained as a lightweight web service, not only as a JSON API workload.

| Path | Purpose |
|---|---|
| `GET /` | Redirects to `/work-orders` |
| `GET /work-orders` | Lists work orders, status summary, and recent audit logs |
| `GET /work-orders/new` | Renders the create-work-order form |
| `POST /work-orders` | Creates a work order from the web form |
| `GET /work-orders/{id}` | Shows work-order detail, status history, evidence metadata, and operational links |
| `POST /work-orders/{id}/status` | Updates work-order status and records event/audit rows |

Web workflow examples:

```bash
curl -i http://localhost:8080/work-orders
curl -i http://localhost:8080/work-orders/new
```

The web pages are intentionally simple. They exist to make the operated service explainable and to exercise WEB/WAS request paths through Nginx, not to become a full front-end project.

## JSON API endpoints

### `GET /healthz`

Process-level health check. It does not require DB connectivity.

```bash
curl -s http://localhost:8080/healthz
```

### `GET /readyz`

Readiness check. It checks PostgreSQL connectivity with a simple query.

```bash
curl -i http://localhost:8080/readyz
```

### `GET /node`

Node identity endpoint. It returns hostname, local address, role, tier, environment, and app version.

```bash
curl -s http://localhost:8080/node
```

### `GET /db/time`

Runs `select now()` against PostgreSQL and returns the DB time.

```bash
curl -i http://localhost:8080/db/time
```

### `GET /api/work-orders`

Lists DB-backed work orders. The first successful call creates the work-order tables and seed data when needed.

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

Creates a work order and stores it in PostgreSQL. This also records a work-order event and an operation audit log.

```bash
curl -s -X POST http://localhost:8080/api/work-orders \
  -H 'Content-Type: application/json' \
  -H 'X-Request-Id: work-order-create-001' \
  -d '{"title":"Validate app-02 after app-01 stop","priority":"HIGH","requester":"service-desk","assignee":"ops-admin","description":"Created during an incident drill."}'
```

### `PATCH /api/work-orders/{id}/status`

Updates work order status. This also records a status event and an operation audit log.

Allowed statuses:

```text
OPEN
IN_PROGRESS
DONE
FAILED
CANCELLED
```

```bash
curl -s -X PATCH http://localhost:8080/api/work-orders/1/status \
  -H 'Content-Type: application/json' \
  -H 'X-Request-Id: work-order-status-001' \
  -d '{"status":"DONE","actor":"ops-admin","message":"Validated through nginx-01 and app-02."}'
```

### `GET /api/work-orders/{id}/events`

Lists work-order lifecycle events.

```bash
curl -s http://localhost:8080/api/work-orders/1/events
```

This endpoint is used to explain the service as an operated work-request system instead of only a DB health-check API.

### `GET /api/work-orders/summary`

Returns count by status.

```bash
curl -s http://localhost:8080/api/work-orders/summary
```

### `GET /api/audit-logs`

Lists recent operation audit logs. The optional `limit` query parameter is bounded to 1-100 rows.

```bash
curl -s http://localhost:8080/api/audit-logs
curl -s 'http://localhost:8080/api/audit-logs?limit=10'
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

This is not a general upload endpoint yet. General file upload/download behavior is a later service-completion PR.

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

Every JSON response includes node identity so the operator can prove which app node handled the request.

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

These fields are evidence aids for Nginx upstream, app failover, DB latency, DB/file consistency, and status-change observations.

## Request log evidence

Every HTTP request is logged with fields suitable for incident evidence:

```text
event=http_request requestId=<id> method=<method> path=<path> status=<status> durationMs=<ms> remoteAddr=<ip> node=<hostname> role=<role> tier=<tier>
```

The service also returns an `X-Request-Id` response header. A caller can provide `X-Request-Id` to correlate curl output, Nginx access logs, app logs, and operation audit rows.

Example:

```bash
curl -i -H 'X-Request-Id: incident-app-01-001' http://<nginx-public-ip>/api/work-orders/summary
```

## Operational evidence examples

### Web workflow evidence

```bash
curl -i http://<nginx-public-ip>/work-orders
curl -i http://<nginx-public-ip>/work-orders/new
```

Expected evidence:

- Nginx can route browser-style HTML requests to the WAS tier.
- the list page depends on PostgreSQL-backed work-order data.
- DB unavailability changes the web workflow from normal list rendering to an operator-visible error page.
- request logs still capture method, path, status, duration, node identity, and request ID.

### Work-order lifecycle evidence

```bash
curl -s -X POST http://<nginx-public-ip>/api/work-orders \
  -H 'Content-Type: application/json' \
  -H 'X-Request-Id: create-lifecycle-001' \
  -d '{"title":"Investigate upload failure","priority":"HIGH","requester":"service-desk","assignee":"ops-admin","description":"Track status and evidence for a WEB/WAS incident."}'

curl -s -X PATCH http://<nginx-public-ip>/api/work-orders/<id>/status \
  -H 'Content-Type: application/json' \
  -H 'X-Request-Id: update-lifecycle-001' \
  -d '{"status":"IN_PROGRESS","actor":"ops-admin","message":"Started WEB/WAS diagnosis."}'

curl -s http://<nginx-public-ip>/api/work-orders/<id>/events
curl -s http://<nginx-public-ip>/api/audit-logs?limit=10
```

Expected evidence:

- work request exists in PostgreSQL
- lifecycle events show creation and status transition
- operation audit logs retain actor/action/result/request ID
- app request logs and Nginx access logs can be correlated by `X-Request-Id`

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
curl -i http://<nginx-public-ip>/work-orders
curl -i http://<nginx-public-ip>/api/work-orders
```

Expected evidence:

- `/healthz` can still return `200`
- `/readyz` returns `503`
- DB-backed API returns `503`
- DB-backed web workflow renders an operator-visible error page
- app request logs show DB-backed endpoints failing while process health remains up
- after PostgreSQL recovery, readiness and data APIs return to normal

## Environment variables

| Variable | Required | Purpose |
| --- | --- | --- |
| `SERVER_PORT` | No | HTTP port. Defaults to `8080`. |
| `OPS_DB_URL` | For DB checks, work orders, event history, audit logs, and evidence metadata | PostgreSQL JDBC URL. |
| `OPS_DB_USERNAME` | For DB checks, work orders, event history, audit logs, and evidence metadata | PostgreSQL username. |
| `OPS_DB_PASSWORD` | For DB checks, work orders, event history, audit logs, and evidence metadata | PostgreSQL password. |
| `OPS_EVIDENCE_FILE_ROOT` | For evidence file objects | Mounted evidence file root. Defaults to `/mnt/ops-sample/files`. |
| `OPS_NODE_ROLE` | No | Logical role. Defaults to `app`. |
| `OPS_NODE_TIER` | No | Logical tier. Defaults to `private-was`. |
| `OPS_ENVIRONMENT` | No | Environment name. Defaults to `lab-full-min`. |
| `APP_VERSION` | No | Version string returned by responses. |

## Out of scope for this PR

This app still does not include:

- authentication
- complex RBAC
- OpenKoda customization
- commercial ITSM clone behavior
- general user-provided file upload/download behavior
- PostgreSQL replication
- backup and restore automation
- deployment automation
- Nginx configuration
- incident drill automation

Those belong to later service-completion and operations-validation PRs.
