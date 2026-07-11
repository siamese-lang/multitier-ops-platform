# lab-full-min rolling restart drill

This runbook describes the controlled rolling restart validation for the `lab-full-min` WEB/WAS/DB environment.

The scenario verifies that one app node can be taken out of service at a time while Nginx continues to serve DB-backed traffic through the remaining app node.

```text
operator -> nginx-01:443 -> app-01/app-02:8080 -> db-primary-01:5432
```

## Purpose

The completed integrated validation proves the normal request path. The app failure drill proves that Nginx can bypass a failed `app-01` node.

This drill proves a related but more controlled maintenance scenario:

```text
stop app-01 -> traffic continues through app-02 -> restore app-01
stop app-02 -> traffic continues through app-01 -> restore app-02
```

This is a rolling restart style validation. It does not deploy a new application version yet. It focuses on WEB/WAS continuity while one WAS node is intentionally unavailable.

## Preconditions

Run this drill only after the full `lab-full-min` stack is active.

Required baseline:

```text
Terraform lab-full-min resources are active
Ansible inventory is current
PostgreSQL primary is active
ops-sample-service is active on app-01 and app-02
Nginx HTTPS reverse proxy is active
```

Do not run this drill against an incomplete environment.

## Local execution path

Use WSL/Linux/macOS for Ansible.

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible
export ANSIBLE_CONFIG="$PWD/ansible.cfg"
```

Do not run Ansible from native Windows Git Bash.

## Inventory

The local inventory must exist and remain untracked:

```text
infra/ansible/inventories/lab-full-min/hosts.yml
```

It must contain the current Terraform outputs for:

```text
bastion_public_ip
nginx_private_ip
app-01 private IP
app-02 private IP
db-primary-01 private IP
private key path
```

## Pre-checks

Check control path:

```bash
ansible-inventory -i inventories/lab-full-min/hosts.yml --graph
ansible -i inventories/lab-full-min/hosts.yml lab_full_min -m ping
```

Confirm baseline service state:

```bash
ansible -i inventories/lab-full-min/hosts.yml nginx-01 -b -m command -a "systemctl is-active nginx"
ansible -i inventories/lab-full-min/hosts.yml app -b -m command -a "systemctl is-active ops-sample-service"
```

Confirm public HTTPS is working from the operator machine:

```bash
curl -k -i https://<nginx_public_ip>/healthz
curl -k -s https://<nginx_public_ip>/api/work-orders/summary
```

## Execute the drill

```bash
ansible-playbook \
  -i inventories/lab-full-min/hosts.yml \
  playbooks/lab-full-min-rolling-restart-drill.yml
```

## What the playbook does

The playbook runs on `nginx-01` and delegates app service control to `app-01` and `app-02`.

It performs the following sequence:

```text
1. Confirm Nginx is active.
2. Confirm app-01 and app-02 are active.
3. Confirm baseline /healthz and /api/work-orders/summary through Nginx.
4. Stop app-01.
5. Confirm app-02 remains active.
6. Send repeated /healthz, /node, and /api/work-orders/summary requests through Nginx.
7. Restart app-01 and confirm it is active.
8. Confirm DB-backed summary through Nginx.
9. Stop app-02.
10. Confirm app-01 remains active.
11. Send repeated /healthz, /node, and /api/work-orders/summary requests through Nginx.
12. Restart app-02 and confirm it is active.
13. Confirm both app nodes are active.
14. Capture Nginx access/error log tails.
15. Write a report under /tmp/multitier-ops-platform/.
```

## Report path

On `nginx-01`:

```text
/tmp/multitier-ops-platform/lab-full-min-rolling-restart-drill-nginx-01.txt
```

Collect it with:

```bash
ansible -i inventories/lab-full-min/hosts.yml nginx-01 -b -m shell -a "cat /tmp/multitier-ops-platform/lab-full-min-rolling-restart-drill-nginx-01.txt"
```

Expected report fields:

```text
drill=rolling_restart_nginx_upstream_continuity
first_target=app-01
first_survivor=app-02
second_target=app-02
second_survivor=app-01
first_target_stopped_state=inactive
first_survivor_state=active
first_window_requests_rc=0
first_target_recovered_state=active
second_target_stopped_state=inactive
second_survivor_state=active
second_window_requests_rc=0
second_target_recovered_state=active
```

## Additional evidence collection

After the playbook completes, collect app states:

```bash
ansible -i inventories/lab-full-min/hosts.yml app -b -m command -a "systemctl is-active ops-sample-service"
```

Collect Nginx logs:

```bash
ansible -i inventories/lab-full-min/hosts.yml nginx-01 -b -m shell -a "tail -n 120 /var/log/nginx/ops_access.log"
ansible -i inventories/lab-full-min/hosts.yml nginx-01 -b -m shell -a "tail -n 120 /var/log/nginx/ops_error.log"
```

The access log should show deterministic request IDs:

```text
lab-full-min-rolling-restart-app01-*-healthz
lab-full-min-rolling-restart-app01-*-node
lab-full-min-rolling-restart-app01-*-summary
lab-full-min-rolling-restart-app02-*-healthz
lab-full-min-rolling-restart-app02-*-node
lab-full-min-rolling-restart-app02-*-summary
```

During the app-01 window, successful responses should use `app-02` as the effective upstream after Nginx observes the unavailable node.

During the app-02 window, successful responses should use `app-01` as the effective upstream after Nginx observes the unavailable node.

Some early requests may show combined upstream attempts such as:

```text
upstream_addr="app-stopped:8080, app-survivor:8080"
upstream_status="502, 200"
```

That is valid evidence. It means Nginx attempted the stopped upstream, retried the survivor, and returned a successful response to the client.

## Acceptance criteria

The drill passes when:

```text
play recap has failed=0
first_window_requests_rc=0
second_window_requests_rc=0
app-01 active after drill
app-02 active after drill
Nginx access log contains rolling request IDs
DB-backed /api/work-orders/summary stays successful during both windows
```

## Cleanup policy

This runbook does not create Terraform resources.

If this drill is part of a continuous validation session and the same live environment will be used by the next validation, do not destroy immediately. Record that cleanup is intentionally deferred.

If no further validation will run immediately, destroy the AWS resources:

```bash
cd /c/Project/test/multitier-ops-platform/infra/terraform/envs/lab-full-min
terraform destroy
terraform state list
```

Expected final state:

```text
terraform state list prints nothing
```

## Out of scope

- blue/green deployment
- new application artifact deployment
- database schema migration
- PostgreSQL failover
- observability stack installation
- backup/restore execution
