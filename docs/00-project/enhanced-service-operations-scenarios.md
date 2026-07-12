# Enhanced service operations scenarios

This document defines the operating scenarios to run before the next AWS runtime validation window.

The goal is not to prove that `ops-sample-service` has many application features. The goal is to prove that a small web service can produce realistic WEB/WAS/DB/file-storage incidents and that the operator can narrow the failing layer with evidence.

Fixed service identity:

```text
운영 작업 요청과 증빙 파일을 관리하는 경량 웹 업무 서비스
```

Fixed project theme:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

## Why this matrix exists

PR #141-#144 completed the repository implementation baseline for:

```text
work-order domain/schema
server-rendered web workflow
status history and audit logs
evidence upload/download workflow
WEB/WAS failure-lab endpoints
```

PR #146 prepared the first enhanced-service validation playbook:

```text
infra/ansible/playbooks/lab-full-ops-enhanced-service-workflow-validation.yml
```

That playbook is valuable, but it is still primarily a baseline workflow check. The next runtime window must go beyond “the service works” and collect evidence for operational decisions:

```text
What failed?
Which tier should be inspected first?
Which log, metric, command result, or checksum supports that conclusion?
```

## Scenario summary

| Scenario | User-facing situation | Main operating question | Priority |
|---|---|---|---|
| S1. Normal workflow baseline | Work orders and evidence files can be created, updated, uploaded, downloaded, and checked | Does the enhanced service path work through Nginx/WAS/DB/NFS? | Required |
| S2. Evidence upload failure isolation | User cannot upload an evidence file | Is the failure caused by WEB limit, WAS multipart limit, file storage, or DB metadata? | Required |
| S3. WAS long request vs DB-backed delay | A request is slow or appears to hang | Is the delay in the WAS request path or DB dependency path? | Required if time permits |
| S4. DB service incident with web impact | Work-order page/API fails while the app process is alive | Is the DB host down, PostgreSQL service down, or application process down? | Required |
| S5. Enhanced backup and restore refresh | Data and uploaded file must survive restore into restore-lab | Do work orders, history, audit logs, metadata, and file objects recover together? | Required for final portfolio |

## Scenario S1. Normal workflow baseline

### Business situation

An operator receives an operations work request, changes its status, uploads an evidence file, downloads it later, and checks whether DB metadata and the NFS file object agree.

### User impact

If this path fails, the service cannot be explained as a web-based operations workload. The rest of the incident scenarios would be weak because there would be no valid baseline.

### Hypotheses

```text
H1. Nginx routes HTML and API requests to the WAS tier.
H2. The WAS can create and update work-order rows in PostgreSQL.
H3. The WAS can write an uploaded evidence file to the mounted file storage path.
H4. The WAS can download the same file and report DB/file consistency.
H5. Nginx and app logs can be correlated with X-Request-Id.
```

### Checks

Use:

```text
infra/ansible/playbooks/lab-full-ops-enhanced-service-workflow-validation.yml
```

It should verify:

```text
GET  /work-orders
GET  /work-orders/new
POST /work-orders
GET  /work-orders/{id}
POST /work-orders/{id}/status
GET  /api/work-orders/{id}
GET  /api/work-orders/{id}/events
GET  /api/audit-logs
POST /work-orders/{id}/evidence
GET  /work-orders/{id}/evidence/{evidenceId}/download
GET  /api/work-orders/{id}/evidence-files
GET  /api/work-orders/{id}/evidence-files/{evidenceId}/consistency
GET  /ops/failure-lab
```

### Evidence to collect

```text
HTTP response files from nginx-01
created work_order_id and evidence_id
ops_work_orders row
ops_work_order_events rows
ops_operation_audit_logs rows
ops_work_order_evidence_files row
NFS file path, size, and SHA-256 on nfs-01
consistency endpoint result
Nginx access log lines with request prefix
app journald lines with request prefix
```

### Success criteria

```text
work_order_status=IN_PROGRESS
status_event_found=true
evidence metadata exists in PostgreSQL
NFS object exists
uploaded SHA-256 = metadata SHA-256 = downloaded SHA-256
consistency=true
Nginx and app logs contain the same request prefix
```

### Safe interview claim after validation

```text
작업 요청 생성, 상태 변경, 조치 이력, 증빙 파일 업로드·다운로드 흐름을 Nginx/WAS/PostgreSQL/NFS 경로에서 검증했고, request ID로 Nginx와 app 로그를 연결했습니다.
```

### Do not claim

```text
production workflow maturity
login/RBAC workflow
commercial ITSM behavior
```

## Scenario S2. Evidence upload failure isolation

### Business situation

A user tries to upload an evidence file to a work order, but the upload fails.

### User impact

The operator cannot attach diagnostic proof to a work request. In a real operations workflow, this blocks incident closure or recovery proof.

### Operating question

```text
Is the failure caused by Nginx, Spring multipart handling, NFS/file permissions, or PostgreSQL metadata persistence?
```

### Hypotheses

```text
H1. Nginx rejects the request before it reaches the WAS because of request size limits.
H2. The WAS rejects the request because the Spring multipart max-file-size or max-request-size is exceeded.
H3. The WAS accepts the request but cannot write to the NFS-mounted file path.
H4. The file object is written but PostgreSQL metadata insert fails.
H5. PostgreSQL metadata exists but the file object is missing or has a checksum mismatch.
```

### Checks

First collect the app-visible limits:

```bash
curl -k -s https://127.0.0.1/api/failure-lab/upload-limits
```

Then run these cases:

```text
1. known-good small upload
2. upload at or near expected limit
3. oversized upload beyond configured limit
4. post-failure DB metadata query
5. post-failure NFS file search
6. Nginx access/error log sample
7. app journald sample
```

### Evidence to collect

```text
upload-limits JSON
HTTP status and response body for normal upload
HTTP status and response body for oversized upload
Nginx access log status and request time
Nginx error log if request is rejected at WEB tier
app journald multipart/storage exception if rejected at WAS tier
PostgreSQL metadata row presence/absence
NFS file object presence/absence
consistency endpoint result when metadata exists
```

### Layer diagnosis table

| Observation | Likely layer | First interpretation |
|---|---|---|
| Nginx returns 413 and app log has no matching request | WEB/Nginx | Request body rejected before WAS |
| App log shows multipart exceeded and DB/NFS has no new object | WAS/Spring multipart | Request reached WAS but was rejected by multipart limits |
| App log shows file write failure and DB row is absent | WAS -> file storage | NFS mount/permission/storage path problem |
| DB row exists but file object is absent | file storage consistency | Metadata/file-object split-brain condition |
| File exists but checksum differs | file integrity | File object corruption or wrong storage path |

### Success criteria

A good validation is not necessarily “oversized upload succeeds.” A good validation proves the failure layer:

```text
normal upload succeeds
oversized upload has a clear rejection point
metadata/file object side effects are verified after failure
logs show whether request reached Nginx only or both Nginx and app
```

### Safe interview claim after validation

```text
증빙 파일 업로드 실패를 Nginx request size 제한, WAS multipart 제한, NFS 쓰기 가능 여부, PostgreSQL metadata 생성 여부로 나누어 확인했습니다.
```

### Do not claim

```text
large-scale file upload tuning completed
production upload policy validated
object storage design completed
```

## Scenario S3. WAS long request vs DB-backed delay

### Business situation

A user reports that some requests are slow or appear to hang.

### User impact

The work-order page may still open, but certain operations take longer and can create timeout symptoms at the WEB/WAS boundary.

### Operating question

```text
Is the delay caused by WAS request handling/thread occupation, or by a DB-backed dependency path?
```

### Hypotheses

```text
H1. /api/failure-lab/sleep consumes WAS request time without using PostgreSQL.
H2. /api/failure-lab/db-sleep consumes both WAS request time and PostgreSQL dependency time.
H3. Nginx sees both as long upstream responses, so Nginx logs alone are insufficient to distinguish the root path.
H4. App logs and DB-specific evidence are needed to separate WAS-only delay from DB-backed delay.
```

### Checks

```bash
curl -k -w 'time_total=%{time_total}\nhttp_code=%{http_code}\n' \
  -o /tmp/was-sleep.json \
  https://127.0.0.1/api/failure-lab/sleep?millis=3000

curl -k -w 'time_total=%{time_total}\nhttp_code=%{http_code}\n' \
  -o /tmp/db-sleep.json \
  https://127.0.0.1/api/failure-lab/db-sleep?millis=3000
```

### Evidence to collect

```text
curl time_total for /sleep
curl time_total for /db-sleep
Nginx upstream_response_time for both request IDs
app log durationMs for both request IDs
app response scenario field: was_sleep vs db_sleep
DB query path evidence for db_sleep if available
```

### Success criteria

```text
/sleep returns scenario=was_sleep and duration roughly matches requestedMillis
/db-sleep returns scenario=db_sleep and duration roughly matches requestedMillis
Nginx upstream_response_time shows both as slow from the WEB perspective
app response/log differentiates the scenario type
```

### Safe interview claim after validation

```text
같은 느린 요청이라도 WAS thread 점유형 요청과 DB dependency 지연 요청을 분리해 관찰했고, Nginx upstream time만으로는 원인을 단정하지 않고 app log와 DB-backed endpoint 특성을 함께 확인했습니다.
```

### Do not claim

```text
load test completed
capacity planning completed
thread-pool tuning completed
connection-pool tuning completed
```

## Scenario S4. DB service incident with web impact

### Business situation

The work-order list page or API fails even though the application process is still running.

### User impact

Users cannot view or update work orders. The service appears partially alive because process-level health can still return 200.

### Operating question

```text
Is this an app process outage, a DB host outage, or PostgreSQL service dependency failure?
```

### Hypotheses

```text
H1. /healthz remains 200 because the WAS process is alive.
H2. /readyz becomes 503 because PostgreSQL dependency fails.
H3. /work-orders and /api/work-orders fail or render a DB-unavailable page.
H4. DB host-level metrics can remain reachable while PostgreSQL service is inactive.
H5. PostgreSQL restart should restore readiness and work-order workflow.
```

### Checks

Normal state:

```text
GET /healthz
GET /readyz
GET /work-orders
GET /api/work-orders/summary
systemctl is-active postgresql on db-primary-01
ss -ltnp | grep 5432 on db-primary-01
```

Incident state:

```text
stop PostgreSQL service on db-primary-01
repeat /healthz, /readyz, /work-orders, /api/work-orders/summary
check systemd and port 5432
collect Nginx/app logs
collect Prometheus node and PostgreSQL service metrics when monitoring is enabled
```

Recovery state:

```text
start PostgreSQL service
repeat /readyz and /work-orders checks
confirm data remains visible
```

### Evidence to collect

```text
healthz normal/incident/recovery HTTP status
readyz normal/incident/recovery HTTP status
work-orders page normal/incident/recovery response
summary API normal/incident/recovery response
PostgreSQL systemd state
port 5432 state
Nginx access log request IDs
app logs with DB unavailable errors
Prometheus node up and postgresql service active metric if monitoring is present
```

### Success criteria

```text
/healthz=200 during incident
/readyz=503 during incident
DB-backed web/API path fails during incident
PostgreSQL service is inactive or port 5432 closed
DB host remains reachable if node_exporter is available
post-recovery /readyz and /work-orders return normal
```

### Safe interview claim after validation

```text
DB 장애 시 작업 요청 화면과 API가 어떤 영향을 받는지 확인했고, /healthz와 /readyz, PostgreSQL systemd 상태, 포트, 로그, Prometheus metric을 함께 봐서 app process 장애와 DB service dependency failure를 구분했습니다.
```

### Do not claim

```text
PostgreSQL HA
automatic failover
zero-downtime DB recovery
```

## Scenario S5. Enhanced backup and restore refresh

### Business situation

A work order, its status history, audit log, evidence metadata, and uploaded file object must be recoverable after backup restoration.

### User impact

If metadata and file objects do not recover together, the service may show a work order but fail to provide evidence files, or show metadata for files that no longer exist.

### Operating question

```text
Do the enhanced service tables and uploaded NFS file objects recover together in restore-lab?
```

### Enhanced recovery target

The restore validation should include:

```text
ops_work_orders
ops_work_order_events
ops_operation_audit_logs
ops_work_order_evidence_files
NFS uploaded evidence file object
downloaded file SHA-256
consistency endpoint result
work-order detail page rendering after restore
```

### Checks

Backup window:

```text
create enhanced service workflow data
record work_order_id, evidence_id, storage_path, SHA-256
run pg_dump backup artifact creation
run restic snapshot for NFS file objects
preserve backup metadata and raw artifacts
```

Restore window:

```text
restore PostgreSQL dump into restore-lab db-primary-01
restore NFS file objects into restore-lab nfs-01
start app and Nginx in restore-lab
GET /work-orders/{id}
GET /api/work-orders/{id}
GET /api/work-orders/{id}/events
GET /api/audit-logs
GET /api/work-orders/{id}/evidence-files
GET /work-orders/{id}/evidence/{evidenceId}/download
GET /api/work-orders/{id}/evidence-files/{evidenceId}/consistency
```

### Evidence to collect

```text
restored work-order row
restored event rows
restored audit log rows
restored evidence metadata row
restored NFS file object path
file size and SHA-256 after restore
downloaded file SHA-256 after restore
consistency=true after restore
restore-lab Nginx/app logs for restored request IDs
```

### Success criteria

```text
work order exists after restore
event history exists after restore
audit log exists after restore
evidence metadata exists after restore
NFS file object exists after restore
expected SHA-256 = restored file SHA-256 = downloaded SHA-256
consistency=true after restore
```

### Safe interview claim after validation

```text
보강된 서비스 기준으로 작업 요청, 상태 이력, 감사 로그, 파일 metadata, NFS file object를 별도 restore-lab에 복구하고, 상세 화면과 consistency endpoint, SHA-256으로 복구 결과를 검증했습니다.
```

### Do not claim

```text
continuous backup
point-in-time recovery
cross-region disaster recovery
RPO/RTO guarantee
```

## Recommended validation PR sequence

Do not open the AWS runtime window until the following prep is complete.

```text
1. [ANSIBLE] Add upload failure and upload-limit scenario validation
2. [ANSIBLE] Add WAS sleep vs DB sleep latency scenario validation
3. [ANSIBLE] Add DB incident web-impact validation
4. [ANSIBLE] Prepare enhanced restore-lab validation refresh
5. [VALIDATION] Run one planned enhanced-service AWS validation window
```

The existing playbook from PR #146 remains the baseline for S1. New playbooks should focus on incident interpretation rather than duplicating the normal workflow.

## Runtime window order

When all prep exists, use one planned runtime window:

```text
1. build or download the latest ops-sample-service jar
2. terraform apply once
3. populate lab-full-ops inventory
4. configure DB baseline
5. configure NFS baseline
6. configure app NFS mount baseline
7. deploy latest app artifact
8. configure Nginx reverse proxy
9. run S1 enhanced workflow baseline
10. run S2 upload failure isolation
11. run S3 latency comparison
12. run S4 DB web-impact incident
13. run backup artifact creation
14. run S5 restore-lab refresh
15. collect evidence archives
16. write evidence documents
17. terraform destroy once
```

If time or cost is constrained, prioritize:

```text
1. S1 normal enhanced workflow baseline
2. S2 upload/download and upload failure isolation
3. S4 DB service incident with web impact
4. S5 enhanced backup/restore refresh
5. S3 latency comparison
```

## Current status

```text
Scenario matrix: defined
AWS runtime required now: no
Next default work: add incident-specific validation prep playbooks
Runtime claim status: pending evidence refresh
```
