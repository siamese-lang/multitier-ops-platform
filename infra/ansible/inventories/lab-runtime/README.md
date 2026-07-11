# lab-runtime Ansible inventory

This inventory targets the Terraform `lab-runtime` environment.

`lab-runtime` differs from `lab-small` in one important way: the app node is still private and has no public IP, but its private subnet has default egress through a NAT Gateway. This inventory is used to verify that Ansible can control the runtime hosts and that the private app node can reach HTTPS endpoints for later dependency retrieval.

## Control node

Run Ansible from WSL Ubuntu, Linux, or macOS.

Do not run this baseline from native Windows Git Bash. Git Bash can be used for Terraform and SSH checks, but Ansible control-node behavior is expected from WSL or another Unix-like control node.

## Security rules

- Do not copy private SSH keys to `bastion-01`.
- Keep the private key only on the Ansible control node.
- Keep real IP addresses and local key paths in ignored `hosts.yml`.
- Commit only `hosts.yml.example`.
- Do not commit `terraform.tfvars`, `tfplan`, `.tfstate`, private keys, credentials, or session tokens.

## Prepare Terraform lab-runtime first

Create the runtime environment before using this inventory.

```bash
cd infra/terraform/envs/lab-runtime
cp terraform.tfvars.example terraform.tfvars
# edit key_name and operator_cidr
terraform init
terraform validate
terraform plan -out tfplan
terraform apply tfplan
terraform output
```

Record these outputs:

```text
bastion_public_ip
app_private_ip
```

Destroy the Terraform resources immediately after evidence collection because this environment includes a NAT Gateway.

## Prepare the local inventory

From `infra/ansible`:

```bash
cp inventories/lab-runtime/hosts.yml.example inventories/lab-runtime/hosts.yml
```

Edit `inventories/lab-runtime/hosts.yml`:

```text
/home/<wsl-user>/.ssh/my-web-key.pem -> your actual WSL private-key path
<bastion_public_ip> -> Terraform output value
<app_private_ip> -> Terraform output value
```

Example WSL private-key preparation:

```bash
mkdir -p ~/.ssh
cp /mnt/d/Project/my-web-key.pem ~/.ssh/my-web-key.pem
chmod 400 ~/.ssh/my-web-key.pem
```

## Validate inventory and connectivity

From `infra/ansible`:

```bash
export ANSIBLE_CONFIG="$PWD/ansible.cfg"
ansible --version
ansible-inventory -i inventories/lab-runtime/hosts.yml --list
ansible -i inventories/lab-runtime/hosts.yml bastion -m ping
ansible -i inventories/lab-runtime/hosts.yml app -m ping
```

Expected result:

```text
bastion-01 | SUCCESS => ping=pong
app-01     | SUCCESS => ping=pong
```

The app ping must use the bastion `ProxyCommand` path. A successful app ping proves that Ansible can control the private app node without copying private keys to the bastion host.

## Run the NAT egress check playbook

```bash
ansible-playbook -i inventories/lab-runtime/hosts.yml playbooks/lab-runtime-egress-check.yml
```

The playbook:

- gathers facts from both hosts
- records host identity
- runs an HTTPS egress check from `app-01`
- writes `/tmp/multitier-ops-platform/lab-runtime-egress.txt`
- reads the report back

Expected app evidence:

```text
app_private_node=True
app_has_public_ip=False
app_private_egress_mode=nat-gateway
https_egress_rc=0
https_egress_first_line=HTTP/2 301
```

A different HTTP status can still be acceptable if the command exits with `rc=0` and shows an HTTP response. The important point is that the private app node reaches the HTTPS endpoint through NAT.

## Out of scope

This inventory and playbook do not install:

- Docker
- Java
- PostgreSQL
- OpenKoda
- Nginx
- monitoring agents
- backup tooling

Those steps belong to later implementation issues.
