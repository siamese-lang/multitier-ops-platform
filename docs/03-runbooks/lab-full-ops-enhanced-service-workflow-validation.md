# lab-full-ops enhanced service workflow validation

## Purpose

This runbook prepares the validation step for the enhanced `ops-sample-service` implementation baseline.

The service is now positioned as:

```text
운영 작업 요청과 증빙 파일을 관리하는 경량 웹 업무 서비스
```

This validation exists to prove that the enhanced service behavior works through the operating path:

```text
operator / validation runner
  -> nginx-01 HTTPS
  -> app-01 ops-sample-service
  -> db-primary-01 PostgreSQL metadata
  -> nfs-01 NFS-backed evidence file object
```

## Scope

The playbook validates:

```text
1. /work-orders web page smoke through nginx-01.
2. /work-orders/new web form smoke through nginx-01.
3. Work order creation through the web form.
4. Work order detail page rendering.
5. Status transition through the web form.
6. Work-order event history through JSON API.
7. Audit log visibility through JSON API.
8. User-provided evidence upload through multipart web form.
9. Evidence metadata list through JSON API.
10. Evidence download through the web workflow.
11. DB metadata and NFS file object consistency API.
12. Direct PostgreSQL row verification on db-primary-01.
13. Direct NFS file object size/SHA-256 verification on nfs-01.
14. /ops/failure-lab web page smoke.
15. /api/failure-lab/sleep long WAS request endpoint.
16. /api/failure-lab/db-sleep DB-backed latency endpoint.
17. /api/failure-lab/file-storage-check storage readiness endpoint.
18. /api/failure-lab/upload-limits multipart limit inspection endpoint.
19. Nginx access-log request-id evidence.
20. app journald request-id evidence from an app node.
```

This is a validation playbook, not an incident drill by itself. It prepares the enhanced service workflow evidence that later WEB/WAS timeout and restore refresh scenarios can build on.

## Preconditions

Run this only during one planned `lab-full-ops` runtime validation window.

Required sequence before this playbook:

```text
1. reduced lab-full-ops Terraform apply completed
2. inventories/lab-full-ops/hosts.yml populated from Terraform outputs
3. Ansible control path validated
4. db-primary-01 PostgreSQL primary configured
5. nfs-01 NFS export baseline configured
6. app-01 NFS client mount baseline configured
7. enhanced ops-sample-service artifact built or downloaded
8. lab-full-ops ops-sample-service deployment completed
9. nginx-01 reverse proxy configured and routing to the app node
```

The deployed jar must include the enhanced service classes checked by `ops_app_required_jar_entries`:

```text
WorkOrderEvidenceController
WorkOrderEvidenceRepository
WorkOrderWebController
FailureLabController
```

Do not run this against an older app artifact. That would make the service appear incomplete and weaken the evidence.

## Execution

From WSL/Linux/macOS:

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible
export ANSIBLE_CONFIG="$PWD/ansible.cfg"

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/lab-full-ops-enhanced-service-workflow-validation.yml
```

## Static syntax check

This check does not contact AWS instances:

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible
export ANSIBLE_CONFIG="$PWD/ansible.cfg"
cp inventories/lab-full-ops/hosts.yml.example /tmp/lab-full-ops-hosts.yml

ansible-playbook \
  -i /tmp/lab-full-ops-hosts.yml \
  playbooks/lab-full-ops-enhanced-service-workflow-validation.yml \
  --syntax-check

rm -f /tmp/lab-full-ops-hosts.yml
```

Expected output:

```text
playbook: playbooks/lab-full-ops-enhanced-service-workflow-validation.yml
```

## Endpoints used

Web pages:

```text
GET  /work-orders
GET  /work-orders/new
POST /work-orders
GET  /work-orders/{id}
POST /work-orders/{id}/status
POST /work-orders/{id}/evidence
GET  /work-orders/{id}/evidence/{evidenceId}/download
GET  /ops/failure-lab
```

JSON APIs:

```text
GET /api/work-orders/{id}
GET /api/work-orders/{id}/events
GET /api/audit-logs?limit=20
GET /api/work-orders/{id}/evidence-files
GET /api/work-orders/{id}/evidence-files/{evidenceId}/consistency
GET /api/failure-lab/sleep?millis=<ms>
GET /api/failure-lab/db-sleep?millis=<ms>
GET /api/failure-lab/file-storage-check
GET /api/failure-lab/upload-limits
```

## Evidence locations

The playbook writes response files on `nginx-01`:

```text
/tmp/multitier-ops-platform/lab-full-ops-enhanced-service-workflow/
```

Representative files include:

```text
work-orders.html
work-order-new.html
work-order-create.headers
work-order-detail.html
work-order-status.headers
work-order.json
work-order-events.json
audit-logs.json
evidence-upload.headers
evidence-list.json
evidence-consistency.json
downloaded-evidence.txt
failure-lab.html
failure-lab-sleep.json
failure-lab-db-sleep.json
failure-lab-file-storage.json
failure-lab-upload-limits.json
parsed-values.env
```

The final report is written to:

```text
/tmp/multitier-ops-platform/lab-full-ops-enhanced-service-workflow-nginx-01.txt
```

## Minimum success criteria

A successful validation should show:

```text
work_order_status=IN_PROGRESS
status_event_found=true
api_consistent=true
file_storage_ready=true
expected_sha256 == metadata_sha256 == downloaded_sha256
PostgreSQL row exists for the created work order and evidence file
NFS file object exists with matching size and SHA-256
Nginx access log contains the enhanced validation request prefix
app journald log contains the enhanced validation request prefix
failure-lab sleep endpoint returns status=ok
failure-lab db-sleep endpoint returns status=ok
failure-lab file-storage-check reports ready=true
failure-lab upload-limits reports relatedFlow=POST /work-orders/{id}/evidence
```

The report should include at least:

```text
work_order_id
work_order_status
event_count
status_event_found
evidence_id
storage_path
expected_size_bytes
expected_sha256
metadata_sha256
downloaded_sha256
api_consistent
file_storage_ready
db_rows
nfs_object_check
nginx_log_sample
app_log_sample
result_dir
```

## Failure interpretation

| Failure point | Likely layer | First checks |
|---|---|---|
| `/work-orders` or `/work-orders/new` fails | WEB/WAS routing | Nginx service, TLS site, upstream, app service |
| web form creation fails | WAS/DB | app readiness, PostgreSQL service, credentials, schema |
| status event missing | App/DB model | `ops_work_order_events`, `WorkOrderRepository.updateStatus` |
| upload fails | WEB/WAS/File | Nginx upload limit, Spring multipart limit, NFS mount, file permissions |
| download SHA mismatch | File path or storage | NFS object, metadata row, download endpoint |
| consistency false | DB/file consistency | metadata row, NFS file object, file size, SHA-256 |
| `db-sleep` fails | DB dependency | PostgreSQL service, credentials, pg_hba, connection path |
| `file-storage-check` not ready | NFS/file storage | mount state, export permissions, app-side path |
| Nginx log missing request prefix | WEB logging | `/var/log/nginx/ops_access.log`, request ID propagation |
| app log missing request prefix | WAS logging | `journalctl -u ops-sample-service`, MDC request ID |

## Runtime batching policy

This playbook is intended to run near the end of one service-refresh validation window:

```text
1. prepare artifacts and inventories statically
2. terraform apply once
3. configure DB/NFS/app/Nginx
4. run enhanced service workflow validation
5. collect evidence
6. optionally run restore/backup refresh validations if planned
7. terraform destroy once
```

Do not create and destroy the AWS lab just to run this playbook alone.

## Supported claim after successful runtime validation

After this playbook succeeds in the planned AWS runtime window, the project can claim:

```text
The enhanced ops-sample-service web workflow was validated through Nginx/WAS/PostgreSQL/NFS with request-id, DB row, file object, checksum, and log evidence.
```

## Claims still not supported by this playbook alone

Do not claim:

```text
Nginx timeout tuning completed
Tomcat thread-pool tuning completed
DB connection-pool tuning completed
production load testing completed
backup/restore refreshed against enhanced service model
SLO/SLA compliance
production-grade monitoring maturity
```

Those require separate runtime evidence documents.

## Cleanup

This validation creates one work order, at least one status event, one audit row set, and one uploaded evidence file.

For the short-lived validation lab, cleanup is normally handled by destroying the environment after evidence collection.

If manual cleanup is needed before destroy:

```text
PostgreSQL: delete from ops_work_orders where id = <work_order_id> cascade behavior removes evidence rows.
NFS: remove /srv/ops-sample/files/<storage_path>
```

Do not skip final Terraform cleanup after the validation window.
