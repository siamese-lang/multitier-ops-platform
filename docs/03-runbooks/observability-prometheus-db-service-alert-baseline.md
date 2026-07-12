# Prometheus DB service alert baseline

## Purpose

This runbook prepares a narrow Prometheus alert-rule validation for the project:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

The goal is not Alertmanager maturity or production alerting. The goal is to prove that Prometheus rule evaluation can distinguish this condition:

```text
DB host is reachable from Prometheus, but PostgreSQL service is inactive.
```

This extends the completed metrics validation:

```text
Prometheus scrape evidence validated host-level node_exporter targets for EC2 WEB/WAS/DB/Storage/Backup diagnosis.
Prometheus metrics helped distinguish DB host reachability from DB service dependency failure.
```

## Why this uses node_exporter systemd metrics

The current Prometheus baseline scrapes node_exporter. It does not scrape application-specific metrics and it does not use blackbox exporter.

That means Prometheus does not directly know that `/readyz` returns HTTP 503. The previous runtime proved `/readyz` and `/api/work-orders/summary` degradation through Nginx and app evidence, while Prometheus proved DB host reachability through node_exporter.

For a minimal Prometheus rule baseline, the correct next metric is PostgreSQL service state from the DB node. The playbook enables the node_exporter systemd collector on the DB tier and loads a Prometheus rule for:

```promql
up{job="node-exporter-operating-nodes",instance="<db-primary-01>:9100"} == 1
and on(instance, job)
node_systemd_unit_state{job="node-exporter-operating-nodes",instance="<db-primary-01>:9100",name="postgresql.service",state="active"} == 0
```

The `on(instance, job)` vector matching is intentional. `up` and `node_systemd_unit_state` have different label sets, so a plain `and` can fail to match even when both metrics are present.

## Prepared playbook

```text
infra/ansible/playbooks/observability-prometheus-db-service-alert-baseline.yml
```

The playbook has two parts:

```text
1. DB tier
   - install prometheus-node-exporter if needed
   - enable node_exporter systemd collector on db-primary-01
   - verify node_systemd_unit_state for postgresql.service
   - fetch DB systemd metric evidence

2. monitoring tier
   - write Prometheus rule file
   - write Prometheus config with rule_files and node_exporter scrape targets
   - run promtool check rules
   - run promtool check config
   - verify the rule is loaded through /api/v1/rules
   - verify the DB systemd metric is queryable through Prometheus
   - optionally stop PostgreSQL to validate firing state
```

## Required topology

Use the monitoring-enabled `lab-full-ops` profile:

```text
bastion-01
nginx-01
app-01
db-primary-01
nfs-01
backup-01
mon-01
```

Required inventory groups:

```text
web
app
db
storage
backup
monitoring
```

## Prerequisites

Complete these first:

```text
1. Terraform apply once with monitoring_node=true.
2. Populate inventories/lab-full-ops/hosts.yml, including mon-01.
3. Confirm Ansible ping reaches operating nodes and mon-01.
4. Configure WEB/WAS/DB/NFS/Backup baseline.
5. Run observability-node-exporter-baseline.yml.
6. Run observability-prometheus-scrape-baseline.yml.
```

Do not create a new AWS runtime only for this playbook unless the previous runtime has already been destroyed. The project policy remains:

```text
apply once -> configure baseline -> collect evidence -> destroy once
```

## Baseline execution without incident

From WSL:

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible

export ANSIBLE_CONFIG="$PWD/ansible.cfg"
export ANSIBLE_HOST_KEY_CHECKING=False

REPO=/mnt/c/Project/test/multitier-ops-platform
EVIDENCE_DIR="$REPO/.tmp/observability-db-service-alert-$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$EVIDENCE_DIR"

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/observability-prometheus-db-service-alert-baseline.yml \
  -e "observability_controller_evidence_dir=${EVIDENCE_DIR}" \
  | tee "$EVIDENCE_DIR/observability-prometheus-db-service-alert-baseline.txt"
```

This validates rule loading and normal-state metric availability only. It should not claim that the alert fired.

## Optional runtime incident execution

Run this only during a planned validation window. This temporarily stops PostgreSQL and restarts it in the playbook `always` block.

```bash
ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/observability-prometheus-db-service-alert-baseline.yml \
  -e "observability_controller_evidence_dir=${EVIDENCE_DIR}" \
  -e "observability_run_db_alert_incident=true" \
  | tee "$EVIDENCE_DIR/observability-prometheus-db-service-alert-incident.txt"
```

Expected firing-state evidence:

```text
ALERTS{alertname="DbPostgresqlServiceInactiveWhileHostReachable",alertstate="firing"}
```

The same evidence should also show:

```text
up{job="node-exporter-operating-nodes",instance="<db-primary-01>:9100"} == 1
node_systemd_unit_state{...,name="postgresql.service",state="active"} == 0
```

## Evidence files

Expected controller-side evidence:

```text
db-node-exporter-systemd-db-primary-01.txt
prometheus-db-service-alert-baseline-mon-01.txt
observability-prometheus-db-service-alert-baseline.txt
```

If optional incident validation is enabled:

```text
prometheus-db-service-alert-incident-mon-01.txt
observability-prometheus-db-service-alert-incident.txt
```

## Success criteria

Baseline success:

```text
node_exporter is active on db-primary-01.
node_systemd_unit_state includes postgresql.service.
promtool check rules succeeds.
promtool check config succeeds.
Prometheus /api/v1/rules includes DbPostgresqlServiceInactiveWhileHostReachable.
Prometheus can query the PostgreSQL systemd active-state metric.
```

Incident success:

```text
During a planned PostgreSQL service stop,
Prometheus keeps DB host up=1,
PostgreSQL active-state metric becomes 0,
and ALERTS shows DbPostgresqlServiceInactiveWhileHostReachable firing.
```

## Supported claim after incident validation

```text
Prometheus rule evaluation detected PostgreSQL service inactivity while the DB host remained reachable.
```

## Not claimed

```text
Alertmanager notification maturity
paging or on-call workflow
production monitoring maturity
PostgreSQL HA
automatic failover
SLO/SLA compliance
Grafana dashboard readiness
```

## Cleanup

After the runtime validation window:

```bash
# Git Bash only
cd /c/Project/test/multitier-ops-platform/infra/terraform/envs/lab-full-ops
terraform destroy -var-file=observability-metrics.tfvars
terraform state list
```

Preserve local raw evidence under `.tmp`, but do not commit raw evidence archives.
