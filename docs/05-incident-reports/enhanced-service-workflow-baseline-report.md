# Enhanced service workflow baseline report

## Scenario

The enhanced `ops-sample-service` is validated as a lightweight operations work-order and evidence-file web service. This baseline is necessary before interpreting upload, latency, DB-impact, and recovery scenarios.

The purpose is to prove that the infrastructure is not only serving a health-check API. It is operating a small but explainable web workload with DB-backed state, audit history, evidence upload/download, and DB/file consistency behavior.

## User-visible symptom

This is a baseline validation, not a failure symptom.

Expected user-visible behavior:

```text
- work-order list page renders through Nginx and WAS
- new work-order form renders
- work order can be created
- work-order status can be changed
- event history and audit logs are visible
- evidence file can be uploaded and downloaded
- DB metadata and NFS file object consistency can be checked
```

## Impact scope

The baseline covers the main operating path:

```text
operator/browser-style request
-> nginx-01 HTTPS reverse proxy
-> app-01 Spring Boot/Tomcat WAS
-> PostgreSQL work-order/event/audit/file metadata
-> NFS evidence file object path
```

## Initial hypotheses

Before incident scenarios, the operator must prove that the normal path works.

Baseline hypotheses:

```text
1. Nginx can route web workflow requests to app-01.
2. The application can render work-order web pages.
3. The application can create and update PostgreSQL-backed work-order records.
4. Status events and audit logs are recorded.
5. Evidence upload writes a file object and metadata.
6. Evidence download and consistency check work through the service path.
7. Request IDs can correlate HTTP calls, Nginx logs, app logs, and audit records.
```

## Layer-by-layer checks

### 1. WEB / Nginx

Check:

```text
- web page response status
- reverse proxy routing to app-01
- access log path/status/upstream
- request ID if provided
```

Question:

```text
Does the WEB tier route both HTML workflow and API requests to the WAS tier?
```

### 2. WAS / Spring Boot

Check:

```text
- app process health
- /healthz and /readyz
- work-order page rendering
- application request logs
- node identity fields
```

Question:

```text
Is the application serving the business workflow, not only a synthetic health endpoint?
```

### 3. DB / PostgreSQL

Check:

```text
- work-order row creation
- status transition event row
- operation audit log row
- file metadata row if evidence upload is used
```

Question:

```text
Does the workflow create persistent DB-backed state?
```

### 4. Storage / NFS

Check:

```text
- mounted evidence root
- uploaded file object existence
- file size
- SHA-256 checksum
```

Question:

```text
Does the workflow write the actual file object to the storage tier?
```

### 5. Consistency path

Check:

```text
- DB metadata path
- NFS file path
- sizeMatches
- checksumMatches
- consistent
```

Question:

```text
Can the service verify that DB metadata and the file object represent the same evidence file?
```

## Observed evidence

The first enhanced validation pass completed the service workflow baseline:

```text
S1 enhanced service workflow validation: completed
```

The current-state document records the completed validation scope:

```text
S1 enhanced service workflow validation: completed
S2 upload-limit incident validation: completed
S3 latency scenario validation: completed
S4 DB web-impact incident validation: completed
Backup baseline: completed
Restore-lab DB/file restore baseline: completed
Restore-lab HTTP/API consistency validation: completed
```

## Root-cause judgment

There is no failure root cause in this baseline report.

The operating judgment is:

```text
The enhanced service is a valid workload for subsequent WEB/WAS/DB/Storage/Backup operating scenarios because it exercises web pages, DB-backed state, audit/event history, file upload/download, and DB/file consistency behavior.
```

## Action taken

The validation checked the enhanced service workflow before using the same service for incident scenarios:

```text
1. Load work-order list and create page.
2. Create a work order.
3. Change status and record an action message.
4. Read events and audit logs.
5. Upload evidence through the web path.
6. Download or inspect evidence file metadata.
7. Check DB/file consistency.
8. Confirm request logs across WEB/WAS paths.
```

## Recovery validation

This baseline itself is not a recovery scenario. However, it provides the service model later used by backup and restore validation.

Recovery relevance:

```text
If work-order metadata and evidence-file objects can be created during normal operation, restore-lab recovery can later verify that those DB rows and file objects survive backup/restore and remain consistent through the application API.
```

## Remaining limits

This scenario does not prove:

```text
- production-scale web traffic
- authentication or RBAC
- commercial ITSM completeness
- front-end product maturity
- HA deployment architecture
```

The service is intentionally lightweight. Its role is to support operations validation, not to become the final product.

## Interview explanation points

Use this scenario to explain:

```text
처음에는 단순 health-check API만으로는 운영 포트폴리오 설명이 약하다고 보았습니다. 그래서 작업 요청 등록, 상태 변경, 감사 로그, 증빙 파일 업로드/다운로드, DB/file consistency를 갖춘 경량 업무 서비스를 운영 대상으로 보강했습니다. 이 baseline을 통해 이후 업로드 장애, 지연, DB 장애, 백업/복구 시나리오가 실제 업무 흐름과 연결되도록 만들었습니다.
```

Short version:

```text
단순 샘플 API가 아니라 작업 요청과 증빙 파일을 다루는 경량 업무 서비스를 운영 대상으로 만들고, 그 정상 경로를 먼저 검증했습니다.
```
