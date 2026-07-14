#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  scripts/run-v1-1-operating-change-validation.sh --syntax-only
  scripts/run-v1-1-operating-change-validation.sh --execute

Options:
  --syntax-only              Run only Ansible syntax checks. This is the default safe mode.
  --execute                  Run the v1.1 operating-change validation against lab-full-ops.
  --inventory PATH           Runtime inventory path. Default: infra/ansible/inventories/lab-full-ops/hosts.yml
  --ci-inventory PATH        CI inventory path for syntax check. Default: infra/ansible/inventories/ci/hosts.yml
  --evidence-dir PATH        Local evidence archive directory. Default: evidence/v1-1 under the repository root.
  -h, --help                 Show this help.

Environment:
  V1_1_EVIDENCE_DIR          Overrides the local evidence archive directory.

This script does not create or destroy AWS resources. It assumes lab-full-ops already exists.
EOF
}

mode="syntax-only"

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/.." && pwd)"
ansible_dir="${repo_root}/infra/ansible"

runtime_inventory="${ansible_dir}/inventories/lab-full-ops/hosts.yml"
ci_inventory="${ansible_dir}/inventories/ci/hosts.yml"
evidence_dir="${V1_1_EVIDENCE_DIR:-${repo_root}/evidence/v1-1}"
playbook="${ansible_dir}/playbooks/lab-full-ops-v1-1-operating-change-validation.yml"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --syntax-only)
      mode="syntax-only"
      shift
      ;;
    --execute)
      mode="execute"
      shift
      ;;
    --inventory)
      runtime_inventory="$2"
      shift 2
      ;;
    --ci-inventory)
      ci_inventory="$2"
      shift 2
      ;;
    --evidence-dir)
      evidence_dir="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

require_file() {
  local path="$1"
  local label="$2"
  if [[ ! -f "${path}" ]]; then
    echo "missing ${label}: ${path}" >&2
    exit 1
  fi
}

require_command() {
  local command_name="$1"
  if ! command -v "${command_name}" >/dev/null 2>&1; then
    echo "required command not found: ${command_name}" >&2
    exit 1
  fi
}

require_command ansible-playbook
require_file "${playbook}" "v1.1 playbook"
require_file "${ci_inventory}" "CI inventory"

export ANSIBLE_CONFIG="${ansible_dir}/ansible.cfg"

cd "${ansible_dir}"

echo "[1/2] Syntax check: ${playbook}"
ansible-playbook -i "${ci_inventory}" "${playbook}" --syntax-check

if [[ "${mode}" == "syntax-only" ]]; then
  cat <<EOF

Syntax check completed.
Runtime validation was not executed.

To execute the v1.1 operating-change validation against an existing lab-full-ops runtime, run:
  V1_1_EVIDENCE_DIR='${evidence_dir}' \\
    scripts/run-v1-1-operating-change-validation.sh --execute
EOF
  exit 0
fi

require_file "${runtime_inventory}" "runtime inventory"
mkdir -p "${evidence_dir}"
export V1_1_EVIDENCE_DIR="${evidence_dir}"

cat <<EOF
[2/2] Executing v1.1 operating-change validation
  inventory:    ${runtime_inventory}
  playbook:     ${playbook}
  evidence dir: ${V1_1_EVIDENCE_DIR}

Expected flow:
  preflight -> bad DB env -> dependency failure isolation -> rollback -> postflight -> evidence fetch
EOF

ansible-playbook -i "${runtime_inventory}" "${playbook}"

cat <<EOF

v1.1 operating-change validation completed.
Review local evidence archives under:
  ${V1_1_EVIDENCE_DIR}
EOF
