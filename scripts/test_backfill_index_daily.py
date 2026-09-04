import unittest
from datetime import date, datetime
from decimal import Decimal
import os
from unittest.mock import patch

import pandas as pd
import requests

from scripts.backfill_index_daily import INDEXES, build_records, disable_proxy_environment


class BackfillIndexDailyTest(unittest.TestCase):

    def test_disables_system_proxy_environment(self) -> None:
        with patch.dict(os.environ, {"HTTPS_PROXY": "http://127.0.0.1:7890"}):
            disable_proxy_environment()

            self.assertNotIn("HTTPS_PROXY", os.environ)
            self.assertEqual(
                {},
                requests.utils.get_environ_proxies(
                    "https://push2his.eastmoney.com/api/qt/stock/kline/get"
                ),
            )

    def test_builds_previous_close_for_requested_trade_dates(self) -> None:
        frame = pd.DataFrame(
            {
                "date": ["2026-08-28", "2026-08-31", "2026-09-01"],
                "close": [100, 102, 101],
            }
        )

        records = build_records(
            INDEXES[0],
            frame,
            [date(2026, 8, 31), date(2026, 9, 1)],
            datetime(2026, 9, 2, 10, 0),
        )

        self.assertEqual(2, len(records))
        self.assertEqual(Decimal("102"), records[0][3])
        self.assertEqual(Decimal("100"), records[0][4])
        self.assertEqual("AKSHARE_SINA", records[0][5])
        self.assertEqual(Decimal("101"), records[1][3])
        self.assertEqual(Decimal("102"), records[1][4])

    def test_rejects_missing_trade_date(self) -> None:
        frame = pd.DataFrame(
            {
                "date": ["2026-08-28", "2026-09-01"],
                "close": [100, 101],
            }
        )

        with self.assertRaisesRegex(ValueError, "指数历史行情不完整"):
            build_records(
                INDEXES[0],
                frame,
                [date(2026, 8, 31)],
                datetime(2026, 9, 2, 10, 0),
            )


if __name__ == "__main__":
    unittest.main()
