# lab-full-ops enhanced latency scenario validation

## Purpose

This runbook prepares scenario S3 from the enhanced service operations scenario matrix:

```text
WAS long request vs DB-backed delay
```

The goal is not to run a load test. The goal is to prove that a slow request should be interpreted by dependency path:

```text
/api/failure-lab/sleep
-> WAS thread is occupied without a DB query.

/api/failure-lab/db-sleep
-> WAS handles the request while PostgreSQL executes pg_sleep.
```

This gives the portfolio a practical operations explanation:

```text
Nginx can show both requests as slow upstream responses, but the root interpretation changes after checking the app response, app logs, and PostgreSQL activity.
```

## Scenario

Business situation:

```text
The work-order service is reachable, but some user requests are slow.
```

Operating question:

```text
Is this slow response caused by WAS request processing/thread occupation, or by a DB dependency delay?
```

Hypotheses:

| Hypothesis | Evidence to check |
|---|---|
| WAS-only long request | `/api/failure-lab/sleep` returns `scenario=was_sleep`, app response duration is long, Nginx upstream time is long, no DB query sample is required |
| DB-backed delay | `/api/failure-lab/db-sleep` returns `scenario=db_sleep`, response includes `select pg_sleep(?)`, Nginx upstream time is long, PostgreSQL `pg_stat_activity` can show `pg_sleep` while the request is in progress |

## Preconditions

Run this only during a planned `lab-full-ops` runtime validation window.

Required sequence before this playbook:

```text
1. Terraform apply completed for the planned lab-full-ops runtime.
2. inventories/lab-full-ops/hosts.yml populated from Terraform outputs.
3. DB baseline configured.
4. NFS baseline and app mount baseline configured.
5. Enhanced ops-sample-service jar deployed.
6. Nginx reverse proxy configured and routing to the app tier.
7. Enhanced normal workflow validation has passed or will run in the same validation window.
```

Do not open an AWS runtime only for this playbook. It should be batched with the enhanced service validation window.

## Execution

From WSL/Linux/macOS:

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible
export ANSIBLE_CONFIG="$PWD/ansible.cfg"

ansible-playbook \
  -i inventories/lab-full-ops/hosts.yml \
  playbooks/lab-full-ops-enhanced-latency-scenario.yml
```

## Static syntax check

This check does not contact AWS instances:

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible
export ANSIBLE_CONFIG="$PWD/ansible.cfg"
cp inventories/lab-full-ops/hosts.yml.example /tmp/lab-full-ops-hosts.yml

ansible-playbook \
  -i /tmp/lab-full-ops-hosts.yml \
  playbooks/lab-full-ops-enhanced-latency-scenario.yml \
  --syntax-check

rm -f /tmp/lab-full-ops-hosts.yml
```

Expected output:

```text
playbook: playbooks/lab-full-ops-enhanced-latency-scenario.yml
```

## What the playbook does

```text
1. Opens /ops/failure-lab through nginx-01.
2. Calls /api/failure-lab/sleep?millis=5000 through nginx-01.
3. Captures HTTP code, curl time_total, app durationMs, scenario, and thread name.
4. Starts /api/failure-lab/db-sleep?millis=5000 through nginx-01 in the background.
5. Samples PostgreSQL pg_stat_activity while the DB-backed request is in progress.
6. Waits for the DB-backed request to finish.
7. Captures HTTP code, curl time_total, app durationMs, scenario, and query marker.
8. Collects Nginx access log lines with the scenario request prefix.
9. Collects app journald lines with the scenario request prefix.
10. Writes a report under /tmp/multitier-ops-platform/.
```

## Endpoints used

```text
GET /ops/failure-lab
GET /api/failure-lab/sleep?millis=5000
GET /api/failure-lab/db-sleep?millis=5000
```

## Evidence locations

The playbook writes response files on `nginx-01`:

```text
/tmp/multitier-ops-platform/lab-full-ops-latency-scenario/failure-lab.html
/tmp/multitier-ops-platform/lab-full-ops-latency-scenario/was-sleep.json
/tmp/multitier-ops-platform/lab-full-ops-latency-scenario/was-sleep.metrics
/tmp/multitier-ops-platform/lab-full-ops-latency-scenario/db-sleep.json
/tmp/multitier-ops-platform/lab-full-ops-latency-scenario/db-sleep.metrics
/tmp/multitier-ops-platform/lab-full-ops-latency-scenario/db-sleep.stderr
```

The final report is written to:

```text
/tmp/multitier-ops-platform/lab-full-ops-latency-scenario-nginx-01.txt
```

## Minimum success criteria

A successful validation should show:

```text
was_sleep_http_code=200
db_sleep_http_code=200
was_sleep_duration_ms roughly follows the configured sleep millis
db_sleep_duration_ms roughly follows the configured DB sleep millis
db_sleep_query=select pg_sleep(?)
Nginx access log includes both request IDs
app journald sample includes both request IDs if request logging is active
```

`pg_stat_activity_pg_sleep_sample` should ideally contain a `pg_sleep` row while the DB-backed request is running. If the sample is empty, do not claim PostgreSQL activity sampling evidence; use the DB sleep response and logs only.

## Failure interpretation

| Failure point | Likely layer | First checks |
|---|---|---|
| `/ops/failure-lab` fails | WEB/WAS routing | Nginx upstream, app service, artifact version |
| `/api/failure-lab/sleep` fails | WAS endpoint or routing | app logs, Nginx upstream status, artifact guard |
| `/api/failure-lab/db-sleep` returns 503 | DB dependency | DB service, credentials, pg_hba.conf, readiness |
| WAS sleep is fast despite requested millis | app logic or wrong artifact | deployed jar entries, FailureLabController version |
| DB sleep is fast despite requested millis | app logic or DB query path | FailureLabController, DB response JSON |
| Nginx log lacks request ID | WEB logging path | `X-Request-Id` header propagation, Nginx log format |
| app log lacks request ID | app logging/MDC | request logging filter and journald collection |

## Safe interview claim after runtime success

Use this only after the runtime evidence is collected:

```text
느린 요청을 하나로 보지 않고, WAS thread를 점유하는 긴 요청과 DB dependency가 포함된 지연 요청을 분리해 검증했습니다. Nginx에서는 둘 다 upstream response time이 길게 보일 수 있지만, app 응답의 scenario/duration, request ID 로그, PostgreSQL pg_stat_activity 샘플을 함께 보면서 어느 계층을 먼저 확인해야 하는지 나눴습니다.
```

## Claims to avoid

Do not claim:

```text
load testing was completed
thread pool tuning was completed
DB connection pool tuning was completed
capacity planning was completed
SLO/SLA latency was validated
production performance testing was performed
```

This scenario is controlled latency-path observation, not performance benchmarking.

## Runtime batching policy

Run this in the same planned validation window as the enhanced normal workflow and other incident scenarios:

```text
1. apply once
2. configure DB/NFS/app/Nginx
3. validate enhanced normal workflow
4. validate upload failure isolation
5. validate latency scenario
6. validate DB web-impact scenario
7. collect evidence
8. destroy once
```

Do not create and destroy the AWS lab just to run this playbook alone.
