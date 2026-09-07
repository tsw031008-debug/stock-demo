import unittest
from datetime import date, datetime
from decimal import Decimal
import os
from unittest.mock import patch

import pandas as pd
import requests

from scripts.backfill_index_etf_daily import (
    INDEX_ETFS,
    build_records,
    disable_proxy_environment,
)


class BackfillIndexEtfDailyTest(unittest.TestCase):

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

    def test_builds_records_for_requested_trade_dates(self) -> None:
        frame = pd.DataFrame(
            {
                "date": ["2026-08-28", "2026-08-31", "2026-09-01"],
                "close": [2.70, 2.73, 2.71],
            }
        )

        records = build_records(
            INDEX_ETFS[0],
            frame,
            [date(2026, 8, 31), date(2026, 9, 1)],
            datetime(2026, 9, 2, 10, 0),
        )

        self.assertEqual(2, len(records))
        self.assertEqual("510050", records[0][0])
        self.assertEqual("上证50", records[0][1])
        self.assertEqual(Decimal("2.73"), records[0][3])
        self.assertEqual("AKSHARE_SINA", records[0][4])
        self.assertEqual(Decimal("2.71"), records[1][3])

    def test_rejects_missing_trade_date(self) -> None:
        frame = pd.DataFrame(
            {
                "date": ["2026-08-28", "2026-09-01"],
                "close": [2.70, 2.71],
            }
        )

        with self.assertRaisesRegex(ValueError, "ETF历史行情不完整"):
            build_records(
                INDEX_ETFS[0],
                frame,
                [date(2026, 8, 31)],
                datetime(2026, 9, 2, 10, 0),
            )

    def test_rejects_missing_required_field(self) -> None:
        frame = pd.DataFrame({"date": ["2026-08-28"]})

        with self.assertRaisesRegex(ValueError, "ETF历史行情缺少字段"):
            build_records(
                INDEX_ETFS[0],
                frame,
                [date(2026, 8, 28)],
                datetime(2026, 9, 2, 10, 0),
            )


if __name__ == "__main__":
    unittest.main()
