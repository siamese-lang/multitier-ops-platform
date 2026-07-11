# Ansible

Ansible configures EC2 instances after Terraform provisions infrastructure.

## Long-term scope

Ansible will eventually be responsible for:

- OS package installation
- user and directory setup
- service configuration
- systemd unit files
- Nginx configuration
- OpenKoda runtime configuration
- PostgreSQL configuration where applicable
- NFS configuration
- monitoring/logging agents
- backup client configuration

## First lab-small baseline

The first Ansible baseline is intentionally narrower than the long-term scope.

It should prove that Ansible can control the Terraform-created `lab-small` hosts:

```text
operator workstation
  -> bastion-01, public subnet
  -> app-01, private subnet through bastion
```

The first baseline should include:

- `bastion` and `app` inventory groups
- SSH jump access to `app-01` through `bastion-01`
- no private key copied to bastion
- `ansible ping` evidence for both groups
- fact gathering from both hosts
- a minimal no-package playbook

The first baseline should not install Docker, Java, OpenKoda, Nginx, PostgreSQL, monitoring agents, or backup tooling.

## No-NAT constraint

The current Terraform `lab-small` baseline places `app-01` in a private subnet without a NAT Gateway.

Therefore Ansible tasks that require outbound internet access from `app-01` are out of scope for the first baseline.

Examples of deferred tasks:

- `apt update` on `app-01`
- package installation from public repositories
- Docker installation
- OpenKoda artifact download

A later issue should decide whether to add NAT Gateway, use a NAT instance, bake an AMI, or stage artifacts internally.

## Directory structure

Expected structure for the first Ansible implementation:

```text
infra/ansible/inventories/lab-small/
infra/ansible/playbooks/
infra/ansible/roles/
```

Initial implementation candidates:

```text
infra/ansible/inventories/lab-small/hosts.yml.example
infra/ansible/inventories/lab-small/README.md
infra/ansible/playbooks/lab-small-baseline.yml
```

A real `hosts.yml` containing public IPs, private IPs, or local private key paths should stay local and be ignored by Git.

## Workflow

Typical execution pattern:

```bash
ansible --version
ansible-inventory -i inventories/lab-small/hosts.yml --list
ansible -i inventories/lab-small/hosts.yml bastion -m ping
ansible -i inventories/lab-small/hosts.yml app -m ping
ansible-playbook -i inventories/lab-small/hosts.yml playbooks/lab-small-baseline.yml
```

Commands may be run from `infra/ansible/`.

## Evidence requirements

Each Ansible issue should include:

- inventory shape used, without secrets
- command executed
- `ansible-inventory` output summary
- `ansible ping` result
- playbook recap
- relevant host facts
- confirmation that no private key was copied to bastion
- explanation of any no-NAT limitations

## Status

Ansible roles and playbooks have not been implemented yet.

The current next step is a design PR for the `lab-small` Ansible baseline, followed by a small implementation PR.
