#!/usr/bin/env python3
"""使用AKShare新浪ETF日线回补index_etf_daily_quote。"""

from __future__ import annotations

import argparse
from dataclasses import dataclass
from datetime import date, datetime, timedelta
from decimal import Decimal, InvalidOperation
import logging
import os
from pathlib import Path
from typing import Any, Callable

import pandas as pd
import pymysql


LOGGER = logging.getLogger("index_etf_daily_backfill")
SOURCE_NAME = "AKSHARE_SINA"
PROXY_ENV_KEYS = (
    "HTTP_PROXY",
    "HTTPS_PROXY",
    "ALL_PROXY",
    "http_proxy",
    "https_proxy",
    "all_proxy",
)


@dataclass(frozen=True)
class IndexEtfDefinition:
    code: str
    index_name: str
    akshare_symbol: str


# 顺序与功能10接口的四根柱状图保持一致。
INDEX_ETFS = (
    IndexEtfDefinition("510050", "上证50", "sh510050"),
    IndexEtfDefinition("510300", "沪深300", "sh510300"),
    IndexEtfDefinition("159949", "创业板50", "sz159949"),
    IndexEtfDefinition("512100", "中证1000", "sh512100"),
)


def disable_proxy_environment() -> None:
    """避免AKShare请求继承当前机器上不可用的系统代理。"""
    for key in PROXY_ENV_KEYS:
        os.environ.pop(key, None)
    os.environ["NO_PROXY"] = "*"
    os.environ["no_proxy"] = "*"


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


def parse_args() -> argparse.Namespace:
    default_end = date.today() - timedelta(days=1)
    default_start = default_end - timedelta(days=30)
    parser = argparse.ArgumentParser(description="回补功能10四只固定ETF的日行情")
    parser.add_argument("--start-date", default=default_start.isoformat())
    parser.add_argument("--end-date", default=default_end.isoformat())
    parser.add_argument("--cache-dir", default=".cache/index-etf-daily-backfill")
    parser.add_argument("--db-host", default=os.getenv("DB_HOST", "127.0.0.1"))
    parser.add_argument("--db-port", type=int, default=int(os.getenv("DB_PORT", "3306")))
    parser.add_argument("--db-name", default=os.getenv("DB_NAME", "stock_demo"))
    parser.add_argument("--db-username", default=os.getenv("DB_USERNAME", "root"))
    args = parser.parse_args()
    args.start_date = date.fromisoformat(args.start_date)
    args.end_date = date.fromisoformat(args.end_date)
    args.cache_dir = Path(args.cache_dir)
    if args.start_date > args.end_date:
        parser.error("start-date不能晚于end-date")
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


def read_or_fetch(
    cache_file: Path,
    fetcher: Callable[[], pd.DataFrame],
) -> pd.DataFrame:
    if cache_file.exists():
        return pd.read_csv(cache_file)
    frame = fetcher()
    if frame.empty:
        raise RuntimeError(f"ETF历史行情为空：{cache_file.stem}")
    cache_file.parent.mkdir(parents=True, exist_ok=True)
    frame.to_csv(cache_file, index=False, encoding="utf-8")
    return frame


def to_decimal(value: Any) -> Decimal:
    try:
        close_price = Decimal(str(value))
    except (InvalidOperation, ValueError) as error:
        raise ValueError(f"ETF收盘价格式错误：{value}") from error
    if close_price <= 0:
        raise ValueError(f"ETF收盘价必须大于0：{value}")
    return close_price


def build_records(
    definition: IndexEtfDefinition,
    frame: pd.DataFrame,
    trade_dates: list[date],
    collected_at: datetime,
) -> list[tuple[Any, ...]]:
    required_fields = {"date", "close"}
    missing_fields = required_fields.difference(frame.columns)
    if missing_fields:
        raise ValueError(f"ETF历史行情缺少字段：{sorted(missing_fields)}")

    actual = frame.loc[:, ["date", "close"]].copy()
    actual["date"] = pd.to_datetime(actual["date"]).dt.date
    actual = actual.sort_values("date").drop_duplicates("date", keep="last")
    close_by_date = dict(zip(actual["date"], actual["close"]))

    records: list[tuple[Any, ...]] = []
    for trade_date in trade_dates:
        close_price = close_by_date.get(trade_date)
        if close_price is None or pd.isna(close_price):
            raise ValueError(
                f"ETF历史行情不完整，etfCode={definition.code}，tradeDate={trade_date}"
            )
        records.append(
            (
                definition.code,
                definition.index_name,
                trade_date,
                to_decimal(close_price),
                SOURCE_NAME,
                collected_at,
            )
        )
    return records


def save_records(
    connection: pymysql.Connection,
    records: list[tuple[Any, ...]],
) -> None:
    sql = """
        INSERT INTO index_etf_daily_quote (
            etf_code, index_name, trade_date, quote_time,
            close_price, data_source, collected_at
        ) VALUES (%s, %s, %s, NULL, %s, %s, %s)
        ON DUPLICATE KEY UPDATE
            index_name=IF(data_source='TENCENT', index_name, VALUES(index_name)),
            quote_time=IF(data_source='TENCENT', quote_time, NULL),
            close_price=IF(data_source='TENCENT', close_price, VALUES(close_price)),
            collected_at=IF(data_source='TENCENT', collected_at, VALUES(collected_at)),
            data_source=IF(data_source='TENCENT', data_source, VALUES(data_source)),
            updated_at=CURRENT_TIMESTAMP
    """
    with connection.cursor() as cursor:
        cursor.executemany(sql, records)
    connection.commit()


def main() -> None:
    load_local_env()
    args = parse_args()
    disable_proxy_environment()
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    import akshare as ak

    connection = connect_database(args)
    try:
        trade_dates = load_trade_dates(connection, args.start_date, args.end_date)
        if not trade_dates:
            raise RuntimeError("指定日期范围内没有交易日")

        collected_at = datetime.now()
        records: list[tuple[Any, ...]] = []
        for definition in INDEX_ETFS:
            cache_file = args.cache_dir / (
                f"{definition.code}-{args.start_date:%Y%m%d}-{args.end_date:%Y%m%d}.csv"
            )
            frame = read_or_fetch(
                cache_file,
                lambda item=definition: ak.fund_etf_hist_sina(
                    symbol=item.akshare_symbol
                ),
            )
            records.extend(build_records(definition, frame, trade_dates, collected_at))

        expected_count = len(trade_dates) * len(INDEX_ETFS)
        if len(records) != expected_count:
            raise RuntimeError(
                f"ETF历史行情数量不完整，expected={expected_count}，actual={len(records)}"
            )
        save_records(connection, records)
        LOGGER.info(
            "ETF日行情回补完成，startDate=%s，endDate=%s，savedCount=%d",
            args.start_date,
            args.end_date,
            len(records),
        )
    except Exception:
        connection.rollback()
        raise
    finally:
        connection.close()


if __name__ == "__main__":
    main()
