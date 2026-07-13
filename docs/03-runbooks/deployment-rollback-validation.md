# Deployment rollback validation runbook

## Purpose

This runbook describes the VM/systemd deployment and rollback validation flow for `ops-sample-service`.

Fixed project theme:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

This is not a CI/CD showcase, Kubernetes/GitOps scenario, blue/green deployment, or production release-management claim. It is a controlled WEB/WAS operations scenario for validating how an operator confirms a deployment, detects a failed candidate, and restores the previous service artifact.

## What was added to support this scenario

Application support:

```text
GET /version
```

The endpoint exposes:

```text
runtime.appVersion
build.name
build.version
build.time
artifact.source
artifact.sha256
deployment.id
deployment.slot
deployment.deployedAt
node.hostname
node.environment
```

Ansible support:

```text
infra/ansible/playbooks/lab-full-ops-deployment-rollback-validation.yml
```

The playbook:

```text
1. Captures the currently deployed jar and environment file.
2. Backs them up under the app host.
3. Copies a candidate jar to the systemd service path.
4. Stamps deployment metadata into the existing environment file.
5. Restarts `ops-sample-service` through systemd.
6. Validates health, version metadata, readiness, and DB-backed summary.
7. Rolls back to the previous jar and environment file.
8. Validates the rollback state.
9. Writes a deployment rollback report under `/tmp/multitier-ops-platform`.
```

## Preconditions

Run this only in a prepared `lab-full-ops` runtime after the normal app deployment baseline has succeeded.

Required before execution:

```text
- app-01 is reachable through the Ansible inventory.
- ops-sample-service is already deployed and running as a systemd service.
- PostgreSQL and NFS baselines are already applied.
- A candidate jar exists on the Ansible control host.
- `ops_db_password` is supplied from ignored inventory or extra-vars.
```

Do not run this during documentation-only work.

## Build candidate artifact

From WSL, after pulling the latest repository changes:

```bash
cd /mnt/c/Project/test/multitier-ops-platform/apps/ops-sample-service
mvn clean package
```

Expected artifact:

```text
apps/ops-sample-service/target/ops-sample-service-0.1.0.jar
```

The Maven build should generate Spring Boot build metadata so `/version` can report build information.

## Static checks before runtime

From WSL:

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/lab-full-ops-deployment-rollback-validation.yml \
  --syntax-check
```

This does not create AWS resources and does not execute the scenario.

## Runtime execution

Run only during a controlled AWS validation window:

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/lab-full-ops-deployment-rollback-validation.yml \
  -e 'ops_db_password=<strong-local-password>' \
  -e 'ops_deploy_candidate_source_path=/mnt/c/Project/test/multitier-ops-platform/apps/ops-sample-service/target/ops-sample-service-0.1.0.jar' \
  -e 'ops_deploy_candidate_version=0.1.0' \
  -e 'ops_deploy_always_rollback=true'
```

`ops_deploy_always_rollback=true` is intentional for this portfolio scenario. Even if the candidate deploy succeeds, the playbook restores the previous jar and environment file so the rollback path is actually verified.

## Evidence to capture

The playbook writes a report like:

```text
/tmp/multitier-ops-platform/lab-full-ops-deployment-rollback-app-01.txt
```

The report should include:

```text
before_service_state
final_service_state
previous_jar_sha256
candidate_local_sha256
candidate_remote_sha256
candidate_id
candidate_failed
candidate_health_rc
candidate_version_rc
candidate_ready_rc
candidate_summary_rc
rollback_health_rc
rollback_version_rc
rollback_ready_rc
rollback_summary_rc
deployment_rollback_status
```

Optional extra evidence:

```bash
journalctl -u ops-sample-service --no-pager -n 80
curl -fsS http://127.0.0.1:8080/version
curl -fsS http://127.0.0.1:8080/readyz
curl -fsS http://127.0.0.1:8080/api/work-orders/summary
```

## Success criteria

The scenario can be claimed only if:

```text
1. The candidate deployment was attempted from a captured previous state.
2. The candidate artifact checksum was recorded.
3. The service was restarted through systemd.
4. Candidate health/version/readiness/summary checks were attempted.
5. The previous jar and environment file were restored.
6. Rollback health/version/readiness/summary checks succeeded.
7. The final service state is active.
```

## Failure interpretation

If candidate validation fails but rollback succeeds:

```text
The scenario is still valuable.
It shows that the operator can detect a failed WAS deployment and restore the previous service artifact.
```

If rollback validation fails:

```text
Do not claim rollback success.
Collect systemctl, journalctl, /version, /healthz, /readyz, Nginx log, and DB/NFS state before making a claim.
```

## Claim boundary

Supported after successful execution:

```text
A controlled VM/systemd deployment and rollback path was validated in the lab, and the post-rollback state was checked with systemd, HTTP health/version/readiness, and DB-backed API evidence.
```

Not supported:

```text
production deployment experience
zero-downtime deployment
blue/green deployment
canary deployment
automated CI/CD maturity
enterprise release management
```
