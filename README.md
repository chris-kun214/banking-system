# Banking System

A full-stack online banking system built as a portfolio project — Spring Boot REST API, React frontend, and AWS-based infrastructure, developed following a real-company workflow (feature branches, PR-gated CI, Jenkins deployment). The full infra stack (ALB/ASG, RDS Multi-AZ + read replica, both Lambdas) was deployed to real AWS for end-to-end verification, not just `terraform validate` — see [CLAUDE.md](./CLAUDE.md#已知问题--真实踩过的坑live-部署验证时发现全部已修复并有对应-commit) for the real bugs that surfaced and got fixed along the way.

## Architecture

```
                                   ┌──────────────┐
                                   │  EventBridge  │
                                   │   Scheduler   │
                                   └──────┬───────┘
                       ┌───────────────────┼───────────────────┐
                       ▼                                       ▼
              ┌─────────────────┐                    ┌──────────────────┐
              │ daily-reconcile  │                    │  monthly-report   │
              │  Lambda (RDS)    │                    │ Lambda (app call) │
              └─────────┬────────┘                    └─────────┬────────┘
                        │                                        │
┌─────────────┐   JWT   ▼                                        ▼
│  React SPA  │ ──────► ┌───────┐   HTTP    ┌──────────────────┐
│  (frontend) │ ◄────── │  ALB  │ ────────► │  Spring Boot API  │
└─────────────┘         └───────┘  (ASG,    │    (backend)       │
                                    2 AZ)    │  Actuator/Micrometer│
                                             │  → CloudWatch       │
                                             └──────────┬──────────┘
                                                         │
                                            ┌────────────▼────────────┐
                                            │   PostgreSQL RDS         │
                                            │   Multi-AZ + read replica │
                                            └──────────────────────────┘

                          ┌──────────────────────────────┐
                          │  EMR Serverless + Spark        │
                          │  batch reconciliation over a   │
                          │  synthetic transaction dataset │
                          └────────────────────────────────┘
```

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.2.1, Java 21, Gradle (Kotlin DSL), Spring Data JPA, Spring Security + JWT |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS, React Router, Redux Toolkit |
| Database | PostgreSQL (AWS RDS, Multi-AZ + optional read replica) |
| Messaging | AWS SQS (async account-credit notifications) |
| Scheduled jobs | AWS EventBridge Scheduler + Lambda (daily reconciliation, monthly report trigger) |
| Big data | AWS EMR Serverless + Apache Spark (batch reconciliation) |
| AI | OpenAI Chat Completions (transaction description suggestions, with a rule-based fallback) |
| Observability | Spring Boot Actuator + Micrometer → CloudWatch metrics, CloudWatch Logs (awslogs Docker driver) |
| High availability | Application Load Balancer + Auto Scaling Group across 2 AZs |
| Infra as Code | Terraform (VPC, RDS, ALB/ASG, Lambda, EventBridge, EMR, IAM, CloudWatch) |
| Containers | Docker (multi-stage build) |
| CI | GitHub Actions (PR gate: build + test + lint), branch-protected `main` |
| CD | Jenkins (builds/deploys after CI passes) |

See [CLAUDE.md](./CLAUDE.md) for the full architecture, coding conventions, secrets-management rules, and an honest list of what's live-verified vs. code-complete-but-unconfirmed. [docs/backend](./docs/backend) has the original detailed setup/API guides plus a Lambda deployment guide and the manual-schema-change notes (no Flyway yet).

## Highlights
- JWT-based authentication with a custom filter chain and centralized exception handling; a separate shared-secret filter (`InternalApiKeyFilter`) guards the Lambda-facing internal API without touching user auth
- Deposit transactions publish an async credit-event notification via AWS SQS
- Monthly account statements generated as PDF (Apache PDFBox), triggerable in bulk via an EventBridge-scheduled Lambda
- A second Lambda reconciles every account's balance against its transaction history nightly, flagging drift into a self-provisioning table
- An EMR Serverless + Spark job reconciles at batch scale against a synthetic ~100k-transaction dataset (chosen over classic EMR specifically because it has zero cost while idle)
- Custom business metrics (`banking.transactions.count`/`amount`) exported to CloudWatch; app logs shipped there too and used live to diagnose an RDS-failover connection-pool issue
- ALB + 2-AZ Auto Scaling Group for the app tier; RDS Multi-AZ standby + optional read replica for DR — both toggleable via Terraform variables to control cost
- LLM-generated transaction descriptions (OpenAI), explicitly kept off the deposit/withdraw/transfer critical path so a third-party API hiccup can never affect money movement
- Two-stage CI/CD: GitHub Actions gates every PR (required status checks, branch protection), Jenkins handles build/deploy on `main`

## Running locally

### Backend
```bash
cd backend
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml # if not already present
./gradlew bootRun --args='--spring.profiles.active=local'
```
Requires a local PostgreSQL instance (see `docs/backend/ENVIRONMENT_CONFIG_GUIDE.md`).

### Frontend
```bash
cd frontend
npm ci
npm run dev
```

### Infrastructure (optional, requires an AWS account — real money if you `apply`)
```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars   # fill in real values, never commit this file
terraform init
terraform plan
```
`enable_multi_az`, `enable_read_replica`, and `asg_desired_capacity` default to the cheapest safe values — only raise them when you actually need to demonstrate HA/DR, and `terraform destroy` (or scale back down) right after. See `docs/backend/LAMBDA_DEPLOY_GUIDE.md` for the one-time image build/push step the `daily-reconciliation` Lambda needs before its first `apply`.

## Repository layout
```
banking-system/
├── backend/    # Spring Boot REST API
├── frontend/   # React + TypeScript SPA
├── infra/
│   ├── terraform/  # VPC, RDS, ALB/ASG, Lambda, EventBridge, EMR, IAM
│   ├── lambda/     # daily_reconciliation (container image), monthly_report (zip)
│   ├── spark/      # EMR Serverless PySpark job + synthetic data generator
│   └── jenkins/    # Jenkins' own Docker image
├── docs/       # Detailed setup/API/deployment guides
└── .github/    # CI workflow
```
