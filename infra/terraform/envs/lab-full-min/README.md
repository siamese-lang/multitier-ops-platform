# lab-full-min Terraform baseline

`lab-full-min` is the Phase 1 infrastructure baseline for the project:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

This environment is not a Terraform showcase and is not an OpenKoda deployment target by itself. It creates the minimum EC2 VM layout required for later WEB/WAS/DB operating evidence.

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
```

## Network intent

| Flow | Purpose |
|---|---|
| operator CIDR -> bastion-01:22 | controlled SSH entry point |
| operator/web CIDR -> nginx-01:80 | HTTP entry and HTTP-to-HTTPS redirect validation |
| operator/web CIDR -> nginx-01:443 | HTTPS entry for Nginx TLS termination and reverse proxy validation |
| bastion SG -> nginx/app/db:22 | private node administration through bastion |
| nginx SG -> app SG:8080 | WEB to WAS upstream traffic |
| app SG -> db SG:5432 | WAS to PostgreSQL traffic |
| private app/db subnets -> NAT Gateway | package/runtime dependency retrieval |

The app and DB nodes do not receive public IP addresses.

## Files

```text
.gitignore
README.md
versions.tf
variables.tf
locals.tf
main.tf
nginx_https_ingress.tf
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

## Expected next issues

This baseline intentionally stops at infrastructure creation. Later issues should add:

1. Ansible inventory for `lab-full-min`.
2. Nginx installation and upstream configuration.
3. Application workload deployment on `app-01` and `app-02`.
4. PostgreSQL installation on `db-primary-01`.
5. First incident drill: stop `app-01`, observe Nginx upstream behavior, verify `app-02` continues serving traffic, then recover `app-01`.

## Out of scope

- Nginx installation
- application deployment
- PostgreSQL installation
- monitoring/logging/backup nodes
- incident drill execution
- OpenKoda customization
- AWS managed service architecture

## Cleanup

After any runtime validation, destroy immediately:

```bash
terraform destroy
terraform state list
```

`terraform state list` should return no resources after teardown.
