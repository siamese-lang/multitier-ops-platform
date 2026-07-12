# restore-lab DB/file restore baseline

## Purpose

Restore the preserved `lab-full-ops` backup artifacts into a fresh `restore-lab` runtime environment and verify the DB/file data pair at the operating layer.

This runbook supports the fixed project theme:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

The goal is not to add another Terraform or Ansible showcase. The goal is to turn the already collected backup artifact evidence into a concrete recovery validation path.

## Boundary

This runbook validates:

```text
local preserved backup archive -> backup-01
pg_dump artifact -> db-primary-01 opsdb
restic repository -> nfs-01 file root
PostgreSQL metadata count -> expected backup evidence
sample evidence file object -> expected size and SHA-256
```

This runbook does not fully prove application recovery by itself. Full recovery still requires the restored app and Nginx path to verify HTTP/API consistency after this DB/file restore baseline.

Explicit boundary:

```text
DB/file artifact restore: covered here
HTTP/API restore consistency: follow-up validation
Observability/dashboard work: out of scope
```

## Source backup evidence

The first restore-lab baseline is tied to the preserved backup artifact from 2026-07-12:

```text
backup_id=lab-full-ops-backup-20260712T072623
pg_dump_size_bytes=7479
pg_dump_sha256=fe58367d5d43101461483a5054da4b8b520d2cc15e1e4c8ce2dc629082f78b0f
nfs_file_count=2
restic_snapshot_id=7f063aa1
metadata_counts=ops_work_order_evidence_files 1, ops_work_orders 6
sample_size_bytes=215
sample_sha256=4b4dc6fd2e07d5cd1713f846d9baf4c659209535872c5add945f65f252290150
```

Primary evidence source:

```text
docs/04-evidence/lab-full-ops-backup-validation-2026-07-12.md
```

Required local archive on the WSL operator workstation:

```text
/tmp/lab-full-ops-backup-validation/lab-full-ops-backup-20260712T072623.tar.gz
```

Do not commit this archive to Git.

## Restore topology

The restore-lab profile should create the same reduced recovery topology:

```text
operator -> bastion-01 -> private nodes
operator -> nginx-01:443
nginx-01 -> app-01:8080
app-01 -> db-primary-01:5432
app-01 -> nfs-01:/srv/ops-sample/files
backup-01 -> db-primary-01:5432
backup-01 -> nfs-01:/srv/ops-sample/files
```

The first DB/file restore playbook runs on `backup-01` as the restore coordinator.

## Files

```text
infra/ansible/inventories/lab-full-ops/group_vars/backup.yml
infra/ansible/playbooks/restore-lab-db-file-restore-baseline.yml
```

The playbook name intentionally uses `restore-lab` even though it reuses the existing `lab-full-ops` Ansible inventory structure. The restore-lab distinction comes from the Terraform profile, runtime node IPs, and evidence document.

## Preconditions

Run this only during a planned restore-lab runtime window.

Required sequence before this playbook:

```text
1. restore-lab Terraform profile applied once
2. inventories/lab-full-ops/hosts.yml populated from restore-lab Terraform outputs
3. Ansible ping succeeds for restore-lab nodes
4. db-primary-01 PostgreSQL primary configured
5. nfs-01 NFS export baseline configured
6. backup-01 package access available through the planned NAT/package strategy
7. local backup archive exists on the WSL operator workstation
```

The app and Nginx path can be deployed after this DB/file restore baseline if the follow-up validation wants to prove HTTP/API consistency in the same runtime window.

## Required runtime variables

Supply secrets at runtime. Do not commit them.

```text
ops_db_password
restic_password
```

Optional override when the local archive is stored elsewhere:

```text
lab_full_ops_restore_local_archive=/tmp/lab-full-ops-backup-validation/lab-full-ops-backup-20260712T072623.tar.gz
```

## Static syntax check

This check does not contact AWS instances and does not need the local backup archive to exist.

From WSL:

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible
export ANSIBLE_CONFIG="$PWD/ansible.cfg"
cp inventories/lab-full-ops/hosts.yml.example /tmp/restore-lab-hosts.yml

ansible-playbook \
  -i /tmp/restore-lab-hosts.yml \
  playbooks/restore-lab-db-file-restore-baseline.yml \
  --syntax-check \
  -e 'ops_db_password=staticcheckpassword123' \
  -e 'restic_password=staticcheckpassword123'

rm -f /tmp/restore-lab-hosts.yml
```

Expected output:

```text
playbook: playbooks/restore-lab-db-file-restore-baseline.yml
```

## Runtime command

Run this after PostgreSQL and NFS are configured in the restore-lab runtime window.

From WSL:

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible
export ANSIBLE_CONFIG="$PWD/ansible.cfg"
export ANSIBLE_HOST_KEY_CHECKING=False
export OPS_DB_PASSWORD='OpsDbRestore20260712'
export RESTIC_PASSWORD='ResticBackup20260712'

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/restore-lab-db-file-restore-baseline.yml \
  -e "ops_db_password=${OPS_DB_PASSWORD}" \
  -e "restic_password=${RESTIC_PASSWORD}" \
  | tee /tmp/restore-lab-recovery-validation/db-file-restore-baseline.txt
```

Use the same restic password that was used when the source backup artifact was created.

## What the playbook does

```text
1. installs postgresql-client, nfs-common, and restic on backup-01
2. writes the restic password file on backup-01
3. copies the preserved local archive from WSL to backup-01
4. unpacks the archive into /srv/ops-restore/unpacked
5. validates the pg_dump file, manifest, and restic repository are present
6. mounts the restore-lab nfs-01 export on backup-01 as a writable target
7. restores opsdb into restore-lab db-primary-01 with pg_restore
8. restores file objects from the restic snapshot into a staging path
9. copies restored file objects into the restore-lab NFS target
10. verifies row counts, sample storage_path, sample file size, and sample SHA-256
11. writes a restore report under /tmp/multitier-ops-platform
```

## Expected report

Default remote report:

```text
/tmp/multitier-ops-platform/restore-lab-db-file-restore-backup-01.txt
```

Minimum success fields:

```text
source_backup_id=lab-full-ops-backup-20260712T072623
pg_restore_status=validated
restic_restore_status=validated
metadata_counts=ops_work_order_evidence_files 1,ops_work_orders 6
expected_work_order_count=6
actual_work_order_count=6
expected_evidence_file_count=1
actual_evidence_file_count=1
expected_sample_size_bytes=215
actual_sample_size_bytes=215
expected_sample_sha256=4b4dc6fd2e07d5cd1713f846d9baf4c659209535872c5add945f65f252290150
actual_sample_sha256=4b4dc6fd2e07d5cd1713f846d9baf4c659209535872c5add945f65f252290150
db_file_restore_status=validated
http_api_restore_status=not_validated_by_this_playbook
```

`http_api_restore_status=not_validated_by_this_playbook` is intentional. It prevents the DB/file restore baseline from being overstated as full application recovery.

## Follow-up validation in the same runtime window

After this baseline succeeds, run app and Nginx deployment and then verify HTTP/API consistency:

```text
1. app-01 NFS mount baseline
2. ops-sample-service deployment
3. Nginx reverse proxy
4. restored evidence consistency endpoint
5. Nginx request log check
6. evidence collection
7. Terraform destroy
```

The final restore evidence document should be created only after the HTTP/API consistency path is checked.

## Failure interpretation

| Failure point | Likely layer | First checks |
|---|---|---|
| local archive copy fails | operator/Ansible control | WSL archive path, SSH path, backup-01 disk space |
| archive unpack fails | backup node/artifact | tar integrity, remote archive path, permissions |
| pg_dump file missing | artifact integrity | archive inventory, backup_id path, tar root layout |
| pg_restore fails | DB restore | opsdb existence, ops_user permissions, dump format, PostgreSQL service |
| metadata counts mismatch | DB/data | wrong archive, partial restore, schema/data mismatch |
| NFS mount fails | storage/network | nfs-01 export, security group 2049, fstab line |
| restic snapshots fail | restic artifact | password, repository path, restic repo integrity |
| file restore fails | file restore | target NFS permissions, restored source path, restic snapshot ID |
| checksum mismatch | data integrity | wrong storage_path, stale file, partial restore, archive mismatch |

Do not destroy and recreate the restore-lab until the failure class is identified. Reuse the same runtime window when possible.

## Cleanup

After DB/file restore and follow-up HTTP/API validation evidence are collected, destroy the restore-lab resources from Git Bash:

```bash
cd /c/Project/test/multitier-ops-platform/infra/terraform/envs/lab-full-ops
terraform destroy -var-file=restore-lab.recovery.tfvars
terraform state list
```

`terraform state list` should return no resources after teardown.
