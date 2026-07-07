"""
Generates a synthetic transaction dataset for the EMR Serverless / Spark
reconciliation demo. The real application doesn't produce anywhere near
enough transaction volume to make a "big data" story credible, so this
script fabricates a representative-scale dataset instead — clearly labelled
as synthetic, never mixed with real application data.

Writes two CSVs:
  accounts.csv:     account_number, balance
  transactions.csv: account_number, transaction_type, amount, created_at

A small fraction of accounts get an intentionally-injected balance
mismatch (a "corrupted" balance not backed by any transaction), so the
Spark reconciliation job has real discrepancies to find rather than
trivially reporting zero.

Usage:
    python3 generate_synthetic_data.py --accounts 2000 --transactions-per-account 100 --out ./data
"""

import argparse
import csv
import os
import random
from datetime import datetime, timedelta

TRANSACTION_TYPES_CREDIT = ["DEPOSIT", "TRANSFER_IN"]
TRANSACTION_TYPES_DEBIT = ["WITHDRAW", "TRANSFER_OUT"]
DISCREPANCY_RATE = 0.005  # ~0.5% of accounts get a synthetic anomaly


def generate(num_accounts: int, transactions_per_account: int, out_dir: str, seed: int = 42):
    random.seed(seed)
    os.makedirs(out_dir, exist_ok=True)

    accounts_path = os.path.join(out_dir, "accounts.csv")
    transactions_path = os.path.join(out_dir, "transactions.csv")

    start_date = datetime(2025, 1, 1)
    injected_discrepancies = 0

    with open(accounts_path, "w", newline="") as accounts_file, \
         open(transactions_path, "w", newline="") as transactions_file:

        accounts_writer = csv.writer(accounts_file)
        accounts_writer.writerow(["account_number", "balance"])

        transactions_writer = csv.writer(transactions_file)
        transactions_writer.writerow(["account_number", "transaction_type", "amount", "created_at"])

        for i in range(num_accounts):
            account_number = f"SYN-{i:07d}"
            balance = 0.0

            for j in range(transactions_per_account):
                is_credit = random.random() < 0.55
                tx_type = random.choice(TRANSACTION_TYPES_CREDIT if is_credit else TRANSACTION_TYPES_DEBIT)
                amount = round(random.uniform(5, 2000), 2)
                created_at = start_date + timedelta(
                    days=random.randint(0, 180), seconds=random.randint(0, 86400)
                )

                balance += amount if is_credit else -amount
                transactions_writer.writerow([account_number, tx_type, f"{amount:.2f}", created_at.isoformat()])

            recorded_balance = balance
            if random.random() < DISCREPANCY_RATE:
                # Inject a synthetic anomaly: recorded balance drifts from the
                # true transaction-derived balance by a random amount.
                recorded_balance += random.choice([-1, 1]) * round(random.uniform(10, 500), 2)
                injected_discrepancies += 1

            accounts_writer.writerow([account_number, f"{recorded_balance:.2f}"])

    total_transactions = num_accounts * transactions_per_account
    print(
        f"Generated {num_accounts} accounts, {total_transactions} transactions, "
        f"{injected_discrepancies} intentional discrepancies -> {out_dir}"
    )


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--accounts", type=int, default=2000)
    parser.add_argument("--transactions-per-account", type=int, default=100)
    parser.add_argument("--out", type=str, default="./data")
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()

    generate(args.accounts, args.transactions_per_account, args.out, args.seed)
