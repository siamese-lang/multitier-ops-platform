# Prometheus scrape baseline

## Purpose

This runbook prepares the next observability layer after the Phase 4 runtime evidence baseline.

The goal is not a dashboard-first monitoring project. The goal is to prove that EC2 operating nodes expose host-level metrics that a monitoring tier can scrape and later use in incident diagnosis.

Supported future claim after runtime validation:

```text
Prometheus scrape evidence validated host-level node_exporter targets for EC2 WEB/WAS/DB/Storage/Backup diagnosis.
```

Do not claim:

```text
production monitoring maturity
complete Prometheus coverage
Grafana readiness
Alertmanager notification maturity
SLO/SLA compliance
```

## Required topology

This baseline expects the optional monitoring node to be enabled and populated in the Ansible inventory:

```text
mon-01 in the monitoring group
```

Target operating nodes:

```text
nginx-01       web
app-01         app
db-primary-01  db
nfs-01         storage
backup-01      backup
```

Terraform security group intent already matches this topology: node_exporter port 9100 on the operating nodes is intended to be scraped from the monitoring security group.

## Prerequisites

Complete these first:

```text
1. Create the lab-full-ops runtime with monitoring node enabled.
2. Populate inventories/lab-full-ops/hosts.yml, including mon-01.
3. Confirm Ansible ping reaches monitoring and operating nodes.
4. Configure the WEB/WAS/DB/Storage/Backup baseline if the incident scenario needs it.
5. Run observability-node-exporter-baseline.yml on the operating nodes.
```

## Execution

From WSL:

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible

export ANSIBLE_CONFIG="$PWD/ansible.cfg"
export ANSIBLE_HOST_KEY_CHECKING=False

EVIDENCE_DIR="/tmp/observability-prometheus-validation-$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$EVIDENCE_DIR"

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/observability-prometheus-scrape-baseline.yml \
  -e "observability_controller_evidence_dir=${EVIDENCE_DIR}" \
  | tee "$EVIDENCE_DIR/observability-prometheus-scrape-baseline.txt"
```

## Evidence produced

Controller evidence directory:

```text
prometheus-scrape-baseline-mon-01.txt
observability-prometheus-scrape-baseline.txt
```

The Prometheus scrape report should include:

```text
prometheus_service_state
prometheus_status
prometheus_listening_port
prometheus_config_check
prometheus_targets_configured
prometheus_target_registration_wait
prometheus_up_wait
prometheus_ready
prometheus_targets_api
prometheus_up_query
```

## Success criteria

Minimum acceptable evidence:

```text
Prometheus service is active.
Port 9090 is listening on mon-01.
promtool check config succeeds.
Configured targets include nginx-01, app-01, db-primary-01, nfs-01, and backup-01 private addresses on port 9100.
Prometheus /-/ready returns success.
/api/v1/targets?state=any returns the node-exporter-operating-nodes scrape job.
The target registration wait reports the expected target count.
The job-specific up query returns each expected operating-node target with value 1.
```

Important runtime note:

```text
Prometheus /-/ready only proves that the HTTP server is ready. It does not by itself prove that scrape targets have registered or returned successful samples. The playbook must wait for both target registration and the job-specific up query before collecting final evidence.
```

The first runtime pass may expose security-group, inventory, node_exporter binding, or Prometheus scrape stabilization issues. Treat those as useful operating findings, but document them separately from the final validated claim.

## Failure diagnosis guide

If `/-/ready` fails:

```text
Inspect Prometheus service state and promtool config output on mon-01.
```

If `/api/v1/targets?state=any` lists no active targets:

```text
Check whether Prometheus was restarted after writing the generated config.
Check whether the playbook waited long enough for target registration.
Check whether the generated config contains the expected static targets.
Check /api/v1/status/config to confirm Prometheus loaded the expected config.
```

If `/api/v1/targets?state=any` lists targets as down:

```text
Check whether node_exporter is active on the target node.
Check whether port 9100 is listening locally on the target node.
Check security group ingress from the monitoring security group.
Check whether hostvars target private IPs match Terraform output.
```

If the job-specific `up{job="node-exporter-operating-nodes"}` query is empty:

```text
Check whether the configured job name in /etc/prometheus/prometheus.yml matches the query.
Check whether old/manual Prometheus scrape jobs are producing stale or duplicate up series.
Use the job-specific query for final evidence instead of an unfiltered up query.
```

If only one tier is down:

```text
Use that as a tier-specific monitoring reachability finding, not as a global monitoring failure.
```

## Cleanup

This runbook does not create AWS resources. The runtime window should still follow the project policy:

```text
apply once -> configure baseline -> collect evidence -> destroy once
```

When a monitoring-node runtime is already open, do not destroy and recreate just to run adjacent observability fixes. Continue collecting related evidence in the same window, then destroy only after the planned validation scope is complete.

After validation, destroy AWS resources from Git Bash and preserve local evidence under `.tmp` or `/tmp` as appropriate.
