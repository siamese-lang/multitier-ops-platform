# lab-full-ops pg_dump and file backup baseline

## Purpose

Prepare the first backup baseline for `lab-full-ops` after the completed storage validation.

This runbook supports the fixed project theme:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

The goal is not to claim that recovery has already been proven. The goal is to create restorable backup artifacts for the already validated DB/file consistency workload so that a later `restore-lab` can verify recovery.

## Operating path

The backup baseline assumes the storage validation path already works:

```text
operator -> nginx-01:443 -> app-01:8080 -> db-primary-01:5432
                                 |
                                 -> nfs-01:/srv/ops-sample/files

backup-01 -> db-primary-01:5432        # pg_dump
backup-01 -> nfs-01:/srv/ops-sample/files  # read-only NFS mount and restic backup
```

## Scope

This baseline covers two backup boundaries:

```text
PostgreSQL metadata/state -> pg_dump from opsdb
NFS-backed file objects  -> restic snapshot of /srv/ops-sample/files
```

The playbook writes a backup run directory on `backup-01` containing:

```text
pg_dump custom-format dump
NFS file inventory
NFS SHA-256 checksum list
restic JSON backup log
restic snapshot summary
backup manifest
backup report
```

## Out of scope

This runbook does not complete or claim:

```text
restore-lab creation
DB restore
file restore
HTTP/API consistency after restore
RTO/RPO measurement
observability stack setup
OpenKoda feature or UI work
Terraform architecture refactoring
```

Do not mark Phase 2B or Phase 3 complete from backup creation alone. The backup artifacts are inputs for a later restore-lab validation.

## Files

```text
infra/ansible/inventories/lab-full-ops/group_vars/backup.yml
infra/ansible/inventories/lab-full-ops/group_vars/db.yml
infra/ansible/playbooks/lab-full-min-postgresql-primary.yml
infra/ansible/playbooks/lab-full-ops-backup-baseline.yml
```

## Preconditions

Run this only during a planned `lab-full-ops` runtime validation window.

Required sequence before backup:

```text
1. reduced lab-full-ops Terraform apply completed
2. inventories/lab-full-ops/hosts.yml populated from Terraform outputs
3. Ansible control path validated
4. db-primary-01 PostgreSQL primary configured
5. nfs-01 NFS export baseline configured
6. app-01 NFS client mount baseline configured
7. ops-sample-service deployed with OPS_EVIDENCE_FILE_ROOT=/mnt/ops-sample/files
8. nginx-01 reverse proxy configured and routing to app-01
9. work-order evidence smoke completed successfully
```

The work-order evidence smoke is important because it creates the dataset that backup must preserve:

```text
PostgreSQL work-order metadata
PostgreSQL evidence-file metadata
NFS-backed evidence file object
SHA-256 and size mapping
```

## PostgreSQL access note

`backup-01` runs `pg_dump` against `db-primary-01` over the private ops subnet.

The `lab-full-ops` DB group vars therefore add the backup subnet to `pg_hba.conf` through the shared PostgreSQL playbook:

```text
10.50.41.0/24 -> opsdb as ops_user
```

This keeps the backup path private to the lab VPC and avoids operator-to-DB direct access.

## Secret handling

Do not commit real secrets.

Supply both values at runtime from ignored inventory, `--extra-vars`, or future Ansible Vault:

```text
ops_db_password
restic_password
```

Example:

```bash
-e 'ops_db_password=<strong-local-password>' \
-e 'restic_password=<strong-local-restic-password>'
```

## No-NAT/package-access constraint

The default reduced `lab-full-ops` profile disables NAT Gateway. `backup-01` is private, so package installation requires one intentional package-access strategy:

```text
temporary NAT Gateway
NAT instance
pre-baked AMI
internal package mirror
staged package artifacts
```

If temporary NAT Gateway is enabled, run the grouped validation, collect evidence, and destroy immediately.

## Static syntax check

This check does not contact AWS instances:

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible
export ANSIBLE_CONFIG="$PWD/ansible.cfg"
cp inventories/lab-full-ops/hosts.yml.example /tmp/lab-full-ops-hosts.yml

ansible-playbook \
  -i /tmp/lab-full-ops-hosts.yml \
  playbooks/lab-full-ops-postgresql-primary.yml \
  --syntax-check \
  -e 'ops_db_password=staticcheckpassword123'

ansible-playbook \
  -i /tmp/lab-full-ops-hosts.yml \
  playbooks/lab-full-ops-backup-baseline.yml \
  --syntax-check \
  -e 'ops_db_password=staticcheckpassword123' \
  -e 'restic_password=staticcheckpassword123'

rm -f /tmp/lab-full-ops-hosts.yml
```

Expected syntax-check output:

```text
playbook: playbooks/lab-full-ops-postgresql-primary.yml
playbook: playbooks/lab-full-ops-backup-baseline.yml
```

## Runtime execution

Run the backup baseline after the work-order evidence smoke succeeds.

From `infra/ansible`:

```bash
export ANSIBLE_CONFIG="$PWD/ansible.cfg"

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/lab-full-ops-postgresql-primary.yml \
  -e 'ops_db_password=<strong-local-password>'

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/lab-full-ops-work-order-evidence-smoke.yml

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/lab-full-ops-backup-baseline.yml \
  -e 'ops_db_password=<strong-local-password>' \
  -e 'restic_password=<strong-local-restic-password>'
```

## Evidence locations on backup-01

Default backup root:

```text
/srv/ops-backups/lab-full-ops
```

Default run directory:

```text
/srv/ops-backups/lab-full-ops/artifacts/<backup_id>
```

Default report:

```text
/tmp/multitier-ops-platform/lab-full-ops-backup-baseline-backup-01.txt
```

Expected files inside the run directory:

```text
opsdb-<backup_id>.dump
nfs-file-inventory.txt
nfs-sha256sums.txt
restic-backup.jsonl
restic-snapshot.txt
backup-manifest.env
```

## Minimum success criteria

The backup baseline is successful only if the report shows:

```text
pg_dump_file exists
pg_dump_size_bytes > 0
pg_dump_sha256 is recorded
nfs_file_count >= 1
nfs_checksum_list exists
restic_snapshot_id is recorded
metadata_counts includes ops_work_orders and ops_work_order_evidence_files
sample_evidence_metadata is recorded
restore_status=not_validated
play recap shows unreachable=0 failed=0
```

`restore_status=not_validated` is intentional. It prevents backup creation from being misread as recovery proof.

## Artifact inventory commands

During evidence collection, capture at least:

```bash
sudo cat /tmp/multitier-ops-platform/lab-full-ops-backup-baseline-backup-01.txt
sudo find /srv/ops-backups/lab-full-ops/artifacts -maxdepth 2 -type f -printf '%p\t%s bytes\n' | sort
sudo restic -r /srv/ops-backups/lab-full-ops/restic-repo --password-file /etc/multitier-ops-platform/restic-password snapshots
```

For the source NFS mount:

```bash
findmnt --target /mnt/ops-sample/files-backup-source --output SOURCE,TARGET,FSTYPE,OPTIONS --noheadings
sudo cat /srv/ops-backups/lab-full-ops/artifacts/<backup_id>/nfs-file-inventory.txt
sudo cat /srv/ops-backups/lab-full-ops/artifacts/<backup_id>/nfs-sha256sums.txt
```

## Failure interpretation

| Failure point | Likely layer | First checks |
|---|---|---|
| package install fails | package-access strategy | NAT/package mirror/pre-baked AMI/staged packages |
| backup NFS mount fails | storage/network | NFS export, security group, `/etc/fstab`, `exportfs -v` |
| `pg_dump` fails to connect | DB access path | `pg_hba.conf`, backup subnet CIDR, PostgreSQL service, credentials |
| metadata table query fails | workload dataset | run work-order evidence smoke before backup |
| file count is zero | storage dataset | evidence file creation, NFS mount path, export root |
| restic init or backup fails | backup node | password file, repository path, disk space, restic package |

## Cleanup and destroy policy

For a short-lived validation window, collect backup evidence first, then destroy the lab.

Before destroying the source lab, Phase 3 must decide how restore-lab will access backup artifacts. Acceptable future approaches include:

```text
copy backup artifacts to the operator workspace before destroy
preserve a dedicated backup volume outside the transient source lab
use an object-store artifact bucket only as a backup artifact store
```

The selected artifact persistence method must be documented in the restore-lab issue or PR before recovery validation starts.
