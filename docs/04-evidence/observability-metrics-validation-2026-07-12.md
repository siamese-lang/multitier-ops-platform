# Observability metrics validation evidence - 2026-07-12

## Project scope

Fixed project theme:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

This validation extends the Phase 4 observability work from log/service/request-path evidence to host-level metric scrape evidence.

The goal was not to build a production monitoring platform or a dashboard gallery. The goal was to prove that a dedicated monitoring node can scrape host-level node metrics from the EC2 WEB/WAS/DB/Storage/Backup tiers and use those metrics together with request-path evidence during a DB service incident.

## Validation claim

Narrow claims supported by this runtime window:

```text
Prometheus scrape evidence validated host-level node_exporter targets for EC2 WEB/WAS/DB/Storage/Backup diagnosis.

Prometheus metrics helped distinguish DB host reachability from DB service dependency failure.
```

This validation does not claim:

```text
production monitoring maturity
complete Prometheus/Grafana/Loki platform coverage
Alertmanager notification maturity
PostgreSQL HA
automatic failover
SLO/SLA compliance
```

## Runtime window

Runtime evidence window:

```text
observability-metrics-20260712T121325Z
```

Execution split followed the project policy:

```text
Git Bash = Terraform apply/destroy
WSL      = Ansible, Git work, evidence organization
AWS      = apply once -> validation -> evidence collect -> destroy once
```

The local evidence archive was preserved outside the repository:

```text
.tmp/observability-metrics-20260712T121325Z.tar.gz
archive_size=24K
```

Raw runtime evidence was not committed to the repository.

## Runtime topology

Terraform created the `lab-full-ops` profile with monitoring enabled:

```text
bastion-01     public=52.78.100.172 private=10.50.1.134
nginx-01       public=52.79.247.232 private=10.50.1.42
app-01         private=10.50.11.35
db-primary-01  private=10.50.21.31
nfs-01         private=10.50.31.146
backup-01      private=10.50.41.197
mon-01         private=10.50.41.129
```

Terraform profile flags:

```text
app_02=false
backup_node=true
loadgen_node=false
logging_node=false
monitoring_node=true
nat_gateway=true
storage_node=true
```

NAT was enabled only for the batched runtime validation window:

```text
nat_eip_public_ip=52.78.160.8
nat_gateway_id=nat-0dff692b6f7e15276
```

The monitoring flow matched the Terraform security group intent:

```text
monitoring SG -> lab node SGs:9100 when mon-01 is enabled
```

## Terraform lifecycle evidence

Terraform apply evidence was collected before Ansible validation:

```text
terraform-plan.txt
terraform-apply.txt
terraform-output.txt
terraform-state-list-after-apply.txt
```

Destroy evidence:

```text
Destroy complete! Resources: 35 destroyed.
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
web-ready-summary-before-metrics.txt
```

The pre-metrics WEB path was healthy:

```text
/healthz 200
/readyz 200
/api/work-orders/summary 200
```

## Node exporter evidence

The node exporter baseline playbook was executed for the operating nodes:

```text
observability-node-exporter-baseline.txt
```

Generated evidence files:

```text
node-exporter-baseline-nginx-01.txt
node-exporter-baseline-app-01.txt
node-exporter-baseline-db-primary-01.txt
node-exporter-baseline-nfs-01.txt
node-exporter-baseline-backup-01.txt
```

The node exporter evidence included sampled host metrics from each operating node, including:

```text
node_cpu_seconds_total
node_filesystem_avail_bytes
node_memory_MemAvailable_bytes
```

This validated that each operating node exposed local host metrics on node_exporter port 9100.

## Prometheus scrape evidence

The Prometheus scrape baseline was executed on `mon-01`:

```text
observability-prometheus-scrape-baseline.txt
prometheus-scrape-baseline-mon-01.txt
```

The first Prometheus pass confirmed that Prometheus was installed and active, but captured an incomplete target state:

```text
prometheus_service_state=active
prometheus_listening_port=:9090
promtool check config=success
prometheus_targets_configured=5 targets
prometheus_ready=Prometheus is Ready
prometheus_targets_api activeTargets=[]
prometheus_up_query result=[]
```

This was treated as a useful runtime finding, not as final evidence. `mon-01` could directly reach all node_exporter targets, and a manual config/restart with additional wait time proved the scrape path itself was valid.

Confirmed scrape targets:

```text
10.50.1.42:9100     nginx-01
10.50.11.35:9100    app-01
10.50.21.31:9100    db-primary-01
10.50.31.146:9100   nfs-01
10.50.41.197:9100   backup-01
```

Manual runtime confirmation after Prometheus restart:

```text
activeTargets=5
10.50.1.42:9100     health=up lastError=""
10.50.11.35:9100    health=up lastError=""
10.50.21.31:9100    health=up lastError=""
10.50.31.146:9100   health=up lastError=""
10.50.41.197:9100   health=up lastError=""
```

Prometheus `up` query returned value `1` for every target:

```text
up{instance="10.50.1.42:9100"}=1
up{instance="10.50.11.35:9100"}=1
up{instance="10.50.21.31:9100"}=1
up{instance="10.50.31.146:9100"}=1
up{instance="10.50.41.197:9100"}=1
```

## Playbook reproducibility fix

The runtime finding showed that Prometheus `/-/ready` alone was not a sufficient scrape validation criterion. Prometheus could be HTTP-ready before `/api/v1/targets` and the `up` query reflected the desired scrape state.

Follow-up fix:

```text
PR #130 [FIX] Wait for Prometheus scrape targets before evidence
```

After PR #130, the Prometheus baseline playbook waits for both target registration and healthy `up` results before collecting final evidence:

```text
/api/v1/targets?state=any -> expected active target count
up{job="node-exporter-operating-nodes"} -> expected healthy result count
```

The fixed playbook was re-run in the same runtime window without recreating Terraform resources. Expected values were observed:

```text
active_targets=5
healthy_targets=5
up_results=5
healthy_up_results=5
```

This proved the Prometheus scrape baseline is reproducible through Ansible, not only through a manual runtime workaround.

## Metric-based DB service incident

### Scenario

PostgreSQL service was stopped on `db-primary-01` while node_exporter and Prometheus remained active.

Expected diagnostic distinction:

```text
Prometheus up{instance="10.50.21.31:9100"}=1
  -> DB host is reachable from mon-01.

/readyz and /api/work-orders/summary return 503
  -> application DB dependency is failing.

PostgreSQL service inactive and port 5432 closed
  -> failure class is DB service unavailable, not DB host unavailable.
```

### Before incident

Prometheus showed all five node_exporter targets as up:

```text
10.50.1.42:9100     up=1
10.50.11.35:9100    up=1
10.50.21.31:9100    up=1
10.50.31.146:9100   up=1
10.50.41.197:9100   up=1
```

DB-specific metric check before the incident:

```text
up{instance="10.50.21.31:9100"}=1
```

WEB path before the incident:

```text
/healthz 200
/readyz 200
/api/work-orders/summary 200
```

### During incident

PostgreSQL service was stopped:

```text
systemctl stop postgresql
systemctl is-active postgresql -> inactive
port 5432 -> no LISTEN evidence
```

WEB path during the incident:

```text
/healthz 200
/readyz 503
/api/work-orders/summary 503
```

Application readiness narrowed the problem to PostgreSQL connectivity:

```text
Connection to 10.50.21.31:5432 refused.
Check that the hostname and port are correct and that the postmaster is accepting TCP/IP connections.
```

Summary endpoint during the incident:

```text
status=db-unavailable
operation=work_orders.summary
message=Connection to 10.50.21.31:5432 refused.
```

Nginx access log showed the WEB tier continued to proxy to app-01 and that the app tier returned DB-dependent 503 responses:

```text
uri="/healthz" status=200 upstream_addr="10.50.11.35:8080" upstream_status="200"
uri="/readyz" status=503 upstream_addr="10.50.11.35:8080" upstream_status="503"
uri="/api/work-orders/summary" status=503 upstream_addr="10.50.11.35:8080" upstream_status="503"
```

Prometheus still showed the DB host exporter as reachable:

```text
up{instance="10.50.21.31:9100"}=1
node_cpu_seconds_total{instance="10.50.21.31:9100",mode="idle"}=sampled
```

### Diagnosis

The failure class was narrowed to:

```text
DB service unavailable while db-primary-01 host remained reachable.
```

This was not a DB host outage, not a WEB tier outage, and not an app process outage. The host-level metrics proved the DB node remained reachable from the monitoring tier, while the application readiness and summary paths proved the DB service dependency failed.

### Recovery

PostgreSQL service was started again:

```text
systemctl start postgresql
systemctl is-active postgresql -> active
0.0.0.0:5432 LISTEN
[::]:5432 LISTEN
```

WEB path recovered:

```text
/readyz 200
/api/work-orders/summary 200
```

Prometheus continued to report the DB host node_exporter as up:

```text
up{instance="10.50.21.31:9100"}=1
```

## Evidence files preserved locally

Local evidence file list after validation:

```text
ansible-ping.txt
diagnose-mon-to-node-exporter-connectivity-bash.txt
diagnose-mon-to-node-exporter-connectivity.txt
diagnose-prometheus-empty-targets.txt
evidence-file-list-final.txt
evidence-file-list.txt
lab-full-ops-app-nfs-mount-baseline.txt
lab-full-ops-nfs-storage-baseline.txt
lab-full-ops-nginx-reverse-proxy.txt
lab-full-ops-ops-sample-service.txt
lab-full-ops-postgresql-primary.txt
metric-incident-after-prometheus-up.txt
metric-incident-after-web-path.txt
metric-incident-before-prometheus-up.txt
metric-incident-before-web-path.txt
metric-incident-db-service-recovered.txt
metric-incident-db-service-stopped.txt
metric-incident-during-prometheus-up.txt
metric-incident-during-web-path.txt
metric-incident-summary.md
node-exporter-baseline-app-01.txt
node-exporter-baseline-backup-01.txt
node-exporter-baseline-db-primary-01.txt
node-exporter-baseline-nfs-01.txt
node-exporter-baseline-nginx-01.txt
observability-metrics-validation-summary.md
observability-node-exporter-baseline.txt
observability-prometheus-scrape-baseline-after-fix.txt
observability-prometheus-scrape-baseline.txt
prometheus-scrape-baseline-mon-01.txt
prometheus-scrape-runtime-fixed-mon-01.txt
runtime-fix-prometheus-config-copy.txt
runtime-fix-prometheus-targets-check-bash.txt
runtime-fix-prometheus-targets-check.txt
terraform-apply.txt
terraform-destroy.txt
terraform-output.txt
terraform-plan.txt
terraform-state-list-after-apply.txt
terraform-state-list-after-destroy.txt
web-ready-summary-before-metrics.txt
```

## Final supported portfolio statement

Safe portfolio wording:

```text
Built an EC2-based WEB/WAS/DB/Storage/Backup lab and validated host-level observability by scraping node_exporter metrics from WEB, WAS, DB, NFS, and backup nodes into Prometheus on a separate monitoring node. During a controlled PostgreSQL service outage, Prometheus showed the DB host remained reachable while application readiness and DB-backed API paths returned 503, allowing the failure class to be narrowed to DB service unavailability rather than host, WEB, or WAS failure.
```

Avoid overclaiming:

```text
This evidence does not prove production-grade monitoring, alert routing, HA, automatic failover, or SLO/SLA compliance.
```
