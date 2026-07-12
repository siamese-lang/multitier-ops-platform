# Observability metrics runtime validation plan

This runbook defines the next AWS runtime validation window after the Phase 4 log/service/request-path evidence and the Phase 4B static metric baseline preparation.

Fixed project theme:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

This is not a Grafana dashboard-first task. The purpose is to prove that host-level metrics can be collected from the EC2 WEB/WAS/DB/Storage/Backup nodes and queried from a Prometheus monitoring node.

## Current prerequisite state

Already completed:

```text
Phase 4A runtime validation:
- observability baseline evidence documented
- controlled PostgreSQL service outage diagnosed with WEB/WAS/DB evidence
- AWS resources destroyed after evidence collection
```

Prepared but not yet runtime-validated:

```text
Phase 4B metric baseline:
- infra/ansible/playbooks/observability-node-exporter-baseline.yml
- infra/ansible/playbooks/observability-prometheus-scrape-baseline.yml
- docs/03-runbooks/observability-node-exporter-baseline.md
- docs/03-runbooks/observability-prometheus-scrape-baseline.md
```

## Validation objective

Validate the following operating question:

```text
Can a dedicated monitoring node collect host-level metrics from WEB/WAS/DB/Storage/Backup nodes and preserve evidence that each target is visible to Prometheus?
```

Supported claim after successful validation:

```text
Prometheus scrape evidence validated host-level node_exporter targets for EC2 WEB/WAS/DB/Storage/Backup diagnosis.
```

Do not claim:

```text
production monitoring maturity
complete Prometheus coverage
Grafana dashboard readiness
Alertmanager notification maturity
HA/failover validation
SLO/SLA compliance
```

## Required runtime topology

This validation requires the reduced lab-full-ops operating topology plus the optional monitoring node:

```text
bastion-01
nginx-01
app-01
db-primary-01
nfs-01
backup-01
mon-01
```

The monitoring node is required because the Terraform security-group model already opens node_exporter port `9100` from the monitoring security group to the operating nodes.

## Git Bash: Terraform profile

Use Git Bash for Terraform only.

Prepare local Terraform variables from the ignored `terraform.tfvars` file and enable only the nodes needed for this validation window:

```hcl
enable_app_02          = false
enable_storage_node    = true
enable_backup_node     = true
enable_monitoring_node = true
enable_logging_node    = false
enable_loadgen_node    = false
enable_nat_gateway     = true
```

`enable_nat_gateway = true` is expected for package installation on private nodes. Keep the runtime window short and destroy immediately after evidence collection.

Suggested evidence directory:

```bash
REPO=/c/Project/test/multitier-ops-platform
EVIDENCE_WIN="$REPO/.tmp/observability-metrics-$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$EVIDENCE_WIN"
```

Apply once:

```bash
cd "$REPO/infra/terraform/envs/lab-full-ops"

terraform init
terraform plan -out tfplan
terraform apply tfplan

terraform output > "$EVIDENCE_WIN/terraform-output.txt"
terraform state list > "$EVIDENCE_WIN/terraform-state-list-after-apply.txt"
```

Do not run repeated apply/destroy cycles for minor issues. If Ansible fails, diagnose inside the same runtime window when safe.

## WSL: inventory preparation

Use WSL for Ansible and evidence organization.

Update the ignored inventory file from Terraform outputs:

```text
infra/ansible/inventories/lab-full-ops/hosts.yml
```

The required placeholders include the existing operating nodes plus `mon-01`:

```text
<bastion_public_ip>
<nginx_private_ip>
<app_01_private_ip>
<db_primary_private_ip>
<nfs_01_private_ip>
<backup_01_private_ip>
<mon_01_private_ip>
```

Then confirm Ansible reachability:

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible

export ANSIBLE_CONFIG="$PWD/ansible.cfg"
export ANSIBLE_HOST_KEY_CHECKING=False

REPO=/mnt/c/Project/test/multitier-ops-platform
EVIDENCE_DIR="$(find "$REPO/.tmp" -maxdepth 1 -type d -name 'observability-metrics-*' | sort | tail -n 1)"
echo "$EVIDENCE_DIR"

ansible lab_full_ops_free_tier,monitoring \
  -i inventories/lab-full-ops/hosts.yml \
  -m ping \
  | tee "$EVIDENCE_DIR/ansible-ping-metrics-validation.txt"
```

## WSL: configure operating baseline

Before metric validation, configure the same operating baseline used by the previous observability validation window:

```bash
OPS_DB_PASSWORD='ChangeMe-Local-Obs-20260712'

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/lab-full-ops-postgresql-primary.yml \
  -e "ops_db_password=${OPS_DB_PASSWORD}" \
  | tee "$EVIDENCE_DIR/postgresql-primary.txt"

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/lab-full-ops-nfs-storage-baseline.yml \
  | tee "$EVIDENCE_DIR/nfs-storage-baseline.txt"

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/lab-full-ops-app-nfs-mount-baseline.yml \
  | tee "$EVIDENCE_DIR/app-nfs-mount-baseline.txt"

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/lab-full-ops-ops-sample-service.yml \
  -e "ops_db_password=${OPS_DB_PASSWORD}" \
  | tee "$EVIDENCE_DIR/ops-sample-service.txt"

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/lab-full-ops-nginx-reverse-proxy.yml \
  | tee "$EVIDENCE_DIR/nginx-reverse-proxy.txt"
```

Confirm the DB-backed request path is healthy before adding metric evidence:

```bash
ansible web \
  -i inventories/lab-full-ops/hosts.yml \
  -b \
  -m shell \
  -a 'date -Iseconds; curl -k -i https://127.0.0.1/readyz || true; echo; curl -k -i https://127.0.0.1/api/work-orders/summary || true' \
  | tee "$EVIDENCE_DIR/pre-metrics-web-ready-summary-check.txt"
```

## WSL: validate node_exporter endpoints

Run the node_exporter baseline:

```bash
ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/observability-node-exporter-baseline.yml \
  -e "observability_controller_evidence_dir=${EVIDENCE_DIR}" \
  | tee "$EVIDENCE_DIR/observability-node-exporter-baseline.txt"
```

Expected evidence:

```text
node-exporter-nginx-01.txt
node-exporter-app-01.txt
node-exporter-db-primary-01.txt
node-exporter-nfs-01.txt
node-exporter-backup-01.txt
```

Each evidence file should show:

```text
node_exporter service active
port 9100 listening
local /metrics query succeeds
sample node_cpu/node_memory/node_filesystem metrics visible
```

## WSL: validate Prometheus scrape from mon-01

Run the Prometheus scrape baseline:

```bash
ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/observability-prometheus-scrape-baseline.yml \
  -e "observability_controller_evidence_dir=${EVIDENCE_DIR}" \
  | tee "$EVIDENCE_DIR/observability-prometheus-scrape-baseline.txt"
```

Expected evidence:

```text
prometheus-scrape-mon-01.txt
```

The evidence should show:

```text
Prometheus service active
port 9090 listening
promtool check config success
/-/ready succeeds
/api/v1/targets contains nginx-01, app-01, db-primary-01, nfs-01, backup-01
/api/v1/query?query=up returns target status data
```

## Optional metric incident check

If time remains in the same runtime window, run a controlled DB service incident after Prometheus is scraping. The purpose is not alerting yet; the purpose is to prove metrics remain queryable during an incident window.

Minimum useful queries:

```text
up
node_systemd_unit_state if available from node_exporter/systemd collector
node_cpu_seconds_total
node_memory_MemAvailable_bytes
node_filesystem_avail_bytes
```

Do not claim alerting or SLO maturity from this step.

## Git Bash: destroy immediately

After evidence collection, return to Git Bash and destroy the runtime environment.

```bash
REPO=/c/Project/test/multitier-ops-platform
EVIDENCE_WIN="$(find "$REPO/.tmp" -maxdepth 1 -type d -name 'observability-metrics-*' | sort | tail -n 1)"

echo "$EVIDENCE_WIN"
cd "$REPO/infra/terraform/envs/lab-full-ops"

terraform destroy 2>&1 | tee "$EVIDENCE_WIN/terraform-destroy.txt"
terraform state list > "$EVIDENCE_WIN/terraform-state-list-after-destroy.txt" 2>&1 || true
```

Expected cleanup evidence:

```text
Destroy complete! Resources: <N> destroyed.
terraform-state-list-after-destroy.txt is empty
```

## WSL: document validation result

After destroy, create a documentation PR with a narrow evidence summary:

```text
docs/04-evidence/observability-metrics-validation-YYYY-MM-DD.md
```

The evidence document should include:

```text
runtime topology
Terraform apply/destroy evidence
node_exporter endpoint evidence by node
Prometheus target evidence from mon-01
query evidence for up
known gaps and non-claims
```

Supported final claim after successful evidence:

```text
Prometheus scrape evidence validated host-level node_exporter targets for EC2 WEB/WAS/DB/Storage/Backup diagnosis.
```
