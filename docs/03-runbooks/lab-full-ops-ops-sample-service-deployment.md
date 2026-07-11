# lab-full-ops ops-sample-service deployment alignment

## Purpose

Deploy `ops-sample-service` in the `lab-full-ops` environment so the app can use the work-order evidence file flow added for storage consistency validation.

This runbook supports the fixed project theme:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

The goal is not to turn the repository into an application feature project. The goal is to run a controlled workload where the operator can validate:

```text
work order metadata in PostgreSQL
+ evidence file metadata in PostgreSQL
+ evidence file object on NFS-mounted path
+ consistency verification between DB metadata and file object
```

## What changed for lab-full-ops

The `lab-full-ops` app inventory now defines the same app deployment variables used by the existing deployment playbook, but with Phase 2 values:

```text
ops_app_environment=lab-full-ops
ops_evidence_file_root=/mnt/ops-sample/files
ops_app_report_file=/tmp/multitier-ops-platform/lab-full-ops-ops-sample-service-<host>.txt
```

The systemd environment file written by Ansible now includes:

```text
OPS_EVIDENCE_FILE_ROOT=/mnt/ops-sample/files
```

That path must match the app-side NFS mount prepared by:

```text
infra/ansible/playbooks/lab-full-ops-app-nfs-mount-baseline.yml
```

## Files

```text
infra/ansible/inventories/lab-full-ops/group_vars/app.yml
infra/ansible/playbooks/lab-full-min-ops-sample-service.yml
infra/ansible/playbooks/lab-full-ops-ops-sample-service.yml
```

`lab-full-ops-ops-sample-service.yml` is a wrapper around the existing app deployment playbook. This avoids duplicating deployment logic while giving Phase 2 a clear entry point.

## Runtime batching policy

Do not create and destroy AWS resources only for this deployment alignment PR.

Use one batched runtime validation window for the storage path:

```text
1. Apply the reduced lab-full-ops Terraform profile once.
2. Prepare inventories/lab-full-ops/hosts.yml from Terraform outputs.
3. Run the nfs-01 storage baseline.
4. Run the app-01 NFS client mount baseline.
5. Deploy the updated ops-sample-service jar.
6. Exercise the work-order evidence file flow through Nginx.
7. Collect DB/file consistency evidence.
8. Destroy the Terraform environment once evidence is collected.
```

This is different from avoiding AWS altogether. AWS runtime validation is required, but it should be grouped around an evidence-producing scenario rather than repeated for every small code change.

## Artifact preparation without local Maven

The operator workstation does not need local Maven.

Use the GitHub Actions artifact from `ops-sample-service-ci`:

```text
artifact name: ops-sample-service-0.1.0-jar
```

Place the jar at:

```text
apps/ops-sample-service/target/ops-sample-service-0.1.0.jar
```

Example local preparation:

```bash
cd /mnt/c/Project/test/multitier-ops-platform
mkdir -p apps/ops-sample-service/target
# unzip the downloaded GitHub Actions artifact and place the jar here
ls -l apps/ops-sample-service/target/ops-sample-service-0.1.0.jar
```

## Execution during the validation window

Run only after the reduced `lab-full-ops` environment is active and the ignored inventory has real IP addresses.

From WSL:

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible
export ANSIBLE_CONFIG="$PWD/ansible.cfg"

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/lab-full-ops-nfs-storage-baseline.yml

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/lab-full-ops-app-nfs-mount-baseline.yml

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/lab-full-ops-ops-sample-service.yml \
  -e 'ops_db_password=<strong-local-password>'
```

Expected app deployment report:

```text
/tmp/multitier-ops-platform/lab-full-ops-ops-sample-service-app-01.txt
```

Minimum report fields:

```text
environment=lab-full-ops
service_state=active
db_host=<db-primary-01-private-ip>
evidence_file_root=/mnt/ops-sample/files
evidence_root_check_rc=0
healthcheck_rc=0
readycheck_rc=0
work_orders_summary_rc=0
```

## Evidence flow smoke checks

After deployment, the app should expose the work-order evidence endpoints added for Phase 2:

```text
POST /api/work-orders/{id}/evidence-files
GET  /api/work-orders/{id}/evidence-files
GET  /api/work-orders/{id}/evidence-files/{evidenceId}/consistency
```

Run the smoke checks through `nginx-01` during the same validation window.

Example shape:

```bash
curl -k -s -X POST https://127.0.0.1/api/work-orders/1/evidence-files \
  -H 'X-Request-Id: lab-full-ops-evidence-create-001'

curl -k -s https://127.0.0.1/api/work-orders/1/evidence-files \
  -H 'X-Request-Id: lab-full-ops-evidence-list-001'

curl -k -s https://127.0.0.1/api/work-orders/1/evidence-files/<evidenceId>/consistency \
  -H 'X-Request-Id: lab-full-ops-evidence-consistency-001'
```

Expected interpretation:

```text
DB metadata exists
file object exists under /mnt/ops-sample/files
size matches metadata
SHA-256 matches metadata
consistency=true
```

## Failure interpretation

If the app deploy succeeds but evidence creation fails, separate the failure class before retrying:

```text
DB failure            -> /readyz or work-order summary fails
NFS mount failure     -> evidence_root_check_rc != 0 or mountpoint missing
permission failure    -> app cannot write under /mnt/ops-sample/files
artifact failure      -> app jar is stale and endpoints are missing
routing failure       -> app works locally but Nginx path fails
application bug       -> endpoint returns 5xx while DB and NFS checks are healthy
```

Do not destroy and recreate Terraform resources until the failure class is identified.

## Out of scope

This runbook does not execute:

- file storage failure drill
- NFS service stop/recovery scenario
- backup with `pg_dump` and restic
- restore-lab validation
- Prometheus/Loki installation
- OpenKoda feature or UI work

Those belong to later issues after the storage consistency path is validated.
