# OpenKoda Original Run Evidence

## Scenario

Verify upstream OpenKoda execution before applying operational modifications.

This evidence directory belongs to Issue #3, but the source and requirement checklist is prepared during Issue #2 so the manual verification can be performed consistently.

## Date

`<TO_BE_FILLED_AFTER_MANUAL_RUN>`

## Environment

| Item | Value |
|---|---|
| Host | `<TO_BE_FILLED>` |
| OS | `<TO_BE_FILLED>` |
| Java | `<TO_BE_FILLED>` |
| Maven | `<TO_BE_FILLED>` |
| PostgreSQL | `<TO_BE_FILLED>` |
| Docker / Docker Compose | `<TO_BE_FILLED_IF_USED>` |
| OpenKoda commit | `<TO_BE_FILLED>` |
| Execution path | `<Docker Compose quick-start / source build>` |

## Upstream baseline to verify

| Area | Expected baseline |
|---|---|
| Upstream repository | `https://github.com/openkoda/openkoda` |
| License | MIT License |
| Source build prerequisites | Git, Java 17, Maven 3.8+, PostgreSQL 14+ |
| Docker Compose app port | `8080` |
| Source run example port | `8030` |
| Default admin username | `admin` |
| Default admin password | `admin123` |
| Default compose DB | PostgreSQL 14.2, database `openkoda`, user `postgres` |

## Commands and outputs

Store sanitized command outputs in separate files.

Required files for source-build verification:

```text
01-environment.txt
02-upstream-commit.txt
03-build-output.txt
04-db-init-log.txt
05-app-startup-log.txt
06-process-and-port-check.txt
07-http-check.txt
08-troubleshooting-notes.md
```

Recommended command set:

```bash
cat /etc/os-release
java -version
mvn -version
psql --version || true
docker version || true
docker compose version || true
git rev-parse HEAD
mvn -f openkoda/pom.xml clean install spring-boot:repackage -DskipTests
find . -name 'openkoda.jar' -o -name '*.jar' | sort | tail -50
ss -lntp | grep -E '8030|8080|5432'
curl -I http://localhost:8030 || true
curl -k -I https://localhost:8080 || true
```

## Expected result

The upstream application builds, starts, connects to its database, listens on the expected port, and responds to HTTP requests without local source modifications.

## Actual result

`<TO_BE_FILLED_AFTER_MANUAL_RUN>`

## Conclusion

`<TO_BE_FILLED_AFTER_MANUAL_RUN>`

## Follow-up

- If the upstream app starts successfully, proceed to Issue #4 and define operational hardening targets.
- If it fails, preserve the failure log and document the first blocking cause before changing any source code.
- Do not start Terraform lab-small until the upstream execution requirements are understood.