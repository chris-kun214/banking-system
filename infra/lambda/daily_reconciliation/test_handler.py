from decimal import Decimal

from handler import compute_discrepancies


def test_no_transactions_is_skipped():
    rows = [("ACC-1", Decimal("100.00"), None)]
    assert compute_discrepancies(rows) == []


def test_matching_balance_is_not_a_discrepancy():
    rows = [("ACC-1", Decimal("100.00"), Decimal("100.00"))]
    assert compute_discrepancies(rows) == []


def test_mismatched_balance_is_flagged():
    rows = [("ACC-1", Decimal("105.00"), Decimal("100.00"))]
    result = compute_discrepancies(rows)
    assert result == [("ACC-1", Decimal("105.00"), Decimal("100.00"), Decimal("5.00"))]


def test_epsilon_tolerates_rounding_noise():
    rows = [("ACC-1", Decimal("100.005"), Decimal("100.00"))]
    assert compute_discrepancies(rows) == []


def test_mixed_batch_only_flags_the_mismatched_account():
    rows = [
        ("ACC-1", Decimal("100.00"), Decimal("100.00")),
        ("ACC-2", Decimal("50.00"), None),
        ("ACC-3", Decimal("80.00"), Decimal("75.00")),
    ]
    result = compute_discrepancies(rows)
    assert result == [("ACC-3", Decimal("80.00"), Decimal("75.00"), Decimal("5.00"))]
