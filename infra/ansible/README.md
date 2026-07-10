# Ansible

Ansible configures EC2 instances after Terraform provisions infrastructure.

## Scope

Ansible is responsible for:

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

## Directory structure

```text
infra/ansible/inventories/
infra/ansible/roles/
```

## Workflow

Typical execution pattern:

```bash
ansible-inventory -i inventories/<env>/hosts.yml --list
ansible-playbook -i inventories/<env>/hosts.yml <playbook>.yml
```

## Evidence requirements

Each Ansible issue should include:

- inventory used
- playbook command
- changed/ok/failed recap
- relevant service status
- relevant log excerpts
- HTTP or port checks where applicable

## Status

Ansible roles and playbooks have not been implemented yet.
