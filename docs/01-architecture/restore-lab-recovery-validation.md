# restore-lab DB/file recovery validation design

## Status

Proposed design for the next required recovery milestone.

This document does not implement Terraform, Ansible, or runtime restore steps. It defines the recovery validation path that should be implemented next.

## Purpose

The fixed project theme remains:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

Phase 2B validated backup artifact creation for the already validated DB/file workload. That is necessary, but it is not recovery proof.

The purpose of `restore-lab` is to prove that preserved backup artifacts can rebuild a working DB/file-backed system in a separate environment and that the restored system can pass application-level consistency checks.

## Source evidence

The restore design starts from the backup validation evidence collected on 2026-07-12:

```text
backup_id=lab-full-ops-backup-20260712T072623
pg_dump_size_bytes=7479
pg_dump_sha256=fe58367d5d43101461483a5054da4b8b520d2cc15e1e4c8ce2dc629082f78b0f
nfs_file_count=2
restic_snapshot_id=7f063aa1
metadata_counts=ops_work_order_evidence_files 1, ops_work_orders 6
sample_evidence_sha256=4b4dc6fd2e07d5cd1713f846d9baf4c659209535872c5add945f65f252290150
restore_status=not_validated
```

Primary evidence document:

```text
docs/04-evidence/lab-full-ops-backup-validation-2026-07-12.md
```

Local artifacts preserved by the operator before destroying the source lab:

```text
/tmp/lab-full-ops-backup-validation/lab-full-ops-backup-20260712T072623.tar.gz
/tmp/lab-full-ops-backup-validation-lab-full-ops-backup-20260712T072623.tar.gz
```

These archives must not be committed to Git. They are local restore inputs for the runtime validation window.

## Recovery claim boundary

A restore-lab run may claim recovery only after all of the following are true:

```text
PostgreSQL dump restored into a fresh database tier
NFS-backed file objects restored into a fresh file tier
restored metadata row counts match the expected backup evidence
sample restored evidence file exists at the expected storage_path
sample restored evidence file SHA-256 matches the backed-up SHA-256
application consistency endpoint reports the restored DB/file pair as consistent
HTTP path through Nginx reaches the restored application
restore-lab resources are destroyed after evidence collection
```

A restore-lab run must not claim recovery from these alone:

```text
backup archive exists locally
pg_restore command exits successfully but app consistency is untested
restic restore command exits successfully but DB metadata is untested
DB row counts exist but file checksums are unverified
file checksums match but HTTP/API path is unverified
```

## Minimal restore-lab topology

The first restore-lab should be intentionally small. It should recreate only the tiers needed to prove recovery.

```text
[Public Subnet]
- bastion-01
- nginx-01

[Private App Subnet]
- app-01

[Private DB Subnet]
- db-primary-01

[Private Storage Subnet]
- nfs-01

[Private Ops Subnet]
- backup-01
```

This mirrors the reduced `lab-full-ops` validation topology. Optional nodes such as `app-02`, `mon-01`, `log-01`, and `loadgen-01` are out of scope for the first restore proof.

## Preferred implementation approach

Use a separate restore-lab environment or profile so the recovery run is not confused with the original source validation.

Acceptable implementation options:

```text
Option A: new Terraform env at infra/terraform/envs/restore-lab
Option B: lab-full-ops Terraform code reused with restore-lab-specific variables, names, tags, and evidence docs
```

The first implementation should prefer the smallest change that still makes the restored environment clearly separate from the source lab.

Required distinction from the source lab:

```text
environment_name=restore-lab or equivalent tag/name marker
separate Ansible inventory entry point or documented restore-lab inventory procedure
separate evidence document under docs/04-evidence
source lab must already be destroyed or explicitly isolated
```

## Artifact injection decision

For the first restore-lab run, use the preserved local archive approach.

Selected approach:

```text
operator WSL local archive -> backup-01:/srv/ops-restore/input
```

Rationale:

```text
The backup validation already preserved the archive locally before source-lab destroy.
It avoids committing binary backup artifacts to Git.
It avoids introducing S3 or another managed service as a core operating dependency.
It keeps the workload recovery behavior VM-based.
```

Future alternatives such as a dedicated preserved EBS volume or an object-store artifact bucket can be considered later, but they should not be introduced before the first restore proof unless the local archive approach fails.

Expected local input:

```text
/tmp/lab-full-ops-backup-validation/lab-full-ops-backup-20260712T072623.tar.gz
```

Expected remote staging path:

```text
/srv/ops-restore/input/lab-full-ops-backup-20260712T072623.tar.gz
/srv/ops-restore/unpacked/lab-full-ops/...
```

## Restore operating path

The restored runtime path should be:

```text
operator -> nginx-01:443 -> app-01:8080 -> db-primary-01:5432
                                 |
                                 -> nfs-01:/srv/ops-sample/files

backup-01 -> db-primary-01:5432       # pg_restore or psql restore coordination
backup-01 -> nfs-01:/srv/ops-sample/files  # restic restore target path
```

## Restore sequence

The runtime restore validation should be one grouped AWS window, not many small apply/destroy cycles.

```text
1. Create restore-lab infrastructure.
2. Populate restore-lab Ansible inventory from Terraform outputs.
3. Validate Ansible control path.
4. Configure PostgreSQL primary on db-primary-01.
5. Configure NFS export on nfs-01.
6. Configure app-01 NFS mount.
7. Copy local backup artifact archive from WSL to backup-01.
8. Unpack the backup artifact archive on backup-01.
9. Restore opsdb from the pg_dump artifact.
10. Restore file objects from the restic repository into the NFS-backed file root.
11. Deploy ops-sample-service against the restored DB and file root.
12. Configure Nginx reverse proxy to app-01.
13. Run DB/file consistency validation.
14. Run HTTP path validation through Nginx.
15. Capture restore evidence.
16. Destroy restore-lab resources.
17. Capture post-destroy Terraform state list.
```

## Restore order details

### DB restore

The backup artifact contains a custom-format `pg_dump` file:

```text
opsdb-lab-full-ops-backup-20260712T072623.dump
```

The restore procedure should either:

```text
A. create an empty opsdb with schema ownership and restore with pg_restore
B. drop/recreate opsdb within the restore-lab DB node before pg_restore
```

The selected procedure must be idempotent enough for one validation rerun inside the same restore-lab window.

Minimum command evidence:

```text
pg_restore command and exit code
restored table list
restored row counts for ops_work_orders and ops_work_order_evidence_files
sample evidence metadata row including storage_path, size_bytes, and sha256
```

### File restore

The backup artifact contains the restic repository created during backup validation.

The restore procedure should restore into the NFS-backed file root used by the application:

```text
nfs-01:/srv/ops-sample/files
app-01 mount path: /mnt/ops-sample/files
```

Minimum command evidence:

```text
restic snapshots output showing snapshot 7f063aa1 or a matching full snapshot ID
restic restore command and exit code
restored file list
sha256sum for the sample evidence file
file ownership and read/write path check as needed
```

The first restore-lab validation can restore the full restic snapshot. Selective restore is not required unless the full snapshot restore fails or complicates the first proof.

## Validation checks

The restore-lab validation should check both infrastructure state and application-level consistency.

### Infrastructure checks

```text
Ansible ping for restore-lab nodes
PostgreSQL service active
NFS service active
app-01 NFS mount present
ops-sample-service active
Nginx active
```

### Data checks

```text
ops_work_orders row count equals expected value from backup evidence: 6
ops_work_order_evidence_files row count equals expected value from backup evidence: 1
sample storage_path exists in restored DB metadata
sample file exists under restored NFS root
sample file size equals expected value: 215
sample file SHA-256 equals expected value: 4b4dc6fd2e07d5cd1713f846d9baf4c659209535872c5add945f65f252290150
```

### Application checks

The application must validate restored DB/file consistency rather than only serving health checks.

Required HTTP/API evidence:

```text
/healthz succeeds
/readyz succeeds
/api/work-orders/summary succeeds
restored evidence consistency endpoint returns consistent=true
Nginx access log contains a request ID for the restored validation request
```

The exact endpoint path should follow the existing ops-sample-service evidence consistency endpoint used by the storage validation smoke.

## Evidence file requirements

The restore-lab validation should produce one evidence directory and one evidence document.

Suggested local evidence directory:

```text
/tmp/restore-lab-recovery-validation
```

Required evidence files:

```text
terraform-output.txt
terraform-state-list-after-apply.txt
ansible-inventory.json
ansible-ping.txt
restore-lab-baseline-check.txt
postgresql-primary.txt
nfs-storage-baseline.txt
app-nfs-mount-baseline.txt
artifact-upload.txt
artifact-unpack.txt
pg-restore.txt
restic-restore.txt
metadata-counts.txt
sample-file-checksum.txt
ops-sample-service.txt
nginx-reverse-proxy.txt
restore-consistency-check.txt
nginx-request-log.txt
terraform-state-list-after-destroy.txt
```

Required repository evidence document after runtime validation:

```text
docs/04-evidence/restore-lab-recovery-validation-YYYY-MM-DD.md
```

The evidence document must state whether restore succeeded or failed. A partial restore should be documented as a recovery gap, not hidden.

## Out of scope for the first restore-lab proof

```text
Prometheus, Grafana, Loki, or Alloy setup
loadgen-01 traffic generation
app-02 or rolling restart behavior
PostgreSQL standby or failover
OpenKoda feature or UI work
managed database or managed file storage replacement
S3-backed application storage
RTO/RPO claims beyond observed manual validation timing
```

## Failure interpretation guide

| Failure point | Likely layer | First checks |
|---|---|---|
| artifact upload fails | control path/local path | WSL archive path, backup-01 SSH, disk space |
| archive unpack fails | backup node | tar file integrity, target directory permissions |
| pg_restore fails | DB restore | role ownership, database existence, dump format, PostgreSQL version |
| metadata counts mismatch | DB/data | wrong dump, partial restore, schema mismatch |
| restic snapshots unavailable | backup artifact | restic repository path, password, archive unpack path |
| restic restore fails | file restore | target path permissions, NFS mount/export, restic password |
| checksum mismatch | file/data integrity | wrong restored path, stale file, partial restore, content corruption |
| app ready fails | app/DB config | env file, DB password, service journal, schema availability |
| consistency endpoint false | DB/file mismatch | metadata storage_path, file existence, checksum |
| Nginx path fails | WEB/WAS path | upstream config, app port, security group, Nginx logs |

## Runtime policy

Do not open AWS for this design PR.

When implementation begins, use one batched runtime window:

```text
apply once -> restore validation -> collect evidence -> destroy once
```

If NAT Gateway is enabled for package installation, keep the window short and destroy immediately after evidence collection.

## Next implementation PRs

Recommended order:

```text
1. [TF] Add restore-lab minimal recovery profile
2. [ANSIBLE] Add restore-lab artifact injection and DB/file restore baseline
3. [VALIDATION] Restore-lab DB/file/API consistency evidence
```

The first implementation PR may combine Terraform and Ansible only if the change remains reviewable and the runtime validation plan is explicit.
