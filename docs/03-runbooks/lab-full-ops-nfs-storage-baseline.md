# lab-full-ops nfs-01 file storage baseline

## Purpose

Configure the first storage-tier baseline for `lab-full-ops` by preparing `nfs-01` as a shared file storage node for later app file operations and backup validation.

This runbook supports the project theme:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

The goal is not to build a product file feature. The goal is to create an operating target that can later be used to verify DB/file consistency, storage failure recovery, backup, and restore evidence.

## Scope

This baseline configures only the storage server side:

```text
nfs-01
- install NFS server package
- create shared export directory
- write export configuration
- enable and start NFS service
- verify export directory and export table state
```

## Out of scope

This runbook does not configure:

- app-node NFS client packages
- app-node mount units
- application file upload/download endpoints
- DB file metadata tables
- checksum workflow
- file storage failure drill
- restic backup
- restore-lab
- monitoring or logging stack
- OpenKoda UI or feature work

## Runtime batching policy

Do not create and destroy the Terraform lab for every small Ansible PR.

Use this sequence instead:

```text
1. Add or review Ansible/storage code.
2. Run static or syntax-oriented checks locally when possible.
3. Do not run Terraform apply for this PR alone.
4. Batch runtime validation with the next app mount and file endpoint checks.
5. During that validation window, apply the reduced Terraform profile once.
6. Run storage, app mount, and file smoke checks together.
7. Collect evidence.
8. Destroy immediately.
```

## No-NAT constraint

The default `lab-full-ops` reduced profile disables NAT Gateway.

That means private nodes cannot install packages from public repositories unless one of the following is intentionally prepared for the validation window:

- temporary NAT Gateway
- NAT instance
- pre-baked AMI with required packages
- internal package mirror
- staged package artifacts

Because `nfs-01` is in a private subnet, the `apt` package-install task should be executed only when a package-access strategy is available.

## Files

```text
infra/ansible/inventories/lab-full-ops/group_vars/storage.yml
infra/ansible/playbooks/lab-full-ops-nfs-storage-baseline.yml
```

## Configuration defaults

The default export root is:

```text
/srv/ops-sample/files
```

The export is scoped to the default private app and private ops subnet CIDRs:

```text
10.50.11.0/24  # app subnet
10.50.41.0/24  # backup/ops subnet
```

The default export options are:

```text
rw,sync,no_subtree_check,root_squash
```

This keeps the export private to the lab VPC assumptions and avoids public exposure.

## Syntax check

From `infra/ansible`:

```bash
export ANSIBLE_CONFIG="$PWD/ansible.cfg"
ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml.example \
  playbooks/lab-full-ops-nfs-storage-baseline.yml \
  --syntax-check
```

This does not contact AWS instances.

## Batched runtime validation commands

Run only during an explicit storage/app validation window.

From `infra/ansible`:

```bash
export ANSIBLE_CONFIG="$PWD/ansible.cfg"
ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/lab-full-ops-nfs-storage-baseline.yml
```

Expected play recap for this playbook:

```text
nfs-01 : unreachable=0 failed=0
```

## Evidence to collect

Collect the following from the play output and from `nfs-01`:

```bash
sudo systemctl status nfs-server --no-pager
sudo exportfs -v
sudo cat /etc/exports.d/multitier-ops-platform.exports
sudo stat /srv/ops-sample/files
```

Minimum evidence should show:

- NFS service active
- export root exists
- export config contains app subnet and backup/ops subnet
- export table reflects `/srv/ops-sample/files`
- no public CIDR export such as `0.0.0.0/0`

## Failure interpretation

If package installation fails, do not treat it as an NFS configuration failure first.

Check the package-access condition:

```text
private subnet without NAT Gateway
no package mirror
no staged artifact
not using pre-baked AMI
```

In that case, the correct next action is to decide the package delivery strategy, not to repeatedly destroy and recreate the Terraform lab.
