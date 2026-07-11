# lab-full-ops Terraform skeleton

`lab-full-ops` is the Phase 2 infrastructure skeleton for the project:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

This environment extends the completed `lab-full-min` WEB/WAS/DB topology with storage, backup, observability, logging, and load generation nodes. It is still not a Terraform showcase. Terraform is used only to create and destroy a repeatable EC2 VM lab for later evidence-producing operations work.

## Node layout

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

## Network intent

| Flow | Purpose |
|---|---|
| operator CIDR -> bastion-01:22 | controlled SSH entry point |
| operator/web CIDR -> nginx-01:80/443 | WEB entry for validation |
| bastion SG -> all lab nodes:22 | private node administration through bastion |
| nginx SG -> app SG:8080 | WEB to WAS upstream traffic |
| app SG -> db SG:5432 | WAS to PostgreSQL traffic |
| app SG -> storage SG:2049 | app file operations against `nfs-01` |
| backup SG -> db SG:5432 | future `pg_dump` from `backup-01` |
| backup SG -> storage SG:2049 | future file backup from `nfs-01` |
| loadgen SG -> nginx SG:443 | private failure-window traffic generation |
| monitoring SG -> lab node SGs:9100 | future node-level metric scrape path |
| VPC CIDR -> log SG:3100 | future log shipping path |
| private subnets -> NAT Gateway | package/runtime dependency retrieval |

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

Runtime validation should be done once in a later validation issue. Do not repeatedly create and destroy this environment for small edits.

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
