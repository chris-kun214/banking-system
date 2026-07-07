"""
Daily reconciliation Lambda.

Runs entirely inside the VPC against RDS Postgres directly — no other AWS API
calls are made, so this function needs neither a NAT Gateway nor a VPC
endpoint (its CloudWatch Logs delivery goes over Lambda's managed logging
path, not the VPC route table).

Reconciliation invariant: every deposit/withdraw/transfer in TransactionService
sets both `account.balance` and the new transaction's `balance_after` to the
same value in the same DB transaction. So for any account with at least one
transaction, its current `balance` should always equal the `balance_after` of
its most recent transaction. A mismatch means the account row was mutated
out of band (a bug, a manual DB edit, or a partially-applied transaction) —
that's what this job flags. Accounts with zero transactions have nothing to
check against and are skipped.

Env vars: DB_HOST, DB_PORT (default 5432), DB_NAME, DB_USER, DB_PASSWORD.
"""

import decimal
import json
import os

import psycopg2

DISCREPANCY_EPSILON = decimal.Decimal("0.01")

CREATE_TABLE_SQL = """
CREATE TABLE IF NOT EXISTS reconciliation_discrepancy (
    id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(30) NOT NULL,
    recorded_balance NUMERIC(18,2) NOT NULL,
    last_transaction_balance NUMERIC(18,2) NOT NULL,
    difference NUMERIC(18,2) NOT NULL,
    detected_at TIMESTAMP NOT NULL DEFAULT now()
)
"""

SELECT_ACCOUNTS_SQL = """
SELECT a.account_number, a.balance, t.balance_after
FROM account a
LEFT JOIN LATERAL (
    SELECT balance_after
    FROM transaction
    WHERE account_id = a.id
    ORDER BY created_at DESC, id DESC
    LIMIT 1
) t ON true
"""

INSERT_DISCREPANCY_SQL = """
INSERT INTO reconciliation_discrepancy
    (account_number, recorded_balance, last_transaction_balance, difference)
VALUES (%s, %s, %s, %s)
"""


def _connect():
    return psycopg2.connect(
        host=os.environ["DB_HOST"],
        port=os.environ.get("DB_PORT", "5432"),
        dbname=os.environ["DB_NAME"],
        user=os.environ["DB_USER"],
        password=os.environ["DB_PASSWORD"],
        connect_timeout=10,
    )


def compute_discrepancies(rows):
    """Pure function, kept separate from the DB I/O above so it's testable without a live Postgres.

    rows: iterable of (account_number, recorded_balance, last_transaction_balance) tuples,
    where last_transaction_balance may be None (account has no transactions yet).
    Returns a list of (account_number, recorded_balance, last_transaction_balance, difference) tuples.
    """
    discrepancies = []
    for account_number, recorded_balance, last_transaction_balance in rows:
        if last_transaction_balance is None:
            continue  # no transactions yet, nothing to reconcile
        difference = recorded_balance - last_transaction_balance
        if abs(difference) > DISCREPANCY_EPSILON:
            discrepancies.append((account_number, recorded_balance, last_transaction_balance, difference))
    return discrepancies


def lambda_handler(event, context):
    conn = _connect()
    try:
        with conn.cursor() as cur:
            cur.execute(CREATE_TABLE_SQL)
            cur.execute(SELECT_ACCOUNTS_SQL)
            rows = cur.fetchall()

            checked = sum(1 for row in rows if row[2] is not None)
            discrepancies = compute_discrepancies(rows)

            for row in discrepancies:
                cur.execute(INSERT_DISCREPANCY_SQL, row)

            conn.commit()

        result = {"accounts_checked": checked, "discrepancies_found": len(discrepancies)}
        print(json.dumps(result))
        return {"statusCode": 200, **result}
    finally:
        conn.close()
