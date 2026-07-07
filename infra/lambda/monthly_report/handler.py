"""
Monthly report trigger Lambda.

Runs inside the VPC and POSTs to the Spring Boot app's internal batch
endpoint over plain HTTP. The handler itself is agnostic to what
INTERNAL_API_BASE_URL points at — Terraform (see lambda.tf) currently sets
it to a specific app instance's private IP:8080, resolved at apply time via
a `data "aws_instances"` lookup, rather than the ALB's DNS name.

That's a live-verification finding worth flagging: calling the ALB's DNS
name from this Lambda consistently timed out at the full function timeout
(consistent with a security-group-style silent drop, not a slow response),
even with healthy targets and no concurrent ASG changes — root cause not
isolated. Calling an app instance directly works reliably and needs zero AWS
API access (an elbv2/ec2-describe-based dynamic lookup was tried instead and
rejected: those calls need the AWS control plane, unreachable from this
NAT-less private subnet). Tradeoff: the baked-in IP goes stale if the ASG
replaces its instances until the next `terraform apply` — acceptable at this
project's scale; a production setup would either pay for a NAT Gateway (to
support live target lookups) or use Cloud Map service discovery instead.

Uses only the standard library (urllib) so this function ships as a plain
zip, unlike the daily-reconciliation function which needs psycopg2's C
extension.

Env vars: INTERNAL_API_BASE_URL (e.g. http://10.0.1.23:8080), INTERNAL_API_KEY.
"""

import json
import os
import urllib.error
import urllib.request

TIMEOUT_SECONDS = 20


def lambda_handler(event, context):
    base_url = os.environ["INTERNAL_API_BASE_URL"].rstrip("/")
    api_key = os.environ["INTERNAL_API_KEY"]

    request = urllib.request.Request(
        url=f"{base_url}/api/internal/reports/monthly-batch",
        method="POST",
        headers={"X-Internal-Api-Key": api_key},
    )

    try:
        with urllib.request.urlopen(request, timeout=TIMEOUT_SECONDS) as response:
            body = json.loads(response.read().decode("utf-8"))
            print(json.dumps({"status": response.status, "body": body}))
            return {"statusCode": response.status, "body": body}
    except urllib.error.HTTPError as e:
        error_body = e.read().decode("utf-8")
        print(json.dumps({"status": e.code, "error": error_body}))
        return {"statusCode": e.code, "error": error_body}
