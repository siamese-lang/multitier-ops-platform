# lab-runtime OpenKoda integrated validation evidence

## 1. Scenario

Validate that the `lab-runtime` environment can run OpenKoda on a private `app-01` node through the merged Terraform and Ansible workflow.

This validation proves the following path:

```text
Terraform lab-runtime apply
-> bastion-to-private-app Ansible control path
-> Docker installation on app-01
-> upstream OpenKoda Docker Compose startup
-> local OpenKoda health check on app-01
-> Terraform destroy after evidence collection
```

OpenKoda is an upstream open-source application. This repository does not implement OpenKoda itself; it provisions and operates OpenKoda as a representative workload.

## 2. Date and timezone

- Date: 2026-07-11
- Operator timezone: KST (UTC+09:00)

## 3. Environment

- Terraform environment: `lab-runtime`
- AWS region: `ap-northeast-2`
- App node: private EC2 instance
- Bastion: public EC2 instance used only for SSH control path
- Runtime dependency egress: NAT Gateway
- Application deployment: Docker Compose on `app-01`

## 4. Related issue and PRs

- Issue: `#30 [VALIDATION] lab-runtime OpenKoda 통합 검증`
- OpenKoda deployment PR: `#29 [ANSIBLE] lab-runtime Docker 및 OpenKoda 단일 노드 실행 구현`
- Health check fix PR: `#31 [FIX] OpenKoda healthcheck URL을 HTTP로 수정`

## 5. Commands executed

### Terraform apply

The runtime environment was created once from `infra/terraform/envs/lab-runtime`.

```bash
terraform plan -out tfplan
terraform apply tfplan
terraform output
```

The Terraform outputs were then copied into the ignored Ansible inventory:

```text
infra/ansible/inventories/lab-runtime/hosts.yml
```

### Ansible validation

From `infra/ansible`:

```bash
export ANSIBLE_CONFIG="$PWD/ansible.cfg"
ansible -i inventories/lab-runtime/hosts.yml app -m ping
ansible-playbook -i inventories/lab-runtime/hosts.yml playbooks/lab-runtime-openkoda.yml
```

### Cleanup

After evidence collection:

```bash
terraform destroy
terraform state list
```

## 6. Expected result

The expected result was:

```text
app_private_node=True
app_has_public_ip=False
app_private_egress_mode=nat-gateway
openkoda_healthcheck_rc=0
```

A local HTTP response from `app-01` on port `8080` was sufficient. Public access to OpenKoda was intentionally out of scope.

## 7. Actual result

The Ansible control path reached the private app node successfully:

```text
app-01 | SUCCESS => {
    "changed": false,
    "ping": "pong"
}
```

The OpenKoda deployment playbook completed successfully:

```text
PLAY RECAP
app-01 : ok=19 changed=2 unreachable=0 failed=0 skipped=0 rescued=0 ignored=0
```

The generated runtime report showed:

```text
project=multitier-ops-platform
environment=lab-runtime
inventory_hostname=app-01
ansible_host=10.30.11.197
hostname=ip-10-30-11-197
default_ipv4=10.30.11.197
distribution=Ubuntu 22.04
app_private_node=True
app_has_public_ip=False
app_private_egress_mode=nat-gateway
openkoda_compose_file=/opt/openkoda/docker-compose.yaml
openkoda_compose_source_url=https://raw.githubusercontent.com/openkoda/openkoda/main/docker/docker-compose.yaml
openkoda_healthcheck_url=http://localhost:8080
openkoda_healthcheck_rc=0
openkoda_healthcheck_first_line=HTTP/1.1 302
openkoda_container_name=openkoda
openkoda_postgres_container_name=postgres-db
```

## 8. Relevant observations

The first run used `https://localhost:8080` for the health check and waited until interrupted. The Docker installation, Compose file download, image pull, and Compose startup had already completed. The failure point was the health check scheme, not the network path or Docker installation.

The fix changed the local OpenKoda health check from HTTPS to HTTP:

```text
openkoda_healthcheck_url=http://localhost:8080
```

After that change, the health check returned:

```text
HTTP/1.1 302
```

## 9. Conclusion

The integrated validation succeeded.

The project now has evidence that:

- Terraform can create a NAT-enabled private runtime environment.
- Ansible can reach the private app node through the bastion host.
- Docker can be installed on the private app node.
- OpenKoda can be started with Docker Compose on the private app node.
- OpenKoda responds locally on `app-01` through `http://localhost:8080`.
- The app node remains private and has no public IP.
- Terraform resources are destroyed after validation to control cost.

## 10. Follow-up actions

Recommended next steps:

1. Add a controlled operator access path, such as an SSH tunnel runbook.
2. Add a lightweight external access layer only when needed, such as ALB later.
3. Add operational checks around container status, logs, restart behavior, and failure handling.
4. Keep OpenKoda customization out of scope unless it is directly needed for operability evidence.
