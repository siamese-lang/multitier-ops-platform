# Observability evidence collection baseline playbook

## Purpose

This runbook explains how to use the first Phase 4 Ansible playbook:

```text
infra/ansible/playbooks/observability-baseline.yml
```

The playbook supports the fixed project theme:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

The goal is not to install or polish a monitoring platform. The goal is to collect enough logs, service state, node metrics, request-path evidence, and one optional incident report so the operator can narrow a failure class with evidence.

## Boundary

This PR prepares an Ansible evidence collection baseline. It does not run AWS and does not validate runtime results by itself.

It may prepare evidence for:

```text
node health/resource state for nginx-01, app-01, db-primary-01, nfs-01, backup-01
Nginx access/error log visibility
ops-sample-service journald visibility
PostgreSQL service/log visibility
NFS/storage service and filesystem visibility
backup/restore job-log visibility
request-path evidence through Nginx
optional controlled DB service incident evidence
```

It does not prove:

```text
production monitoring maturity
Grafana dashboard completeness
Prometheus/Loki platform coverage
Alertmanager routing
HA or automated failover
CloudWatch-managed observability
```

## Workstation separation

Use the established workflow split.

```text
Git Bash = Terraform only
WSL      = Ansible, Git work, evidence organization
AWS      = apply once -> validation -> evidence collect -> destroy once
```

Do not run Terraform from WSL. Do not run Ansible from Git Bash.

## Added file

```text
infra/ansible/playbooks/observability-baseline.yml
```

The playbook has three parts:

```text
1. collect node and tier-specific service/log evidence from WEB/WAS/DB/Storage/Backup nodes
2. validate request-path visibility through nginx-01
3. optionally run one controlled PostgreSQL service outage and write an incident report
```

The optional DB service incident is disabled by default:

```text
observability_run_db_service_incident=false
```

That default prevents an accidental destructive service stop during basic evidence collection.

## Static syntax check

This check does not contact AWS instances and does not create resources.

Run from WSL:

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible
export ANSIBLE_CONFIG="$PWD/ansible.cfg"
cp inventories/lab-full-ops/hosts.yml.example /tmp/observability-baseline-hosts.yml

ansible-playbook \
  -i /tmp/observability-baseline-hosts.yml \
  playbooks/observability-baseline.yml \
  --syntax-check

rm -f /tmp/observability-baseline-hosts.yml
```

Expected output:

```text
playbook: playbooks/observability-baseline.yml
```

## Runtime preconditions for a future validation window

Run this only after the Phase 4 runtime environment is already configured.

Required preconditions:

```text
1. lab-full-ops or equivalent Phase 4 topology applied once from Git Bash
2. inventories/lab-full-ops/hosts.yml populated from Terraform outputs
3. Ansible ping succeeds from WSL
4. nginx-01, app-01, db-primary-01, nfs-01, backup-01 are reachable through the bastion path
5. Nginx, ops-sample-service, PostgreSQL, NFS, and backup paths are configured
6. DB/file sample data exists if the consistency endpoint probe is enabled
```

The consistency endpoint defaults to the restored sample that was validated in Phase 3:

```text
work_order_id=6
evidence_id=1
```

If a future runtime window uses different sample data, override the variables instead of changing the project direction:

```bash
-e observability_consistency_work_order_id=<work_order_id>
-e observability_consistency_evidence_id=<evidence_id>
```

If the runtime window only needs process, readiness, and DB-backed summary visibility, disable the DB/file consistency probe:

```bash
-e observability_run_consistency_probe=false
```

## Basic runtime command without controlled incident

Run from WSL after Terraform apply and host configuration are already complete:

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible
export ANSIBLE_CONFIG="$PWD/ansible.cfg"
export ANSIBLE_HOST_KEY_CHECKING=False
export EVIDENCE_DIR="/tmp/observability-baseline-validation"
mkdir -p "$EVIDENCE_DIR"

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/observability-baseline.yml \
  -e "observability_controller_evidence_dir=${EVIDENCE_DIR}" \
  -e 'observability_run_db_service_incident=false' \
  | tee "$EVIDENCE_DIR/observability-baseline-playbook.txt"
```

This command collects baseline evidence but does not intentionally stop PostgreSQL.

## Runtime command with controlled DB service incident

Use this only inside a planned AWS validation window.

Run from WSL:

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible
export ANSIBLE_CONFIG="$PWD/ansible.cfg"
export ANSIBLE_HOST_KEY_CHECKING=False
export EVIDENCE_DIR="/tmp/observability-baseline-validation"
mkdir -p "$EVIDENCE_DIR"

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/observability-baseline.yml \
  -e "observability_controller_evidence_dir=${EVIDENCE_DIR}" \
  -e 'observability_run_db_service_incident=true' \
  | tee "$EVIDENCE_DIR/observability-baseline-with-db-incident.txt"
```

This intentionally stops PostgreSQL and restarts it in the same playbook. Do not run it outside a planned validation window.

## Expected evidence files

The playbook fetches small text reports to the WSL evidence directory.

Baseline evidence examples:

```text
node-baseline-nginx-01.txt
node-baseline-app-01.txt
node-baseline-db-primary-01.txt
node-baseline-nfs-01.txt
node-baseline-backup-01.txt
service-baseline-web-nginx-01.txt
service-baseline-app-app-01.txt
service-baseline-db-db-primary-01.txt
service-baseline-storage-nfs-01.txt
service-baseline-backup-backup-01.txt
request-path-nginx-01.txt
request-path-nginx-01.tsv
observability-baseline-playbook.txt
```

Optional DB incident evidence examples:

```text
incident-db-service-unavailable.tsv
incident-nginx-db-service-unavailable-nginx-01.txt
incident-report-db-service-unavailable.md
observability-baseline-with-db-incident.txt
```

These raw files should stay local unless a future PR intentionally summarizes the results in a reviewable evidence document.

## Evidence interpretation requirement

The runtime validation is not complete merely because the playbook ran.

A future evidence PR must summarize whether the collected evidence could narrow the failure class, for example:

```text
WEB request reached nginx-01.
WAS process remained inspectable.
DB host was reachable.
PostgreSQL service state was inactive during the controlled incident.
Readiness/API path recovered after PostgreSQL restart.
```

The narrow claim after successful runtime validation should be:

```text
Observability baseline evidence validated for EC2 WEB/WAS/DB/Storage/Backup diagnosis.
```

Do not claim:

```text
production observability
full monitoring platform maturity
complete alerting coverage
HA/failover validation
SLO/SLA compliance
```

## Cleanup after future runtime validation

After evidence collection and recovery confirmation, destroy AWS resources from Git Bash only:

```bash
cd /c/Project/test/multitier-ops-platform/infra/terraform/envs/lab-full-ops
terraform destroy
terraform state list
```

The runtime validation is not complete unless the evidence is preserved and AWS resources are destroyed.

## Recommended follow-up after this PR

After this Ansible baseline is merged, the next milestone should be one batched runtime validation window:

```text
apply once -> configure baseline -> collect observability evidence -> run optional DB service incident -> recover -> collect evidence -> destroy once
```

Then create a separate evidence PR:

```text
[VALIDATION] Document observability baseline evidence
```
