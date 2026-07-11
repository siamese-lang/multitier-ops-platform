# lab-full-min Nginx HTTPS reverse proxy Runbook

## 목적

이 runbook은 `lab-full-min`의 `nginx-01`에 Nginx WEB 계층을 구성하는 절차를 정리한다.

이 작업의 목적은 Nginx 설치 자체가 아니라, 다음 운영 흐름을 검증할 WEB/reverse proxy 계층을 만드는 것이다.

```text
client -> nginx-01 -> app-01/app-02 -> db-primary-01
```

Nginx는 이후 incident report에서 다음 evidence를 제공해야 한다.

```text
HTTPS termination
HTTP -> HTTPS redirect
upstream load balancing
passive upstream failover
proxy timeout / retry
X-Request-Id propagation
access log / error log
upstream_status / upstream_response_time
```

## 사전 조건

다음 작업이 먼저 완료되어 있어야 한다.

```text
1. lab-full-min Terraform apply
2. inventories/lab-full-min/hosts.yml 작성
3. Ansible ping 검증
4. PostgreSQL primary 구성
5. ops-sample-service systemd 배포
```

Nginx는 `app` inventory group을 읽어 upstream을 구성한다. 따라서 `app-01`, `app-02`의 `ansible_host` 값이 올바르게 들어 있어야 한다.

## HTTPS 정책

`lab-full-min`에서는 self-signed TLS 인증서를 사용한다.

이유는 다음과 같다.

```text
- 단기 실험 환경이다.
- 공인 도메인 운영이 목적이 아니다.
- TLS termination, redirect, proxy header, log evidence 검증이 목적이다.
```

따라서 검증 시 `curl -k`를 사용한다.

공인 도메인과 ACME 자동화는 추후 `lab-full` 또는 공개 서비스 단계에서만 고려한다.

## 실행

WSL, Linux, macOS에서 실행한다. native Windows Git Bash에서 Ansible을 실행하지 않는다.

```bash
cd /mnt/c/Project/test/multitier-ops-platform/infra/ansible
export ANSIBLE_CONFIG="$PWD/ansible.cfg"

ansible-playbook \
  -i inventories/lab-full-min/hosts.yml \
  playbooks/lab-full-min-nginx-reverse-proxy.yml
```

## 배포 결과

`nginx-01`에 다음이 생성된다.

```text
/etc/nginx/ssl/lab-full-min/ops-sample-service.crt
/etc/nginx/ssl/lab-full-min/ops-sample-service.key
/etc/nginx/sites-available/ops-sample-service.conf
/etc/nginx/sites-enabled/ops-sample-service.conf
/var/log/nginx/ops_access.log
/var/log/nginx/ops_error.log
/tmp/multitier-ops-platform/lab-full-min-nginx-reverse-proxy-nginx-01.txt
```

기본 site는 제거한다.

```text
/etc/nginx/sites-enabled/default -> absent
```

## Nginx 설정 핵심

### 1. HTTP to HTTPS redirect

HTTP 요청은 HTTPS로 리다이렉트한다.

```text
http://<nginx-public-ip>/...
-> https://<nginx-public-ip>/...
```

검증:

```bash
curl -I http://<nginx-public-ip>/healthz
```

기대값:

```text
301 또는 308
```

### 2. HTTPS reverse proxy

HTTPS 요청은 app upstream으로 전달한다.

```text
https://<nginx-public-ip>/healthz
https://<nginx-public-ip>/node
https://<nginx-public-ip>/api/work-orders/summary
```

검증:

```bash
curl -k -i https://<nginx-public-ip>/healthz
curl -k -s https://<nginx-public-ip>/node
curl -k -s https://<nginx-public-ip>/api/work-orders/summary
```

### 3. Upstream load balancing

Nginx upstream은 `app-01`, `app-02`를 대상으로 한다.

```text
upstream ops_app_backend
  -> app-01:8080
  -> app-02:8080
```

반복 요청으로 응답 노드를 확인한다.

```bash
for i in {1..10}; do
  curl -k -s https://<nginx-public-ip>/node
  echo
done
```

`node.hostname` 또는 `node.localAddress`가 app 노드 간에 바뀌면 upstream 분산 evidence로 사용할 수 있다.

### 4. Passive upstream failure handling

Nginx OSS 기준으로 active health check를 전제로 하지 않는다.

설정 기준:

```text
max_fails=2
fail_timeout=10s
proxy_next_upstream error timeout http_502 http_503 http_504
proxy_next_upstream_tries=2
```

장애 검증은 이후 incident issue에서 수행한다.

```text
app-01 stop
-> 반복 요청
-> app-02 응답 지속 여부 확인
-> access log의 upstream_addr, upstream_status, upstream_response_time 확인
```

## Proxy header 기준

Nginx는 app으로 다음 header를 전달한다.

```text
Host
X-Real-IP
X-Forwarded-For
X-Forwarded-Proto
X-Forwarded-Host
X-Forwarded-Port
X-Request-Id
```

`X-Request-Id`는 client가 보내면 그대로 전달하고, 없으면 Nginx가 생성한 request ID를 사용한다.

이 값은 app log와 Nginx access log를 연결하는 기준이다.

## 로그 evidence

### Access log

access log 위치:

```bash
sudo tail -n 50 /var/log/nginx/ops_access.log
```

주요 필드:

```text
time
request_id
remote_addr
host
method
uri
status
request_time
upstream_addr
upstream_status
upstream_response_time
upstream_connect_time
upstream_header_time
user_agent
```

### Error log

error log 위치:

```bash
sudo tail -n 50 /var/log/nginx/ops_error.log
```

app 장애 시 기대되는 evidence:

```text
connect() failed
upstream timed out
no live upstreams
upstream server temporarily disabled
```

실제 메시지는 장애 유형과 Nginx 버전에 따라 달라질 수 있으므로, incident report에는 원문 로그 일부를 그대로 남긴다.

## 수동 확인 명령

`nginx-01`에서 확인한다.

```bash
sudo nginx -t
systemctl status nginx --no-pager
ss -ltnp | grep -E ':80|:443'
```

local proxy check:

```bash
curl -k -i https://127.0.0.1/healthz
curl -k -s https://127.0.0.1/node
curl -k -s https://127.0.0.1/api/work-orders/summary
```

외부에서 확인한다.

```bash
curl -I http://<nginx-public-ip>/healthz
curl -k -i https://<nginx-public-ip>/healthz
curl -k -s https://<nginx-public-ip>/node
curl -k -s https://<nginx-public-ip>/api/work-orders/summary
```

## 기대 결과

PostgreSQL과 app 서비스가 정상인 경우:

```text
HTTP endpoint       -> 301 또는 308
HTTPS /healthz      -> 200
HTTPS /readyz       -> 200
HTTPS /node         -> 200
HTTPS work-orders   -> 200
```

app 또는 DB 문제가 있는 경우:

```text
/healthz may fail if all app upstreams are down
/readyz returns 503 if DB is unavailable
/api/work-orders/summary returns 503 if DB is unavailable
Nginx access/error log records upstream behavior
```

## 장애 시나리오 연결

이 배포가 완료되면 다음 검증으로 이어진다.

```text
1. app-01/app-02 모두 정상 실행
2. Nginx가 app-01/app-02로 요청 전달
3. /node로 응답 노드 확인
4. /api/work-orders/summary로 DB-backed 데이터 확인
5. app-01 중지
6. Nginx가 app-02로 우회하는지 확인
7. Nginx access/error log와 app journal log를 request ID 기준으로 연결
```

## 정리

이 runbook은 Nginx 설치 절차가 아니라, VM 기반 WEB/WAS/DB 운영환경에서 WEB 계층의 HTTPS, reverse proxy, upstream, log evidence 기준을 고정하기 위한 문서다.
