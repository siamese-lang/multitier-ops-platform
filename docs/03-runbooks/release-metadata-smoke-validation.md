# Release metadata smoke validation

## Purpose

This runbook validates that the running `ops-sample-service` exposes enough release metadata for WEB/WAS deployment and rollback operations.

Fixed project theme:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

This validation is not a production release-management claim. It checks whether an operator can identify the running service artifact, deployment metadata, and node context after a VM/systemd deployment.

## Why this matters

A WEB/WAS operator should not stop at these checks:

```text
systemctl is-active ops-sample-service
curl /healthz
```

Those prove that a process is up, but they do not prove which artifact is running.

This runbook adds these checks:

```text
/version
-> runtime app version
-> environment / role / tier
-> build metadata
-> artifact source
-> artifact SHA-256
-> deployment ID
-> deployment slot
-> deployed-at timestamp
-> node identity
```

## Preconditions

Run this after the app deployment baseline has installed and started `ops-sample-service`.

Required runtime state:

```text
app-01 is reachable by Ansible
ops-sample-service is active through systemd
/healthz returns 200
/version returns 200
```

If `release_metadata_expect_build_info=true`, the jar should be built with Spring Boot build-info metadata.

## Static check

From WSL:

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible

ansible-playbook \
  -i inventories/ci/hosts.yml \
  playbooks/lab-full-ops-release-metadata-smoke.yml \
  --syntax-check
```

## Runtime command

From WSL, after lab-full-ops app deployment:

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/lab-full-ops-release-metadata-smoke.yml
```

If a specific checksum is known and should be enforced:

```bash
ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/lab-full-ops-release-metadata-smoke.yml \
  -e 'release_metadata_expected_artifact_sha256=<expected-sha256>'
```

## What the playbook validates

The playbook checks:

```text
systemctl is-active ops-sample-service
GET /version -> 200
service == ops-sample-service
runtime.appVersion == expected version
runtime.environment == expected environment
runtime.role == expected role
runtime.tier == expected tier
deployment.id == expected deployment ID
deployment.slot == expected deployment slot
artifact.source exists
artifact.sha256 exists
optional artifact checksum match
build metadata exists when expected
node identity exists
```

## Report output

The playbook writes:

```text
/tmp/multitier-ops-platform/lab-full-ops-release-metadata-<inventory_hostname>.txt
```

Report fields include:

```text
service_state
version_url
service
runtime_app_version
runtime_environment
runtime_role
runtime_tier
build_available
build_name
build_version
build_time
artifact_source
artifact_sha256
deployment_id
deployment_slot
deployment_deployed_at
node_hostname
release_metadata_status=validated
```

## Success criteria

The validation is successful when:

```text
1. ops-sample-service is active.
2. /version returns HTTP 200.
3. runtime metadata matches the expected app deployment context.
4. deployment metadata is visible.
5. artifact metadata is visible.
6. build metadata is available when expected.
7. the validation report ends with release_metadata_status=validated.
```

## Failure interpretation

| Symptom | Likely layer | What to check next |
|---|---|---|
| `/healthz` works but `/version` fails | WAS routing/controller layer | app logs, jar contents, deployed version |
| `/version` works but build metadata is unavailable | build/package layer | Maven `build-info`, jar contents |
| artifact checksum is `unknown` | deployment metadata layer | environment file written by Ansible |
| deployment ID is wrong | deployment process layer | deployment variables and rollback procedure |
| node identity is wrong | host/environment metadata layer | systemd env file and inventory group vars |

## Claim boundary

Supported after runtime validation:

```text
The running VM/systemd WAS artifact exposes release metadata that allows the operator to identify service version, deployment ID, artifact checksum, build metadata, and node context after deployment.
```

Not supported:

```text
- production release management
- automated deployment approval workflow
- blue/green deployment
- canary deployment
- zero-downtime guarantee
- enterprise CI/CD maturity
```
