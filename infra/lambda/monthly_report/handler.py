"""
Monthly report trigger Lambda.

Runs inside the VPC and calls the Spring Boot app's internal batch endpoint
over plain HTTP (VPC-internal traffic to the ALB/EC2 security group, no NAT
needed) rather than reimplementing PDF generation here. Uses only the
standard library (urllib) so this function ships as a plain zip, unlike the
daily-reconciliation function which needs psycopg2's C extension.

Env vars: INTERNAL_API_BASE_URL (e.g. http://<alb-dns>), INTERNAL_API_KEY.
"""

import json
import os
import urllib.error
import urllib.request

TIMEOUT_SECONDS = 30


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
