# lab-full-ops WEB/DB foundation alignment

## Purpose

This runbook aligns the inherited WEB and DB deployment path for the `lab-full-ops` storage validation window.

The project theme remains:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

The purpose is not to repeat the `lab-full-min` implementation. The purpose is to reuse the already validated PostgreSQL and Nginx deployment logic with `lab-full-ops` inventory variables so the next runtime window can validate the full operating path:

```text
operator -> nginx-01 -> app-01 -> db-primary-01
                         |
                         -> nfs-01 mounted file path
```

Without this alignment, the work-order evidence flow would be an app-only feature and would not prove the WEB/WAS/DB/storage operating path.

## Scope

This alignment adds only environment-specific wrappers and variables:

```text
infra/ansible/inventories/lab-full-ops/group_vars/db.yml
infra/ansible/inventories/lab-full-ops/group_vars/web.yml
infra/ansible/playbooks/lab-full-ops-postgresql-primary.yml
infra/ansible/playbooks/lab-full-ops-nginx-reverse-proxy.yml
```

The wrapper playbooks import the existing `lab-full-min` implementation playbooks. They do not duplicate the deployment logic.

## Why wrappers instead of copied playbooks

The deployment logic for PostgreSQL primary and Nginx reverse proxy is already validated in the earlier WEB/WAS/DB phase. Copying the playbooks would create drift and make the repository look like repeated scaffolding.

The intended reuse model is:

```text
same deployment logic
+ lab-full-ops inventory groups
+ lab-full-ops group variables
+ lab-full-ops report paths
```

## DB variables

`lab-full-ops` uses the default private app subnet:

```text
10.50.11.0/24
```

The current PostgreSQL playbook still reads this variable name:

```text
lab_full_min_app_cidr
```

For compatibility, `lab-full-ops` sets:

```yaml
lab_full_min_app_cidr: "10.50.11.0/24"
lab_full_ops_app_cidr: "{{ lab_full_min_app_cidr }}"
```

This is an intentional compatibility alias. A later refactor may rename the shared variable to a neutral name, but that is not required for the storage validation window.

The PostgreSQL report path is changed to:

```text
/tmp/multitier-ops-platform/lab-full-ops-postgresql-primary-db-primary-01.txt
```

## WEB variables

`lab-full-ops` keeps the same Nginx reverse proxy behavior:

```text
HTTP -> HTTPS redirect
self-signed TLS termination
Nginx -> app upstream proxy
X-Request-Id propagation
access/error log evidence
```

Environment-specific values are changed:

```text
TLS dir: /etc/nginx/ssl/lab-full-ops
TLS subject: /CN=lab-full-ops.local
report: /tmp/multitier-ops-platform/lab-full-ops-nginx-reverse-proxy-nginx-01.txt
```

The Nginx playbook reads the `app` inventory group. In the default reduced `lab-full-ops` profile, that group contains `app-01` only. That is acceptable for the first storage validation window because the goal is storage consistency, not multi-app upstream failover.

## Runtime batching policy

Do not run Terraform apply and destroy for this PR alone.

Use one explicit validation window:

```text
1. Apply reduced lab-full-ops Terraform profile once.
2. Fill ignored inventories/lab-full-ops/hosts.yml from Terraform outputs.
3. Confirm Ansible control path.
4. Configure PostgreSQL primary.
5. Configure nfs-01 export.
6. Configure app-01 NFS mount.
7. Deploy ops-sample-service with OPS_EVIDENCE_FILE_ROOT.
8. Configure nginx-01 reverse proxy.
9. Run work-order evidence file flow through Nginx.
10. Collect reports and logs.
11. Destroy Terraform resources.
```

AWS runtime validation is allowed in this window. The rule is to avoid opening a new runtime window for every small PR.

## Syntax checks

From `infra/ansible`:

```bash
export ANSIBLE_CONFIG="$PWD/ansible.cfg"
cp inventories/lab-full-ops/hosts.yml.example /tmp/lab-full-ops-hosts.yml

ansible-playbook \
  -i /tmp/lab-full-ops-hosts.yml \
  playbooks/lab-full-ops-postgresql-primary.yml \
  --syntax-check

ansible-playbook \
  -i /tmp/lab-full-ops-hosts.yml \
  playbooks/lab-full-ops-nginx-reverse-proxy.yml \
  --syntax-check

rm -f /tmp/lab-full-ops-hosts.yml
```

The temporary copy is intentional because some Ansible versions do not treat `.yml.example` as a YAML inventory source.

## Batched runtime commands

Run only after Terraform has created the reduced profile and `hosts.yml` has real IP addresses.

From `infra/ansible`:

```bash
export ANSIBLE_CONFIG="$PWD/ansible.cfg"

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/lab-full-ops-postgresql-primary.yml \
  -e 'ops_db_password=<strong-local-password>'

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/lab-full-ops-nfs-storage-baseline.yml

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/lab-full-ops-app-nfs-mount-baseline.yml

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/lab-full-ops-ops-sample-service.yml \
  -e 'ops_db_password=<strong-local-password>'

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/lab-full-ops-nginx-reverse-proxy.yml
```

## Evidence to collect

Minimum reports:

```text
/tmp/multitier-ops-platform/lab-full-ops-baseline-*.txt
/tmp/multitier-ops-platform/lab-full-ops-postgresql-primary-db-primary-01.txt
/tmp/multitier-ops-platform/lab-full-ops-ops-sample-service-app-01.txt
/tmp/multitier-ops-platform/lab-full-ops-nginx-reverse-proxy-nginx-01.txt
```

Minimum service checks:

```bash
systemctl is-active postgresql
systemctl is-active nfs-server
systemctl is-active ops-sample-service
systemctl is-active nginx
```

Minimum HTTP checks from `nginx-01`:

```bash
curl -k -i https://127.0.0.1/healthz
curl -k -i https://127.0.0.1/readyz
curl -k -i https://127.0.0.1/api/work-orders/summary
```

The work-order evidence file flow smoke check should be added as a separate validation playbook after these foundation wrappers are merged.

## Out of scope

This alignment does not add:

- new application endpoints
- OpenKoda feature or UI work
- monitoring/logging stack installation
- file storage failure drill execution
- backup or restore execution
- Terraform changes
