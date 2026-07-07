#!/bin/bash
set -e

sudo dnf update -y
sudo dnf install -y docker

sudo systemctl start docker
sudo systemctl enable docker

sudo usermod -aG docker ec2-user

# AWS ECR 登录 (使用 IAM 角色凭证)
sudo aws configure set default.region ${aws_region}

# 提取 ECR registry URL（去除仓库名和标签）
ECR_FULL_URI="${ecr_repo_uri}"
ECR_REGISTRY=$(echo "$ECR_FULL_URI" | cut -d'/' -f1)

# 登录到 ECR
ECR_PASSWORD=$(aws ecr get-login-password --region ${aws_region})
echo "$ECR_PASSWORD" | sudo docker login --username AWS --password-stdin "$ECR_REGISTRY"

# 拉取最新镜像并替换旧容器
sudo docker pull "${ecr_repo_uri}"
sudo docker rm -f springboot-app || true

# 拉取并运行 Spring Boot 容器
# --log-driver=awslogs：容器 stdout/stderr 直接发到 CloudWatch Logs（日志组已由 Terraform 预建，
# 这里不加 awslogs-create-group，实例角色也没有 logs:CreateLogGroup 权限，权限最小化）
sudo docker run -d \
  --restart always \
  -p 8080:9090 \
  --log-driver=awslogs \
  --log-opt awslogs-region="${aws_region}" \
  --log-opt awslogs-group="${cloudwatch_log_group}" \
  --log-opt awslogs-stream="app" \
  -e SPRING_PROFILES_ACTIVE="prod" \
  -e SERVER_PORT="9090" \
  -e SPRING_DATASOURCE_URL="${db_url}" \
  -e SPRING_DATASOURCE_USERNAME="${db_username}" \
  -e SPRING_DATASOURCE_PASSWORD="${db_password}" \
  -e AWS_REGION="${aws_region}" \
  -e AWS_SQS_ENABLED="true" \
  -e AWS_SQS_CREDIT_QUEUE_URL="${sqs_queue_url}" \
  -e JWT_SECRET="${jwt_secret}" \
  -e JWT_EXPIRATION="${jwt_expiration}" \
  -e HIBERNATE_DDL_AUTO="${hibernate_ddl_auto}" \
  -e MONTHLY_REPORT_OUTPUT_DIR="/tmp/banking/reports/monthly" \
  -e INTERNAL_API_KEY="${internal_api_key}" \
  -e CLOUDWATCH_METRICS_ENABLED="true" \
  --name springboot-app \
  "${ecr_repo_uri}"