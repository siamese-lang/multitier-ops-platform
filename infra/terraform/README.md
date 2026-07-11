# Terraform

Terraform provisions AWS infrastructure for this project.

## Environment directories

```text
infra/terraform/envs/lab-small/
infra/terraform/envs/lab-full/
```

## Scope

Terraform is responsible for infrastructure resources such as:

- VPC, subnets, route tables
- security groups
- EC2 instances
- key pair references
- EBS volumes where required
- outputs used by Ansible inventory generation

Terraform should not install application packages or configure services. Server configuration belongs in Ansible.

## lab-small baseline

The first Terraform implementation should follow:

- `docs/01-architecture/terraform-lab-small-baseline.md`
- `docs/05-decisions/ADR-0003-terraform-lab-small-baseline.md`

The initial baseline is intentionally smaller than the eventual `lab-small` service topology. It should create only the minimum network and EC2 resources needed to prove repeatable provisioning, controlled SSH access, and private app-node placement.

Initial baseline nodes:

```text
bastion-01
app-01
```

Later issues may extend `lab-small` with Nginx, OpenKoda deployment, database separation, and storage after this baseline has apply, SSH, output, and destroy evidence.

## Workflow

For each environment:

```bash
terraform fmt -check
terraform init
terraform validate
terraform plan
terraform apply
terraform output
```

When resources are not in use:

```bash
terraform destroy
```

## Evidence requirements

Each Terraform issue should include:

- `terraform fmt -check` result
- `terraform validate` result
- `terraform plan` result
- `terraform apply` result if applied
- `terraform output` result
- `terraform destroy` result when teardown is part of the scenario

## State files

Terraform state files must not be committed.

The first single-operator baseline may use local state, but `.tfstate` and `.tfstate.backup` files must remain ignored. Remote state can be introduced later if collaboration, locking, or longer-lived environments become necessary.

## Status

Terraform code has not been implemented yet.