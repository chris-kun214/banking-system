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

# 拉取并运行 Spring Boot 容器
sudo docker run -d \
  --restart always \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="${db_url}" \
  -e SPRING_DATASOURCE_USERNAME="${db_username}" \
  -e SPRING_DATASOURCE_PASSWORD="${db_password}" \
  --name springboot-app \
  "${ecr_repo_uri}"