# Terraform lab-small Baseline

## Purpose

This document defines the first Terraform baseline for `lab-small`.

The goal is to create a minimal, repeatable AWS EC2 lab that can be provisioned, inspected, and destroyed before adding Ansible deployment, Nginx routing, separated database nodes, NFS, monitoring, logging, or backup components.

This is a design document. It does not implement Terraform resources.

## Background

Previous steps established the following baseline:

- OpenKoda is the upstream application target.
- OpenKoda runs on a temporary EC2 instance through Docker Compose.
- Initial upstream inspection did not confirm Spring Boot Actuator, health, or Prometheus metrics support.
- The first `lab-small` phase should not modify OpenKoda source code.
- Early health checks should use coarse infrastructure-level verification before introducing custom endpoints.

## Design objective

The first Terraform implementation should prove that the repository can manage AWS infrastructure as code in a controlled way.

It should answer these questions:

1. Can the lab network be created and destroyed consistently?
2. Can the operator reach the environment through a controlled SSH path?
3. Can a private app node be created without direct public exposure?
4. Can Terraform outputs provide enough information for later Ansible inventory?
5. Can evidence be collected for plan, apply, output, and destroy?

## Baseline topology

The first `lab-small` Terraform baseline should create only:

```text
operator
  |
  | SSH from operator public IP only
  v
bastion-01              public subnet
  |
  | SSH from bastion only
  v
app-01                  private subnet
```

`app-01` is the future OpenKoda application host. In the first Terraform implementation it may remain a plain EC2 instance without OpenKoda installed.

## AWS region

Default target region:

```text
ap-northeast-2
```

This region is suitable because the operator is in Korea and the earlier manual EC2 test used the Seoul region.

The region should be configurable through Terraform variables.

## Network resources

Terraform should create:

| Resource | Purpose |
|---|---|
| VPC | Isolated lab network |
| Public subnet | `bastion-01` placement |
| Private subnet | `app-01` placement |
| Internet Gateway | Public egress and SSH access to bastion |
| Public route table | `0.0.0.0/0` route to Internet Gateway |
| Private route table | No direct internet route in first baseline |
| Security groups | Tier-level access boundaries |

### CIDR proposal

Recommended CIDR values:

| Layer | CIDR |
|---|---:|
| VPC | `10.20.0.0/16` |
| Public subnet | `10.20.1.0/24` |
| Private subnet | `10.20.11.0/24` |

The exact values may be changed through variables, but the design should preserve public/private separation.

## EC2 resources

Terraform should create:

| Host | Subnet | Public IP | Initial role |
|---|---|---:|---|
| `bastion-01` | public subnet | yes | SSH entry point |
| `app-01` | private subnet | no | future OpenKoda host |

Recommended AMI family:

```text
Ubuntu Server 22.04 LTS or Ubuntu Server 24.04 LTS
```

Recommended initial instance type:

```text
t3.micro or t3.small for bastion-01
t3.small or t3.medium for app-01
```

`app-01` may require `t3.medium` later when OpenKoda and PostgreSQL run together, but the first Terraform baseline should not assume deployment has happened.

## Security groups

### `sg-bastion`

Inbound:

| Protocol | Port | Source | Reason |
|---|---:|---|---|
| TCP | 22 | operator public IP `/32` | SSH access from operator only |

Outbound:

| Protocol | Port | Destination | Reason |
|---|---:|---|---|
| all | all | VPC CIDR or default all egress | SSH to private hosts and package access if needed |

For stricter follow-up work, outbound can be narrowed after initial access evidence is stable.

### `sg-app`

Inbound:

| Protocol | Port | Source | Reason |
|---|---:|---|---|
| TCP | 22 | `sg-bastion` | SSH through bastion only |
| TCP | 8080 | `sg-bastion` | temporary private OpenKoda check path if needed |

Outbound:

| Protocol | Port | Destination | Reason |
|---|---:|---|---|
| all | all | default or scoped as needed | initial package and Docker access strategy pending |

`app-01` must not have a public IP in the first baseline.

## Key pair handling

The first Terraform implementation should reference an existing AWS key pair instead of creating and storing private keys.

Terraform variable:

```hcl
key_name = "<existing-aws-key-pair-name>"
```

Rules:

- Do not commit `.pem` files.
- Do not generate private keys into the repository.
- Document the local private key path only in operator notes, not in committed files.
- Use `.gitignore` to prevent accidental private key commits if needed.

## Terraform state

For the first baseline, local state is acceptable because this is a single-operator portfolio lab.

Recommended path:

```text
infra/terraform/envs/lab-small/terraform.tfstate
```

Rules:

- Do not commit `.tfstate` or `.tfstate.backup`.
- Add or verify `.gitignore` coverage before implementation.
- Remote state can be introduced later if the project needs collaboration, state locking, or longer-lived environments.

## Terraform outputs

The first implementation should output:

| Output | Purpose |
|---|---|
| `vpc_id` | evidence and troubleshooting |
| `public_subnet_id` | evidence and troubleshooting |
| `private_subnet_id` | evidence and troubleshooting |
| `bastion_public_ip` | SSH entry point |
| `app_private_ip` | Ansible inventory candidate |
| `bastion_security_group_id` | evidence and troubleshooting |
| `app_security_group_id` | evidence and troubleshooting |

The output must not expose secrets.

## What Terraform should not do yet

The first implementation should not create:

- Nginx nodes
- PostgreSQL DB nodes
- NFS nodes
- monitoring or logging nodes
- backup nodes
- load generator nodes
- NAT Gateway unless explicitly needed and justified
- Route 53 records
- TLS certificates
- application packages
- Docker installation
- OpenKoda containers
- Ansible inventory files as committed artifacts

Terraform may expose outputs that later help generate Ansible inventory, but server configuration belongs to Ansible.

## Evidence requirements

Each Terraform baseline PR should include evidence for:

```bash
terraform fmt -check
terraform init
terraform validate
terraform plan
```

When resources are actually created, collect evidence for:

```bash
terraform apply
terraform output
ssh -J ubuntu@<bastion_public_ip> ubuntu@<app_private_ip> hostname
terraform destroy
```

If the SSH jump command differs because of local SSH config, document the actual command used.

## Acceptance criteria for implementation

The first Terraform implementation issue can be considered complete when:

- `terraform fmt -check` passes.
- `terraform validate` passes.
- `terraform plan` shows only the expected baseline resources.
- `terraform apply` creates `bastion-01` and `app-01` successfully.
- `bastion-01` receives a public IP.
- `app-01` does not receive a public IP.
- SSH to bastion succeeds from the operator IP.
- SSH to app succeeds only through bastion.
- Terraform outputs provide bastion public IP and app private IP.
- `terraform destroy` removes created resources.

## Follow-up sequence

After this baseline is implemented and verified:

1. Add Ansible inventory generation or inventory documentation.
2. Install Docker on `app-01` through Ansible.
3. Run OpenKoda Docker Compose on `app-01`.
4. Add `nginx-01` and route private traffic to app.
5. Split database and storage only after app deployment evidence is stable.

## Related issue

This document supports Issue #14.