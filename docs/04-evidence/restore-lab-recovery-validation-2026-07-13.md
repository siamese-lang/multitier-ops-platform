# Restore-lab Recovery Validation - 2026-07-13

## Scope

This document records the restore-lab recovery validation for the project.

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

The validation proves that backup artifacts from lab-full-ops were restored into an isolated restore-lab runtime and verified through both DB/file-level checks and WEB/WAS HTTP/API checks.

This is not a production disaster recovery, automated failover, RPO/RTO, or high availability claim.

## Runtime boundary

```text
source_environment=lab-full-ops
restore_environment=restore-lab
source_backup_id=lab-full-ops-backup-20260712T182247
restore_evidence_root=/mnt/c/Project/test/multitier-ops-platform/.tmp/restore-lab-runtime-20260713T091446
```

## DB/file restore baseline

```text
source_backup_id=lab-full-ops-backup-20260712T182247
pg_restore_status=validated
restic_snapshot_id=e3e2c930
restic_restore_status=validated
metadata_counts=ops_work_order_evidence_files	3,ops_work_orders	13
expected_work_order_count=13
actual_work_order_count=13
expected_evidence_file_count=3
actual_evidence_file_count=3
restored_file_count=4
sample_storage_path=work-order-13/upload-c7885bae-4e9d-421e-bcf4-5fd0c3b34532-normal-upload-limit-evidence.txt
expected_sample_size_bytes=65536
actual_sample_size_bytes=65536
expected_sample_sha256=888492745fd474029b0fac220b6aa228a1a51f82909ad99a1e46fca04bf26809
actual_sample_sha256=888492745fd474029b0fac220b6aa228a1a51f82909ad99a1e46fca04bf26809
db_file_restore_status=validated
http_api_restore_status=not_validated_by_this_playbook
```

The DB/file restore baseline verified restored PostgreSQL metadata, restored NFS file objects, restored row counts, sample file size, and sample SHA-256 checksum.

## HTTP/API consistency validation

```text
source_backup_id=lab-full-ops-backup-20260712T182247
sample_work_order_id=13
sample_evidence_id=3
sample_storage_path=work-order-13/upload-c7885bae-4e9d-421e-bcf4-5fd0c3b34532-normal-upload-limit-evidence.txt
expected_work_order_count=13
restored_work_order_count=13
expected_evidence_count=1
restored_evidence_count=1
api_consistency_status=consistent
api_consistent=true
file_exists=true
size_matches=true
checksum_matches=true
expected_sha256=888492745fd474029b0fac220b6aa228a1a51f82909ad99a1e46fca04bf26809
actual_sha256=888492745fd474029b0fac220b6aa228a1a51f82909ad99a1e46fca04bf26809
app_resolved_path=/mnt/ops-sample/files/work-order-13/upload-c7885bae-4e9d-421e-bcf4-5fd0c3b34532-normal-upload-limit-evidence.txt
db_file_restore_status=validated_by_restore_baseline_prerequisite
http_api_restore_status=validated
recovery_status=validated_when_this_playbook_and_prior_db_file_restore_succeed
```

Direct DB metadata row:

```text
3	13	work-order-13/upload-c7885bae-4e9d-421e-bcf4-5fd0c3b34532-normal-upload-limit-evidence.txt	65536	888492745fd474029b0fac220b6aa228a1a51f82909ad99a1e46fca04bf26809
```

Direct NFS file object check:

```text
file=/srv/ops-sample/files/work-order-13/upload-c7885bae-4e9d-421e-bcf4-5fd0c3b34532-normal-upload-limit-evidence.txt
```

## Evidence conclusion

```text
restore-lab recovery validation status=validated
```

The recovery proof is valid because both conditions were met.

```text
1. DB/file restore baseline succeeded.
2. HTTP/API consistency check through nginx-01 succeeded against restored data.
```

Supported claim:

```text
A preserved lab-full-ops backup archive was restored into an isolated restore-lab runtime, and the restored PostgreSQL metadata plus NFS file objects were verified through both direct DB/NFS checks and WEB/WAS HTTP/API consistency checks.
```

Unsupported claims:

```text
- Production disaster recovery
- Automated failover
- RPO/RTO guarantee
- Multi-AZ high availability
- Continuous backup policy
- Managed database recovery
```

## Raw evidence files

```text
/mnt/c/Project/test/multitier-ops-platform/.tmp/restore-lab-runtime-20260713T091446/reports/restore-lab-db-file-restore-backup-01.txt
/mnt/c/Project/test/multitier-ops-platform/.tmp/restore-lab-runtime-20260713T091446/reports/restore-lab-http-api-consistency-nginx-01.txt
/mnt/c/Project/test/multitier-ops-platform/.tmp/restore-lab-runtime-20260713T091446/ansible
/mnt/c/Project/test/multitier-ops-platform/.tmp/restore-lab-runtime-20260713T091446/reports
/mnt/c/Project/test/multitier-ops-platform/.tmp/restore-lab-runtime-20260713T091446/terraform
```
