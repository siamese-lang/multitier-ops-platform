# Restore-lab recovery incident report

## Scenario

A backup artifact from `lab-full-ops` must be restored into an isolated `restore-lab` environment. The validation goal is to prove recovery through restored PostgreSQL metadata, restored NFS file objects, application consistency, and WEB/WAS access through Nginx.

This report is the strongest recovery story in the portfolio because it separates backup artifact creation from actual recovery proof.

## User-visible symptom

The business-facing situation is:

```text
The original runtime is gone or unavailable, and the operator must prove that backed-up work-order metadata and evidence files can be restored into a separate environment.
```

The recovery is not considered valid just because backup files exist.

## Impact scope

Recovery touches all major operating tiers:

```text
Backup artifact: preserved lab-full-ops backup archive
DB tier: PostgreSQL metadata restored with pg_restore
Storage tier: NFS file objects restored with restic
WAS tier: ops-sample-service reads restored metadata and file paths
WEB tier: nginx-01 exposes restored API consistency path
Direct verification: DB row, file size, and SHA-256 checksum
```

## Initial hypotheses

Initial hypotheses:

```text
1. Backup artifact exists but cannot be restored.
2. PostgreSQL metadata restore succeeds, but NFS file object restore fails.
3. DB metadata and NFS file objects restore, but the application cannot read them.
4. Application reads restored data, but WEB/Nginx path does not prove consistency.
5. Direct DB/NFS checks and API checks disagree.
```

## Layer-by-layer checks

### 1. Backup artifact

Check:

```text
- source_backup_id
- archive preservation
- pg_dump and restic snapshot metadata
```

Question:

```text
Which backup artifact is being restored?
```

### 2. DB / PostgreSQL restore

Check:

```text
- pg_restore_status
- restored work-order count
- restored evidence metadata count
- sample DB metadata row
```

Question:

```text
Were PostgreSQL rows restored into the restore-lab DB tier?
```

### 3. Storage / NFS restore

Check:

```text
- restic_restore_status
- restored file count
- sample storage path
- restored file size
- restored SHA-256
```

Question:

```text
Were file objects restored to the NFS file tier and do they match the expected checksum?
```

### 4. WAS / application read path

Check:

```text
- app_resolved_path
- consistency API result
- restored evidence metadata query through app
```

Question:

```text
Can the application resolve restored DB metadata to an actual restored file object?
```

### 5. WEB / Nginx path

Check:

```text
- HTTP/API consistency check through nginx-01
- HTTP status
- request path
- upstream routing
```

Question:

```text
Can the restored data be verified through the WEB/WAS path, not only by direct shell checks?
```

## Observed evidence

The 2026-07-13 restore-lab validation used this backup artifact:

```text
source_backup_id=lab-full-ops-backup-20260712T182247
```

DB/file restore baseline:

```text
pg_restore_status=validated
restic_restore_status=validated
actual_work_order_count=13
actual_evidence_file_count=3
restored_file_count=4
expected_sample_size_bytes=65536
actual_sample_size_bytes=65536
expected_sample_sha256=888492745fd474029b0fac220b6aa228a1a51f82909ad99a1e46fca04bf26809
actual_sample_sha256=888492745fd474029b0fac220b6aa228a1a51f82909ad99a1e46fca04bf26809
```

HTTP/API consistency validation:

```text
sample_work_order_id=13
sample_evidence_id=3
restored_work_order_count=13
restored_evidence_count=1
api_consistency_status=consistent
api_consistent=true
file_exists=true
size_matches=true
checksum_matches=true
http_api_restore_status=validated
recovery_status=validated_when_this_playbook_and_prior_db_file_restore_succeed
```

## Root-cause judgment

This is not a failure root-cause report. It is a recovery proof report.

The operational judgment is:

```text
Backup creation alone is insufficient.
Recovery can be claimed only after DB restore, file restore, application consistency check, Nginx HTTP/API path, and direct DB/NFS checksum checks all agree.
```

## Action taken

The recovery path was validated as:

```text
backup archive
-> pg_restore to PostgreSQL
-> restic restore to NFS file tier
-> ops-sample-service on app-01
-> nginx-01 HTTPS reverse proxy
-> restored work order/evidence consistency API
-> direct DB row and NFS SHA-256 checks
```

## Recovery validation

Recovery validation succeeded because both conditions were met:

```text
1. DB/file restore baseline succeeded.
2. HTTP/API consistency check through nginx-01 succeeded against restored data.
```

The project can safely claim:

```text
A preserved lab-full-ops backup archive was restored into an isolated restore-lab runtime, and the restored PostgreSQL metadata plus NFS file objects were verified through both direct DB/NFS checks and WEB/WAS HTTP/API consistency checks.
```

## Remaining limits

This scenario does not prove:

```text
- production disaster recovery
- automated failover
- RPO/RTO guarantee
- Multi-AZ high availability
- continuous backup policy
- managed database recovery
```

## Interview explanation points

Use this scenario to explain:

```text
백업 파일을 만들었다고 복구가 검증된 것은 아니라고 보고, 별도의 restore-lab 환경에서 PostgreSQL metadata와 NFS file object를 실제로 복원했습니다. 이후 애플리케이션이 복원된 DB row와 파일 경로를 읽는지 확인했고, Nginx를 통한 HTTP/API consistency와 직접 DB/NFS SHA-256 검증까지 맞춰 recovery proof로 판단했습니다.
```

Short version:

```text
백업 생성과 복구 검증을 구분했고, 별도 restore-lab에서 DB row, NFS 파일, checksum, API consistency가 모두 맞는지 확인했습니다.
```
