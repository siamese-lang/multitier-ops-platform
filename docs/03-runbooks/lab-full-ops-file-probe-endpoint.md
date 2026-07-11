# lab-full-ops file probe endpoint

## Purpose

This runbook describes the `ops-sample-service` file probe endpoints used to validate the `lab-full-ops` file-storage path.

The project theme remains:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

The file probe is not a product upload/download feature. It is a controlled operational probe for verifying that an app node can write to, read from, and clean up a file on the mounted storage path.

## Dependency chain

Run this only after the related storage and app mount baselines are prepared:

```text
nfs-01 export baseline
  -> app-01 NFS client mount baseline
  -> ops-sample-service file probe endpoint
```

## Endpoints

### `GET /file-probe/status`

Reports whether the configured file probe root exists and is a readable/writable directory.

```bash
curl -i http://<nginx-public-ip>/file-probe/status
```

Expected success when the mounted path is ready:

```text
HTTP/1.1 200
status=ready
operation=file_probe.status
directory.exists=true
directory.directory=true
directory.readable=true
directory.writable=true
```

Expected failure when the path is missing, not mounted, or not writable:

```text
HTTP/1.1 503
status=not-ready
operation=file_probe.status
```

### `POST /file-probe/roundtrip`

Writes a small generated probe file, reads it back, compares the content, and deletes it.

```bash
curl -i -X POST http://<nginx-public-ip>/file-probe/roundtrip
```

Expected success:

```text
HTTP/1.1 200
status=ok
operation=file_probe.roundtrip
readMatchesWrite=true
deleted=true
```

The endpoint generates the file name internally. It does not accept arbitrary file names or user-provided file content.

## Configuration

Default file probe root:

```text
/mnt/ops-sample/files
```

Override with:

```bash
export OPS_FILE_PROBE_ROOT=/mnt/ops-sample/files
```

This should match the app-side NFS mount path configured by the Ansible app mount baseline.

## Batched runtime validation

Do not open a separate Terraform runtime window for this endpoint alone.

Validate it together with:

```text
- nfs-01 NFS export baseline
- app-01 NFS client mount baseline
- ops-sample-service deployment
- file probe endpoint checks
```

## Evidence to collect

From the operator workstation:

```bash
curl -i http://<nginx-public-ip>/file-probe/status
curl -i -X POST http://<nginx-public-ip>/file-probe/roundtrip
```

From `app-01`:

```bash
mountpoint /mnt/ops-sample/files
findmnt /mnt/ops-sample/files
ls -la /mnt/ops-sample/files
```

From `nfs-01`:

```bash
sudo exportfs -v
sudo stat /srv/ops-sample/files
```

Minimum evidence should show:

- app node can see the mounted path
- file probe root is readable and writable
- roundtrip write/read/delete succeeds
- the response includes app node identity
- NFS export remains scoped to private app and ops subnet CIDRs

## Failure interpretation

A `/file-probe/status` or `/file-probe/roundtrip` failure should be interpreted by layer:

```text
503 not-ready:
- mount path missing
- NFS mount not active
- directory exists but is not writable

503 file-error:
- write/read/delete failed after path readiness passed
- possible NFS service/export/permission issue
```

Do not treat this as a business application failure first. The endpoint exists to expose storage-path readiness and storage-path failure symptoms.

## Out of scope

This runbook does not cover:

- general file upload/download behavior
- DB metadata records for files
- restore-lab verification
- restic backup execution
- user-facing file management
- OpenKoda feature or UI work
