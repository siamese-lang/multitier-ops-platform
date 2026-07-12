# Observability baseline validation evidence - 2026-07-12

## Project scope

Fixed project theme:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

This validation covers Phase 4 observability evidence for the EC2-based WEB/WAS/DB/Storage/Backup operating environment.

The goal was not to build or polish a production monitoring platform. The goal was to prove that collected logs, service state, node state, and request-path evidence can narrow an operating failure class.

## Validation claim

Narrow claim supported by this runtime window:

```text
Observability baseline evidence validated for EC2 WEB/WAS/DB/Storage/Backup diagnosis.
```

This validation does not claim:

```text
production observability
full Prometheus/Grafana/Loki platform maturity
complete alerting coverage
HA or automated failover validation
SLO/SLA compliance
```

## Runtime window

Runtime evidence window:

```text
observability-baseline-20260712T110244Z
```

Execution split followed the project policy:

```text
Git Bash = Terraform apply/destroy
WSL      = Ansible, Git work, evidence organization
AWS      = apply once -> validation -> evidence collect -> destroy once
```

The local evidence archive was preserved outside the repository:

```text
.tmp/observability-baseline-20260712T110244Z.tar.gz
archive_size=37K
```

Raw runtime evidence was not committed to the repository.

## Runtime topology

Terraform created the reduced `lab-full-ops` profile:

```text
bastion-01     public=3.34.131.38   private=10.50.1.37
nginx-01       public=52.79.222.227 private=10.50.1.237
app-01         private=10.50.11.23
db-primary-01  private=10.50.21.188
nfs-01         private=10.50.31.218
backup-01      private=10.50.41.34
```

Terraform profile flags:

```text
app_02=false
backup_node=true
loadgen_node=false
logging_node=false
monitoring_node=false
nat_gateway=true
storage_node=true
```

The validation intentionally used the reduced Phase 4 topology. `app-02`, `loadgen-01`, `mon-01`, and `log-01` were not part of this window.

## Terraform lifecycle evidence

Terraform apply evidence was collected before Ansible validation:

```text
terraform-output.txt
terraform-state-list-after-apply.txt
```

Terraform state after apply contained the expected EC2/VPC/NAT/subnet/security group resources for the reduced lab profile.

Destroy evidence:

```text
Destroy complete! Resources: 34 destroyed.
```

A post-destroy state-list evidence file was preserved:

```text
terraform-state-list-after-destroy.txt
```

The post-destroy state-list file was empty, indicating that Terraform-managed resources for this runtime window were removed.

## Baseline configuration evidence

The following baseline configuration playbooks were executed and preserved as local evidence:

```text
lab-full-ops-postgresql-primary.txt
lab-full-ops-nfs-storage-baseline.txt
lab-full-ops-app-nfs-mount-baseline.txt
lab-full-ops-ops-sample-service.txt
lab-full-ops-nginx-reverse-proxy.txt
```

After an initial runtime diagnosis, PostgreSQL was re-applied successfully:

```text
rerun-postgresql-primary-after-db-missing.txt
postgresql-primary-after-rerun-check.txt
```

DB recovery check after re-running the PostgreSQL playbook:

```text
PostgreSQL service: active
5432 listen: 0.0.0.0:5432 and [::]:5432
pg_hba app CIDR: 10.50.11.0/24
pg_hba backup CIDR: 10.50.41.0/24
opsdb exists
ops_user exists
```

The app and Nginx tiers were then re-applied against the now-configured DB tier:

```text
rerun-ops-sample-service-after-db-ready.txt
rerun-nginx-reverse-proxy-after-db-ready.txt
post-fix-web-ready-summary-check.txt
```

Post-fix WEB-path check:

```text
/healthz 200
/readyz 200
/api/work-orders/summary 200
```

## Incident 1: DB-backed API 503 during baseline setup

### Symptom

During `lab-full-ops-nginx-reverse-proxy.yml`, the DB-backed summary check failed:

```text
curl -k -fsS https://127.0.0.1/api/work-orders/summary
HTTP 503
```

This was not treated as a generic deployment failure. It was used as an observability diagnosis exercise.

### Evidence chain

WEB/Nginx evidence:

```text
nginx service: active
/healthz: 200
/node: 200
/readyz: 503
/api/work-orders/summary: 503
```

Nginx access log showed the request reached the WEB tier and was proxied to the WAS tier:

```text
uri="/healthz" status=200 upstream_addr="10.50.11.23:8080" upstream_status="200"
uri="/node" status=200 upstream_addr="10.50.11.23:8080" upstream_status="200"
uri="/readyz" status=503 upstream_addr="10.50.11.23:8080" upstream_status="503"
uri="/api/work-orders/summary" status=503 upstream_addr="10.50.11.23:8080" upstream_status="503"
```

WAS/app evidence:

```text
ops-sample-service: active
8080 listen: yes
local /healthz: 200
local /readyz: 503
local /api/work-orders/summary: 503
OPS_DB_URL=jdbc:postgresql://10.50.21.188:5432/opsdb
```

The app response narrowed the dependency failure to PostgreSQL connectivity:

```text
Connection to 10.50.21.188:5432 refused.
Check that the hostname and port are correct and that the postmaster is accepting TCP/IP connections.
```

DB evidence before remediation:

```text
postgresql service: inactive
/etc/postgresql/14/main/pg_hba.conf: missing
/etc/postgresql/14/main/postgresql.conf: missing
postgres user: missing
```

### Diagnosis

The failure class was narrowed to:

```text
DB tier not yet configured, while WEB and WAS process paths remained inspectable.
```

This was not an Nginx root cause and not an app process root cause. The WEB tier forwarded requests to app-01, app-01 responded, and DB-dependent paths failed because db-primary-01 did not yet have PostgreSQL configured.

### Remediation

The PostgreSQL primary playbook was re-run with a password satisfying the playbook validation rule:

```text
ops_db_password=ChangeMe-Local-Obs-20260712
```

The first attempted temporary password contained `!`, which the playbook correctly rejected because the password policy allows only:

```text
^[A-Za-z0-9_@#%+=:,.-]+$
```

After re-running PostgreSQL, app, and Nginx configuration, readiness and summary paths recovered to 200.

## Normal observability baseline evidence

The Phase 4 observability baseline playbook was run with the controlled incident disabled first:

```text
observability_run_db_service_incident=false
observability_run_consistency_probe=false
```

Evidence files collected:

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

Normal request-path evidence:

```text
observability-baseline-readyz   readyz   200
observability-baseline-summary  summary  200
```

Matching Nginx access log evidence:

```text
request_id=observability-baseline-healthz uri="/healthz" status=200 upstream_status="200"
request_id=observability-baseline-readyz  uri="/readyz" status=200 upstream_status="200"
request_id=observability-baseline-summary uri="/api/work-orders/summary" status=200 upstream_status="200"
```

## Incident 2: controlled DB service unavailable

The controlled DB incident playbook was run after baseline evidence succeeded.

### Follow-up playbook finding

The first controlled incident attempt failed before stopping PostgreSQL because `observability_db_target` was not defined.

Evidence note:

```text
observability-db-target-variable-error-note.txt
```

Runtime workaround used:

```text
observability_db_target=db-primary-01
```

This is a playbook default-variable bug, not an AWS runtime failure. A future fix should define the DB target safely from the `db` inventory group or require it explicitly with a clearer assertion.

### Incident execution result

The rerun succeeded:

```text
observability-baseline-with-db-incident-rerun.txt
```

Play recap:

```text
app-01        failed=0
backup-01     failed=0
db-primary-01 failed=0
nfs-01        failed=0
nginx-01      failed=0
```

The playbook sequence confirmed:

```text
Capture healthy readiness before DB service incident: ok
Stop PostgreSQL service on DB target: changed
Confirm PostgreSQL service is not active during incident: ok
Probe service paths during DB outage: ok
Capture Nginx logs during DB service incident: ok
Capture app journal during DB service incident: ok
Capture DB service state during incident: ok
Restart PostgreSQL service after optional DB incident: changed
Wait for DB service port after recovery: ok
Capture post-recovery readiness through Nginx: ok
Write DB service incident report on WEB node: changed
Fetch DB service incident reports to controller: changed
```

Generated incident evidence files:

```text
incident-db-service-unavailable.tsv
incident-nginx-db-service-unavailable-nginx-01.txt
incident-report-db-service-unavailable.md
```

### Incident report summary

The incident report identified the scenario:

```text
PostgreSQL service was intentionally stopped on db-primary-01 during a planned observability validation window.
```

During the outage:

```text
healthz 200
readyz 503
summary 503
```

Nginx logs showed the WEB tier continued to proxy to the app tier:

```text
request_id=observability-db-service-incident-healthz uri="/healthz" status=200 upstream_status="200"
request_id=observability-db-service-incident-readyz  uri="/readyz" status=503 upstream_status="503"
request_id=observability-db-service-incident-summary uri="/api/work-orders/summary" status=503 upstream_status="503"
```

DB service state during incident:

```text
service=postgresql
service_state=inactive
port_5432=<empty>
```

The failure class was narrowed to:

```text
DB service unavailable while db-primary-01 host remained reachable.
```

Post-recovery check:

```text
readyz_after_recovery_rc=0
```

Additional post-incident manual recovery check:

```text
PostgreSQL service: active
5432 listen: yes
/readyz: 200
/api/work-orders/summary: 200
```

## Evidence files preserved locally

Local evidence file list after validation:

```text
ansible-ping.txt
check-db-state-after-controlled-incident-rerun.txt
check-db-state-after-observability-db-target-error.txt
check-web-recovery-after-controlled-incident-rerun.txt
diagnose-app-db-backed-summary-503.txt
diagnose-db-postgresql-summary-503.txt
diagnose-web-nginx-summary-503.txt
evidence-file-list.txt
incident-db-service-unavailable.tsv
incident-nginx-db-service-unavailable-nginx-01.txt
incident-report-db-service-unavailable.md
lab-full-ops-app-nfs-mount-baseline.txt
lab-full-ops-nfs-storage-baseline.txt
lab-full-ops-nginx-reverse-proxy.txt
lab-full-ops-ops-sample-service.txt
lab-full-ops-postgresql-primary.txt
nginx-summary-503-failure-note.txt
node-baseline-app-01.txt
node-baseline-backup-01.txt
node-baseline-db-primary-01.txt
node-baseline-nfs-01.txt
node-baseline-nginx-01.txt
observability-baseline-playbook.txt
observability-baseline-with-db-incident-rerun.txt
observability-db-target-variable-error-note.txt
post-fix-web-ready-summary-check.txt
postgresql-primary-after-rerun-check.txt
request-path-nginx-01.tsv
request-path-nginx-01.txt
rerun-nginx-reverse-proxy-after-db-ready.txt
rerun-ops-sample-service-after-db-ready.txt
rerun-postgresql-primary-after-db-missing.txt
service-baseline-app-app-01.txt
service-baseline-backup-backup-01.txt
service-baseline-db-db-primary-01.txt
service-baseline-storage-nfs-01.txt
service-baseline-web-nginx-01.txt
terraform-output.txt
terraform-state-list-after-apply.txt
terraform-destroy.txt
terraform-state-list-after-destroy.txt
```

## Completion criteria

Phase 4 runtime validation criteria were met:

```text
node baseline evidence exists for nginx-01, app-01, db-primary-01, nfs-01, backup-01
Nginx access/error log evidence was preserved
ops-sample-service journal evidence was preserved
PostgreSQL service/log evidence was preserved
NFS/storage service and filesystem evidence was preserved
backup/ops node visibility evidence was preserved
request-path report was preserved
one DB service incident report used evidence to narrow the failure class
AWS resources were destroyed after evidence collection
```

## Follow-up work

Recommended next PR:

```text
[FIX] Define DB incident target default for observability baseline
```

Reason:

```text
The controlled incident worked after explicitly passing observability_db_target=db-primary-01.
The playbook should either derive this from groups['db'][0] or fail earlier with a clearer assertion.
```

Optional later work after the fix:

```text
add lightweight Prometheus/node-exporter or log-query follow-up only if it supports a new incident report
avoid Grafana/dashboard-first expansion
avoid claiming production monitoring maturity
```
