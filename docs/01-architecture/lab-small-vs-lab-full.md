# lab-small vs lab-full

## Purpose

The project intentionally starts with `lab-small` before building the full two-AZ environment. This reduces debugging complexity and creates a clean evidence trail from a minimal working baseline to a more realistic operations topology.

## lab-small phases

`lab-small` is the first AWS deployment target, but it should not be built all at once.

The project separates `lab-small` into phases:

1. Terraform baseline
2. Ansible baseline
3. OpenKoda deployment
4. Nginx routing
5. database and storage expansion if needed

This prevents a small environment from becoming a hidden full-stack deployment before the provisioning and access path are proven.

## lab-small phase 1: Terraform baseline

The first Terraform baseline is defined in:

- `docs/01-architecture/terraform-lab-small-baseline.md`
- `docs/05-decisions/ADR-0003-terraform-lab-small-baseline.md`

Initial scope:

- bastion-01
- app-01
- VPC
- public subnet
- private subnet
- Internet Gateway
- route tables
- security groups
- Terraform outputs for later Ansible inventory

Primary goals:

- Verify Terraform provisioning workflow.
- Verify controlled SSH access path.
- Verify private app-node placement.
- Verify Terraform output values.
- Verify destroy procedure.

Out of scope for phase 1:

- OpenKoda deployment
- Docker installation
- Ansible server configuration
- Nginx routing
- PostgreSQL separation
- NFS
- monitoring/logging
- backup/restore

## lab-small later phases

After the Terraform baseline is proven, `lab-small` can expand toward:

- bastion-01
- nginx-01
- app-01
- db-primary-01
- optional nfs-01 or local filesystem storage depending on the OpenKoda verification result

Primary goals:

- Verify Ansible server configuration workflow.
- Verify OpenKoda can run on EC2 through repeatable automation.
- Verify basic HTTP routing through Nginx.
- Collect first infrastructure and application evidence.

Out of scope for `lab-small` overall:

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

Do not move from Terraform baseline to Ansible deployment until the baseline has evidence for:

- `terraform fmt -check`
- `terraform validate`
- `terraform plan`
- `terraform apply`
- `terraform output`
- SSH to bastion
- SSH jump to private app node
- `terraform destroy`