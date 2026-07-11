# lab-full-ops app-01 NFS client mount baseline

## Purpose

Configure the first app-tier NFS client baseline for `lab-full-ops` by mounting the `nfs-01` export on the default app node.

This runbook supports the project theme:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

The goal is not to build a product file feature. The goal is to prepare the app tier so later file-operation checks can verify DB/file consistency, storage failure behavior, backup boundaries, and restore evidence.

## Scope

This baseline configures only the app-side NFS client mount:

```text
app-01
- install NFS client package
- create app-side mount directory
- persist the NFS mount in /etc/fstab
- mount the nfs-01 export
- verify mount state
- write and read a small probe file
```

For the default reduced profile, the playbook targets the `app` inventory group, which contains `app-01` only.

## Out of scope

This runbook does not configure:

- NFS server exports, already covered by the `nfs-01` storage baseline
- application file upload/download endpoints
- DB file metadata tables
- checksum workflow
- file storage failure drill
- restic backup
- restore-lab
- monitoring or logging stack
- OpenKoda UI or feature work

## Runtime batching policy

Do not create and destroy the Terraform lab for this PR alone.

Use this sequence instead:

```text
1. Add or review app mount code.
2. Run static or syntax-oriented checks locally.
3. Do not run Terraform apply for this PR alone.
4. Batch runtime validation with storage server and file endpoint checks.
5. During that validation window, apply the reduced Terraform profile once.
6. Run nfs-01 baseline, app mount baseline, and file smoke checks together.
7. Collect evidence.
8. Destroy immediately.
```

## No-NAT constraint

The default `lab-full-ops` reduced profile disables NAT Gateway.

That means `app-01` cannot install `nfs-common` from public repositories unless one of the following is intentionally prepared for the validation window:

- temporary NAT Gateway
- NAT instance
- pre-baked AMI with required packages
- internal package mirror
- staged package artifacts

If package installation fails, treat it as a package-access problem first, not as an NFS mount design failure.

## Files

```text
infra/ansible/inventories/lab-full-ops/group_vars/app.yml
infra/ansible/playbooks/lab-full-ops-app-nfs-mount-baseline.yml
```

## Configuration defaults

The default app-side mount path is:

```text
/mnt/ops-sample/files
```

The default server-side export path is:

```text
/srv/ops-sample/files
```

The NFS server address is resolved from the inventory entry for `nfs-01`:

```text
hostvars['nfs-01']['ansible_host']
```

The default mount options are:

```text
rw,sync,hard,intr,_netdev
```

## Syntax check

From `infra/ansible`:

```bash
export ANSIBLE_CONFIG="$PWD/ansible.cfg"
cp inventories/lab-full-ops/hosts.yml.example /tmp/lab-full-ops-hosts.yml
ansible-playbook \
  -i /tmp/lab-full-ops-hosts.yml \
  playbooks/lab-full-ops-app-nfs-mount-baseline.yml \
  --syntax-check
rm -f /tmp/lab-full-ops-hosts.yml
```

The temporary copy is intentional because some Ansible versions do not treat `.yml.example` as a YAML inventory source.

This syntax check does not contact AWS instances.

## Batched runtime validation commands

Run only during an explicit storage/app validation window.

From `infra/ansible`:

```bash
export ANSIBLE_CONFIG="$PWD/ansible.cfg"

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/lab-full-ops-nfs-storage-baseline.yml

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/lab-full-ops-app-nfs-mount-baseline.yml
```

Expected play recap for this playbook:

```text
app-01 : unreachable=0 failed=0
```

## Evidence to collect

Collect the following from the play output and from `app-01`:

```bash
mountpoint -q /mnt/ops-sample/files
findmnt --target /mnt/ops-sample/files --output SOURCE,TARGET,FSTYPE,OPTIONS --noheadings
cat /mnt/ops-sample/files/.ansible-nfs-write-probe-app-01
cat /etc/fstab | grep /mnt/ops-sample/files
```

Minimum evidence should show:

- app node can resolve and mount the `nfs-01` export
- `/mnt/ops-sample/files` is an NFS mount
- the persisted fstab entry exists
- app node can write a probe file to the mounted path
- the file path aligns with the later app file-operation harness

## Failure interpretation

If the mount fails, separate the likely failure class before retrying:

```text
package failure      -> no NAT/package mirror/staged package/pre-baked AMI
network failure      -> security group, route, subnet, or NFS port 2049
server failure       -> nfs-01 export not active or wrong export CIDR
client failure       -> wrong mount source, fstab line, mount path, or package missing
permission failure   -> export option or directory permission mismatch
```

Do not repeatedly destroy and recreate Terraform resources until the failure class is identified.
