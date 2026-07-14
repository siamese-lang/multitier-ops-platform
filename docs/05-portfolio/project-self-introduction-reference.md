# Project reference for self-introduction and interviews

## Purpose of this document

This document summarizes the `multitier-ops-platform` project in a form that can be reused when writing Korean job applications, resumes, portfolio descriptions, and interview answers.

It is not a runbook. It explains what was built, what was validated, why each part matters, and how to describe the work without exaggeration.

## Project identity

Recommended project title:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

Short title:

```text
VM 기반 WEB/WAS/DB 운영환경 장애·복구 검증 프로젝트
```

One-line description:

```text
EC2 기반 WEB/WAS/DB/NFS/Backup 환경을 직접 구성하고, 운영 변경·장애·복구 상황을 로그, 상태값, DB row, 파일 증거, checksum으로 검증한 운영 포트폴리오 프로젝트입니다.
```

Safe framing:

```text
상용 운영 경험은 아니지만, 운영 직무에서 필요한 계층별 확인 절차와 장애·복구 검증 방식을 EC2 기반 lab에서 evidence로 정리했습니다.
```

Avoid presenting this as:

```text
production 운영 경험
무중단 배포 경험
상용 장애 대응 경험
DR/HA/SLA 검증 경험
대규모 트래픽 운영 경험
Kubernetes/GitOps 프로젝트
AWS managed architecture showcase
Terraform 자체를 보여주기 위한 프로젝트
```

The project is primarily an operations validation portfolio, not an application development project or cloud architecture diagram project.

## Why this project was made

The project was created to show practical operating ability for IT operations, infrastructure, cloud, WEB/WAS, and public/financial IT roles.

The core problem was:

```text
기능이 배포됐다고 해서 운영상 정상이라고 말할 수 있는가?
```

The answer developed through this project was:

```text
운영자는 서비스 상태, dependency readiness, DB row, 파일 실체, 로그, 변경 이력, rollback 결과를 함께 확인해야 한다.
```

This makes the project useful for roles where the evaluator wants evidence that the applicant can think beyond simple implementation and can validate service behavior across layers.

## System scope

The lab environment used the following layers:

```text
Operator
-> Bastion
-> Nginx WEB tier
-> Spring Boot WAS tier
-> PostgreSQL DB tier
-> NFS storage tier
-> Backup node
```

Main components:

```text
Terraform
Ansible
AWS EC2
Ubuntu 22.04
Nginx
Spring Boot with embedded Tomcat
PostgreSQL 14
NFS
restic
systemd
curl / jq / Python validation scripts
```

Important design choice:

```text
The project intentionally uses VM-based WEB/WAS/DB operations instead of EKS, Kubernetes, or managed PaaS. This keeps the focus on traditional WEB/WAS/DB operating flows that appear often in enterprise, public-sector, and financial IT environments.
```

## What was implemented

### 1. Infrastructure provisioning

Terraform was used to provision the EC2-based lab environment.

Meaningful points:

```text
Public and private tiers were separated.
Operator access was restricted by CIDR.
Bastion-based SSH access was used for private nodes.
Security group rules expressed WEB -> WAS -> DB -> Storage flow.
Free-tier validation mode allowed optional nodes to be disabled.
NAT Gateway was enabled only when private nodes needed package installation, then removed to reduce idle cost.
```

This is useful in job applications when describing:

```text
I did not simply launch EC2 instances; I separated access paths and service flows so that each tier had a clear operational boundary.
```

### 2. Configuration and deployment automation

Ansible was used to configure service nodes.

Key configuration work:

```text
Nginx reverse proxy
Spring Boot service deployment through systemd
PostgreSQL primary setup
NFS export and app-side mount
Backup baseline with restic
Preflight and postflight validation
Evidence bundle collection
```

Meaningful point:

```text
The project treated Ansible not only as an installation tool but also as an operating procedure executor. Playbooks were used to verify service state, create controlled failure, restore configuration, and collect evidence.
```

### 3. Operations sample service

The service was intentionally not a feature-heavy business app. It was built as an operating workload.

Main workload features:

```text
/healthz
/readyz
/api/ops/dependencies
work-order API
evidence-file API
DB row persistence
NFS file creation
checksum consistency check
request-id logging
audit log API
```

Meaningful point:

```text
The workload was designed to create observable operating situations: DB dependency failure, storage consistency, request trace, work-order state changes, and post-change validation.
```

Use this when explaining why the app exists:

```text
I did not build the service to show complex business features. I built enough workload behavior to reproduce operating problems and verify them across WEB, WAS, DB, and storage layers.
```

## Key validation phases

## Phase 1: baseline WEB/WAS/DB/NFS/Backup environment

The baseline environment confirmed that the basic runtime was usable.

Validated items included:

```text
Nginx service and config
Spring Boot service status
PostgreSQL connectivity
NFS mount and write behavior
DB-backed API behavior
Evidence file creation and consistency
Backup baseline
```

Why it matters:

```text
Before testing failures, the project established a known-good state. This matters because failure validation is meaningless without a baseline.
```

Job application wording:

```text
장애를 재현하기 전에 정상 기준을 먼저 세웠고, Nginx, WAS, DB, NFS, backup 구간의 상태를 확인한 뒤 운영 시나리오를 진행했습니다.
```

## Phase 2: enhanced operating scenarios

Additional scenarios were used to make the lab more realistic.

Examples:

```text
service workflow validation
upload limit incident
latency scenario
DB impact through WEB/API
backup baseline
restore lab DB/file consistency
```

Why it matters:

```text
These scenarios moved the project beyond a one-time deployment exercise. They showed that the environment could be used repeatedly to observe symptoms, identify the affected layer, and validate recovery.
```

## Phase 3: v1.1 operating-change validation

This became the strongest part of the project.

Scenario title:

```text
운영 변경 요청 기반 WAS DB 환경변수 오설정 감지, 롤백, 변경 요청 추적, 종료 판단 검증
```

Closed flow:

```text
preflight
-> controlled bad DB environment change
-> dependency failure isolation
-> environment rollback
-> postflight business workflow validation
-> CHG-style work-order trace validation
-> change closeout acceptance validation
-> evidence bundle archive/fetch
```

Why it matters:

```text
This scenario shows an end-to-end operating procedure: before-change check, controlled failure, symptom observation, rollback, post-change business validation, change request tracking, and closeout acceptance.
```

## v1.1: what actually happened

### Preflight

Preflight checked that the runtime was healthy before the change.

Validated points:

```text
Nginx was active and config was valid.
WAS service was active.
PostgreSQL was reachable.
NFS was mounted and writable.
/healthz, /readyz, and /api/ops/dependencies were all normal through Nginx.
```

Meaning:

```text
The change was not attempted from an unknown state. A known-good baseline existed immediately before the failure scenario.
```

### Controlled bad DB environment change

The WAS DB connection environment was intentionally changed to an invalid value.

Expected shape:

```text
/healthz = 200
/readyz = 503
/api/ops/dependencies = 503
app process = alive
DB dependency = DOWN
storage dependency = UP
```

Meaning:

```text
The application process was still running, but business readiness failed because the DB dependency was broken. This distinction is important for real operations because service-active alone can be misleading.
```

Strong interview point:

```text
이 시나리오에서 중요하게 본 것은 프로세스 생존 여부와 업무 가능 여부를 분리하는 것이었습니다. systemd active나 /healthz 200만 보면 정상처럼 보일 수 있지만, /readyz와 /api/ops/dependencies에서는 DB dependency 장애가 드러났습니다.
```

### Rollback

The previous systemd environment file was restored and the service was restarted.

Validated result:

```text
rollback_attempted=True
rollback_health_code=200
rollback_ready_code=200
rollback_dependencies_code=200
rollback_status=validated
```

Meaning:

```text
Rollback was not assumed. It was checked through service endpoints and dependency readiness after the environment file was restored.
```

### Postflight

A business workflow was executed after rollback.

Validated points:

```text
work-order creation
status update to DONE
event history
NFS evidence file creation
DB metadata
API consistency
checksum match
```

Meaning:

```text
The service was not considered recovered just because the process restarted. Recovery was accepted only after a DB-backed and NFS-backed workflow succeeded.
```

### CHG-style work-order trace

The v1.1 failure and rollback were linked to an operating change request.

Recorded result:

```text
work_order_id=7
scenario=chg_001_was_db_env_rollback_trace
final_status=DONE
event_count=3
in_progress_event_found=true
done_event_found=true
trace_status=validated
```

DB status history:

```text
CREATED -> OPEN
STATUS_CHANGED OPEN -> IN_PROGRESS
STATUS_CHANGED IN_PROGRESS -> DONE
```

Meaning:

```text
The change was represented as operational data, not just as a shell command or a log file. This gives a clear story for change tracking and evidence-based operation.
```

### Closeout acceptance

The final acceptance check verified whether CHG-001 could be closed.

Validated points:

```text
service readiness returned to normal
dependencies were ready
work_order_id=7 remained DONE
API consistency was true
evidence file existed
size matched
checksum matched
Nginx and app request-id logs existed
acceptance_status=validated
```

Meaning:

```text
The scenario ended with an operator-style closeout decision. The project did not stop at rollback success; it checked whether there was enough evidence to close the change.
```

## Evidence generated

Final v1.1 archives:

```text
lab-full-ops-v1-1-app-01-20260714T074339.tar.gz
lab-full-ops-v1-1-app-01-20260714T074339.tar.gz.sha256
lab-full-ops-v1-1-nginx-01-20260714T074339.tar.gz
lab-full-ops-v1-1-nginx-01-20260714T074339.tar.gz.sha256
```

Final evidence bundle scenario:

```text
scenario=preflight_bad_db_env_rollback_postflight_change_trace_closeout_acceptance
```

The archive includes:

```text
preflight reports
bad DB env rollback reports
postflight reports
CHG-style change trace reports
closeout acceptance reports
Nginx request-id logs
app journal logs
manifest
sha256 file tree
```

Why this matters:

```text
The project has evidence that can be reviewed after the runtime is destroyed. This is important because the portfolio should not depend on keeping cloud resources alive.
```

## NAT and cost handling

NAT Gateway was enabled when private nodes needed package installation. After validation, NAT Gateway and NAT EIP were removed while keeping the runtime temporarily parked for additional low-risk validation.

Meaningful point:

```text
This was not the main project topic, but it showed intentional resource operation: package installation need and idle cost were separated.
```

Do not overstate this as cloud cost optimization. Say instead:

```text
검증이 끝난 뒤에는 NAT가 필요한 작업과 필요 없는 작업을 분리했고, 추가 확인은 NAT 없이 내부 통신과 기존 서비스만으로 수행했습니다.
```

## What this project proves

The project can support the following claims:

```text
I can configure a VM-based WEB/WAS/DB operating environment.
I can verify service behavior across Nginx, WAS, DB, NFS, and backup layers.
I can distinguish process health from business readiness.
I can reproduce a controlled configuration failure and validate rollback.
I can connect operational changes to work-order status, DB rows, evidence files, checksums, and request-id logs.
I can collect evidence before destroying cloud resources.
```

It should not be used to claim:

```text
I operated production systems.
I handled real customer incidents.
I designed HA/DR architecture.
I guaranteed zero downtime.
I managed large-scale traffic.
```

## Strong phrases for job applications

Use these ideas, not necessarily the exact wording.

### Operations mindset

```text
서비스가 정상인지 판단할 때 단일 지표에 의존하지 않고, 프로세스 상태, readiness, dependency 상태, DB row, 파일 실체, 로그를 함께 확인했습니다.
```

### Change validation

```text
운영 변경 전 preflight로 정상 기준을 확인하고, 변경 후에는 장애 증상과 영향 범위를 endpoint와 dependency 결과로 분리해 확인했습니다.
```

### Rollback

```text
rollback 이후에도 서비스 재기동 여부만 보지 않고, DB-backed 업무 흐름과 NFS evidence consistency까지 확인해 복구 여부를 판단했습니다.
```

### Evidence

```text
검증 결과를 단순 실행 로그로 남기지 않고, work-order 상태 이력, DB row, NFS 파일, checksum, Nginx/app request-id 로그로 연결했습니다.
```

### Closeout

```text
장애 복구 후에는 변경 요청을 종료해도 되는지 별도 acceptance check로 확인했습니다. 서비스 readiness, evidence consistency, 이벤트 이력, 로그 존재 여부를 기준으로 종료 판단을 내렸습니다.
```

## Resume bullet examples

### General IT operations

```text
EC2 기반 WEB/WAS/DB/NFS/Backup 운영 lab을 구성하고, Nginx-Spring Boot-PostgreSQL-NFS 구간의 상태·로그·데이터 일관성을 기준으로 장애·복구 시나리오를 검증했습니다.
```

### WEB/WAS operations

```text
WAS DB 환경변수 오설정을 재현해 /healthz와 /readyz, dependency endpoint의 차이를 확인하고, rollback 후 업무 API·DB row·NFS evidence consistency로 복구를 검증했습니다.
```

### Cloud/infrastructure operations

```text
Terraform과 Ansible로 EC2 기반 다계층 운영 환경을 구성하고, bastion 접근, 보안 그룹 흐름, private node 구성, NAT 사용/제거 기준을 운영 절차에 맞게 관리했습니다.
```

### Evidence-based operation

```text
운영 변경 요청을 CHG-001 work-order로 기록하고, OPEN→IN_PROGRESS→DONE 상태 이력, DB row, NFS 파일, checksum, Nginx/app request-id 로그를 evidence bundle로 수집했습니다.
```

## Self-introduction paragraph examples

### Version for WEB/WAS operations

```text
개인 운영 포트폴리오로 EC2 기반 WEB/WAS/DB 환경을 구성하고, Nginx, Spring Boot, PostgreSQL, NFS를 연결해 장애·복구 검증을 수행했습니다. 특히 WAS의 DB 연결 환경변수를 잘못 변경한 상황을 재현해, 프로세스는 살아 있지만 readiness와 dependency가 실패하는 상태를 확인했습니다. 이후 이전 환경 파일로 rollback한 뒤 단순히 서비스가 재시작됐는지 보는 데 그치지 않고, 업무 API, DB row, NFS evidence 파일, checksum, Nginx와 app 로그를 함께 확인했습니다. 이 경험을 통해 운영 업무에서는 '서비스가 떠 있다'는 사실보다, 변경 전후의 상태와 영향 범위를 근거로 확인하는 태도가 중요하다는 점을 체감했습니다.
```

### Version for public/financial IT operations

```text
공공·금융 IT 운영에서는 작은 설정 변경도 업무 처리 결과와 장애 범위에 영향을 줄 수 있다고 생각합니다. 이를 확인하기 위해 EC2 기반 WEB/WAS/DB/NFS 운영 lab을 구성하고, WAS DB 연결 설정 오류를 통제된 변경 시나리오로 재현했습니다. 변경 전 preflight로 정상 기준을 확인하고, 변경 후에는 /healthz, /readyz, dependency endpoint를 비교해 프로세스 상태와 업무 가능 상태를 분리했습니다. rollback 후에는 work-order 상태 이력, DB row, NFS evidence 파일, checksum, request-id 로그까지 확인해 변경 요청을 종료할 수 있는지 판단했습니다. 이 과정을 통해 운영 변경은 실행보다 검증과 증거 확보가 중요하다는 점을 배웠습니다.
```

### Version for cloud/infrastructure role

```text
클라우드 환경을 단순 배포 대상으로만 보지 않고, 운영 절차를 검증하는 환경으로 구성해 보고자 EC2 기반 다계층 운영 lab을 만들었습니다. Terraform으로 네트워크와 인스턴스를 구성하고, Ansible로 Nginx, Spring Boot, PostgreSQL, NFS, backup 구성을 자동화했습니다. 이후 WAS DB 환경변수 오설정, rollback, postflight, work-order trace, closeout acceptance까지 이어지는 시나리오를 수행했습니다. 특히 NAT는 패키지 설치가 필요한 구간에서만 사용하고, 이후에는 제거한 상태에서 내부 통신 기반 검증을 이어가며 불필요한 리소스 사용을 줄였습니다. 이 경험은 클라우드 자원을 만드는 것보다, 운영 목적에 맞게 상태를 확인하고 evidence를 남기는 과정이 중요하다는 인식을 갖게 했습니다.
```

## Interview answer structure

Question:

```text
이 프로젝트에서 가장 의미 있었던 점은 무엇인가요?
```

Suggested answer structure:

```text
1. 단순 배포가 아니라 운영 검증을 목표로 했다.
2. WEB/WAS/DB/NFS 계층을 직접 구성했다.
3. WAS DB 설정 오류를 통제된 변경으로 재현했다.
4. /healthz와 /readyz를 분리해 프로세스 생존과 업무 가능 상태를 구분했다.
5. rollback 후에도 DB-backed API, NFS 파일, checksum, 로그로 복구를 확인했다.
6. 마지막에는 CHG-001 work-order와 closeout acceptance로 변경 종료 판단까지 연결했다.
```

Example answer:

```text
가장 의미 있었던 점은 장애를 단순히 만들고 복구한 것이 아니라, 운영 변경 요청의 흐름으로 끝까지 검증했다는 점입니다. 처음에는 EC2에 Nginx, Spring Boot, PostgreSQL, NFS를 구성하는 수준이었지만, 이후 WAS DB 설정 오류를 통제된 변경으로 재현했습니다. 이때 /healthz는 200이지만 /readyz와 dependency endpoint는 503이 되는 상태를 확인하면서 프로세스 생존과 업무 가능 상태가 다를 수 있다는 점을 검증했습니다. rollback 후에는 서비스 재시작만 보지 않고, 업무 API, DB row, NFS evidence 파일, checksum, Nginx/app request-id 로그까지 확인했습니다. 마지막에는 CHG-001 work-order를 OPEN에서 IN_PROGRESS, DONE으로 남기고 closeout acceptance까지 수행했습니다. 이 과정이 운영 직무에서 필요한 상태 확인, 장애 범위 분리, 복구 검증, 증거 확보를 가장 잘 보여준다고 생각합니다.
```

## Possible job-application connections

### WEB/WAS 운영

Connect to:

```text
Nginx reverse proxy
WAS process and readiness
systemd environment rollback
request-id logs
DB dependency failure
```

Main message:

```text
WEB/WAS 운영은 서버가 떠 있는지만 보는 것이 아니라 요청 흐름과 dependency 상태를 함께 확인하는 일이라고 이해했다.
```

### 전산직 / IT 운영

Connect to:

```text
업무 API
상태 이력
DB row
증거 파일
변경 요청 종료 판단
```

Main message:

```text
전산 운영은 변경사항이 실제 업무 데이터와 처리 흐름에 안전하게 반영됐는지 근거로 확인하는 일이라고 이해했다.
```

### 금융 IT

Connect to:

```text
작은 설정 변경의 영향 범위
권한/이력/상태/로그 중심 검증
업무 흐름 중단 여부
변경 전후 확인
```

Main message:

```text
금융 IT에서는 기능이나 설정 변경 후 업무 처리 가능 여부와 영향 범위를 명확히 확인해야 한다.
```

### 클라우드/인프라

Connect to:

```text
Terraform
Ansible
EC2 tiering
private subnet
NAT use/removal
bastion access
security group flow
```

Main message:

```text
클라우드 자원을 구성하는 것에서 끝나지 않고, 운영 절차를 수행하고 검증할 수 있는 환경으로 만들었다.
```

## Limitations to state honestly

Use these when asked about limitations:

```text
상용 트래픽을 처리한 것은 아닙니다.
실제 고객 장애를 대응한 것은 아닙니다.
HA나 DR을 검증한 것은 아닙니다.
모니터링 스택은 Prometheus/Grafana 중심으로 완성한 프로젝트는 아닙니다.
비용 제약 때문에 Free Tier 중심의 축소 구성을 사용했습니다.
```

But immediately connect back to value:

```text
다만 운영 직무에서 중요한 변경 전후 확인, dependency 장애 분리, rollback 검증, evidence 수집 절차를 직접 설계하고 실행했다는 점에 의미를 두었습니다.
```

## Best project keywords

Use these repeatedly across applications:

```text
상태 확인
변경 전후 검증
장애 범위 분리
dependency readiness
rollback 검증
업무 흐름 확인
DB row
NFS evidence
checksum consistency
request-id 로그
work-order 이력
closeout acceptance
증거 기반 운영
반복 가능한 검증 절차
```

Avoid these unless the job post specifically requires them and the answer is carefully limited:

```text
무중단
대규모
실시간 관제
상용 운영
DR
HA
SLA
SLO
보안 감사
ISMS
```

## Most reusable final summary

Use this as the core summary when adapting the project to applications:

```text
EC2 기반 WEB/WAS/DB/NFS 운영 환경을 구성하고, WAS DB 설정 오류를 통제된 운영 변경 시나리오로 재현했습니다. 변경 전 preflight로 정상 기준을 확인하고, 변경 후에는 /healthz, /readyz, dependency endpoint를 비교해 프로세스 상태와 업무 가능 상태를 분리했습니다. rollback 이후에는 업무 API, DB row, NFS evidence 파일, checksum, Nginx/app request-id 로그를 통해 복구 여부를 검증했습니다. 마지막으로 CHG-001 work-order 이력과 closeout acceptance를 구성해 변경 요청을 종료할 수 있는지까지 확인했습니다. 이를 통해 운영 업무에서 중요한 것은 배포나 재시작 자체가 아니라, 변경 전후 상태를 근거로 확인하고 장애 범위와 복구 결과를 evidence로 남기는 일임을 체감했습니다.
```
