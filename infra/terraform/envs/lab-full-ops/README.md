# lab-full-ops Terraform skeleton

`lab-full-ops` is the Phase 2 infrastructure skeleton for the project:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

This environment extends the completed `lab-full-min` WEB/WAS/DB topology with storage, backup, observability, logging, and load generation tiers. It is still not a Terraform showcase. Terraform is used only to create and destroy a repeatable EC2 VM lab for later evidence-producing operations work.

## Target topology

The full target topology remains:

```text
[Public Subnet]
- bastion-01
- nginx-01

[Private App Subnet]
- app-01
- app-02

[Private DB Subnet]
- db-primary-01

[Private Storage Subnet]
- nfs-01

[Private Ops Subnet]
- backup-01
- mon-01
- log-01
- loadgen-01
```

## Default Free Tier validation mode

The default apply is reduced for AWS Free Tier/vCPU-limited accounts:

```text
created by default:
- bastion-01
- nginx-01
- app-01
- db-primary-01
- nfs-01
- backup-01

not created by default:
- app-02
- mon-01
- log-01
- loadgen-01
- NAT Gateway
```

The defaults use `t3.micro` because the current account rejected `t2.micro` as not Free Tier eligible. The default EBS volume sizes are 8 GiB to keep short-lived validation runs small.

This mode is still not guaranteed to be cost-free. Keep runtime validation short and destroy the environment immediately after collecting evidence.

## Full target options

Enable these only for an intentional full validation run:

```hcl
enable_app_02          = true
enable_monitoring_node = true
enable_logging_node    = true
enable_loadgen_node    = true
enable_nat_gateway     = true
```

`enable_nat_gateway = true` creates NAT Gateway and EIP resources. Do not enable it for simple Ansible control-path validation.

## Network intent

| Flow | Purpose |
|---|---|
| operator CIDR -> bastion-01:22 | controlled SSH entry point |
| operator/web CIDR -> nginx-01:80/443 | WEB entry for validation |
| bastion SG -> created lab nodes:22 | private node administration through bastion |
| nginx SG -> app SG:8080 | WEB to WAS upstream traffic |
| app SG -> db SG:5432 | WAS to PostgreSQL traffic |
| app SG -> storage SG:2049 | app file operations against `nfs-01` when enabled |
| backup SG -> db SG:5432 | future `pg_dump` from `backup-01` when enabled |
| backup SG -> storage SG:2049 | future file backup from `nfs-01` when enabled |
| loadgen SG -> nginx SG:443 | private failure-window traffic generation when enabled |
| monitoring SG -> lab node SGs:9100 | future node-level metric scrape path when enabled |
| VPC CIDR -> log SG:3100 | future log shipping path when enabled |
| private subnets -> NAT Gateway | disabled by default; enabled only when `enable_nat_gateway=true` |

The app, DB, storage, and ops nodes do not receive public IP addresses.

## Files

```text
.gitignore
README.md
versions.tf
variables.tf
locals.tf
main.tf
outputs.tf
terraform.tfvars.example
```

## Usage

Prepare local variables:

```bash
cp terraform.tfvars.example terraform.tfvars
```

Edit these values:

```text
operator_cidr
web_ingress_cidr
key_name
```

Run static validation:

```bash
terraform fmt -check
terraform init
terraform validate
terraform plan -out tfplan
```

For Ansible control-path validation, apply the default reduced profile only:

```bash
terraform apply tfplan
terraform output
```

Then copy the created node IPs into the ignored Ansible inventory. Do not run `apply` again without first checking state.

## What this skeleton intentionally does not do

This PR creates the VM and network skeleton only. It does not configure:

- NFS exports or mounts
- application file upload/download behavior
- Prometheus, Grafana, Alertmanager, Loki, or Alloy
- `pg_dump`, restic, backup manifests, or restore flows
- incident drills
- restore-lab
- OpenKoda feature or UI work

## Expected next issues

1. `[ANSIBLE] lab-full-ops inventory/control path`
2. `[ANSIBLE] nfs-01 file storage baseline`
3. `[APP] minimal file metadata/upload/download operational endpoint or harness`
4. `[INCIDENT] file storage failure and recovery drill`
5. `[ANSIBLE] pg_dump + restic backup baseline`
6. `[VALIDATION] restore-lab DB/file restore verification`

## Cleanup

After any runtime validation, destroy immediately:

```bash
terraform destroy
terraform state list
```

`terraform state list` should return no resources after teardown.
