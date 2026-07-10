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

## Status

Terraform code has not been implemented yet.
