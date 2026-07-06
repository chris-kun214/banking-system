# Banking System

A full-stack online banking system built as a portfolio project — Spring Boot REST API, React frontend, and AWS-based infrastructure, developed following a real-company workflow (feature branches, PR-gated CI, Jenkins deployment).

## Architecture

```
┌─────────────┐      JWT auth       ┌──────────────────┐      ┌─────────────────┐
│  React SPA  │ ──────────────────► │  Spring Boot API  │ ──► │  PostgreSQL RDS  │
│  (frontend) │ ◄────────────────── │    (backend)       │      └─────────────────┘
└─────────────┘                     │                     │
                                     │  ├─ SQS (credit events)
                                     │  └─ PDFBox (monthly statements)
                                     └──────────┬──────────┘
                                                │
                                     ┌──────────▼──────────┐
                                     │   Terraform / AWS    │
                                     │  VPC · RDS · EC2 ·    │
                                     │  SQS · IAM            │
                                     └──────────────────────┘
```

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.2.1, Java 21, Gradle (Kotlin DSL), Spring Data JPA, Spring Security + JWT |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS |
| Database | PostgreSQL (AWS RDS in production) |
| Messaging | AWS SQS (async account-credit notifications) |
| Infra as Code | Terraform (VPC, RDS, EC2, SQS, IAM) |
| Containers | Docker (multi-stage build) |
| CI | GitHub Actions (PR gate: build + test + lint) |
| CD | Jenkins (builds/deploys after CI passes) |

See [CLAUDE.md](./CLAUDE.md) for the full architecture, coding conventions, and secrets-management rules, and [docs/backend](./docs/backend) for the original detailed setup/API guides.

## Highlights
- JWT-based authentication with a custom filter chain and centralized exception handling
- Deposit transactions publish an async credit-event notification via AWS SQS
- Monthly account statements generated as PDF (Apache PDFBox)
- Infrastructure fully defined in Terraform — VPC, RDS, EC2, IAM roles, SQS
- Two-stage CI/CD: GitHub Actions gates every PR, Jenkins handles build/deploy on `main`

## Roadmap
Actively being extended to add: EventBridge + Lambda scheduled reconciliation/reporting, CloudWatch metrics/logs, an Application Load Balancer for high availability, multi-AZ RDS failover, EMR + Spark batch reconciliation, and LLM-assisted transaction description suggestions. Full list in [CLAUDE.md](./CLAUDE.md#当前实现状态--roadmap).

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

### Infrastructure (optional, requires an AWS account)
```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars   # fill in real values, never commit this file
terraform init
terraform plan
```

## Repository layout
```
banking-system/
├── backend/    # Spring Boot REST API
├── frontend/   # React + TypeScript SPA
├── infra/      # Terraform + Jenkins Docker image
├── docs/       # Detailed setup/API guides
└── .github/    # CI workflow
```
