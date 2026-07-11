# Terraform lab-small

This directory contains the first Terraform baseline for the AWS EC2 multi-tier operations portfolio project.

The goal of this baseline is narrow:

- create a small VPC
- place `bastion-01` in a public subnet
- place `app-01` in a private subnet
- allow SSH to `bastion-01` only from the operator CIDR
- allow SSH and OpenKoda port `8080` to `app-01` only from the bastion security group
- produce outputs that can later feed Ansible inventory
- prove that the environment can be destroyed cleanly

This phase does not install Docker, deploy OpenKoda, configure Nginx, create a database node, configure NFS, or add monitoring.

## Files

```text
versions.tf
variables.tf
locals.tf
main.tf
outputs.tf
terraform.tfvars.example
.gitignore
```

## Prerequisites

Required on the operator machine:

- Terraform CLI
- AWS credentials configured outside this repository
- an existing EC2 key pair in the target region
- the operator public IP in CIDR form, for example `203.0.113.10/32`

Do not commit:

- AWS credentials
- private keys
- `terraform.tfvars`
- `.terraform/`
- `*.tfstate`
- saved plan files

## Configure variables

Copy the example file:

```bash
cp terraform.tfvars.example terraform.tfvars
```

Edit at minimum:

```hcl
key_name      = "your-existing-ec2-keypair-name"
operator_cidr = "your.public.ip.address/32"
```

To discover the operator public IP from a local terminal, use a trusted external IP lookup method and append `/32`.

## Run

```bash
terraform fmt -check
terraform init
terraform validate
terraform plan -out tfplan
terraform apply tfplan
terraform output
```

## Verify SSH path

Use the output command templates:

```bash
terraform output ssh_to_bastion
terraform output ssh_to_app_via_bastion
```

Expected access path:

```text
operator laptop -> bastion-01 public IP -> app-01 private IP
```

`app-01` should not have a public IP.

## Evidence to collect

Store sanitized command outputs under the appropriate evidence directory or GitHub issue comment:

```text
terraform fmt -check
terraform init
terraform validate
terraform plan -out tfplan
terraform apply tfplan
terraform output
ssh to bastion result
ssh jump to app result
```

Do not paste private key material, AWS access keys, session tokens, or full state files.

## Destroy

When the test is complete:

```bash
terraform destroy
```

Collect evidence that the destroy plan and result completed successfully.

## Current limitations

- The private subnet has no NAT Gateway in this first baseline.
- `app-01` cannot directly install packages from the internet unless a later issue adds a controlled egress path.
- OpenKoda is not deployed in this issue.
- Ansible inventory generation is not implemented yet, but outputs provide the values needed for it.
