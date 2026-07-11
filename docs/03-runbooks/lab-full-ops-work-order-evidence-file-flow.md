# lab-full-ops work-order evidence file flow

## Purpose

This runbook describes the controlled work-order evidence file flow used by `ops-sample-service` in `lab-full-ops`.

The goal is not to add a product upload feature. The goal is to create an operating surface that proves whether PostgreSQL metadata and NFS-backed file objects remain consistent during storage, backup, and restore scenarios.

```text
work order metadata in PostgreSQL
+ evidence file metadata in PostgreSQL
+ evidence file object on NFS
+ consistency verification between DB metadata and file object
```

This supports the fixed project theme:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

## Scope

The app adds a minimal evidence-file flow tied to existing work orders:

```text
POST /api/work-orders/{id}/evidence-files
GET  /api/work-orders/{id}/evidence-files
GET  /api/work-orders/{id}/evidence-files/{evidenceId}/consistency
```

The app generates a small evidence file itself. It does not accept arbitrary file names, user uploads, or user-provided file bytes.

## Preconditions

Run this only after the following code and infrastructure pieces are available:

```text
lab-full-ops reduced Terraform profile
lab-full-ops Ansible inventory/control path
nfs-01 NFS storage baseline
app-01 NFS client mount baseline
PostgreSQL primary configured
ops-sample-service deployed on app-01
Nginx reverse proxy path to app-01
```

Runtime validation should be batched. Do not create and destroy the Terraform lab for this PR alone.

## Configuration

The evidence file root defaults to:

```text
/mnt/ops-sample/files
```

Override it with:

```bash
export OPS_EVIDENCE_FILE_ROOT=/mnt/ops-sample/files
```

In `lab-full-ops`, this path should match the app-side NFS mount path configured by Ansible.

The service does not create or mount NFS. It only uses the mounted path provided by the operating environment.

## Data model

The existing work-order table remains the business-like workload anchor:

```text
ops_work_orders
```

The evidence flow adds:

```text
ops_work_order_evidence_files
```

Minimum metadata:

```text
id
work_order_id
file_name
storage_path
size_bytes
sha256
created_by_node
created_at
```

The file object is stored under the configured evidence root, for example:

```text
/mnt/ops-sample/files/work-order-1/evidence-<uuid>.txt
```

The DB row stores the relative storage path so restore validation can compare metadata and restored file objects under a new root.

## Endpoint behavior

### Create evidence file

```bash
curl -s -X POST \
  -H 'X-Request-Id: lab-full-ops-evidence-create-001' \
  http://<nginx-public-ip>/api/work-orders/1/evidence-files
```

Expected behavior:

```text
1. Confirm work order 1 exists in PostgreSQL.
2. Confirm the configured evidence root is a writable directory.
3. Generate a small evidence file on the mounted path.
4. Calculate size and SHA-256.
5. Insert metadata into PostgreSQL.
6. Return metadata and immediate consistency result.
```

If DB metadata insert fails after file creation, the app attempts to delete the created file. Any remaining orphan file should be treated as an operating evidence case, not silently ignored.

### List evidence metadata

```bash
curl -s \
  -H 'X-Request-Id: lab-full-ops-evidence-list-001' \
  http://<nginx-public-ip>/api/work-orders/1/evidence-files
```

Expected behavior:

```text
returns metadata rows from PostgreSQL for the selected work order
```

### Check DB/file consistency

```bash
curl -s \
  -H 'X-Request-Id: lab-full-ops-evidence-consistency-001' \
  http://<nginx-public-ip>/api/work-orders/1/evidence-files/<evidenceId>/consistency
```

Expected behavior:

```text
checks DB metadata against the file object on the mounted path
```

The response should distinguish these states:

| State | DB metadata | File object | Size | SHA-256 | Meaning |
| --- | --- | --- | --- | --- | --- |
| consistent | exists | exists | matches | matches | DB/file pair is usable |
| missing file | exists | missing | unavailable | unavailable | metadata points to lost file |
| corrupt file | exists | exists | maybe matches | mismatch | file bytes changed after metadata write |
| storage unavailable | exists | inaccessible | unavailable | unavailable | mount/storage tier problem |

## Evidence to collect

During the later batched runtime validation window, collect:

```bash
# HTTP evidence through Nginx
curl -i http://<nginx-public-ip>/api/work-orders/1
curl -i -X POST http://<nginx-public-ip>/api/work-orders/1/evidence-files
curl -i http://<nginx-public-ip>/api/work-orders/1/evidence-files
curl -i http://<nginx-public-ip>/api/work-orders/1/evidence-files/<evidenceId>/consistency

# App-side mount evidence
ansible -i inventories/lab-full-ops/hosts.yml app -b -m shell -a \
  "findmnt --target /mnt/ops-sample/files --output SOURCE,TARGET,FSTYPE,OPTIONS --noheadings"

# File object evidence
ansible -i inventories/lab-full-ops/hosts.yml app -b -m shell -a \
  "find /mnt/ops-sample/files -maxdepth 3 -type f -print -exec sha256sum {} \\;"

# DB metadata evidence
ansible -i inventories/lab-full-ops/hosts.yml db-primary-01 -b -m shell -a \
  "sudo -u postgres psql -d opsdb -c 'select id, work_order_id, storage_path, size_bytes, sha256, created_by_node, created_at from ops_work_order_evidence_files order by id;'"
```

Minimum evidence should show:

```text
HTTP create returns evidence metadata
DB row exists
file object exists on the mounted path
file size matches metadata
file checksum matches metadata
response contains node identity and durationMs
request IDs appear in Nginx/app logs
```

## Failure interpretation

Do not treat every failure as an application bug.

Separate the failure class first:

```text
DB unavailable
  -> work-order lookup or metadata insert fails
  -> /readyz and DB-backed endpoints should show DB-dependent failure

storage root missing or not writable
  -> evidence create returns storage-not-ready or storage-unavailable
  -> check app mount state and nfs-01 service/export state

metadata exists but file missing
  -> DB/file inconsistency
  -> useful for storage failure, restore gap, or cleanup analysis

file exists but checksum mismatch
  -> file corruption or wrong restore result
  -> useful for restore validation
```

## Batched runtime validation policy

Run this together with the storage and app mount baselines:

```text
1. Apply reduced lab-full-ops Terraform profile once.
2. Prepare Ansible hosts.yml from Terraform outputs.
3. Run control-path validation.
4. Run nfs-01 storage baseline.
5. Run app-01 NFS mount baseline.
6. Deploy updated ops-sample-service artifact.
7. Execute work-order evidence file flow through Nginx.
8. Collect DB/file consistency evidence.
9. Destroy Terraform resources immediately after the validation window.
```

Do not open a separate Terraform runtime window for this app-only change.

## Out of scope

This runbook does not cover:

```text
general file upload/download UI
arbitrary user-provided file content
large attachment handling
file storage failure drill execution
restic backup
restore-lab
OpenKoda feature or UI work
```

Those remain separate operating scenarios.
