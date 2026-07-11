# lab-full-ops work-order evidence file smoke check

## Purpose

This runbook validates the first end-to-end DB/file consistency path for `lab-full-ops`.

The target path is:

```text
operator / validation runner
  -> nginx-01 HTTPS
  -> app-01 ops-sample-service
  -> db-primary-01 PostgreSQL metadata
  -> nfs-01 NFS-backed file object
```

This is not a product file-upload test. It is an operating evidence smoke check proving that a controlled work-order evidence file creates both:

```text
1. PostgreSQL metadata row
2. NFS-backed file object
```

and that the application consistency endpoint agrees with direct DB and storage checks.

## Scope

The playbook performs these checks:

```text
1. Create a work order through nginx-01.
2. Create a generated evidence file for that work order through nginx-01.
3. List evidence metadata through nginx-01.
4. Call the consistency endpoint through nginx-01.
5. Query the evidence metadata row directly on db-primary-01.
6. Check the file object, size, and SHA-256 directly on nfs-01.
7. Capture Nginx access-log lines for the smoke request prefix.
8. Write a report under /tmp/multitier-ops-platform/.
```

## Preconditions

Run this only during a planned `lab-full-ops` runtime validation window.

Required sequence before this playbook:

```text
1. reduced lab-full-ops Terraform apply completed
2. inventories/lab-full-ops/hosts.yml populated from Terraform outputs
3. Ansible control path validated
4. db-primary-01 PostgreSQL primary configured
5. nfs-01 NFS export baseline configured
6. app-01 NFS client mount baseline configured
7. ops-sample-service deployed with OPS_EVIDENCE_FILE_ROOT=/mnt/ops-sample/files
8. nginx-01 reverse proxy configured and routing to app-01
```

Do not run this against a partial app-only environment. That would weaken the project by turning the storage path into a superficial HTTP check.

## Execution

From WSL/Linux/macOS:

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible
export ANSIBLE_CONFIG="$PWD/ansible.cfg"

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/lab-full-ops-work-order-evidence-smoke.yml
```

## Static syntax check

This check does not contact AWS instances:

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible
export ANSIBLE_CONFIG="$PWD/ansible.cfg"
cp inventories/lab-full-ops/hosts.yml.example /tmp/lab-full-ops-hosts.yml

ansible-playbook \
  -i /tmp/lab-full-ops-hosts.yml \
  playbooks/lab-full-ops-work-order-evidence-smoke.yml \
  --syntax-check

rm -f /tmp/lab-full-ops-hosts.yml
```

Expected output:

```text
playbook: playbooks/lab-full-ops-work-order-evidence-smoke.yml
```

## API endpoints used

The smoke check uses only controlled operating endpoints:

```text
POST /api/work-orders
POST /api/work-orders/{id}/evidence-files
GET  /api/work-orders/{id}/evidence-files
GET  /api/work-orders/{id}/evidence-files/{evidenceId}/consistency
```

It does not expose arbitrary upload/download behavior.

## Evidence locations

The playbook writes JSON response files on `nginx-01`:

```text
/tmp/multitier-ops-platform/lab-full-ops-evidence-smoke/work-order-create.json
/tmp/multitier-ops-platform/lab-full-ops-evidence-smoke/evidence-create.json
/tmp/multitier-ops-platform/lab-full-ops-evidence-smoke/evidence-list.json
/tmp/multitier-ops-platform/lab-full-ops-evidence-smoke/evidence-consistency.json
/tmp/multitier-ops-platform/lab-full-ops-evidence-smoke/parsed-values.env
```

It writes the final report to:

```text
/tmp/multitier-ops-platform/lab-full-ops-work-order-evidence-smoke-nginx-01.txt
```

## Minimum success criteria

A successful smoke check should show:

```text
api_consistent=true
db_metadata_row contains the created evidence_id and work_order_id
nfs_object_check shows the same size and SHA-256 as metadata
Nginx access log contains lab-full-ops-evidence-smoke request IDs
play recap shows unreachable=0 failed=0
```

The report should include at least:

```text
work_order_id
evidence_id
storage_path
expected_size_bytes
expected_sha256
api_consistent
db_metadata_row
nfs_object_check
nginx_log_sample
```

## Failure interpretation

Use the failure point to narrow the layer:

| Failure point | Likely layer | First checks |
|---|---|---|
| cannot reach `https://127.0.0.1` on nginx-01 | WEB/Nginx | Nginx service, TLS site, upstream config |
| work-order creation fails | DB path | app readiness, PostgreSQL service, `pg_hba.conf`, credentials |
| evidence creation returns storage-not-ready | app/NFS mount | `/mnt/ops-sample/files`, mount state, permissions |
| API consistency false but DB row exists | file object path | NFS mount, file size, checksum |
| DB direct query fails | DB tier | PostgreSQL service, schema, metadata table |
| NFS direct check fails | storage tier | NFS export, file presence under `/srv/ops-sample/files` |

## Runtime batching policy

This playbook is intended to be the final smoke check in a single validation window:

```text
1. terraform apply once
2. configure DB/NFS/app/Nginx
3. run this smoke check
4. collect evidence
5. terraform destroy once
```

Do not create and destroy the AWS lab just to run this playbook alone.

## Cleanup

This smoke check creates one work order, one metadata row, and one small evidence file.

For the short-lived validation lab, cleanup is normally handled by destroying the environment after evidence collection.

If manual cleanup is needed before destroy:

```text
PostgreSQL: delete from ops_work_order_evidence_files where id = <evidence_id>;
NFS: remove /srv/ops-sample/files/<storage_path>
```

Do not skip final Terraform cleanup after the validation window.
