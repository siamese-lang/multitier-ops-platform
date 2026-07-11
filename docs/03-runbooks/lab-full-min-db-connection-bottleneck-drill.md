# lab-full-min DB connection bottleneck drill

## Purpose

This runbook validates DB-backed request behavior under concurrent traffic in the `lab-full-min` WEB/WAS/DB environment.

Validated path:

```text
operator / local curl
  -> nginx-01:443
  -> app-01/app-02:8080
  -> db-primary-01:5432
```

The intent is operational diagnosis, not application benchmarking.

The evidence should show how a DB-related pressure scenario can be examined from multiple layers:

```text
Nginx access log: request_id, upstream_addr, upstream_status, upstream_response_time
app journal: request_id, path, status, durationMs
PostgreSQL: pg_stat_activity and PostgreSQL logs
```

## When to run

Run this only when the full `lab-full-min` stack is active:

```text
Terraform lab-full-min resources exist
Ansible inventory points to the current Terraform outputs
PostgreSQL primary is configured
ops-sample-service is active on app-01/app-02
Nginx HTTPS reverse proxy is active on nginx-01
```

This playbook does **not** create AWS resources. If the current resources are still active from a continuous validation session, this drill can run without another Terraform apply/destroy cycle.

Final cleanup is still mandatory when the continuous validation session ends.

## What this drill does

The playbook:

1. Checks Nginx state.
2. Checks app-01/app-02 service state.
3. Checks PostgreSQL service state.
4. Confirms baseline `/healthz` through Nginx.
5. Confirms baseline `/api/work-orders/summary` through Nginx.
6. Captures PostgreSQL connection/activity state before load.
7. Sends concurrent DB-backed requests through Nginx.
8. Summarizes request counts and timing.
9. Captures PostgreSQL connection/activity state after load.
10. Captures PostgreSQL log tail.
11. Captures Nginx access/error log tail.
12. Captures app journal samples for the request prefix.
13. Confirms DB-backed summary still succeeds after load.
14. Writes a report under `/tmp/multitier-ops-platform/`.

## Default load profile

Default values:

```text
request_count=80
concurrency=16
endpoint=https://127.0.0.1/api/work-orders/summary
request_prefix=lab-full-min-db-bottleneck
```

The request volume is intentionally moderate. It is designed to generate observable evidence without turning the lab into a stress-test exercise.

## Execute

From WSL/Linux/macOS:

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible
export ANSIBLE_CONFIG="$PWD/ansible.cfg"

ansible-playbook \
  -i inventories/lab-full-min/hosts.yml \
  playbooks/lab-full-min-db-connection-bottleneck-drill.yml
```

Optional override:

```bash
ansible-playbook \
  -i inventories/lab-full-min/hosts.yml \
  playbooks/lab-full-min-db-connection-bottleneck-drill.yml \
  -e db_bottleneck_drill_request_count=120 \
  -e db_bottleneck_drill_concurrency=24
```

Do not raise concurrency aggressively unless you are prepared to inspect failures and clean up immediately afterward.

## Evidence collection

After the playbook finishes, collect the report:

```bash
ansible -i inventories/lab-full-min/hosts.yml nginx-01 -b -m shell -a "cat /tmp/multitier-ops-platform/lab-full-min-db-connection-bottleneck-drill-nginx-01.txt"
```

Collect request result rows if needed:

```bash
ansible -i inventories/lab-full-min/hosts.yml nginx-01 -b -m shell -a "tail -n 100 /tmp/multitier-ops-platform/lab-full-min-db-connection-bottleneck-results.tsv"
```

Collect Nginx logs:

```bash
ansible -i inventories/lab-full-min/hosts.yml nginx-01 -b -m shell -a "tail -n 120 /var/log/nginx/ops_access.log"
ansible -i inventories/lab-full-min/hosts.yml nginx-01 -b -m shell -a "tail -n 120 /var/log/nginx/ops_error.log"
```

Collect app journals:

```bash
ansible -i inventories/lab-full-min/hosts.yml app -b -m shell -a "journalctl -u ops-sample-service -n 120 --no-pager | grep lab-full-min-db-bottleneck || true"
```

Collect PostgreSQL activity and logs:

```bash
ansible -i inventories/lab-full-min/hosts.yml db-primary-01 -b -m shell -a "sudo -u postgres psql -d opsdb -Atc \"select count(*) from pg_stat_activity where datname = 'opsdb';\""
ansible -i inventories/lab-full-min/hosts.yml db-primary-01 -b -m shell -a "tail -n 120 /var/log/postgresql/postgresql-*-main.log"
```

## How to interpret the result

A clean run generally looks like this:

```text
request_count=80
success_count=80
failure_count=0
http_200=80
post_load_summary_rc=0
```

This proves that the current DB-backed workload handled the moderate concurrent request profile.

The value of this drill is the cross-layer evidence:

- Nginx shows upstream timing and routing.
- app journal shows request duration and request ID propagation.
- PostgreSQL shows connection/activity state and slow-query/log settings.

If failures occur, the run is still useful if the evidence identifies where the failure surfaced:

```text
Nginx 5xx or timeout
app journal slow request or DB error
PostgreSQL connection/activity/log evidence
```

## Acceptance criteria

For a successful validation run:

```text
Nginx service active
app-01/app-02 service active
PostgreSQL service active
baseline /api/work-orders/summary succeeds
concurrent request summary is captured
post-load /api/work-orders/summary succeeds
Nginx access log contains lab-full-min-db-bottleneck request IDs
app journal contains request logs for the same time window or request prefix
PostgreSQL activity/log samples are captured
```

## Cleanup policy

This playbook does not change infrastructure or stop services. However, it is intended to run inside a continuous AWS validation session.

When the continuous session ends, clean up with Terraform:

```bash
cd /c/Project/test/multitier-ops-platform/infra/terraform/envs/lab-full-min
terraform destroy
terraform state list
```

Expected final result:

```text
terraform state list prints nothing
```

## Out of scope

- PostgreSQL failover
- standby promotion
- schema migration
- production-grade load testing
- JMeter/k6 benchmark design
- application feature development
