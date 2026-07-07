# 项目说明

## 项目概述
Banking System 是一个网银系统的全栈实现（Spring Boot REST API + React 前端 + AWS 基础设施），作为投递澳洲软件工程岗位的作品集项目，按真实企业实习/新人入职的开发流程运作（feature 分支 → PR → CI 门禁 → code review → 部署）。

## 技术栈

### 后端 `backend/`
- **框架**: Spring Boot 3.2.1，Java 21
- **构建**: Gradle 8.5+，Kotlin DSL（`build.gradle.kts`，不要改成 Groovy DSL）
- **认证**: JWT（`io.jsonwebtoken:jjwt`），`JwtAuthenticationFilter` + `JwtAuthenticationEntryPoint`；`/api/internal/**` 走独立的 `InternalApiKeyFilter`（共享密钥，供 Lambda 调用，不走用户 JWT）
- **数据库**: PostgreSQL（本地 Docker/本机安装，生产为 AWS RDS Multi-AZ + 可选只读副本），Spring Data JPA + Hibernate
- **可观测性**: Spring Boot Actuator + Micrometer CloudWatch2 registry（自定义业务指标 `banking.transactions.count`/`amount`），`/actuator/health` 兼作 ALB 健康检查目标
- **异步通知**: AWS SQS（`software.amazon.awssdk:sqs`），账户入账事件发布到 `account-credit-events` 队列
- **报表**: Apache PDFBox 生成月度对账单 PDF；`MonthlyStatementService.generateMonthlyStatementsForAllAccounts` 供月报 Lambda 批量触发
- **LLM 交易描述推荐**: `OpenAiTransactionDescriptionService` 调用 OpenAI Chat Completions，未启用/无 key/调用失败时走规则兜底，不阻塞交易关键路径
- **测试**: JUnit 5 + Mockito + Spring Security Test + H2（测试专用内存库）+ Spring `MockRestServiceServer`（测 OpenAI 调用）

### 前端 `frontend/`
- **框架**: React 19 + TypeScript（严格模式）
- **构建**: Vite 7
- **样式**: Tailwind CSS 4
- **状态/路由**: React Router（`/login` `/register` `/dashboard` + `ProtectedRoute`）+ Redux Toolkit（`authSlice`，启动时从 `localStorage` 水合）
- **包管理器**: npm（不要用 yarn/pnpm，仓库里只有 `package-lock.json`）

### 基础设施 `infra/`
- **IaC**: Terraform（`infra/terraform`，拆成 `main.tf`/`alb.tf`/`lambda.tf`/`emr.tf` 几个文件），管理 VPC、子网、RDS（Multi-AZ + 可选只读副本）、ALB + ASG、SQS、Lambda、EventBridge Scheduler、EMR Serverless、IAM、CloudWatch 日志组
- **容器化**: Docker（`backend/Dockerfile-springboot` 多阶段构建，`infra/jenkins/Dockerfile-jenkins`，`infra/lambda/daily_reconciliation/Dockerfile`）
- **定时任务**: EventBridge Scheduler 触发两个 Lambda（`infra/lambda/daily_reconciliation` 直连 RDS 核对余额，`infra/lambda/monthly_report` 调用内部批量报表端点）
- **大数据对账**: EMR Serverless + PySpark（`infra/spark/`），合成数据集生成脚本 + 对账 job，架构已验证（本地跑通 4 个 pytest + 5000 账户/10 万笔合成数据端到端测试），实际部署被 AWS 账号级 `SubscriptionRequiredException` 挡住（见下方「已知问题」）
- **CI**: GitHub Actions（`.github/workflows/ci.yml`），PR 门禁：backend 跑 `./gradlew test`，frontend 跑 `npm run build` + lint
- **CD**: Jenkins（根目录 `Jenkinsfile`），负责 CI 通过之后的构建镜像/部署，运行在自建 Jenkins（`docker-compose.yml` 里的 `jenkins` 服务）

## 编码规范

### 后端（Java / Kotlin DSL）
- 包结构固定为 `controller` / `service` / `repository` / `entity` / `dto` / `config` / `exception` / `util`，新代码放进对应的包，不要新建平行的分层
- Controller 只做参数校验和调用 Service，业务逻辑一律放 Service 层
- 类名 PascalCase，方法/变量 camelCase，与现有代码风格一致（Lombok 用于减少样板代码，继续沿用）
- 用 Gradle Wrapper（`./gradlew`），不要求本机全局安装的 Gradle 版本
- 会调外部服务（OpenAI、未来任何第三方 API）的功能一律要有规则兜底，绝不能让第三方故障影响资金操作等关键路径

### 前端（TypeScript）
- 禁止 `any` 类型
- 组件用 PascalCase（`Login.tsx`），普通函数/hook 用 camelCase
- 用 npm（不要用 yarn/pnpm）
- 登录态统一走 Redux（`useAppSelector`/`useAppDispatch`），不要再散落 `localStorage.getItem` 调用；页面跳转统一用 `useNavigate`，不要传 callback prop

### Terraform
- 所有 `sensitive = true` 的变量**禁止带 `default`**，必须通过 `terraform.tfvars`（已 gitignore）或 `TF_VAR_*` 环境变量提供
- 新资源加 `Name` tag，沿用现有命名前缀（`bank-*`）
- Security Group 的 `description` 字段**只能用 ASCII 字符**（em dash/中文都会导致 `CreateSecurityGroup` 400 报错，真实踩过这个坑）
- `aws_db_instance` 加 `apply_immediately = true`，否则 `backup_retention_period` 之类的修改会排到下一个维护窗口才生效，而不是立刻生效
- ASG 用 `launch_template { version = "$Latest" }` 时，`instance_refresh.triggers` 必须显式写 `["launch_template"]`——`terraform validate` 会警告这是多余的，**不要相信这个警告**，这是本项目真实验证过会导致镜像更新后实例不滚动升级的坑
- 按小时计费的资源（ALB、RDS Multi-AZ/只读副本、EMR 经典集群）默认关闭/最小规模，只在验证时临时打开，验证完立刻关掉或 `terraform destroy`

## 密钥与配置管理（强制规则）

> 本项目曾经把真实的 JWT secret 和 RDS 密码作为 `${VAR:default}` 的默认值提交进 `application.yml` / `terraform/variables.tf`，并且已经 push 到 GitHub。密钥已轮换，历史已清洗，但这个规则必须严格执行，不能再发生第二次。

- 任何 `application*.yml` 里的密钥/密码配置项，**禁止**写成 `${ENV_VAR:真实值}` 的形式；只能是 `${ENV_VAR}`（缺失时启动失败）或 `${ENV_VAR:}`（默认空字符串，业务代码自行判断）
- 任何 `.tf` 文件里标了 `sensitive = true` 的变量**禁止**有 `default`
- 本地开发密钥放 `backend/src/main/resources/application-local.yml`（已 gitignore），或项目根 `.env`（已 gitignore）
- 生产密钥：先用环境变量（Jenkins Credentials / GitHub Actions Secrets）注入；未来可以切到 AWS Secrets Manager / SSM Parameter Store
- `terraform.tfvars` 永远不进仓库，改 `terraform/terraform.tfvars.example` 时只放占位符
- AWS access key / RDS 密码等如果怀疑已经泄露，第一步永远是先轮换凭证，再讨论要不要清洗 git 历史

## 禁止行为
- ❌ 不要安装新依赖除非明确要求（Gradle 和 npm 两边都是）
- ❌ 不要修改 `.env` / `terraform.tfvars` 文件
- ❌ 不要删除测试文件
- ❌ 不要把真实密钥/密码/access key 写进任何会被 git 追踪的文件（见上方"密钥与配置管理"）
- ❌ 不要对已推送的 commit 历史做 `force push`，除非和我明确讨论过具体原因和影响范围
- ❌ 不要直接在 `main` 分支上提交，走 feature 分支 + PR
- ❌ 不要在没有明确讨论过预算/时长的情况下对按小时计费的 AWS 资源（ALB、RDS Multi-AZ/副本、EMR 经典集群、NAT Gateway）做 `terraform apply`

## 目录结构
```
banking-system/
├── backend/              # Spring Boot REST API
│   ├── src/main/java/com/banking/{controller,service,repository,entity,dto,config,exception,util}
│   ├── src/main/resources/application*.yml
│   ├── Dockerfile-springboot
│   └── build.gradle.kts
├── frontend/              # React + TS 前端
│   └── src/{components,api.ts,types.ts,store,routes}
├── infra/
│   ├── terraform/         # VPC / RDS / ALB+ASG / Lambda / EventBridge / EMR / IAM
│   ├── lambda/            # daily_reconciliation（容器镜像）、monthly_report（zip）
│   ├── spark/             # EMR Serverless 用的 PySpark 对账 job + 合成数据生成
│   └── jenkins/           # Jenkins 自身镜像
├── docs/
│   └── backend/           # 技术文档（JWT 指南、Lambda 部署指南、生产环境手动建表清单等）
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

## 当前实现状态

已完成并且**通过真实 AWS 部署做过端到端验证**（不只是 `terraform validate`/单元测试）：
- [x] Spring Boot REST API（认证、账户、交易）+ JWT 登录/鉴权
- [x] PostgreSQL（本地 + RDS）+ JPA
- [x] Docker 多阶段构建 + Jenkins CI
- [x] Terraform：VPC / RDS / SQS / IAM 基础资源
- [x] SQS 通知（存款触发 credit event）
- [x] 月度对账单 PDF 导出（PDFBox）
- [x] GitHub Actions PR 门禁 + branch protection（4 个 PR 全部走完整流程验证过）
- [x] CloudWatch Logs：实测收到应用日志，并用它诊断出了下面「真实踩过的坑」里的好几个问题
- [x] ALB + ASG 多可用区高可用：2 台实例都跑通、健康检查通过
- [x] RDS Multi-AZ + 只读副本：都成功创建过
- [x] EventBridge + Lambda：`daily-reconciliation`（直连 RDS 核对余额）和 `monthly-report`（调用内部报表接口）都手动 invoke 验证成功
- [x] LLM 交易描述推荐（OpenAI，代码+单元测试完成；未配置真实 key，走规则兜底）
- [x] 前端接入 React Router + Redux Toolkit

代码/架构已完成，但**未能在本次验证窗口内确认完全生效**（诚实记录，不夸大）：
- [ ] CloudWatch 自定义 metrics：Micrometer CloudWatch2 registry 配置正确（属性前缀踩坑已修好）、IAM 权限已验证，但 `BankingSystem` 命名空间下始终没有观察到指标数据，根因未查明，需要后续单独排查（可能需要在有 ADMIN 权限账号下用 `/actuator/metrics` 直接检查计数器有没有被正确注册和递增）
- [ ] EMR Serverless：Terraform 配置正确、PySpark 对账逻辑本地已用 5000 账户/10 万笔合成数据端到端验证通过（19/19 注入的异常全部被正确检测），但实际 `terraform apply` 被这个 AWS 账号的 `SubscriptionRequiredException`（EMR Serverless 服务级激活）挡住——需要账号所有者登录 AWS 控制台访问一次 EMR Serverless 页面完成账号激活，之后应该能直接跑通

## 已知问题 / 真实踩过的坑（live 部署验证时发现，全部已修复并有对应 commit）
这些是靠实际 `terraform apply` 到 AWS 才暴露出来的问题，`terraform validate`、本地单测都测不出来，留档是因为很有面试展示价值：
1. Security Group `description` 含 em dash（非 ASCII）—— `CreateSecurityGroup` 直接 400
2. RDS 只读副本要求源库已开自动备份（`backup_retention_period > 0`），且这个账号的免费层套餐把可选值限制在比默认建议更低的范围
3. `aws_db_instance` 的修改默认排到下次维护窗口才生效，不加 `apply_immediately = true` 的话看起来"改了"其实还没生效
4. Spring Boot 3.x 的 CloudWatch2 Micrometer 属性前缀是 `management.cloudwatch.metrics.export.*`，不是网上更常见的 `management.metrics.export.cloudwatch.*`
5. ASG 用 `version = "$Latest"` 时必须显式加 `instance_refresh.triggers = ["launch_template"]`，否则镜像更新后运行中的实例不会自动滚动更新——即使 `terraform validate` 警告这个配置"多余"
6. ALB 健康检查超时（5s）比 HikariCP 在 RDS 故障转移后偶尔需要的连接重新验证时间（8-10s）更短，导致健康实例被误判下线
7. ASG `health_check_grace_period`（60s）比 EC2 完整冷启动流程（装 Docker、拉 ECR 镜像、启动 Spring Boot）实际所需时间更短
8. 请求的 RDS `engine_version "17.5"` 实际被 AWS 建成了 `17.9`（映射到当前受支持的小版本），导致后续每次 apply 都想"降级"
9. `monthly-report` Lambda 直接调用 ALB DNS 名称会稳定超时（根因未查明，怀疑是安全组静默丢包类行为），改为由 Terraform 在 apply 时解析一个 ASG 实例的私有 IP 直接调用，绕开了整个问题（代价：实例被 ASG 替换后这个 IP 要等下次 apply 才会更新）
