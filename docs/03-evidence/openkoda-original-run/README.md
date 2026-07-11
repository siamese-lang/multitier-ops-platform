# OpenKoda Original Run Evidence

## Scenario

Verify upstream OpenKoda execution before applying operational modifications.

This evidence belongs to Issue #3. The verification was performed with the upstream Docker Compose quick-start path on a temporary single EC2 instance.

## Date

2026-07-11 KST

## Environment

| Item | Value |
|---|---|
| Host | Temporary AWS EC2 instance |
| Private hostname | `ip-172-31-13-145` |
| Public IPv4 used for test | `13.125.14.138` |
| OS | Ubuntu EC2 instance; exact `/etc/os-release` output should be attached separately if retained |
| Java | OpenKoda container runtime reported Java 17.0.12 |
| Maven | Not used in this verification path |
| PostgreSQL | Docker container `postgres:14.2` |
| Docker / Docker Compose | Docker Compose quick-start path used |
| OpenKoda source strategy | Upstream source was not modified or copied into this repository |
| Execution path | Docker Compose quick-start |

## Upstream baseline verified

| Area | Expected baseline | Observed result |
|---|---|---|
| Upstream repository | `https://github.com/openkoda/openkoda` | Used for compose file and image reference |
| License | MIT License | Documented in `NOTICE.md` and `upstream/README.md` |
| Docker Compose app port | `8080` | `0.0.0.0:8080->8080/tcp` observed |
| Default compose DB | PostgreSQL 14.2 | `postgres-db` container healthy |
| OpenKoda container | `openkoda/openkoda:latest` | Container running |
| Application protocol | HTTP on port 8080 | Tomcat started on `8080 (http)` |
| Local HTTP response | HTML response from OpenKoda | `HTTP/1.1 200` with OpenKoda home page HTML |

## Commands and outputs

The following command group was executed on the EC2 instance.

```bash
sudo docker compose ps
sudo docker ps
sudo docker logs postgres-db --tail=100
sudo docker logs openkoda --tail=200
curl -v http://127.0.0.1:8080
curl -I http://127.0.0.1:8080
ss -lntp | grep 8080
```

Representative observed outputs:

```text
NAME          IMAGE                      SERVICE    STATUS                    PORTS
openkoda      openkoda/openkoda:latest   openkoda   Up                        0.0.0.0:8080->8080/tcp, [::]:8080->8080/tcp
postgres-db   postgres:14.2              postgres   Up (healthy)              5432/tcp
```

```text
Tomcat initialized with port(s): 8080 (http)
Tomcat started on port(s): 8080 (http) with context path ''
Started OpenkodaApp
```

```text
curl -v http://127.0.0.1:8080
< HTTP/1.1 200
<title>Openkoda</title>
<a class="btn btn-primary d-block ml-auto mr-auto mb-2" href="/login">Login</a>
<a class="btn btn-light d-block m-auto " href="/register">Register</a>
```

## Access path verification

| Access path | Result | Interpretation |
|---|---|---|
| EC2 internal `curl http://127.0.0.1:8080` | Success | Application and Docker port mapping are functional |
| SSH tunnel to local `http://localhost:8080` | Success | Web UI is reachable through the SSH path |
| Direct public access `http://13.125.14.138:8080` | Failed | Public inbound path requires separate AWS network investigation |
| PowerShell `Test-NetConnection 13.125.14.138 -Port 8080` | `TcpTestSucceeded: False` | Failure is outside the application runtime |

## Expected result

The upstream application should start from the Docker Compose quick-start path, connect to PostgreSQL, listen on port 8080, and return the OpenKoda web UI without local source modifications.

## Actual result

The expected result was met for the EC2 internal path and SSH tunnel path. OpenKoda returned `HTTP/1.1 200` and the home page HTML with Login/Register links when accessed from EC2 localhost.

Direct access to the EC2 public IP on port 8080 failed even after the user attempted to open the security group inbound rule broadly. Because the same application was reachable through EC2 localhost and SSH tunneling, this was classified as an external AWS network path issue rather than an OpenKoda runtime issue.

## Conclusion

OpenKoda upstream Docker Compose execution on a temporary single EC2 instance is verified.

This satisfies the Issue #3 baseline requirement: the project now has evidence that the upstream application can run as an operational target before local hardening, Terraform infrastructure, or Ansible configuration work begins.

## Follow-up

- Capture or retain a screenshot of the OpenKoda page reached through SSH tunneling if available.
- Terminate the temporary EC2 instance after evidence is retained.
- Treat direct public `8080` failure as a separate network-path finding, not as a blocker for OpenKoda baseline verification.
- Proceed to Issue #4: define operational hardening scope for liveness/readiness, metrics, request ID logging, filesystem storage, and controlled fault injection.
