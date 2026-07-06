# 🚀 Banking 应用 Terraform 部署完整指南

## 📋 概述

本指南提供了使用 Terraform 在 AWS 上部署 Banking Spring Boot 应用的完整说明。

---

## 📁 项目文件结构

```
banking/
├── terraform/                          # Terraform 配置目录
│   ├── README.md                       # 详细部署指南
│   ├── ARCHITECTURE.md                 # 架构设计文档
│   ├── DEPLOYMENT_CHECKLIST.md         # 部署检查清单
│   ├── COST_ESTIMATION.md              # 成本估算文档
│   ├── deploy.sh                       # 自动化部署脚本
│   ├── .gitignore                      # Git 忽略文件
│   │
│   ├── main.tf                         # 主配置文件
│   ├── variables.tf                    # 变量定义
│   ├── outputs.tf                      # 输出定义
│   ├── vpc.tf                          # VPC 和网络配置
│   ├── security_groups.tf              # 安全组配置
│   ├── rds.tf                          # RDS 数据库配置
│   ├── ec2.tf                          # EC2 实例配置
│   ├── iam.tf                          # IAM 角色和策略
│   ├── user_data.sh                    # EC2 启动脚本
│   └── terraform.tfvars.example        # 变量示例文件
│
├── Dockerfile                          # Docker 镜像构建文件
├── database_init.sql                   # 数据库初始化脚本
└── src/                                # 应用源代码
```

---

## 🎯 快速开始（5 分钟）

### 前提条件

1. ✅ AWS 账户已创建
2. ✅ AWS CLI 已安装并配置
3. ✅ Terraform 已安装 (>= 1.0)
4. ✅ Docker 镜像已上传到 ECR

### 快速部署步骤

```bash
# 1. 进入 terraform 目录
cd terraform

# 2. 配置变量
cp terraform.tfvars.example terraform.tfvars
vim terraform.tfvars  # 编辑配置

# 3. 运行部署脚本
./deploy.sh

# 4. 选择选项 5（完整部署流程）
# 5. 确认部署
# 6. 等待 10-15 分钟完成
```

---

## 📚 详细文档索引

### 1. README.md - 完整部署指南
**内容：**
- 详细的部署流程（7 个步骤）
- 前置准备清单
- 变量配置说明
- 部署验证步骤
- 故障排查指南
- 日常运维指南
- 资源清理说明

**何时阅读：** 首次部署前必读

### 2. ARCHITECTURE.md - 架构设计文档
**内容：**
- 系统架构图
- 网络架构设计
- 安全架构说明
- 应用组件架构
- 数据库设计
- 监控和日志架构
- 高可用性设计
- 扩展性考虑

**何时阅读：** 了解系统架构和设计决策

### 3. DEPLOYMENT_CHECKLIST.md - 部署检查清单
**内容：**
- 部署前检查项（6 大类）
- 详细部署步骤
- 部署后验证清单
- 故障排查步骤
- 日常运维指南
- 最终检查清单

**何时使用：** 部署时逐项核对

### 4. COST_ESTIMATION.md - 成本估算文档
**内容：**
- 详细成本分析
- 不同场景成本对比
- 成本优化建议
- 节省方案说明
- 成本监控方法

**何时阅读：** 规划预算和成本优化

---

## 🔑 核心配置文件说明

### main.tf
```hcl
# 主配置文件
- Terraform 版本要求
- AWS Provider 配置
- 数据源定义
- 本地变量
```

### variables.tf
```hcl
# 变量定义（包含默认值）
- 通用变量（区域、项目名等）
- 网络变量（VPC、子网）
- EC2 变量（实例类型、存储）
- RDS 变量（数据库配置）
- 应用变量（端口、JWT）
```

### outputs.tf
```hcl
# 输出定义
- 网络信息（VPC、子网 ID）
- 实例信息（EC2、RDS）
- 连接信息（URL、命令）
- 下一步操作提示
```

### vpc.tf
```hcl
# VPC 和网络资源
- VPC 创建
- 公有/私有子网
- Internet Gateway
- 路由表配置
- VPC Flow Logs
```

### security_groups.tf
```hcl
# 安全组规则
- EC2 安全组（HTTP、SSH）
- RDS 安全组（PostgreSQL）
- 入站/出站规则
```

### rds.tf
```hcl
# RDS 数据库配置
- DB 子网组
- DB 参数组（性能优化）
- RDS 实例
- CloudWatch 告警
```

### ec2.tf
```hcl
# EC2 实例配置
- EC2 实例定义
- 启动脚本配置
- CloudWatch 日志组
- CloudWatch 告警
```

### iam.tf
```hcl
# IAM 角色和策略
- EC2 实例角色
- ECR 访问策略
- SSM 和 CloudWatch 权限
- 实例配置文件
```

### user_data.sh
```bash
# EC2 启动脚本
- 系统更新
- Docker 安装
- ECR 认证
- 镜像拉取
- 容器启动
- 管理脚本创建
```

---

## 🛠️ 部署脚本使用

### deploy.sh - 自动化部署脚本

**功能：**
- 检查必需工具
- 验证 AWS 凭证
- 配置文件检查
- 交互式部署流程
- 彩色输出和错误处理

**使用方法：**
```bash
./deploy.sh

选项：
1) 初始化 Terraform (init)
2) 验证配置 (validate)
3) 查看部署计划 (plan)
4) 执行部署 (apply)
5) 完整部署流程 (推荐)
6) 销毁所有资源 (destroy)
7) 查看输出信息 (output)
8) 退出
```

---

## 📝 关键配置项说明

### terraform.tfvars 必须修改的变量

```hcl
# 1. ECR 镜像 URI（必须）
ecr_image_uri = "123456789012.dkr.ecr.ap-southeast-2.amazonaws.com/banking-app:latest"

# 2. 数据库密码（必须 - 使用强密码）
db_password = "YourStrongPassword123!"

# 3. JWT 密钥（建议修改）
jwt_secret = "your-generated-secret-key"

# 4. SSH 密钥对（如需 SSH 访问）
key_name = "your-key-name"

# 5. IP 访问限制（生产环境必须）
allowed_cidr_blocks = ["your.ip.address/32"]
```

### 环境特定配置

**开发环境：**
```hcl
environment  = "dev"
instance_type = "t3.small"
db_instance_class = "db.t3.micro"
db_multi_az = false
db_backup_retention_period = 3
```

**生产环境：**
```hcl
environment  = "production"
instance_type = "t3.medium"
db_instance_class = "db.t3.small"
db_multi_az = true
db_backup_retention_period = 7
db_deletion_protection = true
```

---

## 🎬 完整部署流程

### Phase 1: 准备阶段（15 分钟）

```bash
# 1. 安装工具
brew install terraform awscli

# 2. 配置 AWS 凭证
aws configure

# 3. 验证凭证
aws sts get-caller-identity

# 4. 构建并推送 Docker 镜像
cd ..  # 回到项目根目录
docker build -t banking-app:latest .
aws ecr get-login-password --region ap-southeast-2 | \
  docker login --username AWS --password-stdin <account-id>.dkr.ecr.ap-southeast-2.amazonaws.com
docker tag banking-app:latest <account-id>.dkr.ecr.ap-southeast-2.amazonaws.com/banking-app:latest
docker push <account-id>.dkr.ecr.ap-southeast-2.amazonaws.com/banking-app:latest
```

### Phase 2: 配置阶段（5 分钟）

```bash
# 5. 进入 terraform 目录
cd terraform

# 6. 配置变量
cp terraform.tfvars.example terraform.tfvars
vim terraform.tfvars

# 必须修改：
# - ecr_image_uri
# - db_password
# - jwt_secret
# 可选修改：
# - key_name (SSH 访问)
# - allowed_cidr_blocks (IP 限制)
```

### Phase 3: 部署阶段（10-15 分钟）

```bash
# 方法 1: 使用自动化脚本（推荐）
./deploy.sh
# 选择选项 5（完整部署流程）
# 确认部署（输入 'yes'）

# 方法 2: 手动执行
terraform init
terraform validate
terraform plan
terraform apply  # 输入 'yes' 确认
```

### Phase 4: 验证阶段（5 分钟）

```bash
# 7. 查看输出信息
terraform output

# 8. 获取关键信息
EC2_IP=$(terraform output -raw ec2_public_ip)
RDS_ENDPOINT=$(terraform output -raw rds_endpoint)
APP_URL=$(terraform output -raw application_url)

# 9. 等待应用启动（2-3 分钟）
sleep 180

# 10. 测试健康检查
curl http://$EC2_IP:8080/api/auth/health

# 预期响应：{"status":"UP"}
```

### Phase 5: 初始化阶段（5 分钟）

```bash
# 11. SSH 到 EC2 实例（如果配置了密钥）
ssh -i ~/.ssh/your-key.pem ec2-user@$EC2_IP

# 12. 查看应用状态
sudo docker ps
sudo docker logs banking-app

# 13. 初始化数据库
# 方法 1: 从 EC2 实例连接
psql -h $RDS_ENDPOINT -U postgres214 -d banking_db
# 然后粘贴 database_init.sql 内容

# 方法 2: 使用 pgAdmin（推荐）
# 从本地通过 pgAdmin 连接并执行 database_init.sql
```

### Phase 6: 测试阶段（10 分钟）

```bash
# 14. 使用 curl 测试 API
BASE_URL="http://$EC2_IP:8080"

# 健康检查
curl $BASE_URL/api/auth/health

# 用户注册
curl -X POST $BASE_URL/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test123!",
    "email": "test@example.com"
  }'

# 用户登录
curl -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test123!"
  }'

# 15. 使用 Postman 完整测试
# 导入 Banking_API_JWT.postman_collection.json
# 设置 base_url 变量
# 运行测试集合
```

---

## 🔍 常见问题解答

### Q1: 部署需要多长时间？
**A:** 首次部署约 10-15 分钟，其中 RDS 创建占用大部分时间。

### Q2: 月度成本是多少？
**A:** 基础配置约 $63/月。详见 `COST_ESTIMATION.md`。

### Q3: 如何更新应用？
**A:** 推送新镜像到 ECR，然后在 EC2 上运行：
```bash
ssh -i ~/.ssh/your-key.pem ec2-user@$EC2_IP
sudo update-banking-app.sh
```

### Q4: 如何备份数据库？
**A:** RDS 自动备份已启用（7 天保留）。手动备份：
```bash
aws rds create-db-snapshot \
  --db-instance-identifier banking-app-db-production \
  --db-snapshot-identifier banking-db-snapshot-$(date +%Y%m%d)
```

### Q5: 如何扩容？
**A:** 修改 `terraform.tfvars` 中的实例类型，然后运行：
```bash
terraform apply
```

### Q6: 生产环境有什么建议？
**A:** 
- 启用 `db_multi_az = true`
- 启用 `db_deletion_protection = true`
- 设置 `db_skip_final_snapshot = false`
- 限制 `allowed_cidr_blocks` 到特定 IP
- 使用 Reserved Instances 节省成本

### Q7: 如何清理所有资源？
**A:** 
```bash
terraform destroy  # 输入 'yes' 确认
```
**警告：** 这将删除所有资源，包括数据库数据！

### Q8: 遇到错误怎么办？
**A:** 参考 `DEPLOYMENT_CHECKLIST.md` 中的故障排查部分，或查看：
- CloudWatch 日志
- EC2 实例日志：`/var/log/user-data.log`
- Docker 容器日志：`sudo docker logs banking-app`

---

## 📊 部署后检查清单

### ✅ 基础设施验证
- [ ] VPC 已创建
- [ ] 子网已创建（2 公有 + 2 私有）
- [ ] Internet Gateway 已附加
- [ ] 安全组规则正确

### ✅ 计算资源验证
- [ ] EC2 实例运行中
- [ ] 公网 IP 已分配
- [ ] IAM 角色已附加
- [ ] Docker 容器运行中

### ✅ 数据库验证
- [ ] RDS 实例可用
- [ ] 端点可访问
- [ ] 自动备份已启用
- [ ] 参数组已应用

### ✅ 应用验证
- [ ] 健康检查通过
- [ ] API 可访问
- [ ] 数据库连接成功
- [ ] JWT 认证工作正常

### ✅ 监控验证
- [ ] CloudWatch 日志组已创建
- [ ] 日志正在写入
- [ ] 告警已配置
- [ ] 指标正在收集

---

## 🎯 下一步操作

### 1. 立即操作
- [ ] 保存部署输出信息
- [ ] 测试所有 API 端点
- [ ] 设置监控告警通知
- [ ] 创建数据库手动快照

### 2. 一周内
- [ ] 审查 CloudWatch 指标
- [ ] 调整资源大小（如需要）
- [ ] 实施成本优化
- [ ] 文档化运维流程

### 3. 一个月内
- [ ] 评估 Reserved Instances
- [ ] 考虑高可用架构
- [ ] 实施自动化备份验证
- [ ] 进行灾难恢复演练

---

## 📞 获取帮助

### 文档资源
- **详细部署指南：** `terraform/README.md`
- **架构设计：** `terraform/ARCHITECTURE.md`
- **部署检查清单：** `terraform/DEPLOYMENT_CHECKLIST.md`
- **成本估算：** `terraform/COST_ESTIMATION.md`

### 在线资源
- [Terraform AWS 文档](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [AWS RDS 文档](https://docs.aws.amazon.com/rds/)
- [AWS EC2 文档](https://docs.aws.amazon.com/ec2/)
- [Spring Boot 文档](https://spring.io/projects/spring-boot)

### 项目文档
- `README_CN.md` - 项目总体说明
- `QUICK_START.md` - 快速开始指南
- `POSTMAN_API_GUIDE.md` - API 测试指南
- `JWT_AUTH_GUIDE.md` - JWT 认证说明

---

## 🎉 总结

您现在拥有：

1. ✅ **完整的 Terraform 配置** - 9 个配置文件，覆盖所有 AWS 资源
2. ✅ **详细的文档** - 4 个主要文档，超过 2000 行说明
3. ✅ **自动化脚本** - 一键部署和管理
4. ✅ **生产就绪** - 包含安全、监控、备份等最佳实践
5. ✅ **成本优化** - 详细的成本分析和优化建议

**部署时间：** ~30 分钟（首次）
**月度成本：** ~$63（基础配置）
**可用性：** ~99.5%（单 AZ）或 ~99.95%（Multi-AZ）

---

**祝您部署顺利！** 🚀

如有问题，请参考详细文档或检查 AWS 控制台。

**文档版本：** 1.0
**创建日期：** 2025-12-16
**维护者：** DevOps Team
