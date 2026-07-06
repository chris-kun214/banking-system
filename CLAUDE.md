# 项目说明

## 项目概述
Banking System 是一个网银系统的全栈实现（Spring Boot REST API + React 前端 + AWS 基础设施），作为投递澳洲软件工程岗位的作品集项目，按真实企业实习/新人入职的开发流程运作（feature 分支 → PR → CI 门禁 → code review → 部署）。

## 技术栈

### 后端 `backend/`
- **框架**: Spring Boot 3.2.1，Java 21
- **构建**: Gradle 8.5+，Kotlin DSL（`build.gradle.kts`，不要改成 Groovy DSL）
- **认证**: JWT（`io.jsonwebtoken:jjwt`），`JwtAuthenticationFilter` + `JwtAuthenticationEntryPoint`
- **数据库**: PostgreSQL（本地 Docker/本机安装，生产为 AWS RDS），Spring Data JPA + Hibernate
- **异步通知**: AWS SQS（`software.amazon.awssdk:sqs`），账户入账事件发布到 `account-credit-events` 队列
- **报表**: Apache PDFBox 生成月度对账单 PDF
- **测试**: JUnit 5 + Mockito + Spring Security Test + H2（测试专用内存库）

### 前端 `frontend/`
- **框架**: React 19 + TypeScript（严格模式）
- **构建**: Vite 7
- **样式**: Tailwind CSS 4
- **状态/路由**: Redux Toolkit、React Router 已安装但尚未接入（目前是手写的页面状态机，见下方 Roadmap）
- **包管理器**: npm（不要用 yarn/pnpm，仓库里只有 `package-lock.json`）

### 基础设施 `infra/`
- **IaC**: Terraform（`infra/terraform`），管理 VPC、子网、RDS、EC2、SQS、IAM
- **容器化**: Docker（`backend/Dockerfile-springboot` 多阶段构建，`infra/jenkins/Dockerfile-jenkins`）
- **CI**: GitHub Actions（`.github/workflows/ci.yml`），PR 门禁：backend 跑 `./gradlew test`，frontend 跑 `npm run build` + lint
- **CD**: Jenkins（根目录 `Jenkinsfile`），负责 CI 通过之后的构建镜像/部署，运行在自建 Jenkins（`docker-compose.yml` 里的 `jenkins` 服务）
- **计划中**（见 Roadmap）: EventBridge + Lambda 定时对账/月报、CloudWatch metrics/logs、ALB 高可用、RDS 多可用区容灾、EMR + Spark 批量对账、LLM 交易描述推荐

## 编码规范

### 后端（Java / Kotlin DSL）
- 包结构固定为 `controller` / `service` / `repository` / `entity` / `dto` / `config` / `exception` / `util`，新代码放进对应的包，不要新建平行的分层
- Controller 只做参数校验和调用 Service，业务逻辑一律放 Service 层
- 类名 PascalCase，方法/变量 camelCase，与现有代码风格一致（Lombok 用于减少样板代码，继续沿用）
- 用 Gradle Wrapper（`./gradlew`），不要求本机全局安装的 Gradle 版本

### 前端（TypeScript）
- 禁止 `any` 类型
- 组件用 PascalCase（`Login.tsx`），普通函数/hook 用 camelCase
- 用 npm（不要用 yarn/pnpm）
- 新页面/新状态优先使用已安装的 React Router + Redux Toolkit，不要再扩展手写的页面状态机

### Terraform
- 所有 `sensitive = true` 的变量**禁止带 `default`**，必须通过 `terraform.tfvars`（已 gitignore）或 `TF_VAR_*` 环境变量提供
- 新资源加 `Name` tag，沿用现有命名前缀（`bank-*`）

## 密钥与配置管理（强制规则）

> 本项目曾经把真实的 JWT secret 和 RDS 密码作为 `${VAR:default}` 的默认值提交进 `application.yml` / `terraform/variables.tf`，并且已经 push 到 GitHub。密钥已轮换，历史已清洗，但这个规则必须严格执行，不能再发生第二次。

- 任何 `application*.yml` 里的密钥/密码配置项，**禁止**写成 `${ENV_VAR:真实值}` 的形式；只能是 `${ENV_VAR}`（缺失时启动失败）或 `${ENV_VAR:}`（默认空字符串，业务代码自行判断）
- 任何 `.tf` 文件里标了 `sensitive = true` 的变量**禁止**有 `default`
- 本地开发密钥放 `backend/src/main/resources/application-local.yml`（已 gitignore），或项目根 `.env`（已 gitignore）
- 生产密钥：先用环境变量（Jenkins Credentials / GitHub Actions Secrets）注入；Roadmap 里计划切到 AWS Secrets Manager / SSM Parameter Store
- `terraform.tfvars` 永远不进仓库，改 `terraform/terraform.tfvars.example` 时只放占位符
- AWS access key / RDS 密码等如果怀疑已经泄露，第一步永远是先轮换凭证，再讨论要不要清洗 git 历史

## 禁止行为
- ❌ 不要安装新依赖除非明确要求（Gradle 和 npm 两边都是）
- ❌ 不要修改 `.env` / `terraform.tfvars` 文件
- ❌ 不要删除测试文件
- ❌ 不要把真实密钥/密码/access key 写进任何会被 git 追踪的文件（见上方"密钥与配置管理"）
- ❌ 不要对已推送的 commit 历史做 `force push`，除非和我明确讨论过具体原因和影响范围
- ❌ 不要直接在 `main` 分支上提交，走 feature 分支 + PR

## 目录结构
```
banking-system/
├── backend/              # Spring Boot REST API
│   ├── src/main/java/com/banking/{controller,service,repository,entity,dto,config,exception,util}
│   ├── src/main/resources/application*.yml
│   ├── Dockerfile-springboot
│   └── build.gradle.kts
├── frontend/              # React + TS 前端
│   └── src/{components,api.ts,types.ts}
├── infra/
│   ├── terraform/         # VPC / RDS / EC2 / SQS / IAM
│   └── jenkins/           # Jenkins 自身镜像
├── docs/
│   └── backend/           # 历史技术文档（JWT 指南、Terraform 部署指南等）
├── .github/workflows/     # GitHub Actions（PR 门禁）
├── Jenkinsfile            # 部署流水线
├── docker-compose.yml     # 本地起 jenkins + springboot-app
└── CLAUDE.md
```

## Git / 协作流程
1. 从 `main` 切 `feature/xxx` 或 `fix/xxx` 分支
2. 提交时信息用 `feat:` / `fix:` / `refactor:` / `docs:` / `chore:` 前缀
3. 开 PR，GitHub Actions（backend test + frontend build/lint）必须全绿
4. Code review（自审或请求 review）通过后 squash merge 进 `main`
5. `main` 上的变更由 Jenkins 负责后续构建镜像、（可选）部署到 EC2

## 当前实现状态 / Roadmap
已完成：
- [x] Spring Boot REST API（认证、账户、交易）
- [x] JWT 登录/鉴权
- [x] PostgreSQL（本地 + RDS）+ JPA
- [x] Docker 多阶段构建
- [x] Terraform：VPC / RDS / EC2 / SQS / IAM 基础资源
- [x] Jenkins CI（build + test + 可选发布 jar）
- [x] SQS 通知（存款触发 credit event）
- [x] 月度对账单 PDF 导出（PDFBox）
- [x] GitHub Actions PR 门禁

待实现（后续任务）：
- [ ] AWS EventBridge + Lambda：每日对账、月度报告定时触发
- [ ] CloudWatch metrics（服务监控）+ CloudWatch Logs（在线 debug）
- [ ] ALB：多可用区高可用
- [ ] RDS 多可用区 / 只读副本：数据库高可用与容灾
- [ ] EMR + Apache Spark：大数据对账
- [ ] LLM 交易描述推荐（调用 Claude/其他模型给交易生成可读描述）
- [ ] 前端接入 React Router + Redux Toolkit（目前是手写页面状态机）
