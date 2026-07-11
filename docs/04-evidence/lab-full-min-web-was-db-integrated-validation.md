# lab-full-min WEB/WAS/DB integrated validation evidence

## 1. Validation purpose

This evidence document summarizes the completed `lab-full-min` integrated validation for the project theme:

```text
AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증
```

The validation goal was to prove that the repository now contains an operating baseline, not separate Terraform and Ansible examples.

The proven runtime path was:

```text
operator -> nginx-01:443 -> app-01/app-02:8080 -> db-primary-01:5432
```

Related work:

- Issue #59: `[VALIDATION] lab-full-min WEB/WAS/DB 통합 검증`
- PR #61: `[CI] ops-sample-service jar artifact 생성`
- PR #63: `[TF] lab-full-min Nginx HTTPS ingress 허용`

## 2. Environment summary

The validation used the following VM-based multi-tier layout.

```text
[Public subnet]
- bastion-01
- nginx-01

[Private app subnet]
- app-01
- app-02

[Private DB subnet]
- db-primary-01
```

Key runtime addresses captured during validation:

```text
bastion_public_ip = 52.78.9.182
nginx_public_ip   = 43.201.7.135
nginx_private_ip  = 10.40.1.143
app-01_private_ip = 10.40.11.136
app-02_private_ip = 10.40.11.56
db_private_ip     = 10.40.21.184
```

Security group flow verified by Terraform outputs:

```text
operator CIDR -> bastion-01:22
operator CIDR -> nginx-01:80
operator CIDR -> nginx-01:443
bastion SG    -> nginx/app/db:22
nginx SG      -> app SG:8080
app SG        -> db SG:5432
private app/db subnets -> NAT Gateway
```

The operator public IP is intentionally omitted from this document. It remains visible only in transient validation output.

## 3. Terraform validation

Terraform apply created the `lab-full-min` baseline and later applied an in-place HTTPS ingress update for Nginx.

Initial apply created the VM network and nodes. A later Terraform update for PR #63 added only one security group rule:

```text
Apply complete! Resources: 1 added, 0 changed, 0 destroyed.
```

After the update, `tier_flow_summary` showed both HTTP and HTTPS entry paths:

```text
web_to_nginx_http  = <operator-cidr> -> nginx-01:80
web_to_nginx_https = <operator-cidr> -> nginx-01:443
nginx_to_app       = nginx SG -> app SG:8080
app_to_db          = app SG -> db SG:5432
```

This confirmed that public HTTPS validation could reach the Nginx WEB tier without broad ingress.

## 4. Ansible control path validation

The generated Ansible inventory reached all `lab-full-min` nodes through the intended SSH paths.

Control path summary:

```text
operator -> bastion-01:22
operator -> nginx-01 via bastion ProxyCommand
operator -> app-01/app-02 via bastion ProxyCommand
operator -> db-primary-01 via bastion ProxyCommand
```

The inventory, ping, and baseline checks completed successfully before service deployment.

## 5. PostgreSQL primary validation

PostgreSQL was installed and configured on `db-primary-01`.

Play recap:

```text
db-primary-01 : ok=20 changed=9 unreachable=0 failed=0
```

Validation report:

```text
project=multitier-ops-platform
environment=lab-full-min
inventory_hostname=db-primary-01
ansible_host=10.40.21.184
hostname=ip-10-40-21-184
node_role=db-primary
node_tier=private-db
postgresql_service_state=active
postgresql_port=5432
lab_full_min_app_cidr=10.40.11.0/24
ops_db_name=opsdb
ops_db_user=ops_user
db_now=2026-07-11 18:43:10.482652+00
log_min_duration_statement_ms=500
```

Manual checks confirmed:

```text
systemctl is-active postgresql -> active
ss -ltnp | grep ':5432'      -> PostgreSQL listening
pg_hba.conf                  -> opsdb/ops_user allowed from 10.40.11.0/24 only
select now()                 -> successful query against opsdb
```

This established the DB tier for the WAS layer.

## 6. WAS systemd deployment validation

`ops-sample-service` was deployed as a systemd-managed workload on both app nodes.

The first run exposed one transient app-02 `503` response on `/api/work-orders/summary` while the DB-backed schema/data path was being initialized. Subsequent checks and the idempotent rerun succeeded.

Final play recap:

```text
app-01 : ok=23 changed=0 unreachable=0 failed=0 skipped=1 rescued=0 ignored=0
app-02 : ok=23 changed=0 unreachable=0 failed=0 skipped=1 rescued=0 ignored=0
```

Final app-01 report:

```text
inventory_hostname=app-01
ansible_host=10.40.11.136
hostname=ip-10-40-11-136
node_role=app
node_tier=private-was
service_name=ops-sample-service
service_state=active
app_port=8080
app_version=0.1.0
db_host=10.40.21.184
db_name=opsdb
db_user=ops_user
healthcheck_rc=0
readycheck_rc=0
work_orders_summary_rc=0
```

Final app-02 report:

```text
inventory_hostname=app-02
ansible_host=10.40.11.56
hostname=ip-10-40-11-56
node_role=app
node_tier=private-was
service_name=ops-sample-service
service_state=active
app_port=8080
app_version=0.1.0
db_host=10.40.21.184
db_name=opsdb
db_user=ops_user
healthcheck_rc=0
readycheck_rc=0
work_orders_summary_rc=0
```

Direct app checks showed:

```text
app-01 /healthz -> 200
app-02 /healthz -> 200
app-01 /readyz  -> 200, database connection ok
app-02 /readyz  -> 200, database connection ok
app-01 /node    -> hostname ip-10-40-11-136
app-02 /node    -> hostname ip-10-40-11-56
```

DB-backed summary response from both app nodes:

```json
{
  "service": "ops-sample-service",
  "status": "ok",
  "operation": "work_orders.summary",
  "data": {
    "total": 5,
    "open": 3,
    "in_progress": 1,
    "done": 1,
    "cancelled": 0
  }
}
```

Application logs included request-level operational fields:

```text
event=http_request
requestId=<uuid>
method=GET
path=/api/work-orders/summary
status=200
durationMs=<duration>
remoteAddr=127.0.0.1
node=ip-10-40-11-136 or ip-10-40-11-56
role=app
tier=private-was
```

This established the WAS tier as a controlled operational workload rather than the project subject itself.

## 7. Nginx WEB reverse proxy validation

Nginx was configured as the WEB tier with HTTP-to-HTTPS redirect, self-signed TLS, reverse proxying, and upstream logging.

Play recap:

```text
nginx-01 : ok=28 changed=8 unreachable=0 failed=0 skipped=0 rescued=0 ignored=0
```

Nginx report:

```text
project=multitier-ops-platform
environment=lab-full-min
inventory_hostname=nginx-01
ansible_host=10.40.1.143
hostname=ip-10-40-1-143
node_role=web
node_tier=public-web
nginx_service_state=active
http_port=80
https_port=443
upstream_name=ops_app_backend
upstream_hosts=app-01,app-02
upstream_app_port=8080
redirect_status=301
https_health_rc=0
https_node_rc=0
https_summary_rc=0
access_log=/var/log/nginx/ops_access.log
error_log=/var/log/nginx/ops_error.log
config_test=nginx: the configuration file /etc/nginx/nginx.conf syntax is ok
nginx: configuration file /etc/nginx/nginx.conf test is successful
```

Local WEB checks on `nginx-01`:

```text
nginx -t                  -> successful
systemctl is-active nginx -> active
http://127.0.0.1/healthz  -> 301 redirect to HTTPS
https://127.0.0.1/healthz -> 200 app response
https://127.0.0.1/node    -> app node identity
https://127.0.0.1/api/work-orders/summary -> DB-backed data
```

Public HTTPS checks from the operator machine:

```text
curl -k -i https://43.201.7.135/healthz -> 200
curl -k -s https://43.201.7.135/node -> app node identity
curl -k -s https://43.201.7.135/api/work-orders/summary -> DB-backed data
```

Public DB-backed response sample:

```json
{
  "service": "ops-sample-service",
  "status": "ok",
  "operation": "work_orders.summary",
  "node": {
    "hostname": "ip-10-40-11-136",
    "localAddress": "10.40.11.136",
    "role": "app",
    "tier": "private-was",
    "environment": "lab-full-min",
    "version": "0.1.0"
  },
  "data": {
    "total": 5,
    "open": 3,
    "in_progress": 1,
    "done": 1,
    "cancelled": 0
  }
}
```

## 8. Nginx access log evidence

Nginx access logs proved reverse proxying to both app nodes.

Sample lines:

```text
time=2026-07-11T19:06:19+00:00 request_id=6b6357754bc2e9cba6d788b06353c5ac remote_addr=127.0.0.1 host=127.0.0.1 method=GET uri="/healthz" status=200 bytes=286 request_time=0.006 upstream_addr="10.40.11.56:8080" upstream_status="200" upstream_response_time="0.006" upstream_connect_time="0.000" upstream_header_time="0.005"

time=2026-07-11T19:06:20+00:00 request_id=4ee92810cad70d5c2fbde1da75adc81e remote_addr=127.0.0.1 host=127.0.0.1 method=GET uri="/node" status=200 bytes=270 request_time=0.005 upstream_addr="10.40.11.136:8080" upstream_status="200" upstream_response_time="0.005" upstream_connect_time="0.000" upstream_header_time="0.004"

time=2026-07-11T19:06:22+00:00 request_id=afcd74dca2f739e41e77f2ac97d06b6b remote_addr=127.0.0.1 host=127.0.0.1 method=GET uri="/api/work-orders/summary" status=200 bytes=366 request_time=0.041 upstream_addr="10.40.11.56:8080" upstream_status="200" upstream_response_time="0.042" upstream_connect_time="0.000" upstream_header_time="0.041"
```

These lines show:

```text
Nginx received the client request.
Nginx forwarded the request to private WAS upstreams.
Both app-01 and app-02 served traffic.
Nginx recorded upstream status and latency fields.
```

## 9. Cleanup status

After evidence collection, the lab resources were destroyed.

Recorded cleanup status:

```text
Terraform destroy completed.
```

The active validation issue was closed as completed after cleanup. Future validation runs should also paste the `terraform state list` output to explicitly show empty state in the evidence record.

## 10. Result

This validation proved the minimum operating baseline:

```text
Public WEB tier  : nginx-01
Private WAS tier : app-01, app-02
Private DB tier  : db-primary-01
```

The validated path was:

```text
operator -> nginx-01 HTTPS -> app systemd workload -> PostgreSQL opsdb
```

The project now has a defensible baseline for later operation-focused incident reports.

## 11. Remaining limitations

This validation intentionally did not cover:

- app-01 failure and Nginx passive upstream bypass
- rolling deployment behavior
- DB connection pool or bottleneck analysis
- PostgreSQL standby/failover
- Prometheus/Grafana/Loki/Alertmanager observability stack
- backup and restore drills

Those should be implemented as separate validation and incident-report issues.

## 12. Next recommended validation targets

Recommended next issues:

```text
1. [INCIDENT] app-01 장애 발생 시 Nginx upstream 우회 검증
2. [DEPLOY] app rolling restart/deploy 검증
3. [OBS] Prometheus/Grafana/Loki 최소 관측성 구성
4. [BACKUP] PostgreSQL dump/restore 검증
```
