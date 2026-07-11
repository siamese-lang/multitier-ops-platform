# lab-runtime OpenKoda single-node runbook

This runbook describes the first OpenKoda runtime validation on the private `app-01` node in the NAT-enabled `lab-runtime` environment.

OpenKoda is an upstream open-source business application platform. This repository does not implement OpenKoda itself; it provisions and operates OpenKoda as a representative workload for the portfolio environment.

## Validation policy

To reduce AWS cost and waiting time, do not create and destroy `lab-runtime` for every small code change.

Use this policy instead:

1. Merge Ansible code after PR review and static inspection.
2. Run one integrated AWS validation after Docker and OpenKoda deployment code is ready.
3. Collect evidence once.
4. Destroy `lab-runtime` immediately after evidence collection.

## Prerequisites

The following milestones must already exist in `main`:

- `lab-runtime` Terraform environment
- `lab-runtime` Ansible inventory template
- `lab-runtime` bastion-to-private-app control path
- NAT egress validation from private `app-01`

The local ignored inventory must exist:

```bash
infra/ansible/inventories/lab-runtime/hosts.yml
```

This file must contain the current Terraform outputs:

- `bastion_public_ip`
- `app_private_ip`
- local private key path on the WSL/Linux control node

## One-shot integrated validation

Create the runtime environment once:

```bash
cd infra/terraform/envs/lab-runtime
terraform plan -out tfplan
terraform apply tfplan
terraform output
```

Update the ignored Ansible inventory with the new outputs.

Run the OpenKoda playbook:

```bash
cd infra/ansible
export ANSIBLE_CONFIG="$PWD/ansible.cfg"
ansible -i inventories/lab-runtime/hosts.yml app -m ping
ansible-playbook -i inventories/lab-runtime/hosts.yml playbooks/lab-runtime-openkoda.yml
```

## Expected evidence

The playbook should produce:

```text
/tmp/multitier-ops-platform/openkoda-runtime.txt
```

The report should show:

```text
app_private_node=True
app_has_public_ip=False
app_private_egress_mode=nat-gateway
openkoda_healthcheck_rc=0
```

The first HTTP response line may vary. A successful result is any response metadata from the local OpenKoda endpoint with command exit code `0`.

## Access model

OpenKoda runs on private `app-01`.

This stage does not add:

- public ALB
- DNS
- TLS certificate management
- public inbound access to the app node

Any operator access should use SSH through the bastion path or a temporary SSH tunnel.

## Cleanup

Destroy the environment immediately after evidence collection:

```bash
cd infra/terraform/envs/lab-runtime
terraform destroy
terraform state list
```

`terraform state list` should return no resources.

## Out of scope

- production hardening
- external database separation
- secret manager integration
- monitoring and log shipping
- backup and restore
- OpenKoda customization
