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

### 방식 1. 로컬 jar copy

control node에 jar가 존재해야 한다.

기본 경로는 다음이다.

```text
apps/ops-sample-service/target/ops-sample-service-0.1.0.jar
```

로컬에 Maven이 있는 경우:

```bash
cd apps/ops-sample-service
mvn clean package
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

### 방식 2. 원격 artifact URL

GitHub Actions artifact, release asset, 사내 artifact repository 등을 사용할 수 있다.

```bash
ansible-playbook \
  -i inventories/lab-full-min/hosts.yml \
  playbooks/lab-full-min-ops-sample-service.yml \
  -e 'ops_db_password=<strong-local-password>' \
  -e 'ops_app_jar_url=https://example.invalid/ops-sample-service-0.1.0.jar'
```

현재 단계에서는 실제 artifact URL을 커밋하지 않는다.

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
