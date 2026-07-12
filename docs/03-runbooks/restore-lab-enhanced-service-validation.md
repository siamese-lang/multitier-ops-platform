# restore-lab enhanced service validation

This runbook prepares the final pre-runtime restore validation step for the enhanced `ops-sample-service` model.

It is a validation-prep document. Do not use it to claim refreshed restore-lab evidence until the playbook is executed in a planned AWS runtime window and the result is documented in `docs/04-evidence/`.

## Purpose

The earlier restore-lab validation proved DB/file artifact recovery and API consistency for the previous workload model. The enhanced service now includes:

```text
work-order web detail page
status event history
operation audit logs
evidence upload/download workflow
PostgreSQL evidence metadata
NFS uploaded file object
consistency endpoint
```

The enhanced restore validation checks that these artifacts recover together.

## Playbook

```text
infra/ansible/playbooks/restore-lab-enhanced-service-validation.yml
```

## When to run

Run this only after all prerequisites are true:

```text
1. lab-full-ops enhanced service workflow data has been created.
2. The target sample values have been recorded:
   - work_order_id
   - evidence_id
   - storage_path
   - size_bytes
   - sha256
   - expected work-order status
3. lab-full-ops DB dump and NFS restic backup have been created.
4. restore-lab infrastructure is deployed.
5. DB/file restore baseline has already restored PostgreSQL and NFS artifacts.
6. restore-lab app and Nginx are deployed using a current jar that includes:
   - WorkOrderWebController
   - FailureLabController
   - evidence upload/download routes
7. The restore-lab inventory has web, app, db, and storage groups.
```

## What this validates

The playbook validates the following through Nginx:

```text
GET /healthz
GET /readyz
GET /work-orders/{id}
GET /api/work-orders/{id}
GET /api/work-orders/{id}/events
GET /api/audit-logs?limit=100
GET /api/work-orders/{id}/evidence-files
GET /api/work-orders/{id}/evidence-files/{evidenceId}/consistency
GET /work-orders/{id}/evidence/{evidenceId}/download
```

It also checks the restored artifacts directly on the DB and NFS tiers:

```text
ops_work_orders row
ops_work_order_events rows
ops_operation_audit_logs rows
ops_work_order_evidence_files row
NFS file object size
NFS file object SHA-256
```

## Required variables

The playbook does not hard-code old sample IDs. Supply the actual values collected during the enhanced lab-full-ops runtime validation window.

Example:

```bash
ansible-playbook \
  -i infra/ansible/inventories/restore-lab/hosts.yml \
  infra/ansible/playbooks/restore-lab-enhanced-service-validation.yml \
  --extra-vars 'restore_lab_enhanced_work_order_id=12' \
  --extra-vars 'restore_lab_enhanced_evidence_id=4' \
  --extra-vars 'restore_lab_enhanced_storage_path=work-order-12/upload-example.txt' \
  --extra-vars 'restore_lab_enhanced_expected_size_bytes=65536' \
  --extra-vars 'restore_lab_enhanced_expected_sha256=<64-char-sha256>' \
  --extra-vars 'restore_lab_enhanced_expected_status=IN_PROGRESS'
```

Optional variables:

```text
restore_lab_enhanced_min_event_count
restore_lab_enhanced_min_audit_count
```

Defaults:

```text
restore_lab_enhanced_expected_status=IN_PROGRESS
restore_lab_enhanced_min_event_count=2
restore_lab_enhanced_min_audit_count=1
```

## Expected success pattern

```text
/healthz succeeds
/readyz succeeds
/work-orders/{id} renders the detail page
work-order JSON has the expected id and status
events API returns at least the expected number of status/history rows
audit log API contains at least one row for the restored work order
evidence list contains the expected evidence_id and storage_path
consistency endpoint reports consistent=true
downloaded file SHA-256 equals the expected SHA-256
DB rows exist directly on db-primary-01
NFS file object exists directly on nfs-01
```

## Important interpretation

This validation is stronger than the previous restore-lab HTTP/API check because it includes both the data model and the user-facing web workflow:

```text
DB row restored
+ event history restored
+ audit log restored
+ file metadata restored
+ file object restored
+ web detail page renders restored data
+ downloaded file matches SHA-256
+ consistency endpoint agrees
```

This is the evidence needed to support the final recovery claim.

## Safe claim after successful runtime validation

```text
보강된 서비스 모델 기준으로 작업 요청, 상태 이력, 감사 로그, 증빙 파일 metadata, NFS file object를 restore-lab에 복구했고, 상세 화면, 다운로드, consistency endpoint, SHA-256 비교로 DB와 파일 저장소가 함께 복구됐는지 확인했습니다.
```

## Claims to avoid

Do not claim:

```text
production disaster recovery
zero data loss guarantee
RPO/RTO compliance
cross-region DR
automatic failover
HA recovery
```

This is controlled restore validation in a lab environment.

## Suggested runtime order

Use this sequence in the one planned AWS validation window:

```text
1. Run enhanced service workflow validation in lab-full-ops.
2. Record work_order_id, evidence_id, storage_path, size_bytes, sha256, and expected status.
3. Run upload failure incident validation.
4. Run latency scenario validation.
5. Run DB web-impact incident validation.
6. Create enhanced backup artifacts.
7. Deploy restore-lab.
8. Run restore-lab DB/file restore baseline.
9. Deploy app and Nginx in restore-lab.
10. Run restore-lab enhanced service validation with the recorded sample values.
11. Document evidence.
12. Destroy runtime resources once evidence is collected.
```

## Report output

The playbook writes a report on the WEB node:

```text
/tmp/multitier-ops-platform/restore-lab-enhanced-service-<inventory_hostname>.txt
```

The report includes:

```text
work_order_id
actual_status
event_count
audit_count
evidence_id
storage_path
expected_size_bytes
expected_sha256
actual_downloaded_sha256
consistent/fileExists/sizeMatches/checksumMatches flags
detail page evidence markers
DB row samples
NFS file object sample
Nginx request-id log sample
app journald request-id log sample
```

## Current boundary

```text
Prepared: yes
Runtime executed: no
Evidence document written: no
```

Do not move this scenario from prepared to validated until the report is collected and summarized in `docs/04-evidence/`.
