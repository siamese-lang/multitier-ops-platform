# lab-full-min Ansible inventory

This inventory targets the Terraform `lab-full-min` environment.

`lab-full-min` is the first Phase 1 WEB/WAS/DB operating environment. It is not an OpenKoda single-node runtime. The goal of this inventory is to verify that Ansible can control the WEB, WAS, and DB nodes through the intended bastion path before any service installation starts.

## Target nodes

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

## Control node

Run Ansible from WSL Ubuntu, Linux, or macOS.

Do not run this baseline from native Windows Git Bash. Git Bash can be used for Terraform, but Ansible control-node behavior is expected from WSL or another Unix-like control node.

Recommended C-drive workspace paths after migration:

```text
Git Bash / Terraform:
  /c/Project/test/multitier-ops-platform

WSL / Ansible:
  /mnt/c/Project/test/multitier-ops-platform
```

## Security rules

- Do not copy private SSH keys to `bastion-01`.
- Keep the private key only on the Ansible control node.
- Operators SSH only to `bastion-01` directly.
- `nginx-01`, `app-01`, `app-02`, and `db-primary-01` are controlled through the bastion `ProxyCommand` path.
- Keep real IP addresses and local key paths in ignored `hosts.yml`.
- Commit only `hosts.yml.example`.
- Do not commit `terraform.tfvars`, `tfplan`, `.tfstate`, private keys, credentials, or session tokens.

## Prepare Terraform lab-full-min first

Create the Terraform plan and later the runtime environment from Git Bash:

```bash
cd /c/Project/test/multitier-ops-platform/infra/terraform/envs/lab-full-min
cp terraform.tfvars.example terraform.tfvars
# edit operator_cidr, web_ingress_cidr, key_name
terraform init
terraform validate
terraform plan -out tfplan
# apply is intentionally deferred to a later validation issue
```

When a validation issue asks you to apply the environment, record these outputs:

```text
bastion_public_ip
nginx_private_ip
nginx_public_ip
app_01_private_ip
app_02_private_ip
db_primary_private_ip
```

## Prepare the local inventory

From `infra/ansible` in WSL:

```bash
cp inventories/lab-full-min/hosts.yml.example inventories/lab-full-min/hosts.yml
```

Edit `inventories/lab-full-min/hosts.yml`:

```text
/home/<wsl-user>/.ssh/my-web-key.pem -> your actual WSL private-key path
<bastion_public_ip>                 -> Terraform output value
<nginx_private_ip>                  -> Terraform output value
<app_01_private_ip>                 -> Terraform output value
<app_02_private_ip>                 -> Terraform output value
<db_primary_private_ip>             -> Terraform output value
```

`nginx_public_ip` is used for browser or HTTP ingress checks later. Ansible should still connect to `nginx-01` through bastion using the node's private IP.

Example WSL private-key preparation:

```bash
mkdir -p ~/.ssh
cp /mnt/c/Project/keys/my-web-key.pem ~/.ssh/my-web-key.pem
chmod 400 ~/.ssh/my-web-key.pem
```

## Validate inventory and control path

From `infra/ansible`:

```bash
export ANSIBLE_CONFIG="$PWD/ansible.cfg"
ansible --version
ansible-inventory -i inventories/lab-full-min/hosts.yml --list
ansible -i inventories/lab-full-min/hosts.yml lab_full_min -m ping
ansible-playbook -i inventories/lab-full-min/hosts.yml playbooks/lab-full-min-baseline-check.yml
```

Expected high-level result:

```text
bastion-01    | SUCCESS => ping=pong
nginx-01      | SUCCESS => ping=pong
app-01        | SUCCESS => ping=pong
app-02        | SUCCESS => ping=pong
db-primary-01 | SUCCESS => ping=pong
```

The successful pings prove that the control path is ready for later service installation and incident drills.

## Baseline report

The baseline check playbook writes one report per node:

```text
/tmp/multitier-ops-platform/lab-full-min-baseline-<inventory_hostname>.txt
```

Each report records:

- project and environment
- inventory host
- node role
- expected tier
- SSH path
- default private IP observed from host facts
- whether the node is expected to have a public entry point
- Ansible group membership

## Out of scope

This inventory and baseline playbook do not install:

- Nginx
- Java
- application workload
- PostgreSQL
- monitoring agents
- backup tooling
- OpenKoda-specific components

Those steps belong to later implementation issues.
