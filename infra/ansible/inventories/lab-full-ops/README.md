# lab-full-ops Ansible inventory

This inventory targets the Terraform `lab-full-ops` environment.

`lab-full-ops` is the Phase 2 VM-based operating environment for storage, backup, observability, logging, and load generation expansion. This inventory does not configure those services. It only verifies that Ansible can control every node through the intended bastion path before service installation starts.

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

[Private Storage Subnet]
- nfs-01

[Private Ops Subnet]
- backup-01
- mon-01
- log-01
- loadgen-01
```

## Control node

Run Ansible from WSL Ubuntu, Linux, or macOS.

Do not run this baseline from native Windows Git Bash. Git Bash can be used for Terraform, but Ansible control-node behavior is expected from WSL or another Unix-like control node.

Recommended paths:

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
- All other nodes are controlled through the bastion `ProxyCommand` path.
- Keep real IP addresses and local key paths in ignored `hosts.yml`.
- Commit only `hosts.yml.example`.
- Do not commit `terraform.tfvars`, `tfplan`, `.tfstate`, private keys, credentials, or session tokens.

## Prepare Terraform lab-full-ops first

From Git Bash:

```bash
cd /c/Project/test/multitier-ops-platform/infra/terraform/envs/lab-full-ops
terraform init
terraform validate
terraform plan -out tfplan
terraform apply tfplan
```

Record these outputs:

```bash
terraform output public_node_ips
terraform output private_node_ips
```

You need these values for `hosts.yml`:

```text
bastion_public_ip
nginx_private_ip
app_01_private_ip
app_02_private_ip
db_primary_private_ip
nfs_01_private_ip
backup_01_private_ip
mon_01_private_ip
log_01_private_ip
loadgen_01_private_ip
```

Destroy the environment immediately after the validation window if no follow-up runtime check is planned.

## Prepare the local inventory

From `infra/ansible` in WSL:

```bash
cp inventories/lab-full-ops/hosts.yml.example inventories/lab-full-ops/hosts.yml
```

Edit `inventories/lab-full-ops/hosts.yml`:

```text
/home/<wsl-user>/.ssh/my-web-key.pem -> your actual WSL private-key path
<bastion_public_ip>                 -> Terraform output value
<nginx_private_ip>                  -> Terraform output value
<app_01_private_ip>                 -> Terraform output value
<app_02_private_ip>                 -> Terraform output value
<db_primary_private_ip>             -> Terraform output value
<nfs_01_private_ip>                 -> Terraform output value
<backup_01_private_ip>              -> Terraform output value
<mon_01_private_ip>                 -> Terraform output value
<log_01_private_ip>                 -> Terraform output value
<loadgen_01_private_ip>             -> Terraform output value
```

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
ansible-inventory -i inventories/lab-full-ops/hosts.yml --list
ansible -i inventories/lab-full-ops/hosts.yml lab_full_ops -m ping
ansible-playbook -i inventories/lab-full-ops/hosts.yml playbooks/lab-full-ops-baseline-check.yml
```

Expected high-level result:

```text
bastion-01    | SUCCESS => ping=pong
nginx-01      | SUCCESS => ping=pong
app-01        | SUCCESS => ping=pong
app-02        | SUCCESS => ping=pong
db-primary-01 | SUCCESS => ping=pong
nfs-01        | SUCCESS => ping=pong
backup-01     | SUCCESS => ping=pong
mon-01        | SUCCESS => ping=pong
log-01        | SUCCESS => ping=pong
loadgen-01    | SUCCESS => ping=pong
```

The successful pings prove that the control path is ready for later NFS, backup, observability, logging, and load generation configuration.

## Baseline report

The baseline check playbook writes one report per node:

```text
/tmp/multitier-ops-platform/lab-full-ops-baseline-<inventory_hostname>.txt
```

Each report records:

- project and environment
- inventory host
- node role
- expected tier
- SSH path
- default private IP observed from host facts
- expected port metadata
- Ansible group membership

## Out of scope

This inventory and baseline playbook do not install:

- NFS server or mounts
- application file upload/download behavior
- PostgreSQL
- Prometheus, Grafana, Alertmanager, Loki, or Alloy
- backup tooling
- OpenKoda-specific components

Those steps belong to later implementation issues.
