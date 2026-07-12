# WEB/WAS failure lab

`ops-sample-service` includes a small failure-lab surface for WEB/WAS operations validation.

This is not a production-grade chaos engineering tool. It exists to create controlled, explainable symptoms for the portfolio:

```text
Nginx -> Spring Boot WAS -> PostgreSQL / file storage
```

## Web page

```text
GET /ops/failure-lab
```

The page exposes links and curl examples for the failure-lab endpoints.

## Endpoints

| Endpoint | Purpose | Main observation points |
|---|---|---|
| `GET /api/failure-lab/sleep?millis=3000` | Hold a WAS request thread for a bounded duration | Nginx upstream response time, app request duration, thread occupation |
| `GET /api/failure-lab/db-sleep?millis=3000` | Run PostgreSQL `pg_sleep` through the WAS DB dependency | DB-backed latency, DB unavailability behavior, request duration |
| `GET /api/failure-lab/file-storage-check` | Check app-side evidence file storage readiness | NFS mount state, readable/writable flags, upload-failure triage |
| `GET /api/failure-lab/upload-limits` | Show effective Spring multipart limits | Compare Nginx upload size limits with WAS multipart limits |

All delay values are bounded to 0-30000 ms to avoid accidental long-running requests.

## Example commands

```bash
curl -i -H 'X-Request-Id: was-sleep-001' \
  'http://<nginx-public-ip>/api/failure-lab/sleep?millis=3000'

curl -i -H 'X-Request-Id: db-sleep-001' \
  'http://<nginx-public-ip>/api/failure-lab/db-sleep?millis=3000'

curl -i -H 'X-Request-Id: file-storage-check-001' \
  'http://<nginx-public-ip>/api/failure-lab/file-storage-check'

curl -i -H 'X-Request-Id: upload-limits-001' \
  'http://<nginx-public-ip>/api/failure-lab/upload-limits'
```

## Intended operating evidence

### WAS long request

A request to `/api/failure-lab/sleep` should show:

```text
HTTP request reaches Nginx
Nginx proxies to the WAS tier
WAS holds the request thread for the requested bounded duration
app request log records path, status, durationMs, node identity, and request ID
```

This supports later validation of:

```text
proxy_read_timeout
upstream response time
WAS request duration
concurrent request/thread occupation
```

### DB dependency latency

A request to `/api/failure-lab/db-sleep` should show:

```text
WAS process remains healthy
request latency is tied to PostgreSQL query latency
DB unavailability returns a 503 response from the DB-backed endpoint
```

This supports later validation of:

```text
DB dependency latency
readiness vs process health distinction
DB-backed endpoint failure behavior
```

### File storage readiness

A request to `/api/failure-lab/file-storage-check` should show:

```text
configured storage root
exists/directory/readable/writable/ready flags
node identity
```

This supports later validation of:

```text
NFS mount missing
file storage permission issue
upload failure root-cause separation
```

### Upload limit comparison

A request to `/api/failure-lab/upload-limits` should show the WAS-side multipart limits:

```text
OPS_MULTIPART_MAX_FILE_SIZE
OPS_MULTIPART_MAX_REQUEST_SIZE
```

This supports later comparison with Nginx upload controls such as:

```text
client_max_body_size
proxy_request_buffering
proxy_read_timeout
```

## Guardrails

This failure lab does not claim:

```text
production chaos engineering
load testing completeness
SLO/SLA validation
thread-pool tuning completion
connection-pool tuning completion
Nginx timeout validation by itself
```

Runtime validation must still collect Nginx logs, app logs, HTTP response evidence, and relevant metrics before any operational claim is made.
