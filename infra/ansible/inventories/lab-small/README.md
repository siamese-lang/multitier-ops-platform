# lab-small Ansible inventory

## Purpose

This inventory is the first Ansible control-plane baseline for `lab-small`.

It proves that Ansible can reach:

- `bastion-01` through its public IP
- `app-01` through `bastion-01` by SSH `ProxyCommand`

It does not install Docker, Java, OpenKoda, Nginx, PostgreSQL, monitoring agents, or backup tooling.

## Control node requirement

Run Ansible from WSL/Ubuntu, Linux, or macOS.

The earlier manual SSH tests were performed from Windows Git Bash, but this Ansible baseline is written for a Unix-like Ansible control node. On Windows, use WSL/Ubuntu for the Ansible commands.

Recommended WSL key handling:

```bash
mkdir -p ~/.ssh
cp /mnt/d/Project/my-web-key.pem ~/.ssh/my-web-key.pem
chmod 400 ~/.ssh/my-web-key.pem
```

Do not copy the private key to `bastion-01`.

## Prerequisite: recreate Terraform lab-small

If `terraform destroy` has already been run, recreate the Terraform baseline first:

```bash
cd infra/terraform/envs/lab-small
terraform plan -out tfplan
terraform apply tfplan
terraform output
```

Record these output values:

- `bastion_public_ip`
- `app_private_ip`

## Create local inventory

From the repository root:

```bash
cp infra/ansible/inventories/lab-small/hosts.yml.example \
   infra/ansible/inventories/lab-small/hosts.yml
```

Edit `hosts.yml` and replace:

- `<BASTION_PUBLIC_IP>` with `terraform output bastion_public_ip`
- `<APP_PRIVATE_IP>` with `terraform output app_private_ip`
- `/home/ubuntu-user/.ssh/my-web-key.pem` with the WSL/Linux key path

The real `hosts.yml` file is ignored by Git because it contains runtime IPs and local key paths.

## Validate inventory

Run from `infra/ansible` so `ansible.cfg` is picked up:

```bash
cd infra/ansible
ansible --version
ansible-inventory -i inventories/lab-small/hosts.yml --list
```

## Connectivity checks

```bash
ansible -i inventories/lab-small/hosts.yml bastion -m ping
ansible -i inventories/lab-small/hosts.yml app -m ping
```

Expected result:

```text
SUCCESS => {
    "changed": false,
    "ping": "pong"
}
```

## Baseline playbook

```bash
ansible-playbook -i inventories/lab-small/hosts.yml playbooks/lab-small-baseline.yml
```

The playbook only:

- gathers facts
- prints host identity
- creates `/tmp/multitier-ops-platform/`
- writes `/tmp/multitier-ops-platform/ansible-baseline.txt`
- reads the report back

## No-NAT constraint

`app-01` is in a private subnet without NAT Gateway. Therefore, package installation from external repositories is intentionally out of scope for this baseline.

Avoid tasks such as:

```yaml
ansible.builtin.apt:
  update_cache: true
```

Those tasks will be introduced only after the project explicitly decides how private nodes should obtain packages.
