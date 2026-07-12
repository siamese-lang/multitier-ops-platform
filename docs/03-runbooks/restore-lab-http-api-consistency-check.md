# restore-lab HTTP/API consistency check

## Purpose

Validate the restored application path after the DB/file restore baseline has completed.

This runbook supports the fixed project theme:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

The purpose is not to create more sample data. The purpose is to prove that the data restored from the Phase 2B backup artifacts is usable through the WEB/WAS/DB/file path.

## Position in the restore-lab sequence

Run this after the restore-lab runtime window has completed these steps:

```text
1. Terraform restore-lab profile applied.
2. restore-lab inventory populated from Terraform outputs.
3. Ansible control path verified.
4. PostgreSQL primary configured.
5. NFS storage baseline configured.
6. app-01 NFS mount configured.
7. DB/file restore baseline completed from the preserved backup archive.
8. ops-sample-service deployed against restored DB and file path.
9. Nginx reverse proxy configured.
10. This HTTP/API consistency check runs through nginx-01.
```

## Operating path

```text
operator -> nginx-01:443 -> app-01:8080 -> db-primary-01:5432
                                 |
                                 -> nfs-01:/srv/ops-sample/files
```

The check also verifies the DB and NFS tiers directly from Ansible delegation:

```text
nginx-01 play context -> db-primary-01 direct metadata query
nginx-01 play context -> nfs-01 direct file size/checksum verification
```

## What this check validates

The playbook checks the restored sample from the backup evidence:

```text
source_backup_id=lab-full-ops-backup-20260712T072623
work_order_id=6
evidence_id=1
storage_path=work-order-6/evidence-40219c94-cef8-4c1c-aa07-962938ed4b64.txt
size_bytes=215
sha256=4b4dc6fd2e07d5cd1713f846d9baf4c659209535872c5add945f65f252290150
```

Expected validation results:

```text
/healthz succeeds through Nginx
/readyz succeeds through Nginx
/api/work-orders/summary returns total restored work-order count = 6
/api/work-orders/6/evidence-files returns restored evidence metadata count = 1
/api/work-orders/6/evidence-files/1/consistency returns consistent=true
PostgreSQL metadata row exists for work_order_id=6 and evidence_id=1
NFS file exists at the restored storage path
NFS file size = 215
NFS file SHA-256 matches the backed-up SHA-256
Nginx access log contains restore-lab request IDs
```

## Scope boundary

This check can support a recovery claim only when combined with the preceding DB/file restore baseline.

```text
DB/file restore baseline failed -> this check must not be run as recovery proof
DB/file restore baseline succeeded + this check succeeded -> restore-lab recovery proof can be documented
```

This runbook does not include:

```text
Terraform apply or destroy
artifact upload
pg_restore
restic restore
application deployment
Nginx deployment
Prometheus/Grafana/Loki work
OpenKoda feature or UI work
new work-order or evidence-file creation
```

## Static syntax check

From WSL:

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible
export ANSIBLE_CONFIG="$PWD/ansible.cfg"
cp inventories/lab-full-ops/hosts.yml.example /tmp/restore-lab-hosts.yml

ansible-playbook \
  -i /tmp/restore-lab-hosts.yml \
  playbooks/restore-lab-http-api-consistency-check.yml \
  --syntax-check

rm -f /tmp/restore-lab-hosts.yml
```

Expected output:

```text
playbook: playbooks/restore-lab-http-api-consistency-check.yml
```

## Runtime execution

Run this only during the grouped restore-lab runtime window, after app and Nginx are deployed.

From WSL:

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible
export ANSIBLE_CONFIG="$PWD/ansible.cfg"

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/restore-lab-http-api-consistency-check.yml \
  | tee "$EVIDENCE_DIR/restore-lab-http-api-consistency.txt"
```

## Evidence report

Default report path on `nginx-01`:

```text
/tmp/multitier-ops-platform/restore-lab-http-api-consistency-nginx-01.txt
```

Expected report fields:

```text
source_backup_id
sample_work_order_id
sample_evidence_id
sample_storage_path
restored_work_order_count
restored_evidence_count
api_consistency_status
api_consistent
file_exists
size_matches
checksum_matches
expected_sha256
actual_sha256
app_resolved_path
db_metadata_row
nfs_file_object_check
nginx_log_sample
http_api_restore_status=validated
```

## Failure interpretation

| Failure point | Likely layer | First checks |
|---|---|---|
| `/healthz` fails | WEB/WAS | Nginx upstream, app service state, port 8080 |
| `/readyz` fails | WAS/DB | app env file, DB password, PostgreSQL service, restored schema |
| summary count mismatch | DB restore | wrong dump, partial restore, app wrote extra rows |
| evidence list count mismatch | DB restore | wrong sample ID, missing evidence metadata table |
| consistency endpoint false | DB/file mismatch | storage_path, NFS restore target, checksum |
| direct DB row missing | DB restore | pg_restore status, database name, sample ID |
| direct NFS checksum mismatch | file restore | restic restore path, stale file, wrong snapshot |
| Nginx log missing request ID | WEB/logging | access log path, Nginx config, request did not reach Nginx |

## Runtime policy

Do not open a separate AWS window just for this check. It must be part of one grouped restore-lab validation window:

```text
Terraform apply once
-> Ansible configure/restore/check sequence
-> evidence collection
-> Terraform destroy once
```

After this check succeeds, create a separate evidence document under:

```text
docs/04-evidence/restore-lab-recovery-validation-YYYY-MM-DD.md
```

That document may claim recovery only if the DB/file restore baseline and this HTTP/API consistency check both succeeded.
