# lab-runtime OpenKoda operations and recovery runbook

This runbook describes the first operational check and recovery workflow for OpenKoda on the private `app-01` node.

OpenKoda is an upstream open-source application. This repository operates it as a representative workload; it does not implement OpenKoda itself.

## Purpose

The previous validation proved that OpenKoda can run on private `app-01` and respond locally on `http://localhost:8080`.

This runbook adds the next operational layer:

```text
check current runtime state
-> capture healthy or unhealthy evidence
-> recover service with Docker Compose
-> capture post-recovery evidence
```

## Cost and validation policy

Do not create `lab-runtime` only to review this runbook or the playbooks.

Use these playbooks during the next planned one-shot incident drill. When the drill is finished, destroy `lab-runtime` immediately.

## Prerequisites

The following must already be true:

- `lab-runtime` is created.
- `infra/ansible/inventories/lab-runtime/hosts.yml` has the current bastion public IP and app private IP.
- Ansible can reach `app-01` through the bastion `ProxyCommand` path.
- OpenKoda has already been deployed with `playbooks/lab-runtime-openkoda.yml`.

## 1. Normal operational check

From `infra/ansible`:

```bash
export ANSIBLE_CONFIG="$PWD/ansible.cfg"
ansible-playbook -i inventories/lab-runtime/hosts.yml playbooks/lab-runtime-openkoda-ops-check.yml
```

The playbook writes:

```text
/tmp/multitier-ops-platform/openkoda-ops-check.txt
```

Expected healthy fields:

```text
app_private_node=True
app_has_public_ip=False
app_private_egress_mode=nat-gateway
docker_service_state=active
openkoda_healthcheck_url=http://localhost:8080
openkoda_healthcheck_rc=0
```

The HTTP first line may be `HTTP/1.1 302` or another successful response line.

## 2. Manual container-stop incident drill

Use this only during a planned validation window.

Stop the OpenKoda service container from the app node through Ansible:

```bash
ansible -i inventories/lab-runtime/hosts.yml app -b -m shell -a 'docker compose -f /opt/openkoda/docker-compose.yaml stop openkoda || docker compose -f /opt/openkoda/docker-compose.yaml stop'
```

Then run the check playbook again:

```bash
ansible-playbook -i inventories/lab-runtime/hosts.yml playbooks/lab-runtime-openkoda-ops-check.yml
```

Expected incident evidence:

```text
openkoda_healthcheck_rc=<non-zero value>
```

The check playbook intentionally does not fail hard when the health check fails. It records the unhealthy state so the incident can be documented.

## 3. Recovery

Run the recovery playbook:

```bash
ansible-playbook -i inventories/lab-runtime/hosts.yml playbooks/lab-runtime-openkoda-recover.yml
```

The recovery playbook runs:

```text
docker compose -f /opt/openkoda/docker-compose.yaml up -d
```

Then it waits until the local OpenKoda health check succeeds.

The playbook writes:

```text
/tmp/multitier-ops-platform/openkoda-recovery.txt
```

Expected recovery fields:

```text
app_private_node=True
app_has_public_ip=False
app_private_egress_mode=nat-gateway
openkoda_recover_compose_up_rc=0
openkoda_recovery_healthcheck_rc=0
```

## 4. Cleanup after drill

After evidence is captured, destroy the environment:

```bash
cd ../terraform/envs/lab-runtime
terraform destroy
terraform state list
```

`terraform state list` should return no resources.

## Evidence to keep

Create an evidence directory under `docs/03-evidence/` after the drill. Include:

- command sequence
- healthy check report
- incident check report
- recovery report
- Terraform destroy result
- conclusion and follow-up actions

Do not store:

- private keys
- `terraform.tfvars`
- Terraform state
- full `hosts.yml`
- AWS credentials
- secrets or session tokens

## Out of scope

This runbook does not add:

- public load balancer
- DNS or TLS
- automated monitoring
- external alerting
- high availability
- database restore
