# lab-small vs lab-full

## Purpose

The project intentionally starts with `lab-small` before building the full two-AZ environment. This reduces debugging complexity and creates a clean evidence trail from a minimal working baseline to a more realistic operations topology.

## lab-small

`lab-small` is the first AWS deployment target.

Expected scope:

- bastion-01
- nginx-01
- app-01
- db-primary-01
- optional nfs-01 or local filesystem storage depending on the OpenKoda verification result

Primary goals:

- Verify Terraform provisioning workflow.
- Verify Ansible server configuration workflow.
- Verify OpenKoda can run on EC2.
- Verify basic HTTP routing through Nginx.
- Collect first infrastructure and application evidence.

Out of scope for initial `lab-small`:

- Multi-AZ redundancy
- PostgreSQL standby
- Full Prometheus/Grafana/Loki stack
- Restic restore-lab
- High-volume load testing

## lab-full

`lab-full` is the final target environment.

Expected scope:

- bastion-01
- nginx-01, nginx-02
- app-01, app-02, app-03
- db-primary-01, db-standby-01
- nfs-01
- mon-01
- log-01
- backup-01
- loadgen-01

Primary goals:

- Verify tier separation.
- Verify operational observability.
- Verify backup and restore procedures.
- Run controlled failure scenarios.
- Produce runbooks and incident reports suitable for a portfolio review.

## Progression rule

Do not move to `lab-full` until `lab-small` has evidence for:

- Terraform plan/apply success
- SSH access path
- Ansible recap
- service status
- HTTP response check
- application log check
- rollback or destroy procedure
