# lab-full-ops Ansible inventory

This inventory targets the Terraform `lab-full-ops` environment.

`lab-full-ops` is the Phase 2 VM-based operating environment for storage, backup, observability, logging, and load generation expansion. This inventory does not configure those services. It only verifies that Ansible can control the created nodes through the intended bastion path before service installation starts.

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

## Default validation profile

The default Terraform profile is reduced for AWS Free Tier and vCPU-limited accounts. The default Ansible validation group is therefore also reduced:

```text
lab_full_ops / lab_full_ops_free_tier:
- bastion-01
- nginx-01
- app-01
- db-primary-01
- nfs-01
- backup-01
```

Optional nodes are kept in `lab_full_ops_optional` and should be used only when the matching Terraform toggles were enabled:

```text
lab_full_ops_optional:
- app-02
- mon-01
- log-01
- loadgen-01
```

Do not run control-path validation against optional nodes unless Terraform actually created them.

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
- All other created nodes are controlled through the bastion `ProxyCommand` path.
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
terraform output
```

For the default reduced profile, record these outputs:

```bash
terraform output public_node_ips
terraform output private_node_ips
terraform output validation_profile
```

You need these values for the default `hosts.yml` validation:

```text
bastion_public_ip
nginx_private_ip
app_01_private_ip
db_primary_private_ip
nfs_01_private_ip
backup_01_private_ip
```

These values are optional and should be filled only if the matching Terraform toggles were enabled:

```text
app_02_private_ip
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
<db_primary_private_ip>             -> Terraform output value
<nfs_01_private_ip>                 -> Terraform output value
<backup_01_private_ip>              -> Terraform output value
```

Leave optional placeholders unchanged unless the optional Terraform nodes were created.

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

Expected high-level result for the default reduced profile:

```text
bastion-01    | SUCCESS => ping=pong
nginx-01      | SUCCESS => ping=pong
app-01        | SUCCESS => ping=pong
db-primary-01 | SUCCESS => ping=pong
nfs-01        | SUCCESS => ping=pong
backup-01     | SUCCESS => ping=pong
```

The successful pings prove that the Free Tier validation control path is ready for later NFS and backup configuration work.

## Optional full-target check

Only after creating optional Terraform nodes, validate them explicitly:

```bash
ansible -i inventories/lab-full-ops/hosts.yml lab_full_ops_optional -m ping
```

Do not include this in the default validation run.

## Baseline report

The baseline check playbook writes one report per default validation node:

```text
/tmp/multitier-ops-platform/lab-full-ops-baseline-<inventory_hostname>.txt
```

Each report records:

- project and environment
- validation profile
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
