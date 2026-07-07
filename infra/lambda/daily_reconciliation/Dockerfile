# Container image packaging (not a zip) because psycopg2 needs its C extension
# built against the Lambda runtime's glibc — the AWS-managed Python base image
# guarantees that, whereas a plain zip built on a dev machine would not.
FROM public.ecr.aws/lambda/python:3.12

COPY requirements.txt ${LAMBDA_TASK_ROOT}/
RUN pip install --no-cache-dir -r requirements.txt

COPY handler.py ${LAMBDA_TASK_ROOT}/

CMD ["handler.lambda_handler"]
