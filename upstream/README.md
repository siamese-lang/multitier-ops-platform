# Upstream Application

This directory documents upstream OpenKoda source information and verification notes.

Do not place unmodified upstream source code here unless there is a clear reason and the license allows it. Prefer recording the upstream repository URL, checked commit, and local modification strategy.

## OpenKoda source information

| Item | Value |
|---|---|
| Upstream repository | `https://github.com/openkoda/openkoda` |
| Repository full name | `openkoda/openkoda` |
| Default branch | `main` |
| Clone URL | `https://github.com/openkoda/openkoda.git` |
| License | MIT License |
| Copyright notice | `Copyright (c) 2023 openkoda` |
| Product version shown in README badge | Openkoda 1.7.1 |
| Java version shown in README badge | Java 17.0.2 |
| Spring Boot version shown in README badge | Spring Boot 3.0.5 |
| Verification date | 2026-07-10 KST |

## Upstream evidence

The upstream repository search result identifies `openkoda/openkoda` as a public repository owned by the `openkoda` organization, with default branch `main` and clone URL `https://github.com/openkoda/openkoda.git`.

The upstream README describes OpenKoda as a platform for building enterprise systems, SaaS applications, and internal tools. It also states that OpenKoda is open-source under the MIT license and uses Java, Spring Boot, JavaScript, HTML, Hibernate, and PostgreSQL.

The upstream LICENSE file is the MIT License and requires the copyright notice and permission notice to be included in all copies or substantial portions of the software.

## Runtime and build requirements from upstream documentation

| Area | Upstream requirement or default |
|---|---|
| Docker quick start | `curl https://raw.githubusercontent.com/openkoda/openkoda/main/docker/docker-compose.yaml \| docker compose -f - up` |
| Quick start URL | `https://localhost:8080` |
| Default admin login | `admin` |
| Default admin password | `admin123` |
| Source build prerequisites | Git, Java 17, Maven 3.8+, PostgreSQL 14+ |
| Build command | `mvn -f openkoda/pom.xml clean install spring-boot:repackage -DskipTests` |
| Database initialization profile | `openkoda,drop_and_init_database` |
| Runtime profile | `openkoda` |
| Example source runtime port | `8030` |
| Docker Compose application port | `8080:8080` |
| Docker Compose PostgreSQL image | `postgres:14.2` |
| Default database name | `openkoda` |
| Default database user | `postgres` |
| Default database password in compose example | `postgres` |
| Filesystem storage path variable | `FILE_STORAGE_FILESYSTEM_PATH`, default `/data` |
| Storage type variable | `STORAGE_TYPE`, default `database` |
| Secure cookie variable | `SECURE_COOKIE`, default `true` |

## Local source strategy

The preferred strategy is:

1. Verify upstream OpenKoda without modification.
2. Document required operational hardening.
3. Apply local modifications in `app/` or in a clearly identified fork/branch.
4. Keep every modification tied to an issue and verification evidence.

## Required notes before modification

- Why the modification is required
- Which upstream files are affected
- How the modification is verified
- Whether the change is operational or functional
- Related issue and PR

## Project boundary

This repository must not imply that OpenKoda was authored by this project owner. The portfolio value of this project is the EC2-based operations environment, automation, observability, backup/restore, incident handling, and evidence quality around an existing upstream business application.