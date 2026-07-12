# restore-lab recovery validation - 2026-07-12

## Purpose

Document the first `restore-lab` recovery validation for the fixed project theme:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

This evidence proves the missing step after backup artifact creation: a preserved `lab-full-ops` backup artifact was restored into a separate `restore-lab` environment and then verified through DB, file, checksum, application API, and Nginx reverse-proxy paths.

## Result summary

```text
Backup artifact creation: previously validated in lab-full-ops
Restore-lab DB/file restore: validated
Restore-lab HTTP/API consistency: validated
AWS cleanup: completed by operator after evidence preservation
```

The important boundary is:

```text
Backup artifact creation was not claimed as recovery proof.
Recovery was claimed only after restore-lab DB/file/API validation succeeded.
```

## Source backup artifact

The restore-lab validation used the preserved local backup artifact from Phase 2B:

```text
backup_id=lab-full-ops-backup-20260712T072623
local_restore_input=/tmp/lab-full-ops-backup-validation/lab-full-ops-backup-20260712T072623.tar.gz
pg_dump_sha256=fe58367d5d43101461483a5054da4b8b520d2cc15e1e4c8ce2dc629082f78b0f
restic_snapshot_id=7f063aa1
sample_storage_path=work-order-6/evidence-40219c94-cef8-4c1c-aa07-962938ed4b64.txt
sample_size_bytes=215
sample_sha256=4b4dc6fd2e07d5cd1713f846d9baf4c659209535872c5add945f65f252290150
```

The backup artifact itself was intentionally **not** committed to the repository.

## Runtime profile

The restore-lab window used a separate CIDR and reduced recovery topology:

```text
restore-lab VPC CIDR: 10.60.0.0/16

[Public Subnet]
- bastion-01  public=13.209.14.30  private=10.60.1.32
- nginx-01    public=3.34.196.34   private=10.60.1.241

[Private App Subnet]
- app-01      private=10.60.11.11

[Private DB Subnet]
- db-primary-01 private=10.60.21.4

[Private Storage Subnet]
- nfs-01      private=10.60.31.15

[Private Ops Subnet]
- backup-01   private=10.60.41.41

Temporary validation support:
- NAT Gateway enabled only for this batched restore-lab runtime window
```

The validated recovery path was:

```text
operator -> nginx-01:443 -> app-01:8080 -> db-primary-01:5432
                                 |
                                 -> nfs-01:/srv/ops-sample/files

backup-01 -> db-primary-01:5432       # pg_restore
backup-01 -> nfs-01:/srv/ops-sample/files  # restic restore output copied to target export
```

## Tooling split used during validation

The operator intentionally split local tool usage by terminal environment:

```text
Git Bash: Terraform apply/destroy only
WSL:      Ansible inventory, playbook execution, evidence collection only
```

This distinction matters because SSH keys, `/tmp` paths, and installed tools differ between Git Bash and WSL.

## Validation sequence

The validation window was used for one grouped restore-lab runtime pass:

```text
1. Terraform apply with restore-lab recovery profile
2. Populate lab-full-ops Ansible hosts.yml from restore-lab Terraform outputs
3. Validate Ansible control path to restore-lab nodes
4. Configure PostgreSQL primary
5. Configure NFS storage baseline
6. Fix NFS export CIDR for restore-lab app/backup subnets
7. Fix PostgreSQL pg_hba CIDR for restore-lab app/backup subnets
8. Run DB/file restore baseline from backup-01
9. Patch restore copy to avoid preserving source uid/gid on root_squash NFS
10. Mount app-01 NFS client path
11. Deploy ops-sample-service against restored DB/file tiers
12. Configure Nginx reverse proxy
13. Run HTTP/API consistency validation through Nginx
14. Preserve WSL evidence bundle
15. Terraform destroy after evidence collection
```

No additional AWS runtime windows were created for the follow-up fix PR.

## Runtime findings fixed after validation

The restore-lab run exposed four automation issues:

| Finding | Runtime symptom | Follow-up fix |
|---|---|---|
| NFS export CIDR was source-lab specific | `backup-01` in `10.60.41.0/24` could not mount `nfs-01` | make storage CIDRs profile-aware |
| PostgreSQL client CIDR was source-lab specific | `pg_restore` from `10.60.41.41` failed with `no pg_hba.conf entry` | make DB client CIDRs profile-aware |
| tar extraction preserved source uid/gid | root_squash NFS rejected ownership restore | use `tar --no-same-owner --no-same-permissions -xf -` |
| summary total parsing double-counted buckets | API `data.total=6`, but playbook calculated `6+4+1+1+0=12` | read `data.total` directly |

These fixes were merged separately in:

```text
PR #119 [FIX] Make restore-lab runtime validation profile-aware
```

## DB/file restore result

The DB/file restore baseline ran on `backup-01` and completed successfully after the restore-lab runtime fixes were applied locally:

```text
PLAY RECAP
backup-01 : ok=... unreachable=0 failed=0
```

Key restore report fields:

```text
project=multitier-ops-platform
source_environment=lab-full-ops
restore_environment=restore-lab
inventory_hostname=backup-01
ansible_host=10.60.41.41
source_backup_id=lab-full-ops-backup-20260712T072623
pg_restore_status=validated
db_host=10.60.21.4
db_name=opsdb
expected_work_order_count=6
actual_work_order_count=6
expected_evidence_file_count=1
actual_evidence_file_count=1
restic_snapshot_id=7f063aa1
restic_restore_status=validated
nfs_target=10.60.31.15:/srv/ops-sample/files
sample_storage_path=work-order-6/evidence-40219c94-cef8-4c1c-aa07-962938ed4b64.txt
expected_sample_size_bytes=215
actual_sample_size_bytes=215
expected_sample_sha256=4b4dc6fd2e07d5cd1713f846d9baf4c659209535872c5add945f65f252290150
actual_sample_sha256=4b4dc6fd2e07d5cd1713f846d9baf4c659209535872c5add945f65f252290150
db_file_restore_status=validated
http_api_restore_status=not_validated_by_this_playbook
```

## HTTP/API consistency result

The HTTP/API validation ran from `nginx-01` and completed successfully:

```text
PLAY RECAP
nginx-01 : ok=13 changed=1 unreachable=0 failed=0 skipped=0 rescued=0 ignored=0
```

The final report showed:

```text
project=multitier-ops-platform
environment=restore-lab
inventory_hostname=nginx-01
ansible_host=10.60.1.241
request_prefix=restore-lab-recovery
base_url=https://127.0.0.1
source_backup_id=lab-full-ops-backup-20260712T072623
sample_work_order_id=6
sample_evidence_id=1
sample_storage_path=work-order-6/evidence-40219c94-cef8-4c1c-aa07-962938ed4b64.txt
expected_work_order_count=6
restored_work_order_count=6
expected_evidence_count=1
restored_evidence_count=1
api_consistency_status=consistent
api_consistent=true
file_exists=true
size_matches=true
checksum_matches=true
expected_sha256=4b4dc6fd2e07d5cd1713f846d9baf4c659209535872c5add945f65f252290150
actual_sha256=4b4dc6fd2e07d5cd1713f846d9baf4c659209535872c5add945f65f252290150
app_resolved_path=/mnt/ops-sample/files/work-order-6/evidence-40219c94-cef8-4c1c-aa07-962938ed4b64.txt
db_metadata_row=1\t6\twork-order-6/evidence-40219c94-cef8-4c1c-aa07-962938ed4b64.txt\t215\t4b4dc6fd2e07d5cd1713f846d9baf4c659209535872c5add945f65f252290150
nfs_file_object_check=file=/srv/ops-sample/files/work-order-6/evidence-40219c94-cef8-4c1c-aa07-962938ed4b64.txt
size=215
sha256=4b4dc6fd2e07d5cd1713f846d9baf4c659209535872c5add945f65f252290150
http_api_restore_status=validated
recovery_status=validated_when_this_playbook_and_prior_db_file_restore_succeed
```

The Nginx access log evidence showed the expected restore-lab request IDs with HTTP 200 responses and upstream routing to `10.60.11.11:8080`:

```text
request_id=restore-lab-recovery-healthz                 uri="/healthz"                                      status=200 upstream_addr="10.60.11.11:8080"
request_id=restore-lab-recovery-readyz                  uri="/readyz"                                       status=200 upstream_addr="10.60.11.11:8080"
request_id=restore-lab-recovery-summary                 uri="/api/work-orders/summary"                     status=200 upstream_addr="10.60.11.11:8080"
request_id=restore-lab-recovery-list-restored-evidence  uri="/api/work-orders/6/evidence-files"            status=200 upstream_addr="10.60.11.11:8080"
request_id=restore-lab-recovery-restored-consistency    uri="/api/work-orders/6/evidence-files/1/consistency" status=200 upstream_addr="10.60.11.11:8080"
```

## Direct DB and NFS final-state checks

PostgreSQL access rules after restore-lab fixes:

```text
host    opsdb    ops_user    10.60.41.0/24    scram-sha-256
host    opsdb    ops_user    10.60.11.0/24    scram-sha-256
```

NFS export rules after restore-lab fixes:

```text
/srv/ops-sample/files 10.60.11.0/24(rw,sync,no_subtree_check,root_squash)
/srv/ops-sample/files 10.60.41.0/24(rw,sync,no_subtree_check,root_squash)
```

Restored NFS file object:

```text
work-order-6/evidence-40219c94-cef8-4c1c-aa07-962938ed4b64.txt  215
4b4dc6fd2e07d5cd1713f846d9baf4c659209535872c5add945f65f252290150  /srv/ops-sample/files/work-order-6/evidence-40219c94-cef8-4c1c-aa07-962938ed4b64.txt
```

## Success criteria mapping

| Criterion | Evidence | Result |
|---|---|---|
| Restore uses preserved Phase 2B artifact | `source_backup_id=lab-full-ops-backup-20260712T072623` | met |
| Restore runs in a separate environment | `restore_environment=restore-lab`, `10.60.0.0/16` runtime profile | met |
| PostgreSQL dump restored | `pg_restore_status=validated` | met |
| Work order count restored | `actual_work_order_count=6` | met |
| Evidence metadata restored | `actual_evidence_file_count=1` | met |
| NFS file object restored | `file_exists=true`, direct NFS file check | met |
| File size matches | `actual_sample_size_bytes=215`, `size_matches=true` | met |
| File checksum matches | `actual_sha256=4b4dc6...`, `checksum_matches=true` | met |
| App reads restored DB/file data | `api_consistency_status=consistent` | met |
| Nginx path reaches app | request IDs return `status=200`, `upstream_addr=10.60.11.11:8080` | met |
| AWS cleanup completed | operator reported Terraform resource deletion completed | met |

## Preserved local evidence files

The operator preserved the following WSL-side evidence files under `/tmp/restore-lab-recovery-validation`:

```text
ansible-baseline-check.txt
ansible-ping.txt
app-nfs-mount-baseline.txt
backup-restore-target-unmount-before-rerun.txt
backup-restore-target-unmount-before-tar-fix-rerun.txt
nfs-export-after-restore-fix.txt
nfs-export-before-restore-fix.txt
nfs-storage-baseline-restore-cidr-fix.txt
nfs-storage-baseline.txt
nginx-reverse-proxy.txt
ops-sample-service.txt
postgresql-pg-hba-after-app-backup-restore-fix.txt
postgresql-pg-hba-after-restore-fix.txt
postgresql-pg-hba-before-restore-fix.txt
postgresql-primary-restore-cidr-fix-app-and-backup.txt
postgresql-primary-restore-cidr-fix.txt
postgresql-primary.txt
restore-db-count-debug.txt
restore-db-file-baseline.txt
restore-db-file-remote-report.txt
restore-db-final-state.txt
restore-http-api-consistency.txt
restore-http-api-json-debug.txt
restore-http-api-remote-report.txt
restore-lab-runtime-local-fixes.diff
restore-nfs-final-state.txt
restore-nginx-access-log-final.txt
```

The operator also preserved the WSL evidence bundle:

```text
/tmp/restore-lab-recovery-validation-20260712.tar.gz  12K
```

The evidence archive was intentionally **not** committed to the repository.

## Cleanup status

The operator reported that Terraform resource deletion completed after evidence preservation.

The submitted post-cleanup output included the local evidence directory and evidence bundle. It did not include a pasted `terraform state list` after destroy output in this chat segment, so this document records cleanup as operator-reported rather than independently transcribed from state output.

## Final claim

This runtime window supports the following project claim:

```text
Restore-lab DB/file/API recovery validation succeeded.
Backup artifact creation had already been validated separately in Phase 2B.
Restore was validated separately in restore-lab on 2026-07-12.
```
