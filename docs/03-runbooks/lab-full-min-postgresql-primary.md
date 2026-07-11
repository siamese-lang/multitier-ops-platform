# lab-full-min PostgreSQL Primary Runbook

## Purpose

This runbook configures `db-primary-01` as the PostgreSQL primary node for `lab-full-min`.

The goal is not to showcase PostgreSQL in isolation. PostgreSQL is the DB tier required for WEB/WAS/DB operating evidence:

```text
Nginx -> app-01/app-02 -> db-primary-01
```

The DB tier enables:

- `/readyz` DB-dependent readiness
- `/db/time`
- DB-backed work order read/write traffic
- app-node continuity over the same DB state
- future DB failure and recovery drills

## Scope

This runbook uses:

```text
infra/ansible/playbooks/lab-full-min-postgresql-primary.yml
infra/ansible/inventories/lab-full-min/group_vars/db.yml
```

It configures only PostgreSQL primary. It does not configure standby replication, backup, Nginx, application deployment, or monitoring.

## Security rule

Do not commit the real DB password.

Supply `ops_db_password` from one of these methods:

```bash
ansible-playbook \
  -i inventories/lab-full-min/hosts.yml \
  playbooks/lab-full-min-postgresql-primary.yml \
  -e 'ops_db_password=<strong-local-password>'
```

or add it temporarily to ignored local inventory only:

```yaml
# inventories/lab-full-min/hosts.yml
# This file is ignored and must not be committed.
all:
  vars:
    ops_db_password: "<strong-local-password>"
```

Future improvement: move this value to Ansible Vault.

## Required inventory

The `db` group must contain `db-primary-01`.

The app subnet CIDR defaults to:

```text
10.40.11.0/24
```

Override `lab_full_min_app_cidr` in local inventory if Terraform changes the subnet CIDR.

## Run

From WSL/Linux/macOS, not native Windows Git Bash:

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible
export ANSIBLE_CONFIG="$PWD/ansible.cfg"

ansible-playbook \
  -i inventories/lab-full-min/hosts.yml \
  playbooks/lab-full-min-postgresql-primary.yml \
  -e 'ops_db_password=<strong-local-password>'
```

## Expected configuration

PostgreSQL should be configured with:

```text
listen_addresses = '*'
port = 5432
log_line_prefix = '%m [%p] user=%u db=%d app=%a client=%h '
log_min_duration_statement = 500
```

`pg_hba.conf` should allow only the app subnet to access `opsdb` as `ops_user`:

```text
host    opsdb    ops_user    10.40.11.0/24    scram-sha-256
```

Network-level restriction still belongs to the DB security group. `pg_hba.conf` is an additional DB-layer boundary, not a replacement for security groups.

## Evidence commands

On `db-primary-01`:

```bash
systemctl is-active postgresql
ss -ltnp | grep ':5432'
sudo -u postgres psql -c '\l'
sudo -u postgres psql -d opsdb -c 'select now();'
sudo grep -E "listen_addresses|log_line_prefix|log_min_duration_statement" /etc/postgresql/*/main/postgresql.conf
sudo tail -n 50 /etc/postgresql/*/main/pg_hba.conf
```

The playbook writes a report under:

```text
/tmp/multitier-ops-platform/lab-full-min-postgresql-primary-db-primary-01.txt
```

## Evidence expected in incident reports

Later incident reports should use the DB tier evidence to prove:

- DB is reachable from app nodes under normal operation.
- DB-backed data survives app-node failure.
- DB unavailability changes app readiness and DB-backed API status.
- PostgreSQL logs contain enough context to narrow down failure causes.
- Slow-query thresholds can be used as evidence during DB bottleneck drills.

## Cleanup

This playbook changes only the EC2 instance. Environment cleanup is still done by Terraform destroy from:

```bash
cd /c/Project/test/multitier-ops-platform/infra/terraform/envs/lab-full-min
terraform destroy
```
