# ADR-0005: OpenKoda lab-runtime Deployment Path

## Status

Accepted

## Context

The project has completed two baselines:

1. Terraform `lab-small` baseline
2. Ansible `lab-small` baseline

Together, they prove that the operator can provision a small AWS environment, access a bastion host, reach a private app node through the bastion, verify the no-NAT behavior of the private subnet, control both nodes with Ansible, and destroy the resources afterward.

The next goal is to run OpenKoda as the target workload.

OpenKoda deployment is not just an access problem. It requires a runtime provisioning path. The private app node may need OS packages, Docker, image pulls, application artifacts, or dependency downloads.

The current `lab-small` baseline intentionally has no NAT Gateway. Therefore, `app-01` cannot assume direct outbound internet access.

## Decision

Keep `lab-small` as the no-NAT control-plane baseline.

Create a separate NAT-enabled runtime variant for OpenKoda deployment:

```text
infra/terraform/envs/lab-runtime/
```

The first OpenKoda runtime implementation should use `lab-runtime`, not mutate the already validated meaning of `lab-small`.

`lab-runtime` will preserve the bastion/private-app security model while adding outbound dependency retrieval through a NAT Gateway.

## Selected approach

Use a controlled NAT-enabled runtime variant.

Expected Terraform direction:

- start from the `lab-small` topology
- add Elastic IP for NAT Gateway
- add NAT Gateway in the public subnet
- route private subnet default egress through the NAT Gateway
- keep app node private with no public IP
- keep SSH to app restricted to the bastion security group
- keep port `8080` restricted to the bastion security group for verification

Expected Ansible direction:

- run from WSL/Linux/macOS control node
- reuse bastion jump access pattern
- install runtime dependencies on `app-01`
- deploy upstream OpenKoda as the target application workload
- verify service state and HTTP liveness
- avoid public production exposure in the first runtime milestone

## Alternatives considered

### Option A: Add NAT directly to lab-small

Rejected for the first runtime step.

It would make the earlier no-NAT evidence ambiguous and weaken the separation between isolation baseline and runtime deployment baseline.

### Option B: Pre-baked AMI

Deferred.

A pre-baked AMI is a strong infrastructure pattern, but it introduces an image-building workflow before the project has demonstrated a simple private runtime deployment.

### Option C: VPC endpoints and private registries

Deferred.

This is closer to a private production egress model, but it expands the scope into ECR, S3 gateway endpoints, ECR API/DKR endpoints, IAM, and possibly log endpoints. It is too large for the immediate next milestone.

### Option D: Copy artifacts through bastion

Rejected as the primary first runtime path.

It can work for small files, but it can become ad hoc and still does not solve Docker, Java, package, or image dependency management cleanly.

## Consequences

### Positive

- Keeps the no-NAT `lab-small` evidence intact.
- Creates a clear path for OpenKoda runtime deployment.
- Keeps the app node private.
- Allows normal package and image retrieval during deployment.
- Produces a more realistic private-subnet runtime architecture.
- Makes the next implementation issue unambiguous.

### Negative

- Adds AWS cost during runtime validation.
- Requires careful teardown evidence.
- Adds another Terraform environment to maintain.
- May duplicate some structure from `lab-small` unless refactoring is introduced later.

## Implementation guardrails

- Do not claim authorship of OpenKoda.
- Do not expose OpenKoda publicly in the first runtime milestone.
- Do not commit private keys, real tfvars, Terraform state, local inventory, application secrets, or session tokens.
- Do not remove or redefine the completed no-NAT `lab-small` baseline.
- Collect evidence before merging runtime implementation PRs.
- Destroy resources after runtime evidence collection.

## Follow-up work

1. Create a Terraform implementation issue for `lab-runtime`.
2. Implement `infra/terraform/envs/lab-runtime/`.
3. Verify NAT-enabled private egress and teardown.
4. Create an Ansible implementation issue for OpenKoda runtime.
5. Deploy OpenKoda to private `app-01`.
6. Verify `8080` liveness from app, bastion, and optionally operator SSH tunnel.
7. Destroy resources and record evidence.
