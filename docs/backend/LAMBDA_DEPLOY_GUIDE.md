# Lambda 部署说明

`daily-reconciliation` 用容器镜像打包（因为 `psycopg2` 需要原生扩展），必须在 `terraform apply` **之前**手动构建并推送到 ECR，否则 `aws_lambda_function.daily_reconciliation` 会因为镜像不存在而创建失败。

```bash
cd infra/terraform
terraform apply -target=aws_ecr_repository.daily_reconciliation   # 先建仓库

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text --profile work-account)
REGION=ap-southeast-2
REPO_URI="${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com/banking-system/daily-reconciliation"

aws ecr get-login-password --region $REGION --profile work-account \
  | docker login --username AWS --password-stdin "${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"

cd ../lambda/daily_reconciliation
docker build --platform linux/amd64 -t "${REPO_URI}:latest" .
docker push "${REPO_URI}:latest"

cd ../../terraform
terraform apply   # 现在镜像已存在，可以正常创建 Lambda
```

`monthly-report` 是纯标准库 zip 包，Terraform 的 `data.archive_file` 会在 `apply` 时自动打包，不需要手动步骤。

## 生产环境手动建表

`daily-reconciliation` Lambda 自己会执行 `CREATE TABLE IF NOT EXISTS reconciliation_discrepancy`，首次运行即可自动建表，不需要额外操作。
