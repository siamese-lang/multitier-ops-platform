# lab-full-ops storage validation runtime record - 2026-07-12

## Purpose

Record the first batched `lab-full-ops` storage validation window after adding the work-order evidence-file workload.

This evidence belongs to the project theme:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

The goal was not to demonstrate an application upload feature. The goal was to validate whether a VM-based WEB/WAS/DB/storage environment can support an operational DB/file consistency workflow:

```text
operator -> nginx-01 -> app-01 -> db-primary-01
                         |
                         -> nfs-01 mounted file path
```

## Runtime profile

The validation used the reduced `lab-full-ops` profile with NAT intentionally enabled for the package-install window.

Created runtime nodes:

```text
bastion-01
nginx-01
app-01
db-primary-01
nfs-01
backup-01
```

Intentionally disabled nodes:

```text
app-02
mon-01
log-01
loadgen-01
```

NAT Gateway was enabled only for the grouped runtime validation window and the Terraform environment was destroyed after evidence collection.

## Validation sequence

The validation window followed this sequence:

```text
1. Terraform apply for reduced lab-full-ops profile with NAT enabled
2. Ansible control path check through bastion
3. PostgreSQL primary configuration
4. nfs-01 export baseline
5. app-01 NFS client mount baseline
6. ops-sample-service systemd deployment
7. nginx-01 HTTPS reverse proxy configuration
8. work-order evidence file smoke check
9. evidence collection
10. Terraform destroy
```

## Findings

### 1. NFS mount succeeded but app-side write failed

Initial app-side NFS mount reached the server export successfully:

```text
/mnt/ops-sample/files -> 10.50.31.216:/srv/ops-sample/files
```

However, the write probe failed with:

```text
Destination /mnt/ops-sample/files not writable
```

Server-side inspection showed:

```text
/srv/ops-sample/files root:root 0775
export options: rw,sync,no_subtree_check,root_squash
```

This meant the network path and NFS mount were healthy, but the exported directory was not writable under `root_squash` semantics. A root write from the client was mapped to the anonymous NFS identity, which did not have write permission on the server-side export root.

Temporary runtime remediation:

```text
chown nobody:nogroup /srv/ops-sample/files
chmod 0777 /srv/ops-sample/files
exportfs -ra
```

After the remediation, app-side manual write through the mount succeeded.

Repository follow-up:

```text
infra/ansible/inventories/lab-full-ops/group_vars/storage.yml
```

The default export root owner/mode should match the intended `root_squash` behavior.

### 2. App NFS mount playbook was not idempotent after the mount became active

After correcting server-side permissions, the app NFS mount playbook failed on rerun because it attempted to apply owner/group to the mounted NFS root from the app node:

```text
chown failed: Operation not permitted: /mnt/ops-sample/files
```

This is expected under `root_squash`. Once the NFS export is mounted, ownership and mode are controlled on `nfs-01`, not from `app-01`.

Repository follow-up:

```text
infra/ansible/playbooks/lab-full-ops-app-nfs-mount-baseline.yml
```

The playbook should check whether the mount path is already mounted and skip ownership/mode changes on the mounted NFS root.

### 3. Evidence smoke initially failed because the deployed jar did not contain the evidence endpoint

The Nginx and app base route were healthy:

```text
GET  /healthz                  -> 200
GET  /node                     -> 200
GET  /api/work-orders/summary  -> 200
POST /api/work-orders          -> 201
```

The first smoke run failed at:

```text
POST /api/work-orders/{id}/evidence-files -> 404
```

The access log showed that Nginx correctly proxied the request to `app-01` and the application returned the 404 from the upstream app, not from Nginx. This isolated the failure to the deployed app artifact rather than WEB routing.

Root cause:

```text
A stale ops-sample-service jar had been deployed. It included the DB-backed work-order API but did not include the WorkOrderEvidence* classes.
```

Runtime remediation:

```text
replace the deployed jar with the correct GitHub Actions artifact containing WorkOrderEvidenceController and WorkOrderEvidenceRepository
redeploy ops-sample-service
rerun the smoke check
```

Repository follow-up:

```text
infra/ansible/inventories/lab-full-ops/group_vars/app.yml
infra/ansible/playbooks/lab-full-min-ops-sample-service.yml
```

The app deployment path should validate that the local artifact contains required runtime classes before copying it to the app node.

### 4. Final smoke succeeded after artifact correction

After deploying the correct jar artifact, the work-order evidence smoke check succeeded.

The smoke verifies the complete consistency path:

```text
Nginx API path
+ PostgreSQL work-order metadata
+ PostgreSQL evidence-file metadata
+ NFS-backed file object
+ checksum/size consistency endpoint
+ direct DB and NFS evidence checks
+ Nginx request log evidence
```

## Operational interpretation

The validation produced two useful operational findings before the final success:

```text
1. NFS mount success is not enough; write semantics must be verified under export options.
2. App health and DB work-order API success are not enough; deployed artifact contents must match the required validation workload.
```

These are portfolio-relevant because the validation did not stop at instance creation or green process checks. It found and isolated failures across storage permission semantics, idempotent configuration management, artifact provenance, API routing, and DB/file consistency.

## Cleanup

The AWS lab was destroyed after evidence collection. Future runtime validation should continue to use the same pattern:

```text
apply once -> run grouped validation -> collect evidence -> destroy immediately
```

Do not run Terraform create/destroy cycles for each small Ansible or documentation change.
