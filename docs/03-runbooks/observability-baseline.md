# Observability baseline runbook

## Purpose

Define the operator procedure for collecting the minimum logs and metrics needed to diagnose EC2-based WEB/WAS/DB/Storage/Backup failures.

This runbook supports the fixed project theme:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

The goal is not to prove a polished dashboard. The goal is to collect evidence that helps narrow the failure class during an operating incident.

## Boundary

This runbook is a Phase 4 baseline procedure. It is intended to be implemented and validated after the design PR is reviewed.

It validates or prepares evidence for:

```text
node metrics/state for nginx-01, app-01, db-primary-01, nfs-01, backup-01
Nginx access/error log visibility
ops-sample-service journald visibility
PostgreSQL service/log visibility
NFS/storage service and filesystem visibility
backup/restore job log visibility
one evidence-based incident report
```

It does not validate:

```text
production monitoring maturity
Grafana dashboard quality
complete alerting coverage
HA/failover automation
Kubernetes/EKS observability
OpenKoda application behavior
```

## Workstation separation

Use the established local workflow split:

```text
Git Bash = Terraform only
WSL      = Ansible, Git work, evidence organization
AWS      = apply once -> validation -> evidence collect -> destroy once
```

Do not run Terraform from WSL in this project workflow. Do not run Ansible from Git Bash.

## Files

Design document:

```text
docs/01-architecture/observability-evidence-baseline.md
```

Runbook document:

```text
docs/03-runbooks/observability-baseline.md
```

Future implementation may add an Ansible playbook, but this documentation PR intentionally does not add one.

## Preconditions for future runtime validation

Run this only during a planned observability runtime window.

Required sequence before collecting runtime evidence:

```text
1. lab-full-ops or equivalent Phase 4 topology applied once
2. inventories/lab-full-ops/hosts.yml populated from Terraform outputs
3. Ansible ping succeeds for target nodes
4. nginx-01, app-01, db-primary-01, nfs-01, backup-01 are reachable through bastion path
5. WEB/WAS/DB/NFS baseline services are configured
6. evidence directory is created on the WSL operator workstation
```

The first observability baseline should not require OpenKoda, EKS, GitOps, managed RDS, or dashboard customization.

## Static document check

This check does not contact AWS instances and does not create resources.

From WSL:

```bash
cd /mnt/c/Project/test/multitier-ops-platform

git status --short

test -f docs/01-architecture/observability-evidence-baseline.md
test -f docs/03-runbooks/observability-baseline.md

grep -R "Observability evidence baseline" docs/01-architecture/observability-evidence-baseline.md
grep -R "AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증" \
  docs/01-architecture/observability-evidence-baseline.md \
  docs/03-runbooks/observability-baseline.md
```

Expected result:

```text
git status shows only the intended documentation changes
both documentation files exist
the fixed project theme appears in both documents
```

## Runtime evidence directory

Suggested WSL evidence directory for a future validation window:

```bash
export EVIDENCE_ID="observability-baseline-$(date -u +%Y%m%dT%H%M%SZ)"
export EVIDENCE_DIR="/tmp/${EVIDENCE_ID}"
mkdir -p "$EVIDENCE_DIR"
```

Do not commit the raw evidence directory or compressed evidence archive unless a future PR explicitly documents a small, reviewable text summary under `docs/04-evidence`.

## Expected evidence files

A future runtime validation should preserve at least these files:

```text
terraform-output.txt
terraform-state-list-after-apply.txt
ansible-ping.txt
node-baseline-all-targets.txt
service-baseline-web.txt
service-baseline-app.txt
service-baseline-db.txt
service-baseline-storage.txt
service-baseline-backup.txt
nginx-access-log.txt
nginx-error-log.txt
app-journal.txt
postgresql-journal-or-log.txt
nfs-service-and-export.txt
backup-restore-job-log.txt
incident-db-service-unavailable.txt
recovery-after-db-service-restart.txt
incident-report-db-service-unavailable.md
terraform-state-list-after-destroy.txt
```

The final evidence PR should summarize the results in:

```text
docs/04-evidence/observability-baseline-validation-YYYY-MM-DD.md
```

## Ansible control-path check

Future runtime command from WSL:

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible
export ANSIBLE_CONFIG="$PWD/ansible.cfg"
export ANSIBLE_HOST_KEY_CHECKING=False

ansible lab_full_ops_free_tier \
  -i inventories/lab-full-ops/hosts.yml \
  -m ping \
  | tee "$EVIDENCE_DIR/ansible-ping.txt"
```

The runtime window should not continue if basic Ansible reachability fails.

## Node baseline collection

Future runtime command from WSL:

```bash
ansible lab_full_ops_free_tier \
  -i inventories/lab-full-ops/hosts.yml \
  -b \
  -a 'date -Is; hostname; uptime; free -m; df -h; df -i; ss -lntp' \
  | tee "$EVIDENCE_DIR/node-baseline-all-targets.txt"
```

Minimum interpretation:

```text
host reachable
basic resource pressure visible
expected ports visible or absent in a diagnosable way
filesystem pressure visible for DB/NFS/backup nodes
```

## WEB evidence collection

Future runtime command from WSL:

```bash
ansible web \
  -i inventories/lab-full-ops/hosts.yml \
  -b \
  -a 'systemctl status nginx --no-pager; tail -n 100 /var/log/nginx/access.log; tail -n 100 /var/log/nginx/error.log' \
  | tee "$EVIDENCE_DIR/service-baseline-web.txt"
```

The evidence should answer:

```text
Is Nginx active?
Did the request reach WEB?
Did Nginx fail locally, or did it receive an upstream failure?
Is there a request-id or enough timestamp evidence to correlate with app logs?
```

If separate log files are easier to review, split access and error logs into:

```text
nginx-access-log.txt
nginx-error-log.txt
```

## WAS evidence collection

Future runtime command from WSL:

```bash
ansible app \
  -i inventories/lab-full-ops/hosts.yml \
  -b \
  -a 'systemctl status ops-sample-service --no-pager; journalctl -u ops-sample-service -n 200 --no-pager' \
  | tee "$EVIDENCE_DIR/service-baseline-app.txt"
```

The evidence should answer:

```text
Is the app service active?
Is the app listening on the expected port?
Does readiness fail because of DB, NFS, or another dependency?
Can app logs be correlated with Nginx request time or request ID?
```

## DB evidence collection

Future runtime command from WSL:

```bash
ansible db \
  -i inventories/lab-full-ops/hosts.yml \
  -b \
  -a 'systemctl status postgresql --no-pager; journalctl -u postgresql -n 200 --no-pager; ss -lntp | grep 5432 || true; df -h' \
  | tee "$EVIDENCE_DIR/service-baseline-db.txt"
```

The evidence should answer:

```text
Is the DB host reachable?
Is PostgreSQL active?
Is port 5432 listening?
Is there disk or memory pressure that could explain DB failure?
Do PostgreSQL logs show connection refusal, authentication failure, or service crash evidence?
```

## Storage evidence collection

Future runtime command from WSL:

```bash
ansible storage \
  -i inventories/lab-full-ops/hosts.yml \
  -b \
  -a 'systemctl status nfs-kernel-server --no-pager || systemctl status nfs-server --no-pager; exportfs -v; df -h /srv/ops-sample || true; df -i /srv/ops-sample || true; journalctl -n 200 --no-pager' \
  | tee "$EVIDENCE_DIR/service-baseline-storage.txt"
```

The evidence should answer:

```text
Is the NFS service active?
Is the expected export visible?
Is the file root full or inode-exhausted?
Is the issue storage service, filesystem capacity, permission, or client mount related?
```

## Backup/Ops evidence collection

Future runtime command from WSL:

```bash
ansible backup \
  -i inventories/lab-full-ops/hosts.yml \
  -b \
  -a 'date -Is; hostname; df -h; ls -lah /srv/ops-backup /srv/ops-restore 2>/dev/null || true; journalctl -n 200 --no-pager' \
  | tee "$EVIDENCE_DIR/service-baseline-backup.txt"
```

If backup or restore commands run during the same window, pipe their output into a dedicated file:

```text
backup-restore-job-log.txt
```

The evidence should distinguish:

```text
control-path failure
artifact staging failure
DB dump/restore failure
file backup/restore failure
local disk capacity failure
```

## Controlled incident target

Recommended first incident:

```text
PostgreSQL service unavailable while db-primary-01 host remains reachable
```

The point is not to prove HA or failover. The point is to prove that logs and metrics narrow the failure class to DB service state.

Future runtime outline from WSL:

```bash
# Capture healthy baseline first.
# Then intentionally stop PostgreSQL during the same planned runtime window.
ansible db \
  -i inventories/lab-full-ops/hosts.yml \
  -b \
  -a 'systemctl stop postgresql; systemctl is-active postgresql || true' \
  | tee "$EVIDENCE_DIR/incident-db-service-unavailable.txt"

# Run the existing HTTP/API checks or documented curl checks through nginx-01.
# Capture Nginx logs, app journal, DB service state, and node baseline again.

# Recover before destroying the environment.
ansible db \
  -i inventories/lab-full-ops/hosts.yml \
  -b \
  -a 'systemctl start postgresql; systemctl is-active postgresql; ss -lntp | grep 5432 || true' \
  | tee "$EVIDENCE_DIR/recovery-after-db-service-restart.txt"
```

Do not proceed with destructive experiments beyond this controlled service stop in the first observability baseline window.

## Incident report requirement

The evidence is not complete until the operator writes a short incident report.

Required report file in the WSL evidence directory:

```text
incident-report-db-service-unavailable.md
```

Minimum sections:

```text
# Incident report: DB service unavailable

## Scenario
## User-visible symptom
## Evidence collected
## Failure class narrowed to
## Why WEB was not the root cause
## Why WAS was not the root cause
## Why DB service state was the root cause
## Recovery action
## Post-recovery check
## Remaining gaps
```

The later repository evidence document should summarize this report without committing large raw logs.

## Cleanup

After evidence is collected and recovery is confirmed, destroy AWS resources from Git Bash only:

```bash
cd /c/Project/test/multitier-ops-platform/infra/terraform/envs/lab-full-ops
terraform destroy
terraform state list
```

Preserve the post-destroy state list as:

```text
terraform-state-list-after-destroy.txt
```

The runtime validation is not complete unless evidence is preserved and AWS resources are destroyed.
