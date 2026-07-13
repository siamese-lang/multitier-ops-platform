# Upload limit incident report

## Scenario

A user attempts to upload an evidence file through the enhanced `ops-sample-service` web workflow. The validation goal is to determine whether an upload failure is caused by the WEB tier, the WAS tier, the storage tier, or DB/file consistency behavior.

This scenario belongs to the fixed project theme:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

## User-visible symptom

The user-visible symptom is one of the following:

```text
- file upload succeeds and the evidence file appears on the work-order detail path
- file upload fails because the request exceeds the configured upload limit
- upload appears to reach the application but DB metadata or file object consistency must be checked
```

From an operations perspective, the important point is not only whether the upload succeeds. The important point is whether the operator can tell which layer accepted, rejected, or failed to persist the request.

## Impact scope

Potential impact scope:

```text
WEB tier: Nginx may reject an oversized request before the request reaches WAS.
WAS tier: Spring multipart configuration may reject or fail the upload after Nginx forwards it.
DB tier: PostgreSQL metadata row may or may not be created.
Storage tier: NFS file object may or may not be written.
Application consistency path: DB metadata and file object must match size and SHA-256.
```

## Initial hypotheses

Initial hypotheses to separate:

```text
1. WEB tier request-size limit blocked the upload.
2. WAS multipart limit blocked the upload.
3. File reached WAS but storage write failed.
4. File object was written but DB metadata was not created.
5. DB metadata exists but file object is missing or checksum does not match.
```

## Layer-by-layer checks

### 1. WEB / Nginx

Check:

```text
- HTTP status returned to the caller
- Nginx access log path and status
- Nginx error log if the request is rejected before upstream
- request ID if present
```

Operating question:

```text
Did Nginx reject the request, or did it forward the request to the WAS upstream?
```

### 2. WAS / Spring Boot

Check:

```text
- application request log
- request ID correlation
- multipart size configuration
- upload endpoint result
- application response fields
```

Operating question:

```text
Did the request reach the application process, and did Spring handle or reject the multipart payload?
```

### 3. DB / PostgreSQL metadata

Check:

```text
- evidence metadata row count
- work-order ID and evidence ID
- relative storage path
- stored file size
- stored SHA-256 value
```

Operating question:

```text
Was the file metadata committed to PostgreSQL?
```

### 4. Storage / NFS file object

Check:

```text
- NFS mount path on app-01
- file object existence under the expected relative path
- actual file size
- actual SHA-256 checksum
```

Operating question:

```text
Was the actual file object written to the storage tier, and does it match DB metadata?
```

### 5. Application consistency API

Check:

```text
- fileExists
- sizeMatches
- checksumMatches
- consistent
```

Operating question:

```text
Does the service see DB metadata and NFS file object as one consistent evidence record?
```

## Observed evidence

The enhanced runtime validation completed the upload-limit incident validation as part of the first enhanced validation pass.

Current completed state:

```text
S2 upload-limit incident validation: completed
```

The related restore-lab evidence also confirmed that a restored uploaded evidence file could be checked by DB row, file size, and SHA-256 after recovery.

Representative restored sample from the 2026-07-13 recovery validation:

```text
sample_work_order_id=13
sample_evidence_id=3
expected_sample_size_bytes=65536
actual_sample_size_bytes=65536
size_matches=true
checksum_matches=true
api_consistency_status=consistent
```

## Root-cause judgment

The incident report should not claim a single permanent production root cause. In this project, the root-cause judgment is scenario-specific:

```text
- If Nginx rejects the request, classify it as WEB-tier request-size handling.
- If Nginx forwards the request and Spring rejects it, classify it as WAS multipart limit handling.
- If application handling succeeds but DB/file consistency fails, classify it as DB/storage consistency failure.
```

The validation value is the ability to separate these layers with evidence rather than treating every upload failure as an application bug.

## Action taken

The runtime validation process used controlled upload requests and checked:

```text
- HTTP response behavior
- Nginx request path
- WAS request handling
- PostgreSQL evidence metadata
- NFS file object state
- consistency API result
```

Follow-up fixes from the enhanced validation window are treated as validation-script or environment issues, not exaggerated as production incidents.

## Recovery validation

Recovery validation is complete only when the uploaded evidence file is restored and verified through both direct file checks and application/API checks.

Recovery proof path:

```text
backup archive
-> pg_restore to PostgreSQL
-> restic restore to NFS file tier
-> app-01 reads restored DB metadata and NFS file object
-> nginx-01 HTTPS reverse proxy
-> restored consistency API
-> direct DB row and NFS SHA-256 checks
```

## Remaining limits

This scenario does not prove:

```text
- production upload traffic handling
- WAF or CDN behavior
- large-scale multipart tuning
- object storage operation
- production capacity planning
```

## Interview explanation points

Use this scenario to explain:

```text
파일 업로드 장애를 단순히 애플리케이션 오류로 보지 않고, Nginx에서 차단됐는지, WAS multipart 설정에서 거부됐는지, DB metadata와 NFS file object가 서로 맞는지 순서대로 확인했습니다. 특히 업로드 성공 여부만 본 것이 아니라 PostgreSQL row, NFS file size, SHA-256, consistency API까지 확인해 저장 경로의 정합성을 검증했습니다.
```

Short version:

```text
업로드 장애를 WEB/WAS/DB/NFS 계층으로 나누어 확인하고, 최종적으로 DB metadata와 파일 checksum까지 맞는지 검증했습니다.
```
