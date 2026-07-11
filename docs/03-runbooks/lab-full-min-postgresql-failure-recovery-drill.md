# lab-full-min PostgreSQL failure and recovery drill

## Purpose

This runbook validates how a DB-layer outage is observed from the WEB/WAS/DB operating path.

The target architecture is:

```text
operator
  -> nginx-01:443
  -> app-01/app-02:8080
  -> db-primary-01:5432
```

The drill intentionally stops PostgreSQL on `db-primary-01`, verifies that the app processes remain alive, observes DB-dependent endpoint failures or degradation through Nginx, restarts PostgreSQL, and verifies service recovery.

This is not an application feature test. It is an operations drill for failure isolation:

```text
process health != DB-dependent readiness
```

## Scenario

```text
1. Confirm Nginx, app-01/app-02, and PostgreSQL are active.
2. Confirm /healthz, /readyz, and /api/work-orders/summary through Nginx.
3. Stop PostgreSQL on db-primary-01.
4. Confirm app systemd services remain active.
5. Send process-level and DB-dependent requests through Nginx.
6. Capture Nginx access/error logs and app journal samples.
7. Restart PostgreSQL.
8. Confirm /readyz and /api/work-orders/summary recover.
9. Capture PostgreSQL activity/log samples.
```

## Preconditions

Run this after the `lab-full-min` environment has already been provisioned and configured:

```text
Terraform lab-full-min apply completed
Ansible inventory generated
PostgreSQL primary configured
ops-sample-service deployed on app-01/app-02
Nginx HTTPS reverse proxy configured
```

The inventory should include:

```text
bastion-01
nginx-01
app-01
app-02
db-primary-01
```

The app must expose at least:

```text
/healthz
/readyz
/api/work-orders/summary
```

Expected endpoint meaning:

```text
/healthz                  process-level health
/readyz                   DB-dependent readiness
/api/work-orders/summary  DB-backed application endpoint
```

## Execution

From WSL/Linux:

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible
export ANSIBLE_CONFIG="$PWD/ansible.cfg"

ansible-playbook \
  -i inventories/lab-full-min/hosts.yml \
  playbooks/lab-full-min-postgresql-failure-recovery-drill.yml
```

## What the playbook does

The playbook runs from `nginx-01` and delegates DB/app service checks to the corresponding hosts.

It checks the baseline state first:

```text
nginx service active
app-01 ops-sample-service active
app-02 ops-sample-service active
db-primary-01 postgresql active
/healthz through Nginx succeeds
/readyz through Nginx succeeds
/api/work-orders/summary through Nginx succeeds
```

Then it stops PostgreSQL on `db-primary-01`:

```text
systemctl stop postgresql
```

During the failure window, the playbook checks:

```text
app-01 service is still active
app-02 service is still active
/healthz still returns process-level result if app process is alive
/readyz should fail or return non-2xx because DB is unavailable
/api/work-orders/summary should fail or return non-2xx because DB is unavailable
```

The playbook does not fail simply because DB-backed endpoints fail during the failure window. That failure is the intended signal.

Finally, the playbook restarts PostgreSQL and confirms recovery:

```text
systemctl start postgresql
/readyz through Nginx succeeds again
/api/work-orders/summary through Nginx succeeds again
```

## Evidence locations

The playbook writes a report on `nginx-01`:

```text
/tmp/multitier-ops-platform/lab-full-min-postgresql-failure-recovery-drill-nginx-01.txt
```

The request probe result file is:

```text
/tmp/multitier-ops-platform/lab-full-min-postgresql-failure-results.tsv
```

The result file uses columns:

```text
request_id    endpoint    http_code    time_total    size_download    result
```

Example request IDs:

```text
lab-full-min-postgresql-failure-1-healthz
lab-full-min-postgresql-failure-1-readyz
lab-full-min-postgresql-failure-1-summary
```

Log sources:

```text
Nginx access log: /var/log/nginx/ops_access.log
Nginx error log:  /var/log/nginx/ops_error.log
App journal:      journalctl -u ops-sample-service
PostgreSQL log:   /var/log/postgresql/postgresql-*.log or systemd journal fallback
```

## Manual evidence collection

After execution, collect the report:

```bash
ansible -i inventories/lab-full-min/hosts.yml nginx-01 -b -m shell -a \
  "cat /tmp/multitier-ops-platform/lab-full-min-postgresql-failure-recovery-drill-nginx-01.txt"
```

Collect probe results:

```bash
ansible -i inventories/lab-full-min/hosts.yml nginx-01 -b -m shell -a \
  "cat /tmp/multitier-ops-platform/lab-full-min-postgresql-failure-results.tsv"
```

Confirm app services recovered:

```bash
ansible -i inventories/lab-full-min/hosts.yml app -b -m command -a \
  "systemctl is-active ops-sample-service"
```

Confirm PostgreSQL recovered:

```bash
ansible -i inventories/lab-full-min/hosts.yml db-primary-01 -b -m command -a \
  "systemctl is-active postgresql"
```

Check DB-backed recovery through Nginx:

```bash
ansible -i inventories/lab-full-min/hosts.yml nginx-01 -b -m shell -a \
  "curl -k -i https://127.0.0.1/readyz && echo && curl -k -i https://127.0.0.1/api/work-orders/summary"
```

Collect app journal samples:

```bash
ansible -i inventories/lab-full-min/hosts.yml app -b -m shell -a \
  "journalctl -u ops-sample-service -n 160 --no-pager | grep lab-full-min-postgresql-failure || true"
```

Collect Nginx logs:

```bash
ansible -i inventories/lab-full-min/hosts.yml nginx-01 -b -m shell -a \
  "tail -n 120 /var/log/nginx/ops_access.log"

ansible -i inventories/lab-full-min/hosts.yml nginx-01 -b -m shell -a \
  "tail -n 120 /var/log/nginx/ops_error.log"
```

Collect PostgreSQL log sample:

```bash
ansible -i inventories/lab-full-min/hosts.yml db-primary-01 -b -m shell -a \
  'latest_log=$(ls -1 /var/log/postgresql/postgresql-*.log 2>/dev/null | tail -n 1 || true); if [ -n "$latest_log" ]; then tail -n 120 "$latest_log"; else journalctl -u postgresql -n 120 --no-pager || true; fi'
```

## Expected interpretation

A successful drill should show:

```text
Before failure:
  /healthz succeeds
  /readyz succeeds
  /api/work-orders/summary succeeds

During DB failure:
  app systemd services remain active
  /healthz may continue to return 2xx
  /readyz returns non-2xx or fails
  /api/work-orders/summary returns non-2xx or fails
  app journal shows request IDs and DB-related failure duration/status
  Nginx access log shows request IDs, upstream status, and request timing

After recovery:
  PostgreSQL service active
  /readyz succeeds again
  /api/work-orders/summary succeeds again
```

The key operational conclusion is:

```text
WAS process availability and DB-dependent service readiness are separate signals.
```

## Safety and cleanup

The playbook uses an `always` block to restart PostgreSQL even when the failure window produces errors.

After the continuous validation session is complete, clean up AWS resources:

```bash
cd /c/Project/test/multitier-ops-platform/infra/terraform/envs/lab-full-min
terraform destroy
terraform state list
```

`terraform state list` should return no resources.
