# OpenKoda lab-runtime Deployment Path

## Purpose

This document defines how OpenKoda will be deployed after the Terraform and Ansible `lab-small` baselines.

The project has already proven a no-NAT private app-node baseline. The next milestone is different: prove that the upstream OpenKoda application can run on a private app node while keeping operator access and service checks controlled through the bastion path.

## Current baseline

The completed `lab-small` baseline established:

- VPC with public and private subnets
- `bastion-01` in the public subnet
- `app-01` in the private subnet
- no public IP on `app-01`
- SSH to `app-01` through `bastion-01`
- no NAT Gateway in the private subnet
- Ansible control of both nodes through the bastion path
- Terraform teardown after verification

This baseline must remain intact because it demonstrates network isolation and controlled administrative access.

## Deployment problem

OpenKoda runtime deployment needs a dependency retrieval path.

Depending on the implementation, the app node may need to perform one or more of the following:

- install Docker or Docker Compose
- pull container images
- install OS packages
- clone or download upstream OpenKoda artifacts
- fetch Java or application dependencies
- reach package repositories over HTTPS

The existing no-NAT `lab-small` private subnet intentionally blocks this behavior. That is correct for the control-plane baseline, but it is not sufficient for runtime provisioning.

## Decision

Do not change the meaning of `lab-small`.

Create a separate runtime variant for OpenKoda deployment:

```text
infra/terraform/envs/lab-runtime/
```

`lab-runtime` should preserve the access model from `lab-small`, but add controlled outbound egress from the private subnet for dependency retrieval.

The selected first runtime path is:

```text
lab-small baseline
  -> separate lab-runtime Terraform environment
  -> NAT Gateway in the public subnet
  -> private route table default route to NAT Gateway
  -> Ansible installs runtime dependencies on app-01
  -> Ansible runs OpenKoda on private app-01
  -> service checked from app and bastion paths
  -> Terraform destroy after evidence collection
```

## Why not modify lab-small directly?

`lab-small` has already produced useful evidence that the private app node has no NAT egress. If NAT is added directly to `lab-small`, the original baseline becomes ambiguous.

Keeping separate variants gives a cleaner operational story:

```text
lab-small
  purpose: network isolation, bastion access, Ansible control path, no-NAT behavior

lab-runtime
  purpose: private app deployment with explicit NAT egress for dependency retrieval
```

This separation also makes review and rollback easier.

## Terraform scope for lab-runtime

The first `lab-runtime` Terraform implementation should be derived from `lab-small` and add only the runtime egress resources required for deployment.

Expected additions:

- Elastic IP for NAT Gateway
- NAT Gateway in the public subnet
- default route from the private route table to the NAT Gateway
- outputs required by Ansible runtime deployment

Expected preserved behavior:

- `bastion-01` remains the only public SSH entry point
- `app-01` remains in a private subnet
- `app-01` has no public IP
- SSH to `app-01` is allowed only from the bastion security group
- port `8080` on `app-01` is allowed only from the bastion security group for runtime verification
- Terraform state, tfvars, private keys, and saved plans are not committed

The first runtime implementation should not introduce a public load balancer, DNS, TLS, or production exposure.

## Ansible scope for OpenKoda runtime

The first OpenKoda runtime Ansible implementation should remain small and evidence-driven.

Expected scope:

- reuse bastion/app inventory pattern
- install Docker runtime packages on `app-01`
- create an application directory such as `/opt/openkoda`
- place a documented Compose file or runtime descriptor
- start the OpenKoda-related containers
- verify container process state
- verify HTTP liveness on port `8080`
- collect logs or status output sufficient for review

The implementation must be explicit that OpenKoda is an upstream open-source application used as the target workload. The portfolio work is the AWS, Terraform, Ansible, access-control, deployment, and verification workflow around it.

## Verification path

The first runtime evidence should prove both local and network-level access.

Recommended evidence commands:

```bash
terraform fmt -check
terraform init
terraform validate
terraform plan -out tfplan
terraform apply tfplan
terraform output
```

Ansible runtime setup:

```bash
ansible-inventory -i inventories/lab-runtime/hosts.yml --list
ansible -i inventories/lab-runtime/hosts.yml bastion -m ping
ansible -i inventories/lab-runtime/hosts.yml app -m ping
ansible-playbook -i inventories/lab-runtime/hosts.yml playbooks/openkoda-runtime.yml
```

App-node checks:

```bash
docker ps
curl -I http://127.0.0.1:8080
```

Bastion-to-app check:

```bash
curl -I http://<app_private_ip>:8080
```

Optional operator tunnel check:

```bash
ssh -i <key_path> -L 8080:<app_private_ip>:8080 ubuntu@<bastion_public_ip>
```

Then access from the operator workstation:

```text
http://localhost:8080
```

## Cost and teardown rule

`lab-runtime` is expected to cost more than `lab-small` because it adds runtime-capable egress resources.

Every runtime verification PR must include teardown evidence:

```bash
terraform destroy
terraform state list
```

The desired final state after verification is an empty Terraform state list.

## Out of scope for the first runtime milestone

- public ALB
- domain name
- TLS certificate
- production hardening
- external user traffic
- monitoring stack
- backup and restore
- multi-AZ design
- autoscaling
- blue/green deployment
- claiming authorship of OpenKoda

## Next implementation sequence

1. Implement `infra/terraform/envs/lab-runtime/`.
2. Verify NAT-enabled private egress and teardown.
3. Implement `infra/ansible/inventories/lab-runtime/` and OpenKoda runtime playbook.
4. Verify OpenKoda liveness from `app-01`, `bastion-01`, and optionally an SSH tunnel from the operator workstation.
5. Destroy resources and record evidence.
