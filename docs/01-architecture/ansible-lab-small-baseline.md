# Ansible lab-small Baseline

## Purpose

This document defines the first Ansible baseline for `lab-small` before any playbook or role implementation.

The Terraform baseline has already proven that the project can create and destroy a minimal AWS environment with:

- `bastion-01` in a public subnet
- `app-01` in a private subnet
- SSH access to bastion from the operator CIDR
- SSH access to app only through bastion
- no public IP on app
- no NAT Gateway in the private subnet

The first Ansible baseline should prove that Ansible can use this access pattern safely and repeatably.

## Design goal

The first Ansible milestone is not software deployment.

The first milestone is connection and control-plane proof:

```text
Terraform outputs
  -> manual or generated Ansible inventory
  -> SSH to bastion
  -> SSH to app through bastion
  -> ansible ping
  -> gather facts
  -> minimal host report
```

This keeps the next implementation review small and prevents the project from hiding application deployment problems inside a broad Ansible change.

## Terminology

### Bastion host

`bastion-01` is the public entry point for operator access.

It has a public IP and accepts SSH only from the configured `operator_cidr`.

### Jump host

A jump host is a host used as an SSH hop to reach another host.

In this project, `bastion-01` is the jump host used to reach `app-01`.

### Private app node

`app-01` is placed in a private subnet and has no public IP.

It should be reached only through `bastion-01`.

## Inventory strategy

The first implementation should use a checked-in inventory example and a local, ignored inventory file.

Recommended files:

```text
infra/ansible/inventories/lab-small/hosts.yml.example
infra/ansible/inventories/lab-small/group_vars/all.yml
infra/ansible/inventories/lab-small/group_vars/app.yml
infra/ansible/inventories/lab-small/README.md
```

The operator copies the example file to an ignored local inventory:

```bash
cp infra/ansible/inventories/lab-small/hosts.yml.example \
   infra/ansible/inventories/lab-small/hosts.yml
```

Then the operator fills in values from Terraform outputs:

- `bastion_public_ip`
- `app_private_ip`
- SSH private key path

A fully dynamic inventory is deferred until after the first static baseline works.

## Inventory shape

The inventory should define two groups:

```yaml
all:
  children:
    bastion:
      hosts:
        bastion-01:
          ansible_host: <bastion_public_ip>
          ansible_user: ubuntu
    app:
      hosts:
        app-01:
          ansible_host: <app_private_ip>
          ansible_user: ubuntu
```

The app host should use SSH proxy settings rather than copying private keys to bastion.

## SSH jump strategy

Do not copy the operator private key to `bastion-01`.

The recommended approach is to use SSH `ProxyCommand` from the operator workstation:

```yaml
ansible_ssh_common_args: >-
  -o ProxyCommand="ssh -i <private_key_path> -o IdentitiesOnly=yes -W %h:%p ubuntu@<bastion_public_ip>"
  -o StrictHostKeyChecking=accept-new
```

`ProxyJump` may be added later, but `ProxyCommand` is the baseline because it worked in the Windows Git Bash environment used during manual verification.

## First playbook scope

The first playbook should perform only safe, no-package, no-application actions.

Recommended playbook:

```text
infra/ansible/playbooks/lab-small-baseline.yml
```

Allowed tasks:

- gather facts from `bastion` and `app`
- print hostname, private IP, default route, OS distribution
- run a no-change connectivity check such as `ansible.builtin.ping`
- write a small host report under `/tmp/multitier-ops-baseline.txt`, or print the same report through `debug`

Do not install packages in the first baseline.

## No-NAT constraint

`app-01` is in a private subnet without a NAT Gateway.

Therefore tasks that require outbound internet access from `app-01` will fail by design:

- `apt update`
- package installation from public repositories
- Docker installation from public repositories
- downloading OpenKoda artifacts from the internet

The first Ansible baseline must not treat this as an error.

Package installation requires a later design decision, such as:

- adding NAT Gateway for later lab phases
- using a NAT instance
- pre-baking an AMI
- staging artifacts through bastion or an internal repository
- changing the topology for a later `lab-small-app` milestone

## Evidence commands

The next implementation PR should collect evidence for:

```bash
ansible --version
ansible-inventory -i infra/ansible/inventories/lab-small/hosts.yml --list
ansible -i infra/ansible/inventories/lab-small/hosts.yml bastion -m ping
ansible -i infra/ansible/inventories/lab-small/hosts.yml app -m ping
ansible -i infra/ansible/inventories/lab-small/hosts.yml all -m setup -a 'filter=ansible_default_ipv4'
ansible-playbook -i infra/ansible/inventories/lab-small/hosts.yml infra/ansible/playbooks/lab-small-baseline.yml
```

Evidence should include:

- command executed
- success/failure result
- Ansible recap
- hostnames and private IPs
- confirmation that no private key was copied to bastion

## Out of scope

The first Ansible baseline must not include:

- OpenKoda deployment
- Docker installation
- Java installation
- Nginx configuration
- PostgreSQL installation
- NAT Gateway creation
- package installation on `app-01`
- monitoring/logging agents
- backup client configuration
- production hardening

## Acceptance criteria for implementation

The implementation PR is acceptable when:

- inventory example exists and does not contain real public IPs or private key paths
- local inventory is ignored by Git
- `bastion` and `app` groups are defined
- app SSH access uses bastion as a jump host
- no private key is copied to bastion
- `ansible ping` works for both bastion and app
- the baseline playbook runs successfully
- evidence is recorded in the PR without secrets

## Next step

Create an Ansible implementation issue for the static `lab-small` inventory example and minimal baseline playbook.
