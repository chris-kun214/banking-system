# -----------------------------------
# EMR Serverless + Spark — large-scale batch reconciliation
#
# EMR Serverless over classic EMR-on-EC2: aws_emrserverless_application has
# NO cost while idle (classic EMR bills master+core instance-hours from the
# moment the cluster exists, whether or not a step has run). Billing here is
# per-second for vCPU/memory/storage only while a job is actually executing,
# and auto_stop_configuration releases any pre-initialized capacity after
# idle_timeout_minutes. That fits a "run once, screenshot, done" portfolio
# verification far better than a persistent cluster you have to remember to
# tear down.
#
# The real application only produces a modest number of transactions, not
# enough to make a "big data" story credible — infra/spark/generate_synthetic_data.py
# produces a synthetic, clearly-labelled dataset at a representative scale so
# the Spark job itself (the actual skill being demonstrated) has something
# realistic to chew on.
# -----------------------------------

resource "aws_s3_bucket" "emr_data" {
  bucket = "bank-emr-reconciliation-${data.aws_caller_identity.current.account_id}"
}

resource "aws_s3_bucket_lifecycle_configuration" "emr_data" {
  bucket = aws_s3_bucket.emr_data.id

  rule {
    id     = "expire-old-runs"
    status = "Enabled"

    filter {}

    expiration {
      days = 30
    }
  }
}

resource "aws_iam_role" "emr_job_execution_role" {
  name = "bank-emr-serverless-job-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action    = "sts:AssumeRole"
        Effect    = "Allow"
        Principal = { Service = "emr-serverless.amazonaws.com" }
      }
    ]
  })
}

resource "aws_iam_role_policy" "emr_s3_access" {
  name = "bank-emr-s3-access-policy"
  role = aws_iam_role.emr_job_execution_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["s3:GetObject", "s3:PutObject", "s3:ListBucket"]
        Resource = [aws_s3_bucket.emr_data.arn, "${aws_s3_bucket.emr_data.arn}/*"]
      }
    ]
  })
}

resource "aws_emrserverless_application" "reconciliation" {
  name          = "bank-reconciliation"
  release_label = "emr-7.1.0"
  type          = "SPARK"

  maximum_capacity {
    cpu    = "4 vCPU"
    memory = "8 GB"
  }

  auto_start_configuration {
    enabled = true
  }

  auto_stop_configuration {
    enabled              = true
    idle_timeout_minutes = 15
  }

  tags = {
    Name = "bank-reconciliation"
  }
}
