# ops-sample-service completion scope

This document redefines `ops-sample-service` from a thin controlled workload into a lightweight web operations service that can be explained clearly in interviews.

## Why this change is needed

The infrastructure, backup, restore, and observability evidence already proves meaningful operating behaviors.

However, the operated service must also be clear enough to answer this interview question:

```text
What service did you operate?
```

The previous framing, `controlled workload`, is technically accurate but too defensive for portfolio presentation. The service does not need to become a production-grade business system, but it must provide enough web-based workflow to connect WEB/WAS operating issues to service behavior.

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

## Minimum user-facing workflow

The service must support this basic workflow:

```text
1. A requester creates an operations work order.
2. An operator opens the work order detail page.
3. The operator changes the work order status.
4. The operator adds an action memo.
5. The operator uploads an evidence file.
6. The service stores metadata in PostgreSQL and the file object on NFS.
7. The service can download the evidence file.
8. The service can show work order history and file consistency status.
```

This is enough to explain what the service does without claiming full business-system maturity.

## Minimum web pages

Avoid a SPA or heavy frontend. Server-rendered HTML with Spring Boot and Thymeleaf is sufficient.

Required pages:

| Page | Path | Purpose |
|---|---|---|
| Work order list | `GET /work-orders` | Show work orders, status, requester, assignee, updated time, file count |
| Work order detail | `GET /work-orders/{id}` | Show description, status, action history, evidence files |
| New work order | `GET /work-orders/new` | Show create form |
| Create work order | `POST /work-orders` | Insert work order row |
| Change status | `POST /work-orders/{id}/status` | Change status and create event history |
| Add action memo | `POST /work-orders/{id}/events` | Add operator note or action record |
| Upload evidence | `POST /work-orders/{id}/evidence` | Store file and metadata |
| Download evidence | `GET /evidence/{id}/download` | Download stored file object |
| Operations dashboard | `GET /ops` | Show version, readiness, DB status, file storage status |
| Failure lab | `GET /ops/failure-lab` | Trigger or link operating scenarios |

Existing JSON APIs can remain for validation playbooks. The web pages make the service explainable to human reviewers.

## Minimum data model

The existing DB/file consistency model should be extended, not replaced.

### `work_orders`

```text
id
title
description
status
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

### `work_order_events`

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

Event types:

```text
CREATED
STATUS_CHANGED
ACTION_MEMO_ADDED
EVIDENCE_UPLOADED
EVIDENCE_DOWNLOADED
RECOVERY_VALIDATED
```

### `evidence_files`

```text
id
work_order_id
original_filename
storage_path
content_type
size_bytes
sha256
uploaded_by
uploaded_at
```

### `operation_audit_logs`

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
| Evidence upload | Nginx upload limit, Spring multipart limit, NFS write, DB/file consistency |
| Evidence download | Nginx timeout, NFS read failure, missing file object |
| Operations dashboard | readiness, dependency visibility, artifact version check |
| Slow report endpoint | Nginx timeout, Tomcat thread occupancy, HikariCP waiting |
| Slow query endpoint | PostgreSQL slow query and index comparison |
| Failure lab | controlled DB/file/latency incidents for evidence collection |

## WEB/WAS operations completion criteria

The service becomes portfolio-ready when these can be demonstrated:

```text
1. A reviewer can open a web page and understand what the service does.
2. Work orders can be created, listed, opened, and updated.
3. Evidence files can be uploaded, stored on NFS, and downloaded.
4. PostgreSQL metadata and NFS file objects can be checked together.
5. Request IDs appear in HTTP responses and logs.
6. /healthz and /readyz remain separate.
7. /ops shows service version and dependency status.
8. A slow request can be used to reproduce timeout/thread behavior.
9. A DB-backed slow path can be used for slow-query or connection-pool analysis.
10. The existing backup/restore validation can be rerun against the enhanced service model.
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

## Codex-friendly PR sequence

Implement in small PRs so the project does not drift.

### PR 1. Service domain and schema

```text
Add or extend tables for work_orders, work_order_events, evidence_files, operation_audit_logs.
Add seed data.
Keep existing API compatibility where possible.
Update service naming from controlled workload to lightweight web operations service.
```

### PR 2. Basic web UI

```text
Add Thymeleaf or simple server-rendered HTML pages:
- work order list
- work order detail
- new work order form
- status change form
- action memo form
```

### PR 3. Evidence upload/download workflow

```text
Add web upload/download flow.
Preserve NFS storage path, size, and SHA-256 metadata.
Show file consistency state on the work order detail page.
```

### PR 4. Operations dashboard and failure lab

```text
Add /ops page.
Add /ops/failure-lab page.
Expose slow request, slow query, DB dependency, and file-storage checks.
Do not add production admin features.
```

### PR 5. WEB/WAS scenario validation

```text
Add or update Ansible validation for:
- page smoke test
- create work order
- upload evidence
- download evidence
- status change
- request ID log trace
- slow endpoint timeout evidence
```

### PR 6. Recovery evidence refresh

```text
Run one planned runtime validation window only after static implementation is ready.
Validate backup and restore against the enhanced service model.
Collect evidence and destroy once.
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

## New project positioning after completion

Use this positioning after the service enhancement is implemented and validated:

```text
작업 요청·증빙 파일 관리형 경량 웹 업무 서비스를 Nginx, Spring Boot WAS, PostgreSQL, NFS, backup, Prometheus 계층으로 분리 구성하고, WEB/WAS 운영 문제와 백업·복구 절차를 evidence로 검증한 프로젝트
```

Do not use this stronger positioning until the web pages, upload/download workflow, status history, and refreshed validation evidence exist.

## Current status

```text
Status: design approved for next implementation phase
Runtime required now: no
Next default work: implement service domain and schema statically
AWS runtime: only after implementation is ready for one planned validation window
```
