from datetime import date
from decimal import Decimal
import unittest

from scripts.backfill_stock_fund_flow import (
    Stock,
    build_records,
    build_sina_symbol,
    calculate_ratio,
    create_source_session,
    validate_payload,
)


class BuildFundFlowRecordsTest(unittest.TestCase):

    def test_maps_sina_history_fields_and_calculates_percentages(self) -> None:
        records = build_records(
            Stock("000002", "万科A"),
            self.sample_payload(),
            date(2026, 8, 24),
            date(2026, 8, 25),
        )

        self.assertEqual(2, len(records))
        latest = records[0]
        self.assertEqual("000002", latest[0])
        self.assertEqual(date(2026, 8, 25), latest[2])
        self.assertEqual(Decimal("3.0900"), latest[3])
        self.assertEqual(Decimal("1.3115"), latest[4])
        self.assertEqual(Decimal("28445213.2900"), latest[5])
        self.assertEqual(Decimal("13.7735"), latest[6])
        self.assertEqual(Decimal("32912028.2900"), latest[7])
        self.assertEqual(Decimal("15.9364"), latest[8])
        self.assertEqual(Decimal("-4466815.0000"), latest[9])
        self.assertEqual(Decimal("-2.1629"), latest[10])
        self.assertEqual("SINA_MONEY_FLOW", latest[11])
        self.assertEqual("COMPLETE", latest[12])

    def test_filters_records_outside_requested_range(self) -> None:
        records = build_records(
            Stock("000002", "万科A"),
            self.sample_payload(),
            date(2026, 8, 25),
            date(2026, 8, 25),
        )

        self.assertEqual(1, len(records))
        self.assertEqual(date(2026, 8, 25), records[0][2])

    def test_builds_shanghai_and_shenzhen_symbols(self) -> None:
        self.assertEqual("sh600519", build_sina_symbol("600519"))
        self.assertEqual("sz000001", build_sina_symbol("000001"))
        self.assertEqual("sz300750", build_sina_symbol("300750"))
        with self.assertRaises(ValueError):
            build_sina_symbol("920001")

    def test_rejects_missing_history_field(self) -> None:
        payload = self.sample_payload()
        del payload[0]["r0_net"]

        with self.assertRaises(RuntimeError):
            validate_payload(payload, "000002")

    def test_source_session_does_not_use_system_proxy(self) -> None:
        session = create_source_session()
        try:
            self.assertFalse(session.trust_env)
        finally:
            session.close()

    def test_ratio_is_null_when_turnover_amount_is_zero(self) -> None:
        self.assertIsNone(calculate_ratio(Decimal("1"), Decimal("0")))

    @staticmethod
    def sample_payload() -> list[dict]:
        return [
            {
                "opendate": "2026-08-25",
                "trade": "3.0900",
                "changeratio": "0.0131148",
                "turnover": "1.0000",
                "netamount": "32653601.8700",
                "ratioamount": "0.158144",
                "r0": "104595936.3300",
                "r1": "40650809.0000",
                "r2": "33176035.0000",
                "r3": "28098902.4200",
                "r0_net": "32912028.2900",
                "r1_net": "-4466815.0000",
                "r2_net": "2765729.0000",
                "r3_net": "1442659.5800",
            },
            {
                "opendate": "2026-08-24",
                "trade": "3.0500",
                "changeratio": "-0.0097000",
                "turnover": "1.0000",
                "netamount": "0.0000",
                "ratioamount": "0.0000",
                "r0": "100.0000",
                "r1": "200.0000",
                "r2": "300.0000",
                "r3": "400.0000",
                "r0_net": "10.0000",
                "r1_net": "-5.0000",
                "r2_net": "-3.0000",
                "r3_net": "-2.0000",
            },
        ]


if __name__ == "__main__":
    unittest.main()
