from datetime import date
from decimal import Decimal
import unittest

from scripts.backfill_market_daily_turnover import build_record


class BuildMarketDailyTurnoverTest(unittest.TestCase):

    def test_sums_complete_turnover_amounts(self) -> None:
        record = build_record(
            date(2026, 8, 28),
            [Decimal("100.25"), Decimal("200.75")],
            2,
        )

        self.assertEqual(Decimal("301.00"), record[1])
        self.assertEqual(2, record[2])
        self.assertEqual(2, record[3])
        self.assertEqual("COMPLETE", record[5])

    def test_rejects_missing_turnover_amount(self) -> None:
        with self.assertRaises(RuntimeError):
            build_record(date(2026, 8, 28), [Decimal("100.00"), None], 2)

    def test_rejects_empty_trading_day(self) -> None:
        with self.assertRaises(RuntimeError):
            build_record(date(2026, 8, 28), [], 2)

    def test_rejects_missing_stock_rows_with_non_null_amounts(self) -> None:
        with self.assertRaises(RuntimeError):
            build_record(date(2026, 8, 28), [Decimal("100.00")], 2)

    def test_rejects_unknown_expected_coverage(self) -> None:
        with self.assertRaises(RuntimeError):
            build_record(date(2026, 8, 28), [Decimal("100.00")], 0)


if __name__ == "__main__":
    unittest.main()
