# lab-full-min app-01 failure drill

## Purpose

This runbook validates the first incident scenario for the `lab-full-min` WEB/WAS/DB operating environment.

The normal path validated in the integrated baseline is:

```text
operator -> nginx-01:443 -> app-01/app-02:8080 -> db-primary-01:5432
```

This drill intentionally stops the application service on `app-01` and verifies that Nginx continues to serve DB-backed traffic through the remaining `app-02` upstream.

```text
app-01 stopped
  -> nginx-01 keeps serving HTTPS traffic
  -> app-02 continues reaching db-primary-01
  -> app-01 is restored before the playbook completes
```

This is not an application feature test. It is an operating evidence scenario for upstream failure handling, service recovery, and log-based verification.

## Prerequisites

Run this drill only after the full `lab-full-min` stack is active and verified.

Required baseline:

```text
Terraform lab-full-min resources are running
Ansible inventory hosts.yml is prepared
PostgreSQL primary is active on db-primary-01
ops-sample-service is active on app-01 and app-02
Nginx HTTPS reverse proxy is active on nginx-01
```

Do not run this drill against a partially configured environment.

## Files

```text
infra/ansible/playbooks/lab-full-min-app-failure-drill.yml
```

Runtime report path on `nginx-01`:

```text
/tmp/multitier-ops-platform/lab-full-min-app-failure-drill-nginx-01.txt
```

## Execution

Run from WSL/Linux/macOS, not native Windows Git Bash.

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible
export ANSIBLE_CONFIG="$PWD/ansible.cfg"

ansible-playbook \
  -i inventories/lab-full-min/hosts.yml \
  playbooks/lab-full-min-app-failure-drill.yml
```

## What the playbook does

The playbook runs from the WEB tier and delegates service control to the app nodes.

1. Confirms the `app` inventory group contains at least two nodes.
2. Confirms `app-01` and `app-02` exist.
3. Checks Nginx and app service state before the drill.
4. Confirms baseline `/healthz` and DB-backed `/api/work-orders/summary` through Nginx.
5. Stops `ops-sample-service` on `app-01`.
6. Confirms `app-01` is inactive and `app-02` remains active.
7. Sends repeated HTTPS requests through local Nginx:

```text
/healthz
/node
/api/work-orders/summary
```

8. Adds deterministic `X-Request-Id` values to drill traffic.
9. Reads Nginx access and error logs after the failure window.
10. Restarts `ops-sample-service` on `app-01` in an `always` block.
11. Confirms `app-01` and `app-02` are active again.
12. Writes a drill report.

## Acceptance criteria

The drill is successful when all of the following are true:

```text
app-01 becomes inactive during the failure window
app-02 remains active during the failure window
Nginx HTTPS requests continue to return success
/api/work-orders/summary returns DB-backed data during the failure window
app-01 is restored before completion
app-01 and app-02 are active after recovery
play recap shows unreachable=0 failed=0
```

## Evidence to capture

Capture the following output for the later evidence document.

```text
1. Play recap
2. app failure drill report
3. failure window request output
4. Nginx access log tail
5. Nginx error log tail
6. post-drill app service state
```

Additional manual checks:

```bash
ansible -i inventories/lab-full-min/hosts.yml app -b -m command -a "systemctl is-active ops-sample-service"

ansible -i inventories/lab-full-min/hosts.yml nginx-01 -b -m shell -a "cat /tmp/multitier-ops-platform/lab-full-min-app-failure-drill-nginx-01.txt"

ansible -i inventories/lab-full-min/hosts.yml nginx-01 -b -m shell -a "grep 'lab-full-min-app01-failure' /var/log/nginx/ops_access.log | tail -n 30"

ansible -i inventories/lab-full-min/hosts.yml nginx-01 -b -m shell -a "tail -n 30 /var/log/nginx/ops_error.log"
```

Expected report fields:

```text
nginx_pre_state=active
target_stopped_state=inactive
survivor_during_failure_state=active
target_recovered_state=active
survivor_recovered_state=active
baseline_health_rc=0
baseline_summary_rc=0
failure_window_requests_rc=0
post_recovery_summary_rc=0
```

## Log interpretation

The most important evidence is the Nginx access log.

Look for lines containing the drill request prefix:

```text
lab-full-min-app01-failure
```

Useful fields:

```text
request_id
status
upstream_addr
upstream_status
upstream_response_time
```

During the failure window, successful lines should show Nginx returning `status=200` while proxying to the available app upstream.

Example expected shape:

```text
request_id=lab-full-min-app01-failure-3-summary status=200 upstream_addr="10.40.11.x:8080" upstream_status="200"
```

If the failed upstream is attempted first, the log may show multiple upstream values in one line depending on Nginx retry behavior. That is acceptable if the final client response remains successful and the survivor app handles the request.

## Recovery requirement

The playbook uses an `always` block to restart `app-01` even if the failure-window request check fails.

After the playbook, confirm both app nodes are active:

```bash
ansible -i inventories/lab-full-min/hosts.yml app -b -m command -a "systemctl is-active ops-sample-service"
```

Do not proceed to destroy until both app nodes are restored or the failure state is intentionally captured as a failed incident.

## Cleanup policy

This runbook itself does not create or destroy AWS resources.

When executing the drill in a real validation issue, follow the same cleanup rule as the integrated validation:

```bash
cd /c/Project/test/multitier-ops-platform/infra/terraform/envs/lab-full-min
terraform destroy
terraform state list
```

`terraform state list` should print nothing after teardown.

## Limitations

This drill verifies Nginx upstream continuity for one stopped app service. It does not yet cover:

```text
rolling deploy
DB connection bottleneck
PostgreSQL primary failure
file storage failure
monitoring alert delivery
backup and restore
```

Those should remain separate incident or recovery scenarios.
