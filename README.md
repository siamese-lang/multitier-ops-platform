# AWS EC2 기반 다계층 업무시스템 운영환경 구축 및 장애·복구 검증

이 저장소는 AWS EC2를 VM처럼 사용하여 다계층 업무시스템 운영환경을 구축하고, 장애·복구·백업·관측성 검증을 수행하기 위한 운영형 포트폴리오 프로젝트입니다.

운영 대상 애플리케이션은 OpenKoda입니다. OpenKoda는 외부 오픈소스 원본 애플리케이션이며, 본 저장소 작성자가 원본을 개발한 것이 아닙니다. 본 프로젝트는 OpenKoda를 운영 대상으로 삼아 인프라 구성, 서버 설정 자동화, 운영성 보강, 장애 분석, 복구 검증, runbook 및 evidence 문서화를 수행합니다.

## 핵심 목표

- AWS EC2 기반 VM-style 운영환경 구성
- Terraform 기반 인프라 구성
- Ansible 기반 서버 설정 자동화
- Nginx, Spring Boot 내장 Tomcat, PostgreSQL, NFS 계층 분리
- Prometheus, Grafana, Alertmanager, Loki, Grafana Alloy 기반 관측성 구성
- Restic 기반 백업 및 복구 검증
- 장애 시나리오 수행 및 incident report 작성

## 제외 범위

- OpenKoda 원본을 직접 개발한 것처럼 표현하지 않습니다.
- AWS 관리형 서비스 중심 아키텍처로 만들지 않습니다.
- 기존 EKS/Terraformers 프로젝트와 겹치지 않도록 EC2/VM 운영 중심으로 유지합니다.

## 최종 목표 아키텍처

- bastion-01
- nginx-01, nginx-02
- app-01, app-02, app-03
- db-primary-01, db-standby-01
- nfs-01
- mon-01
- log-01
- backup-01
- loadgen-01

## 작업 원칙

모든 작업은 GitHub Issue 단위로 수행합니다.

각 Issue는 다음 흐름을 따릅니다.

1. 설계
2. 구현
3. 검증
4. evidence 수집
5. runbook 또는 incident report 문서화

## 현재 단계

현재 단계는 OpenKoda 원본 실행 검증 전, 저장소 구조와 문서 기준을 만드는 초기화 단계입니다. Terraform, Ansible, OpenKoda 수정 코드는 이후 Issue에서 단계적으로 추가합니다.
