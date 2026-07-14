# lab-full-ops runtime parking mode

## Purpose

This runbook defines how to keep the `lab-full-ops` runtime available after a validation window without repeatedly destroying and recreating EC2 resources.

The project theme remains:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

Parking mode is useful when the runtime has already been configured and additional low-risk validation may follow.

## When to use parking mode

Use parking mode when all of the following are true:

```text
1. Terraform apply completed.
2. Ansible control path works through bastion-01.
3. WEB/WAS/DB/NFS/backup baseline configuration completed.
4. The current validation evidence was already collected.
5. Additional runtime checks may still be useful.
6. NAT Gateway is no longer required for package installation or external downloads.
```

Do not use parking mode when no further runtime validation is planned. In that case, destroy the lab runtime.

## Parking mode state

The desired parked state is:

```text
EC2 runtime: kept
bastion-01: kept
nginx-01: kept
app-01: kept
db-primary-01: kept
nfs-01: kept
backup-01: kept
NAT Gateway: removed
NAT Elastic IP: released
private subnet default egress: disabled
```

This preserves the already-installed services while removing the highest avoidable idle cost from the validation window.

## Confirm NAT is disabled

Run from Git Bash:

```bash
cd /c/Project/test/multitier-ops-platform/infra/terraform/envs/lab-full-ops

terraform output validation_profile
terraform state list | grep -E 'aws_nat_gateway|aws_eip.nat' || true
```

Expected result:

```text
nat_gateway = false
```

The `terraform state list` command should not show `aws_nat_gateway` or `aws_eip.nat`.

## Confirm runtime is still reachable

Run from WSL:

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible
export ANSIBLE_CONFIG="$PWD/ansible.cfg"

ansible -i inventories/lab-full-ops/hosts.yml lab_full_ops -m ping
```

Expected result:

```text
bastion-01    SUCCESS
nginx-01      SUCCESS
app-01        SUCCESS
db-primary-01 SUCCESS
nfs-01        SUCCESS
backup-01     SUCCESS
```

## Confirm service readiness without mutating the lab

Run the preflight check only:

```bash
ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/lab-full-ops-preflight-check.yml
```

This does not create AWS resources and does not intentionally break the application. It checks:

```text
Nginx config and service state
ops-sample-service active state
PostgreSQL active state
DB local query
app-side NFS mount and write probe
/healthz through Nginx
/readyz through Nginx
/api/ops/dependencies through Nginx
/version through Nginx
```

## What still works in parking mode

The following checks remain appropriate:

```text
Ansible ping through bastion
preflight checks
Nginx/app/PostgreSQL/NFS service status checks
DB-backed work-order API checks
NFS evidence consistency checks
log and report collection
v1.1 evidence bundle re-fetch from existing reports
```

These checks rely on already-installed packages and internal VPC communication.

## What should not be attempted without re-enabling NAT

Do not attempt the following while NAT is disabled:

```text
apt package installation on private nodes
external package downloads from private nodes
new agent installation on private nodes
new tooling that requires internet egress from app/db/storage/backup nodes
```

If such work is required, temporarily re-enable NAT, perform the installation, collect evidence, and disable NAT again.

## Re-enable NAT temporarily

Run from Git Bash:

```bash
cd /c/Project/test/multitier-ops-platform/infra/terraform/envs/lab-full-ops

grep -q '^enable_nat_gateway' terraform.tfvars \
  && sed -i 's/^enable_nat_gateway.*/enable_nat_gateway = true/' terraform.tfvars \
  || printf '\nenable_nat_gateway = true\n' >> terraform.tfvars

terraform plan -out nat-on.tfplan
terraform apply nat-on.tfplan
terraform output validation_profile
```

After completing the package-dependent work, disable NAT again.

## Disable NAT after package-dependent work

Run from Git Bash:

```bash
cd /c/Project/test/multitier-ops-platform/infra/terraform/envs/lab-full-ops

grep -q '^enable_nat_gateway' terraform.tfvars \
  && sed -i 's/^enable_nat_gateway.*/enable_nat_gateway = false/' terraform.tfvars \
  || printf '\nenable_nat_gateway = false\n' >> terraform.tfvars

terraform plan -out nat-off.tfplan
terraform apply nat-off.tfplan
terraform output validation_profile
```

Verify that no NAT Gateway or NAT EIP remains in Terraform state:

```bash
terraform state list | grep -E 'aws_nat_gateway|aws_eip.nat' || true
```

## When to destroy instead of parking

Destroy the lab when any of the following is true:

```text
1. No additional runtime validation is planned.
2. The evidence archive and summary documents are already committed or backed up.
3. The current lab state is no longer needed for comparison.
4. AWS cost matters more than preserving configured runtime state.
5. A future scenario requires a clean rebuild anyway.
```

Destroy from Git Bash:

```bash
cd /c/Project/test/multitier-ops-platform/infra/terraform/envs/lab-full-ops
terraform destroy
```

## Portfolio interpretation

Parking mode should not be presented as a production cost-optimization feature. It is a lab operating practice:

```text
검증이 끝난 뒤 무조건 destroy하지 않고, 추가 검증 가능성과 비용을 분리해 판단했다.
패키지 설치에 필요한 NAT는 제거했고, 이미 구성된 WEB/WAS/DB/NFS/Backup runtime은 필요한 동안 유지했다.
```

This reinforces the portfolio theme: operate the environment intentionally, preserve evidence, and avoid unnecessary resource churn.
