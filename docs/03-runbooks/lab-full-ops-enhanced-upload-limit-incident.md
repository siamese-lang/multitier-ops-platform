# lab-full-ops enhanced upload-limit incident validation

## Purpose

This runbook prepares scenario S2 from:

```text
docs/00-project/enhanced-service-operations-scenarios.md
```

The scenario validates a practical upload failure question:

```text
A user can create a work order, but evidence file upload fails.
Which tier rejected the upload, and did the failed upload leave DB metadata or an NFS file object behind?
```

This is a WEB/WAS operations scenario, not a product file-upload feature test.

## Scope

The playbook validates both the normal path and the failure path through `nginx-01`:

```text
1. Read application upload limits from /api/failure-lab/upload-limits.
2. Create a work order through the web form.
3. Upload a normal-sized evidence file through the web multipart form.
4. Verify DB metadata and NFS file object for the normal upload.
5. Upload an intentionally oversized evidence file through the same web multipart form.
6. Confirm the oversized upload is rejected.
7. Confirm the oversized upload did not create evidence metadata.
8. Confirm the oversized upload did not leave an NFS file object.
9. Collect Nginx access/error log samples and app journald samples.
10. Classify the likely rejection layer from HTTP status and Nginx upstream evidence.
```

## Why this matters

A failed upload can be caused by multiple layers:

| Hypothesis | Evidence to check |
|---|---|
| Nginx request body limit rejected the upload | HTTP 413, Nginx access log with `upstream_status="-"`, Nginx error log body-size entry |
| WAS multipart limit rejected the upload | HTTP 4xx/5xx with upstream status from app, app log around request ID |
| File storage failed after request reached app | app log, file-storage error message, NFS directory state |
| DB metadata insert failed after file write | DB error, metadata row absence, possible orphan file |
| Request unexpectedly succeeded | metadata row and NFS object exist for the oversized filename |

The goal is not to force a specific failure layer. The goal is to collect enough evidence to explain where the failure was stopped.

## Preconditions

Run this only during one planned `lab-full-ops` runtime validation window.

Required before this playbook:

```text
1. Terraform apply completed for the planned lab-full-ops runtime.
2. inventories/lab-full-ops/hosts.yml populated from Terraform outputs.
3. PostgreSQL baseline configured.
4. NFS storage baseline configured.
5. App NFS mount baseline configured.
6. Enhanced ops-sample-service jar deployed.
7. Nginx reverse proxy configured and routing to the app.
8. lab-full-ops-enhanced-service-workflow-validation.yml should pass first.
```

Do not run this as the first validation after provisioning. It assumes the normal enhanced workflow is already healthy.

## Execution

From WSL/Linux/macOS:

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible
export ANSIBLE_CONFIG="$PWD/ansible.cfg"

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/lab-full-ops-enhanced-upload-limit-incident.yml
```

## Static syntax check

This check does not contact AWS instances:

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible
export ANSIBLE_CONFIG="$PWD/ansible.cfg"
cp inventories/lab-full-ops/hosts.yml.example /tmp/lab-full-ops-hosts.yml

ansible-playbook \
  -i /tmp/lab-full-ops-hosts.yml \
  playbooks/lab-full-ops-enhanced-upload-limit-incident.yml \
  --syntax-check

rm -f /tmp/lab-full-ops-hosts.yml
```

Expected output:

```text
playbook: playbooks/lab-full-ops-enhanced-upload-limit-incident.yml
```

## API and page paths used

```text
GET  /api/failure-lab/upload-limits
POST /work-orders
POST /work-orders/{id}/evidence       # normal upload
GET  /api/work-orders/{id}/evidence-files
GET  /api/work-orders/{id}/evidence-files/{evidenceId}/consistency
POST /work-orders/{id}/evidence       # oversized upload
```

## Evidence locations

The playbook writes response files on `nginx-01`:

```text
/tmp/multitier-ops-platform/lab-full-ops-upload-limit-incident/upload-limits.json
/tmp/multitier-ops-platform/lab-full-ops-upload-limit-incident/work-order-create.headers
/tmp/multitier-ops-platform/lab-full-ops-upload-limit-incident/work-order-create.html
/tmp/multitier-ops-platform/lab-full-ops-upload-limit-incident/normal-upload.headers
/tmp/multitier-ops-platform/lab-full-ops-upload-limit-incident/normal-upload.html
/tmp/multitier-ops-platform/lab-full-ops-upload-limit-incident/evidence-list-after-normal.json
/tmp/multitier-ops-platform/lab-full-ops-upload-limit-incident/normal-evidence-consistency.json
/tmp/multitier-ops-platform/lab-full-ops-upload-limit-incident/oversized-upload.headers
/tmp/multitier-ops-platform/lab-full-ops-upload-limit-incident/oversized-upload.html
/tmp/multitier-ops-platform/lab-full-ops-upload-limit-incident/oversized-upload.stderr
/tmp/multitier-ops-platform/lab-full-ops-upload-limit-incident/evidence-list-after-oversized.json
/tmp/multitier-ops-platform/lab-full-ops-upload-limit-incident/parsed-values.env
```

Final report:

```text
/tmp/multitier-ops-platform/lab-full-ops-upload-limit-incident-nginx-01.txt
```

## Minimum success criteria

A successful validation should show:

```text
normal_upload_http_code=302
normal_consistency_status=consistent
normal_consistent=true
oversized_http_code=4xx or 5xx
oversized_metadata_match_count=0
classified_failure_layer is not UNEXPECTED_NOT_REJECTED
DB direct check has the normal evidence row
DB direct check has zero oversized evidence rows
NFS direct check finds the normal file object
NFS direct check finds zero oversized file objects
Nginx access log contains the request IDs
```

## Failure interpretation

| Observation | Likely meaning | First checks |
|---|---|---|
| Oversized upload returns 413 and `upstream_status="-"` | Nginx rejected request before forwarding to WAS | Nginx site config, default body limit, error log |
| Oversized upload returns 413/500 with upstream status | Request reached WAS and was rejected by multipart/app handling | app journald, Spring multipart limits |
| Oversized upload returns 302 and creates metadata | Upload limit did not trigger | file size, Nginx body limit, WAS multipart limit |
| Oversized upload rejected but metadata exists | DB side effect occurred despite failure | app transaction/order of operations |
| Oversized upload rejected but NFS object exists | possible orphan file object | app cleanup logic, NFS path |
| Normal upload fails | baseline service/storage problem, not upload-limit incident | run enhanced workflow validation first |

## Safe interview claim after runtime evidence exists

Use this only after the playbook succeeds during an AWS runtime window and evidence is documented:

```text
증빙 파일 업로드 실패를 Nginx 요청 크기 제한, WAS multipart 제한, DB metadata 생성 여부, NFS file object 생성 여부로 나누어 확인했습니다. 정상 업로드는 metadata와 NFS object가 일치했고, 제한 초과 업로드는 어느 계층에서 거부됐는지 로그와 HTTP status로 분류했으며, 실패 요청이 DB/NFS에 부정합을 남기지 않았는지도 확인했습니다.
```

## Claims to avoid

Do not claim:

```text
all upload failure cases are covered
large-file performance tuning is complete
production upload hardening is complete
Nginx/WAS upload limits are optimally tuned
security validation for uploaded files is complete
```

## Runtime batching policy

This playbook should run after the normal enhanced workflow validation in one planned runtime window:

```text
1. apply once
2. configure DB/NFS/app/Nginx
3. run enhanced service workflow validation
4. run upload-limit incident validation
5. collect evidence
6. destroy once
```

Do not create and destroy AWS resources solely for this playbook.
