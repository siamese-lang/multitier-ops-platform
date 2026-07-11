# lab-full-min ops-sample-service systemd 배포 Runbook

## 목적

이 runbook은 `lab-full-min`의 `app-01`, `app-02`에 `ops-sample-service`를 systemd 서비스로 배포하는 절차를 정리한다.

이 작업의 목적은 애플리케이션 개발이 아니라, 다음 운영 흐름을 검증할 WAS 계층을 만드는 것이다.

```text
nginx-01 -> app-01/app-02 -> db-primary-01
```

`ops-sample-service`는 WEB/WAS/DB 운영 evidence를 만들기 위한 controlled workload다.

## 사전 조건

다음 작업이 먼저 완료되어 있어야 한다.

```text
1. lab-full-min Terraform apply
2. inventories/lab-full-min/hosts.yml 작성
3. Ansible ping 검증
4. PostgreSQL primary 구성
5. ops-sample-service jar artifact 준비
```

## Secret 주의

DB 비밀번호는 저장소에 커밋하지 않는다.

다음 중 하나로 공급한다.

```bash
ansible-playbook \
  -i inventories/lab-full-min/hosts.yml \
  playbooks/lab-full-min-ops-sample-service.yml \
  -e 'ops_db_password=<strong-local-password>'
```

또는 ignored inventory인 `inventories/lab-full-min/hosts.yml`의 app group vars에 넣는다.

장기적으로는 Ansible Vault로 전환한다.

## Artifact 공급 방식

`ops-sample-service` 배포 playbook은 두 가지 artifact 공급 방식을 지원한다.

```text
1. control node에 있는 local jar를 app 노드로 copy
2. app 노드가 remote artifact URL에서 jar를 download
```

현재 `lab-full-min` 통합 검증에서는 **로컬 Maven이 없는 상황도 고려해야 하므로**, GitHub Actions가 빌드한 artifact를 내려받아 local jar copy 방식으로 사용하는 것을 기본 우회 경로로 둔다.

### 방식 1. 로컬 Maven으로 jar 생성

control node에 Maven이 있으면 직접 빌드한다.

```bash
cd apps/ops-sample-service
mvn clean package
```

기본 artifact 경로는 다음이다.

```text
apps/ops-sample-service/target/ops-sample-service-0.1.0.jar
```

그 뒤 Ansible을 실행한다.

```bash
cd infra/ansible
export ANSIBLE_CONFIG="$PWD/ansible.cfg"

ansible-playbook \
  -i inventories/lab-full-min/hosts.yml \
  playbooks/lab-full-min-ops-sample-service.yml \
  -e 'ops_db_password=<strong-local-password>'
```

### 방식 2. GitHub Actions artifact를 내려받아 local jar로 사용

로컬에 Maven이 없으면 GitHub Actions artifact를 사용한다.

workflow:

```text
.github/workflows/ops-sample-service-ci.yml
```

artifact name:

```text
ops-sample-service-0.1.0-jar
```

GitHub UI에서 다운로드하는 절차:

```text
1. GitHub repository -> Actions
2. ops-sample-service-ci workflow 선택
3. main 또는 해당 PR의 성공한 run 선택
4. Artifacts에서 ops-sample-service-0.1.0-jar 다운로드
5. zip 압축 해제
6. ops-sample-service-0.1.0.jar를 아래 경로에 배치
```

배치 경로:

```text
apps/ops-sample-service/target/ops-sample-service-0.1.0.jar
```

필요하면 디렉터리를 먼저 만든다.

```bash
mkdir -p apps/ops-sample-service/target
```

이 방식은 Ansible 입장에서는 방식 1과 동일하게 local jar copy로 동작한다.

### 방식 3. 원격 artifact URL

GitHub Release asset, 사내 artifact repository, 사전 서명된 객체 스토리지 URL처럼 app 노드가 직접 다운로드할 수 있는 URL이 있을 때 사용한다.

```bash
ansible-playbook \
  -i inventories/lab-full-min/hosts.yml \
  playbooks/lab-full-min-ops-sample-service.yml \
  -e 'ops_db_password=<strong-local-password>' \
  -e 'ops_app_jar_url=https://example.invalid/ops-sample-service-0.1.0.jar'
```

GitHub Actions artifact의 웹 다운로드 URL은 보통 인증·리다이렉트·만료 조건이 있어 Ansible `get_url`에 직접 넣는 안정적인 배포 URL로 보기 어렵다. 따라서 현재 단계에서는 Actions artifact를 수동 다운로드한 뒤 local jar copy 방식으로 사용하는 것을 우선한다. 안정적인 원격 URL이 필요해지면 별도 release asset publishing workflow를 추가한다.

## 실행

WSL, Linux, macOS에서 실행한다. native Windows Git Bash에서 Ansible을 실행하지 않는다.

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible
export ANSIBLE_CONFIG="$PWD/ansible.cfg"

ansible-playbook \
  -i inventories/lab-full-min/hosts.yml \
  playbooks/lab-full-min-ops-sample-service.yml \
  -e 'ops_db_password=<strong-local-password>'
```

## 배포 결과

각 app 노드에 다음이 생성된다.

```text
/opt/ops-sample-service/ops-sample-service.jar
/etc/ops-sample-service/ops-sample-service.env
/etc/systemd/system/ops-sample-service.service
/var/log/ops-sample-service/
/tmp/multitier-ops-platform/lab-full-min-ops-sample-service-<host>.txt
```

`ops-sample-service.env`에는 DB 비밀번호가 들어가므로 `0600 root:root` 권한으로 생성한다.

## 수동 확인 명령

app 노드에서 확인한다.

```bash
systemctl status ops-sample-service --no-pager
journalctl -u ops-sample-service -n 100 --no-pager
ss -ltnp | grep ':8080'
```

local HTTP check:

```bash
curl -i http://127.0.0.1:8080/healthz
curl -i http://127.0.0.1:8080/readyz
curl -s http://127.0.0.1:8080/node
curl -s http://127.0.0.1:8080/api/work-orders/summary
```

## 기대 결과

PostgreSQL primary가 정상이고 DB 비밀번호가 올바르면 다음이 기대된다.

```text
/healthz                 -> 200
/readyz                  -> 200
/node                    -> 200
/api/work-orders/summary -> 200
```

PostgreSQL이 중지되었거나 DB 설정이 잘못되면 다음이 기대된다.

```text
/healthz                 -> 200
/readyz                  -> 503
/api/work-orders/summary -> 503
```

이 차이가 이후 DB 장애 incident report의 핵심 evidence가 된다.

## 로그 evidence

앱 로그는 journald에서 확인한다.

```bash
journalctl -u ops-sample-service -n 100 --no-pager
```

요청 로그에는 다음 필드가 포함된다.

```text
event=http_request
requestId
method
path
status
durationMs
remoteAddr
node
role
tier
```

이 값은 나중에 Nginx access log의 request ID와 연결해 요청 경로를 추적하는 데 사용한다.

## 장애 시나리오 연결

이 배포가 완료되면 다음 검증으로 이어진다.

```text
1. app-01/app-02 모두 systemd로 실행
2. Nginx upstream에 app-01/app-02 등록
3. /node로 응답 노드 확인
4. /api/work-orders/summary로 DB-backed 데이터 확인
5. app-01 중지
6. Nginx가 app-02로 우회하는지 확인
7. journalctl, Nginx access log, error log로 evidence 수집
```

## 정리

이 runbook은 app 배포 자체가 목적이 아니다. 목적은 VM 기반 WEB/WAS/DB 운영환경에서 WAS 계층을 systemd 단위로 관리하고, 장애·복구 분석에 필요한 로그와 상태 점검 기준을 만드는 것이다.
