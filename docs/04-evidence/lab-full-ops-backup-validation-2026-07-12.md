# lab-full-ops backup validation - 2026-07-12

## Purpose

Document the first `lab-full-ops` backup artifact validation for the fixed project theme:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

This evidence does **not** prove restore completion. It proves that the already validated DB/file workload can produce backup artifacts that are suitable inputs for a later `restore-lab` recovery validation.

## Result summary

```text
Backup artifact creation: validated
Restore validation: not validated yet
AWS cleanup: completed by operator after artifact preservation
```

The important boundary is:

```text
Backup creation is not recovery proof.
Phase 3 must restore these DB/file artifacts into a separate restore-lab and verify API consistency.
```

## Runtime profile

The runtime window used the reduced `lab-full-ops` topology with NAT Gateway enabled for package installation.

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

Temporary validation support:
- NAT Gateway enabled only for this batched runtime window
```

Ephemeral Terraform outputs from the validation window:

```text
bastion-01     public=3.35.229.82   private=10.50.1.81
nginx-01       public=3.36.87.167   private=10.50.1.7
app-01         private=10.50.11.70
db-primary-01  private=10.50.21.102
nfs-01         private=10.50.31.209
backup-01      private=10.50.41.152
NAT Gateway    nat-0f490aa951a97eb7b / EIP 52.79.156.16
```

These addresses were short-lived validation resources and should not be reused as stable configuration.

## Tooling split used during validation

The operator intentionally split local tool usage by terminal environment:

```text
Git Bash: Terraform apply/destroy only
WSL:      Ansible inventory, playbook execution, evidence collection only
```

This distinction matters because SSH keys, `/tmp` paths, and installed tools differ between Git Bash and WSL.

## Validation sequence

The validation window was used for one grouped runtime pass:

```text
1. Terraform apply with enable_nat_gateway=true
2. Populate lab-full-ops Ansible hosts.yml from Terraform outputs
3. Repair local WSL SSH/ProxyCommand inventory values
4. Validate Ansible control path to all reduced-profile nodes
5. Run lab-full-ops baseline check
6. Configure PostgreSQL primary
7. Configure nfs-01 export baseline
8. Configure app-01 NFS mount baseline
9. Deploy ops-sample-service
10. Configure Nginx reverse proxy
11. Run work-order evidence smoke
12. Run pg_dump + restic backup baseline
13. Preserve backup artifacts and evidence under WSL /tmp
14. Terraform destroy after evidence collection
```

No additional Terraform apply/destroy cycles were used for individual Ansible steps.

## Control path finding

The initial Ansible ping reached `bastion-01` but failed for private nodes with:

```text
Connection closed by UNKNOWN port 65535
```

Verbose Ansible output showed that the private-node `ProxyCommand` contained an empty key path:

```text
ProxyCommand=ssh -i  -o IdentitiesOnly=yes ... ubuntu@3.35.229.82
```

The local `hosts.yml` was corrected so the ProxyCommand used the WSL private key path:

```text
/home/kimsanggkyun/.ssh/my-web-key.pem
```

After clearing stale Ansible SSH control sockets, the full `lab_full_ops` ping succeeded.

This was a local inventory preparation issue, not an AWS network or Terraform issue.

## Backup baseline result

The backup baseline ran on `backup-01` and completed successfully:

```text
PLAY RECAP
backup-01 : ok=16 changed=6 unreachable=0 failed=0 skipped=0 rescued=0 ignored=0
```

Backup boundary reported by the playbook:

```text
PostgreSQL boundary: opsdb via pg_dump
File boundary: 10.50.31.209:/srv/ops-sample/files via restic snapshot
```

Report summary:

```text
project=multitier-ops-platform
environment=lab-full-ops
inventory_hostname=backup-01
ansible_host=10.50.41.152
backup_id=lab-full-ops-backup-20260712T072623
backup_started_at=2026-07-12T07:27:03Z
backup_finished_at=2026-07-12T07:27:08Z
db_boundary=opsdb via pg_dump
file_boundary=10.50.31.209:/srv/ops-sample/files via restic
pg_dump_file=/srv/ops-backups/lab-full-ops/artifacts/lab-full-ops-backup-20260712T072623/opsdb-lab-full-ops-backup-20260712T072623.dump
pg_dump_size_bytes=7479
pg_dump_sha256=fe58367d5d43101461483a5054da4b8b520d2cc15e1e4c8ce2dc629082f78b0f
nfs_file_count=2
nfs_file_inventory=/srv/ops-backups/lab-full-ops/artifacts/lab-full-ops-backup-20260712T072623/nfs-file-inventory.txt
nfs_checksum_list=/srv/ops-backups/lab-full-ops/artifacts/lab-full-ops-backup-20260712T072623/nfs-sha256sums.txt
restic_repository=/srv/ops-backups/lab-full-ops/restic-repo
restic_repo_state=created
restic_snapshot_id=7f063aa1
manifest_file=/srv/ops-backups/lab-full-ops/artifacts/lab-full-ops-backup-20260712T072623/backup-manifest.env
metadata_counts=ops_work_order_evidence_files\t1,ops_work_orders\t6
sample_evidence_metadata=1\t6\twork-order-6/evidence-40219c94-cef8-4c1c-aa07-962938ed4b64.txt\t215\t4b4dc6fd2e07d5cd1713f846d9baf4c659209535872c5add945f65f252290150
findmnt=10.50.31.209:/srv/ops-sample/files /mnt/ops-sample/files-backup-source nfs4 ro,relatime,sync,vers=4.2,rsize=131072,wsize=131072,namlen=255,hard,proto=tcp,timeo=600,retrans=2,sec=sys,clientaddr=10.50.41.152,local_lock=none,addr=10.50.31.209
restore_status=not_validated
```

## Success criteria mapping

| Criterion | Evidence | Result |
|---|---|---|
| `pg_dump` artifact exists | `pg_dump_file=...opsdb-lab-full-ops-backup-20260712T072623.dump` | met |
| DB dump is non-empty | `pg_dump_size_bytes=7479` | met |
| DB dump checksum recorded | `pg_dump_sha256=fe58367...` | met |
| NFS file backup input is non-empty | `nfs_file_count=2` | met |
| NFS checksum list generated | `nfs_checksum_list=.../nfs-sha256sums.txt` | met |
| restic snapshot created | `restic_snapshot_id=7f063aa1` | met |
| metadata rows captured | `ops_work_order_evidence_files=1`, `ops_work_orders=6` | met |
| sample metadata/file mapping captured | `sample_evidence_metadata=...storage_path...size...sha256` | met |
| restore not overclaimed | `restore_status=not_validated` | met |
| playbook completed | `unreachable=0 failed=0` | met |

## Preserved local evidence files

The operator preserved the following WSL-side evidence files under `/tmp/lab-full-ops-backup-validation`:

```text
ansible-baseline-check.txt
ansible-ping.txt
app-nfs-mount-baseline.txt
backup-artifact-inventory.txt
backup-baseline.txt
backup-manifest.txt
backup-report.txt
lab-full-ops-backup-20260712T072623.tar.gz
nfs-file-inventory.txt
nfs-sha256sums.txt
nfs-storage-baseline.txt
nginx-reverse-proxy.txt
ops-sample-service.txt
postgresql-primary.txt
restic-snapshots.txt
work-order-evidence-smoke.txt
```

A second archive preserved the full WSL evidence directory:

```text
/tmp/lab-full-ops-backup-validation-lab-full-ops-backup-20260712T072623.tar.gz
```

The backup artifact archive was intentionally **not** committed to the repository. It should be treated as a local restore input for the next restore-lab validation.

## Cleanup status

The operator reported that Terraform resource deletion completed after backup artifact preservation.

The supplied file list did not include a captured `terraform state list` output after destroy. Future runtime windows should always save this file explicitly:

```bash
terraform state list | tee /tmp/lab-full-ops-backup-validation/terraform-state-list-after-destroy.txt
```

## Non-blocking warnings

The backup baseline emitted an Ansible deprecation warning for top-level fact injection:

```text
INJECT_FACTS_AS_VARS default to True is deprecated
```

A future cleanup PR can replace top-level fact usage such as `ansible_date_time` with `ansible_facts.date_time`.

The run also emitted a remote temporary directory warning for `/root/.ansible/tmp`. This did not block the validation.

## Interpretation

This validation proves:

```text
backup-01 can reach db-primary-01 over the private ops-to-DB path and create a pg_dump of opsdb.
backup-01 can mount nfs-01:/srv/ops-sample/files and inventory/checksum NFS-backed file objects.
backup-01 can create a restic snapshot and backup manifest tying DB metadata to file artifacts.
```

This validation does not prove:

```text
pg_restore into a fresh database
restic restore into a fresh file tier
restored app consistency endpoint
restored HTTP path through Nginx
RTO/RPO or recovery gap analysis
```

## Required next step

The next roadmap-aligned work should be restore validation, not observability or additional feature work.

Recommended next task:

```text
[DESIGN] Define restore-lab DB/file recovery validation path
```

or, if implementing directly:

```text
[TF] Add restore-lab minimal recovery profile
```

The restore-lab work must decide how the preserved local artifact archive is injected into the restore environment and then verify:

```text
pg_restore succeeds
restic restore succeeds
metadata row counts match expected values
sample evidence file path exists
sample evidence SHA-256 matches the backed-up value
application consistency endpoint returns true
nginx -> app -> DB/NFS path works after restore
```
