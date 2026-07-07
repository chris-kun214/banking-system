import pytest
from pyspark.sql import SparkSession

from reconciliation_job import compute_discrepancies


@pytest.fixture(scope="module")
def spark():
    session = (
        SparkSession.builder.master("local[1]")
        .appName("test-bank-reconciliation")
        .getOrCreate()
    )
    yield session
    session.stop()


def test_matching_balance_is_not_flagged(spark):
    accounts = spark.createDataFrame([("ACC-1", 100.0)], ["account_number", "balance"])
    transactions = spark.createDataFrame(
        [("ACC-1", "DEPOSIT", 100.0)], ["account_number", "transaction_type", "amount"]
    )

    result = compute_discrepancies(accounts, transactions).collect()

    assert result == []


def test_mismatched_balance_is_flagged(spark):
    accounts = spark.createDataFrame([("ACC-1", 150.0)], ["account_number", "balance"])
    transactions = spark.createDataFrame(
        [("ACC-1", "DEPOSIT", 100.0)], ["account_number", "transaction_type", "amount"]
    )

    result = compute_discrepancies(accounts, transactions).collect()

    assert len(result) == 1
    row = result[0]
    assert row["account_number"] == "ACC-1"
    assert row["computed_balance"] == 100.0
    assert row["difference"] == 50.0


def test_account_with_no_transactions_compares_against_zero(spark):
    accounts = spark.createDataFrame([("ACC-2", 25.0)], ["account_number", "balance"])
    transactions = spark.createDataFrame(
        [], "account_number string, transaction_type string, amount double"
    )

    result = compute_discrepancies(accounts, transactions).collect()

    assert len(result) == 1
    assert result[0]["difference"] == 25.0


def test_debit_transactions_reduce_computed_balance(spark):
    accounts = spark.createDataFrame([("ACC-3", 40.0)], ["account_number", "balance"])
    transactions = spark.createDataFrame(
        [
            ("ACC-3", "DEPOSIT", 100.0),
            ("ACC-3", "WITHDRAW", 60.0),
        ],
        ["account_number", "transaction_type", "amount"],
    )

    result = compute_discrepancies(accounts, transactions).collect()

    assert result == []  # 100 - 60 = 40, matches recorded balance
