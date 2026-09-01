#!/usr/bin/env python3
"""使用 Tushare 日线和 ZZShare 估值回补 stock_daily_quote。"""

from __future__ import annotations

import argparse
from dataclasses import dataclass
from datetime import date, timedelta
from decimal import Decimal, InvalidOperation, ROUND_HALF_UP
import logging
import os
from pathlib import Path
import time
from typing import Any, Callable

import pandas as pd
import pymysql


LOGGER = logging.getLogger("stock_daily_backfill")
SOURCE_WITH_VALUATION = "TUSHARE_ZZSHARE"
SOURCE_WITHOUT_VALUATION = "TUSHARE"
MARKET_CAP_UNIT = Decimal("100000000")
SHARE_CAPITAL_UNIT = Decimal("10000")
AMOUNT_UNIT = Decimal("1000")


@dataclass(frozen=True)
class Stock:
    code: str
    name: str


class RateLimiter:
    def __init__(self, interval_seconds: float) -> None:
        self.interval_seconds = interval_seconds
        self.next_allowed_at = 0.0

    def wait(self) -> None:
        now = time.monotonic()
        remaining = self.next_allowed_at - now
        if remaining > 0:
            time.sleep(remaining)
        self.next_allowed_at = time.monotonic() + self.interval_seconds


def load_local_env(path: Path = Path(".env")) -> None:
    if not path.exists():
        return
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", maxsplit=1)
        key = key.strip()
        value = value.strip().strip('"').strip("'")
        if key and value:
            os.environ.setdefault(key, value)


def default_date_range() -> tuple[date, date]:
    end_date = date.today() - timedelta(days=1)
    try:
        start_date = end_date.replace(year=end_date.year - 2)
    except ValueError:
        start_date = end_date.replace(year=end_date.year - 2, day=28)
    return start_date, end_date


def parse_args() -> argparse.Namespace:
    default_start, default_end = default_date_range()
    parser = argparse.ArgumentParser(description="回补最近两年沪深京A股日行情")
    parser.add_argument("--start-date", default=default_start.isoformat())
    parser.add_argument("--end-date", default=default_end.isoformat())
    parser.add_argument("--limit-dates", type=int, default=0, help="仅处理前N个交易日，0表示全部")
    parser.add_argument("--minimum-daily-count", type=int, default=3000)
    parser.add_argument("--minimum-valuation-count", type=int, default=3000)
    parser.add_argument("--tushare-interval", type=float, default=1.5)
    parser.add_argument("--zzshare-interval", type=float, default=1.0)
    parser.add_argument("--max-retries", type=int, default=2)
    parser.add_argument("--skip-valuation", action="store_true", help="仅回补Tushare日线")
    parser.add_argument("--cache-dir", default=".cache/stock-daily-backfill")
    parser.add_argument("--db-host", default=os.getenv("DB_HOST", "127.0.0.1"))
    parser.add_argument("--db-port", type=int, default=int(os.getenv("DB_PORT", "3306")))
    parser.add_argument("--db-name", default=os.getenv("DB_NAME", "stock_demo"))
    parser.add_argument("--db-username", default=os.getenv("DB_USERNAME", "root"))
    parser.add_argument("--batch-size", type=int, default=1000)
    args = parser.parse_args()

    args.start_date = date.fromisoformat(args.start_date)
    args.end_date = date.fromisoformat(args.end_date)
    args.cache_dir = Path(args.cache_dir)
    if args.start_date > args.end_date:
        parser.error("start-date不能晚于end-date")
    if args.limit_dates < 0 or args.minimum_daily_count < 1 or args.minimum_valuation_count < 1:
        parser.error("limit-dates不能为负数，最低记录数必须大于0")
    if args.tushare_interval < 0 or args.zzshare_interval < 0:
        parser.error("请求间隔不能为负数")
    if args.max_retries < 0 or args.batch_size < 1:
        parser.error("max-retries不能为负数，batch-size必须大于0")
    if not os.getenv("TUSHARE_TOKEN"):
        parser.error("请通过环境变量TUSHARE_TOKEN提供Tushare Token")
    if not os.getenv("DB_PASSWORD"):
        parser.error("请通过环境变量DB_PASSWORD提供数据库密码")
    return args


def connect_database(args: argparse.Namespace) -> pymysql.Connection:
    return pymysql.connect(
        host=args.db_host,
        port=args.db_port,
        database=args.db_name,
        user=args.db_username,
        password=os.environ["DB_PASSWORD"],
        charset="utf8mb4",
        autocommit=False,
        cursorclass=pymysql.cursors.DictCursor,
    )


def load_trade_dates(
    connection: pymysql.Connection, start_date: date, end_date: date
) -> list[date]:
    with connection.cursor() as cursor:
        cursor.execute(
            """
            SELECT trade_date
              FROM trade_calendar
             WHERE market_code='CN_A'
               AND is_trading_day=1
               AND trade_date BETWEEN %s AND %s
             ORDER BY trade_date
            """,
            (start_date, end_date),
        )
        return [row["trade_date"] for row in cursor.fetchall()]


def load_stocks(connection: pymysql.Connection) -> dict[str, Stock]:
    with connection.cursor() as cursor:
        cursor.execute("SELECT stock_code, stock_name FROM stock_basic ORDER BY stock_code")
        return {
            row["stock_code"]: Stock(row["stock_code"], row["stock_name"])
            for row in cursor.fetchall()
        }


def call_with_retry(
    operation: Callable[[], pd.DataFrame],
    limiter: RateLimiter,
    max_retries: int,
    description: str,
) -> pd.DataFrame:
    last_error: BaseException | None = None
    for attempt in range(max_retries + 1):
        limiter.wait()
        try:
            return operation()
        except Exception as error:
            last_error = error
            if attempt < max_retries:
                backoff_seconds = 2**attempt
                LOGGER.warning(
                    "%s失败，第%d次重试将在%d秒后执行：%s",
                    description, attempt + 1, backoff_seconds, error,
                )
                time.sleep(backoff_seconds)
    assert last_error is not None
    raise last_error


def read_or_fetch(
    cache_file: Path,
    fetcher: Callable[[], pd.DataFrame],
    limiter: RateLimiter,
    max_retries: int,
    description: str,
    minimum_rows: int,
    required_fields: set[str],
) -> pd.DataFrame:
    if cache_file.exists():
        frame = pd.read_csv(cache_file, dtype={"ts_code": str, "code": str})
    else:
        frame = call_with_retry(fetcher, limiter, max_retries, description)
    if frame is None:
        frame = pd.DataFrame()
    missing_fields = required_fields.difference(frame.columns)
    if missing_fields:
        raise RuntimeError(f"{description}缺少字段：{sorted(missing_fields)}")
    if len(frame.index) < minimum_rows:
        raise RuntimeError(
            f"{description}仅返回{len(frame.index)}条，低于最低要求{minimum_rows}条"
        )
    if not cache_file.exists():
        cache_file.parent.mkdir(parents=True, exist_ok=True)
        frame.to_csv(cache_file, index=False, encoding="utf-8-sig")
    return frame


def normalize_code(value: Any) -> str:
    return str(value).split(".", maxsplit=1)[0].zfill(6)


def decimal_or_none(value: Any) -> Decimal | None:
    if value is None or pd.isna(value) or value == "":
        return None
    try:
        result = Decimal(str(value))
        return result if result.is_finite() else None
    except (InvalidOperation, ValueError):
        return None


def rounded_hand_or_none(value: Any) -> int | None:
    number = decimal_or_none(value)
    if number is None:
        return None
    return int(number.quantize(Decimal("1"), rounding=ROUND_HALF_UP))


def calculate_percent(numerator: Decimal | None, denominator: Decimal | None) -> Decimal | None:
    if numerator is None or denominator is None or denominator == 0:
        return None
    return numerator / denominator * Decimal("100")


def valuation_by_code(frame: pd.DataFrame) -> dict[str, dict[str, Any]]:
    if frame is None or frame.empty:
        return {}
    if "code" not in frame.columns:
        raise ValueError("ZZShare估值数据缺少code字段")
    return {normalize_code(row["code"]): row for row in frame.to_dict("records")}


def market_cap_yuan(
    valuation: dict[str, Any], market_cap_field: str, share_field: str, close_price: Decimal | None
) -> Decimal | None:
    market_cap = decimal_or_none(valuation.get(market_cap_field))
    if market_cap is not None:
        return market_cap * MARKET_CAP_UNIT
    share_capital = decimal_or_none(valuation.get(share_field))
    if share_capital is None or close_price is None:
        return None
    return close_price * share_capital * SHARE_CAPITAL_UNIT


def build_records(
    trade_date: date,
    daily_frame: pd.DataFrame,
    valuation_frame: pd.DataFrame,
    stocks: dict[str, Stock],
) -> list[tuple[Any, ...]]:
    required_fields = {
        "ts_code", "open", "high", "low", "close", "pre_close", "pct_chg", "vol", "amount"
    }
    missing_fields = required_fields.difference(daily_frame.columns)
    if missing_fields:
        raise ValueError(f"Tushare日线缺少字段：{sorted(missing_fields)}")

    valuations = valuation_by_code(valuation_frame)
    records: list[tuple[Any, ...]] = []
    for row in daily_frame.to_dict("records"):
        stock_code = normalize_code(row["ts_code"])
        stock = stocks.get(stock_code)
        if stock is None:
            continue

        open_price = decimal_or_none(row.get("open"))
        high_price = decimal_or_none(row.get("high"))
        low_price = decimal_or_none(row.get("low"))
        close_price = decimal_or_none(row.get("close"))
        previous_close = decimal_or_none(row.get("pre_close"))
        amplitude = None
        if high_price is not None and low_price is not None:
            amplitude = calculate_percent(high_price - low_price, previous_close)

        valuation = valuations.get(stock_code, {})
        turnover_rate = decimal_or_none(valuation.get("turnover_ratio"))
        if turnover_rate is None:
            volume_hand = decimal_or_none(row.get("vol"))
            circulating_cap = decimal_or_none(valuation.get("circulating_cap"))
            if volume_hand is not None and circulating_cap is not None and circulating_cap != 0:
                turnover_rate = volume_hand / (circulating_cap * Decimal("100")) * Decimal("100")

        source = SOURCE_WITH_VALUATION if valuation else SOURCE_WITHOUT_VALUATION
        amount = decimal_or_none(row.get("amount"))
        records.append(
            (
                stock_code,
                stock.name,
                trade_date,
                close_price,
                previous_close,
                open_price,
                high_price,
                low_price,
                rounded_hand_or_none(row.get("vol")),
                amount * AMOUNT_UNIT if amount is not None else None,
                decimal_or_none(row.get("pct_chg")),
                amplitude,
                turnover_rate,
                decimal_or_none(valuation.get("pe_ratio")),
                decimal_or_none(valuation.get("pb_ratio")),
                market_cap_yuan(valuation, "circulating_market_cap", "circulating_cap", close_price),
                market_cap_yuan(valuation, "market_cap", "capitalization", close_price),
                source,
            )
        )
    return records


UPSERT_SQL = """
    INSERT INTO stock_daily_quote (
        stock_code, stock_name, trade_date, quote_time,
        close_price, previous_close_price, open_price, high_price, low_price,
        volume_hand, turnover_amount_yuan,
        bid1_price, bid1_volume_hand, ask1_price, ask1_volume_hand,
        change_percent, amplitude_percent, turnover_rate, pe_ratio, pb_ratio,
        circulating_market_cap_yuan, total_market_cap_yuan,
        data_source, data_status, collected_at
    ) VALUES (
        %s, %s, %s, NULL,
        %s, %s, %s, %s, %s,
        %s, %s,
        NULL, NULL, NULL, NULL,
        %s, %s, %s, %s, %s,
        %s, %s,
        %s, 'PARTIAL', NOW()
    )
    ON DUPLICATE KEY UPDATE
        stock_name=IF(data_source='TENCENT', stock_name, VALUES(stock_name)),
        quote_time=IF(data_source='TENCENT', quote_time, VALUES(quote_time)),
        close_price=IF(data_source='TENCENT', close_price, VALUES(close_price)),
        previous_close_price=IF(data_source='TENCENT', previous_close_price, VALUES(previous_close_price)),
        open_price=IF(data_source='TENCENT', open_price, VALUES(open_price)),
        high_price=IF(data_source='TENCENT', high_price, VALUES(high_price)),
        low_price=IF(data_source='TENCENT', low_price, VALUES(low_price)),
        volume_hand=IF(data_source='TENCENT', volume_hand, VALUES(volume_hand)),
        turnover_amount_yuan=IF(data_source='TENCENT', turnover_amount_yuan, VALUES(turnover_amount_yuan)),
        change_percent=IF(data_source='TENCENT', change_percent, VALUES(change_percent)),
        amplitude_percent=IF(data_source='TENCENT', amplitude_percent, VALUES(amplitude_percent)),
        turnover_rate=IF(data_source='TENCENT', turnover_rate, VALUES(turnover_rate)),
        pe_ratio=IF(data_source='TENCENT', pe_ratio, VALUES(pe_ratio)),
        pb_ratio=IF(data_source='TENCENT', pb_ratio, VALUES(pb_ratio)),
        circulating_market_cap_yuan=IF(data_source='TENCENT', circulating_market_cap_yuan, VALUES(circulating_market_cap_yuan)),
        total_market_cap_yuan=IF(data_source='TENCENT', total_market_cap_yuan, VALUES(total_market_cap_yuan)),
        data_status=IF(data_source='TENCENT', data_status, VALUES(data_status)),
        collected_at=IF(data_source='TENCENT', collected_at, VALUES(collected_at)),
        data_source=IF(data_source='TENCENT', data_source, VALUES(data_source)),
        updated_at=CURRENT_TIMESTAMP
"""


def save_records(connection: pymysql.Connection, records: list[tuple[Any, ...]], batch_size: int) -> None:
    try:
        with connection.cursor() as cursor:
            for start in range(0, len(records), batch_size):
                cursor.executemany(UPSERT_SQL, records[start : start + batch_size])
        connection.commit()
    except Exception:
        connection.rollback()
        raise


def main() -> int:
    import tushare as ts
    from zzshare.client import DataApi

    load_local_env()
    args = parse_args()
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    connection = connect_database(args)
    tushare_api = ts.pro_api(os.environ["TUSHARE_TOKEN"])
    zzshare_token = os.getenv("ZZSHARE_TOKEN")
    zzshare_api = None
    if not args.skip_valuation:
        zzshare_api = DataApi(token=zzshare_token) if zzshare_token else DataApi()
    tushare_limiter = RateLimiter(args.tushare_interval)
    zzshare_limiter = RateLimiter(args.zzshare_interval)

    try:
        trade_dates = load_trade_dates(connection, args.start_date, args.end_date)
        stocks = load_stocks(connection)
        if args.limit_dates:
            trade_dates = trade_dates[: args.limit_dates]
        if not trade_dates:
            raise RuntimeError("指定范围内没有交易日")
        if not stocks:
            raise RuntimeError("stock_basic中没有股票数据")

        LOGGER.info(
            "开始回补：交易日=%d，股票基础信息=%d，范围=%s至%s",
            len(trade_dates), len(stocks), trade_dates[0], trade_dates[-1],
        )
        total_saved = 0
        for index, trade_date in enumerate(trade_dates, start=1):
            date_text = trade_date.strftime("%Y%m%d")
            daily_file = args.cache_dir / f"{date_text}-tushare-daily.csv"
            daily_frame = read_or_fetch(
                daily_file,
                lambda value=date_text: tushare_api.daily(trade_date=value),
                tushare_limiter,
                args.max_retries,
                f"Tushare {trade_date} 日线",
                args.minimum_daily_count,
                {"ts_code", "open", "high", "low", "close", "pre_close", "pct_chg", "vol", "amount"},
            )

            valuation_frame = pd.DataFrame()
            if zzshare_api is not None:
                valuation_file = args.cache_dir / f"{date_text}-zzshare-valuation.csv"
                valuation_frame = read_or_fetch(
                    valuation_file,
                    lambda value=trade_date.isoformat(): zzshare_api.finance_valuation(value),
                    zzshare_limiter,
                    args.max_retries,
                    f"ZZShare {trade_date} 估值",
                    args.minimum_valuation_count,
                    {"code", "turnover_ratio", "pe_ratio", "pb_ratio"},
                )

            records = build_records(trade_date, daily_frame, valuation_frame, stocks)
            if not records:
                raise RuntimeError(f"{trade_date}与stock_basic合并后没有可写入记录")
            save_records(connection, records, args.batch_size)
            total_saved += len(records)
            LOGGER.info(
                "[%d/%d] %s 完成：日线=%d，估值=%d，写入=%d",
                index, len(trade_dates), trade_date, len(daily_frame.index),
                len(valuation_frame.index), len(records),
            )

        LOGGER.info("回补完成：交易日=%d，累计写入=%d", len(trade_dates), total_saved)
        return 0
    except KeyboardInterrupt:
        LOGGER.warning("任务已中断；已完成日期可以通过缓存和唯一键安全续跑")
        return 130
    except Exception:
        LOGGER.exception("历史日线回补失败")
        return 1
    finally:
        connection.close()


if __name__ == "__main__":
    raise SystemExit(main())
