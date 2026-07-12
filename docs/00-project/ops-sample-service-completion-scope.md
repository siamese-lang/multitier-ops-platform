# ops-sample-service completion scope

This document defines `ops-sample-service` as the lightweight web operations service used by this portfolio.

## Why this change was needed

The infrastructure, backup, restore, and observability evidence already proved meaningful operating behaviors.

However, the operated service also needed to be clear enough to answer this interview question:

```text
What service did you operate?
```

The previous framing, `controlled workload`, was technically accurate but too defensive for portfolio presentation. The service did not need to become a production-grade business system, but it needed enough web-based workflow to connect WEB/WAS operating issues to service behavior.

## Target service identity

Use this identity going forward:

```text
운영 작업 요청과 증빙 파일을 관리하는 경량 웹 업무 서비스
```

English description:

```text
A lightweight web service for managing operations work orders and evidence files.
```

Interview-safe description:

```text
작업 요청과 증빙 파일을 다루는 경량 웹 업무 서비스를 운영 대상으로 구성했습니다. 사용자는 작업 요청을 등록하고, 운영자는 상태를 변경하며, 조치 메모와 증빙 파일을 남길 수 있습니다. PostgreSQL에는 작업 요청과 파일 메타데이터를 저장하고, NFS에는 실제 첨부파일을 저장하도록 분리했습니다. 이 서비스를 Nginx, Spring Boot WAS, PostgreSQL, NFS 환경에 배포하고 WEB/WAS 운영 문제와 백업·복구 절차를 검증했습니다.
```

Important distinction:

```text
Repository implementation baseline: completed.
Enhanced AWS runtime validation: pending.
```

## What this service is not

Do not describe it as:

```text
production service
commercial ITSM system
full asset management system
complete groupware or ERP
OpenKoda-based service
large-scale enterprise web application
```

It is intentionally a lightweight service. Its purpose is to make WEB/WAS/DB/file-storage operating scenarios realistic enough to analyze.

## Implemented user-facing workflow

The service supports this basic workflow in code:

```text
1. A requester creates an operations work order.
2. An operator opens the work order detail page.
3. The operator changes the work order status.
4. The operator leaves an action memo during the status change.
5. The operator uploads an evidence file.
6. The service stores metadata in PostgreSQL and the file object on the configured file storage path.
7. The service can download the evidence file.
8. The service can show work order history, audit logs, and file consistency links.
```

This is enough to explain what the service does without claiming full business-system maturity.

## Implemented web pages and endpoints

Avoid a SPA or heavy frontend. Server-rendered HTML with Spring Boot and Thymeleaf is sufficient.

Implemented pages:

| Page | Path | Purpose |
|---|---|---|
| Root redirect | `GET /` | Redirect to `/work-orders` |
| Work order list | `GET /work-orders` | Show work orders, status summary, and recent audit logs |
| Work order detail | `GET /work-orders/{id}` | Show description, status history, evidence files, and operational links |
| New work order | `GET /work-orders/new` | Show create form |
| Create work order | `POST /work-orders` | Insert work order row |
| Change status | `POST /work-orders/{id}/status` | Change status and create event/audit history |
| Upload evidence | `POST /work-orders/{id}/evidence` | Store file and metadata |
| Download evidence | `GET /work-orders/{id}/evidence/{evidenceId}/download` | Download stored file object |
| Failure lab | `GET /ops/failure-lab` | Link operating scenarios and diagnostic endpoints |

Implemented failure-lab APIs:

```text
GET /api/failure-lab/sleep?millis=3000
GET /api/failure-lab/db-sleep?millis=3000
GET /api/failure-lab/file-storage-check
GET /api/failure-lab/upload-limits
```

Existing JSON APIs remain for validation playbooks. The web pages make the service explainable to human reviewers.

## Implemented data model

The existing DB/file consistency model was extended rather than replaced.

### `ops_work_orders`

```text
id
title
description
status
priority
requester
assignee
created_at
updated_at
```

Allowed statuses:

```text
OPEN
IN_PROGRESS
DONE
FAILED
CANCELLED
```

### `ops_work_order_events`

```text
id
work_order_id
event_type
from_status
to_status
message
actor
created_at
```

Current event behavior includes creation/seed and status transition events.

### `ops_work_order_evidence_files`

```text
id
work_order_id
file_name
storage_path
size_bytes
sha256
created_by_node
created_at
```

The same metadata table is used for deterministic generated evidence files and user-provided uploaded files.

### `ops_operation_audit_logs`

```text
id
request_id
actor
action
target_type
target_id
result
message
created_at
```

This table does not need to be a full security audit system. It exists to show service-level operation history and connect request logs with DB state.

## Operations scenarios mapped to service behavior

The service must not add features for their own sake. Each feature must support one or more WEB/WAS operating scenarios.

| Service behavior | Operating scenario enabled |
|---|---|
| Work order list and detail | Nginx -> WAS -> DB request path validation |
| Work order creation | DB insert, transaction, request log validation |
| Status change | DB update, event history, audit trail validation |
| Evidence upload | Nginx upload limit, Spring multipart limit, NFS/file write, DB/file consistency |
| Evidence download | Nginx timeout, NFS read failure, missing file object |
| Failure-lab sleep endpoint | Nginx timeout and WAS long-request/thread occupation evidence |
| Failure-lab DB sleep endpoint | DB dependency latency and connection-path observation |
| Failure-lab file storage check | file-storage readiness and permission evidence |
| Failure-lab upload-limit endpoint | WEB/WAS upload-size configuration inspection |

## WEB/WAS operations completion criteria

Repository implementation now satisfies these code-level criteria:

```text
1. A reviewer can open a web page and understand what the service does.
2. Work orders can be created, listed, opened, and updated.
3. Evidence files can be uploaded, stored, and downloaded.
4. PostgreSQL metadata and file objects can be checked together.
5. Request IDs appear in HTTP responses and logs.
6. /healthz and /readyz remain separate.
7. A slow request endpoint exists for timeout/thread behavior.
8. A DB-backed slow path exists for DB dependency latency analysis.
```

Still pending as runtime/evidence criteria:

```text
1. Validate enhanced web workflow through Nginx on AWS runtime.
2. Validate upload/download through Nginx -> WAS -> NFS/file storage -> PostgreSQL metadata.
3. Validate failure-lab sleep/db-sleep paths with Nginx and app logs.
4. Refresh backup/restore validation against the enhanced service model.
```

## Implementation boundaries

Keep the service small.

Do not add:

```text
full login/session system
complex RBAC
SPA frontend
OAuth/OIDC
admin dashboard framework
notification system
approval workflow engine
multi-tenant organization management
commercial ITSM features
```

Acceptable simplification:

```text
Use plain requester/assignee text fields instead of real user accounts.
Use simple server-rendered HTML.
Use fixed actor names for operator actions.
Use a small set of seed data.
Use basic validation instead of complex business rules.
```

These simplifications are acceptable because the project is a WEB/WAS operations portfolio, not an application-development portfolio.

## Completed Codex-friendly PR sequence

Implemented service PRs:

```text
PR #141 [APP] Add work order domain and schema
PR #142 [APP] Add basic web workflow pages
PR #143 [APP] Add user evidence upload and download workflow
PR #144 [APP] Add WEB/WAS failure lab endpoints
```

## Next PR sequence

### PR 5. WEB/WAS scenario validation prep

```text
Add or update Ansible validation for:
- page smoke test
- create work order
- upload evidence
- download evidence
- status change
- request ID log trace
- slow endpoint response evidence
```

### PR 6. Runtime validation window

```text
Run one planned runtime validation window only after static validation prep is ready.
Validate enhanced web workflow and failure-lab paths.
Collect evidence and destroy once.
```

### PR 7. Recovery evidence refresh

```text
Validate backup and restore against the enhanced service model.
Refresh evidence docs and evidence index.
```

## Existing evidence reuse

Reusable evidence concepts:

```text
WEB/WAS/DB request path
DB/file consistency
backup artifact creation
restore-lab recovery
logs/service/request-path observability
Prometheus DB service incident diagnosis
Prometheus alert rule evaluation
```

Evidence that must be refreshed after service enhancement:

```text
work order workflow evidence
web page smoke evidence
upload/download evidence
status history evidence
recovery validation using enhanced tables
WEB/WAS timeout or slow request evidence
```

## New project positioning after implementation

Use this positioning for repository implementation status:

```text
작업 요청·증빙 파일 관리형 경량 웹 업무 서비스를 Nginx, Spring Boot WAS, PostgreSQL, NFS, backup, Prometheus 계층으로 분리 구성하기 위한 서비스 구현 baseline을 완료했고, WEB/WAS 운영 문제와 백업·복구 절차를 검증할 runtime evidence를 갱신하는 단계입니다.
```

Use the stronger positioning only after enhanced runtime evidence exists:

```text
작업 요청·증빙 파일 관리형 경량 웹 업무 서비스를 Nginx, Spring Boot WAS, PostgreSQL, NFS, backup, Prometheus 계층으로 분리 구성하고, WEB/WAS 운영 문제와 백업·복구 절차를 evidence로 검증한 프로젝트
```

## Current status

```text
Status: service implementation baseline completed
Runtime required now: not for docs/static prep
Next default work: enhanced-service Ansible validation prep
AWS runtime: only after validation prep is ready for one planned validation window
```
