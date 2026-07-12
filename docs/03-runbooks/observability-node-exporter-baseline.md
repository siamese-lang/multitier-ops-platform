# Observability node exporter baseline

## Purpose

This runbook defines the minimum host-metric endpoint baseline for the EC2 WEB/WAS/DB/Storage/Backup operating nodes.

The goal is not to claim a complete monitoring platform. The goal is to make each operating node expose basic host metrics that can later be scraped by Prometheus and used in incident diagnosis.

## Scope

Target nodes:

```text
nginx-01
app-01
db-primary-01
nfs-01
backup-01
```

Evidence collected per node:

```text
node exporter service state
node exporter listening port 9100
local /metrics endpoint availability
sampled host metrics
controller-side evidence report
```

Sampled metrics include:

```text
node_uname_info
node_boot_time_seconds
node_cpu_seconds_total
node_memory_MemAvailable_bytes
node_filesystem_avail_bytes
node_network_receive_bytes_total
```

## Execution

Run from WSL, not Git Bash.

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible

export ANSIBLE_CONFIG="$PWD/ansible.cfg"
export ANSIBLE_HOST_KEY_CHECKING=False

EVIDENCE_DIR="/tmp/observability-node-exporter-validation-$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$EVIDENCE_DIR"

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/observability-node-exporter-baseline.yml \
  -e "observability_controller_evidence_dir=${EVIDENCE_DIR}" \
  | tee "$EVIDENCE_DIR/observability-node-exporter-baseline-playbook.txt"
```

## Success criteria

For each operating node:

```text
prometheus-node-exporter package is installed
prometheus-node-exporter service is active
local TCP port 9100 is listening
http://127.0.0.1:9100/metrics responds successfully
node-exporter-baseline-<host>.txt is fetched to the controller evidence directory
```

Expected evidence files:

```text
node-exporter-baseline-nginx-01.txt
node-exporter-baseline-app-01.txt
node-exporter-baseline-db-primary-01.txt
node-exporter-baseline-nfs-01.txt
node-exporter-baseline-backup-01.txt
```

## Operating interpretation

This baseline supports host-level diagnosis questions such as:

```text
Is the host reachable and exporting metrics?
Is node memory pressure visible?
Is filesystem free space visible?
Is CPU activity visible?
Is network byte activity visible?
```

It does not yet answer higher-level monitoring questions such as:

```text
Which Prometheus scrape target is down?
Was an alert fired?
Was an operator notified?
What was the p95/p99 application latency before and after the incident?
Which dashboard panel showed the symptom first?
```

Those require the next monitoring stages.

## Boundary

This runbook does not claim:

```text
production monitoring maturity
complete Prometheus coverage
Grafana dashboard readiness
Alertmanager notification maturity
SLO/SLA compliance
```

The supported claim after runtime validation is narrower:

```text
Node exporter metric endpoints were prepared for EC2 WEB/WAS/DB/Storage/Backup host-level monitoring evidence.
```

## Next steps

Recommended follow-up sequence:

```text
1. Validate node exporter runtime evidence in a planned AWS window.
2. Add a Prometheus scrape baseline.
3. Validate scrape target health and metric retention.
4. Add one metric-based incident report.
5. Add a minimal Grafana dashboard only after Prometheus evidence exists.
6. Add one alert rule after the metric source is proven.
```
