from datetime import date
from decimal import Decimal
import os
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

import pandas as pd

from scripts.backfill_stock_daily import Stock, build_records, load_local_env


class BuildRecordsTest(unittest.TestCase):

    def test_maps_tushare_and_zzshare_fields(self) -> None:
        daily = pd.DataFrame([
            {
                "ts_code": "600519.SH", "open": 1400, "high": 1450, "low": 1390,
                "close": 1440, "pre_close": 1400, "pct_chg": 2.8571,
                "vol": 12345.67, "amount": 17890.12,
            }
        ])
        valuation = pd.DataFrame([
            {
                "code": "600519.SH", "capitalization": 125619.78,
                "circulating_cap": 125619.78, "market_cap": 18087.25,
                "circulating_market_cap": 18087.25, "turnover_ratio": 0.0983,
                "pe_ratio": 25.12, "pb_ratio": 8.45,
            }
        ])

        records = build_records(
            date(2026, 8, 20), daily, valuation,
            {"600519": Stock("600519", "贵州茅台")},
        )

        self.assertEqual(1, len(records))
        record = records[0]
        self.assertEqual("600519", record[0])
        self.assertEqual(12346, record[8])
        self.assertEqual(Decimal("17890120.00"), record[9])
        self.assertEqual(Decimal("4.285714285714285714285714286"), record[11])
        self.assertEqual(Decimal("1808725000000.00"), record[15])
        self.assertEqual("TUSHARE_ZZSHARE", record[17])

    def test_calculates_market_cap_from_share_capital(self) -> None:
        daily = pd.DataFrame([
            {
                "ts_code": "920001.BJ", "open": 10, "high": 11, "low": 9,
                "close": 10, "pre_close": 10, "pct_chg": 0,
                "vol": 1000, "amount": 100,
            }
        ])
        valuation = pd.DataFrame([
            {
                "code": "920001.BJ", "capitalization": 20000,
                "circulating_cap": 8000, "turnover_ratio": None,
                "pe_ratio": None, "pb_ratio": 1.2,
            }
        ])

        record = build_records(
            date(2026, 8, 20), daily, valuation,
            {"920001": Stock("920001", "北交样本")},
        )[0]

        self.assertEqual(Decimal("0.125"), record[12])
        self.assertEqual(Decimal("800000000"), record[15])
        self.assertEqual(Decimal("2000000000"), record[16])

    def test_filters_non_current_stock(self) -> None:
        daily = pd.DataFrame([
            {
                "ts_code": "000001.SZ", "open": 1, "high": 1, "low": 1,
                "close": 1, "pre_close": 1, "pct_chg": 0, "vol": 1, "amount": 1,
            }
        ])

        self.assertEqual([], build_records(date(2026, 8, 20), daily, pd.DataFrame(), {}))

    def test_loads_local_env_without_overwriting_process_environment(self) -> None:
        original_token = os.environ.get("TUSHARE_TOKEN")
        original_password = os.environ.get("DB_PASSWORD")
        try:
            os.environ["TUSHARE_TOKEN"] = "process-token"
            os.environ.pop("DB_PASSWORD", None)
            with TemporaryDirectory() as directory:
                env_file = Path(directory) / ".env"
                env_file.write_text(
                    "TUSHARE_TOKEN=file-token\nDB_PASSWORD=local-password\n",
                    encoding="utf-8",
                )
                load_local_env(env_file)
            self.assertEqual("process-token", os.environ["TUSHARE_TOKEN"])
            self.assertEqual("local-password", os.environ["DB_PASSWORD"])
        finally:
            if original_token is None:
                os.environ.pop("TUSHARE_TOKEN", None)
            else:
                os.environ["TUSHARE_TOKEN"] = original_token
            if original_password is None:
                os.environ.pop("DB_PASSWORD", None)
            else:
                os.environ["DB_PASSWORD"] = original_password


if __name__ == "__main__":
    unittest.main()
