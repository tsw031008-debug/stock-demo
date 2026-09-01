#!/usr/bin/env python3
"""从本地股票日行情全量回填每日沪深A股成交额。"""

from __future__ import annotations

import argparse
from datetime import date
from decimal import Decimal
import logging
import os
from pathlib import Path
from typing import Any

import pymysql


LOGGER = logging.getLogger("market_daily_turnover_backfill")


def load_local_env(path: Path = Path(".env")) -> None:
    """加载本地未跟踪环境变量，且不覆盖进程已有配置。"""
    if not path.exists():
        return
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", maxsplit=1)
        os.environ.setdefault(key.strip(), value.strip().strip('"').strip("'"))


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="全量回填每日沪深A股成交额")
    parser.add_argument("--start-date")
    parser.add_argument("--end-date")
    parser.add_argument("--batch-size", type=int, default=50)
    parser.add_argument("--progress-every", type=int, default=20)
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--db-host", default=os.getenv("DB_HOST", "127.0.0.1"))
    parser.add_argument("--db-port", type=int, default=int(os.getenv("DB_PORT", "3306")))
    parser.add_argument("--db-name", default=os.getenv("DB_NAME", "stock_demo"))
    parser.add_argument("--db-username", default=os.getenv("DB_USERNAME", "root"))
    args = parser.parse_args()
    args.start_date = date.fromisoformat(args.start_date) if args.start_date else None
    args.end_date = date.fromisoformat(args.end_date) if args.end_date else None
    if args.start_date and args.end_date and args.start_date > args.end_date:
        parser.error("start-date不能晚于end-date")
    if args.batch_size < 1 or args.progress_every < 1:
        parser.error("batch-size和progress-every必须大于0")
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
    connection: pymysql.Connection,
    start_date: date | None,
    end_date: date | None,
) -> list[date]:
    """读取日行情中实际存在的沪深A股交易日。"""
    conditions = [
        "(stock_code LIKE '0%%' OR stock_code LIKE '3%%' OR stock_code LIKE '6%%')"
    ]
    parameters: list[date] = []
    if start_date:
        conditions.append("trade_date >= %s")
        parameters.append(start_date)
    if end_date:
        conditions.append("trade_date <= %s")
        parameters.append(end_date)
    sql = (
        "SELECT DISTINCT trade_date FROM stock_daily_quote WHERE "
        + " AND ".join(conditions)
        + " ORDER BY trade_date"
    )
    with connection.cursor() as cursor:
        cursor.execute(sql, parameters)
        return [row["trade_date"] for row in cursor.fetchall()]


def load_turnover_amounts(
    connection: pymysql.Connection,
    trade_date: date,
) -> list[Decimal | None]:
    """读取单个交易日的成交额原始值，不在SQL中做业务聚合。"""
    with connection.cursor() as cursor:
        cursor.execute(
            """
            SELECT turnover_amount_yuan
              FROM stock_daily_quote
             WHERE trade_date = %s
               AND (stock_code LIKE '0%%' OR stock_code LIKE '3%%' OR stock_code LIKE '6%%')
             ORDER BY stock_code
            """,
            (trade_date,),
        )
        return [row["turnover_amount_yuan"] for row in cursor.fetchall()]


def build_record(trade_date: date, amounts: list[Decimal | None]) -> tuple[Any, ...]:
    """校验完整性并在进程内汇总一个交易日。"""
    if not amounts:
        raise RuntimeError(f"没有沪深A股日行情，tradeDate={trade_date}")
    if any(amount is None or amount < 0 for amount in amounts):
        raise RuntimeError(f"沪深A股成交额不完整，tradeDate={trade_date}")
    total = sum(amounts, Decimal("0"))
    return trade_date, total, len(amounts), len(amounts), "STOCK_DAILY_QUOTE", "COMPLETE"


UPSERT_SQL = """
    INSERT INTO market_daily_turnover (
        trade_date, turnover_amount_yuan, stock_count, amount_record_count,
        data_source, data_status, calculated_at
    ) VALUES (%s, %s, %s, %s, %s, %s, NOW())
    ON DUPLICATE KEY UPDATE
        turnover_amount_yuan=IF(data_status='COMPLETE', turnover_amount_yuan, VALUES(turnover_amount_yuan)),
        stock_count=IF(data_status='COMPLETE', stock_count, VALUES(stock_count)),
        amount_record_count=IF(data_status='COMPLETE', amount_record_count, VALUES(amount_record_count)),
        data_source=IF(data_status='COMPLETE', data_source, VALUES(data_source)),
        calculated_at=IF(data_status='COMPLETE', calculated_at, VALUES(calculated_at)),
        updated_at=IF(data_status='COMPLETE', updated_at, CURRENT_TIMESTAMP),
        data_status=IF(data_status='COMPLETE', data_status, VALUES(data_status))
"""


def save_records(connection: pymysql.Connection, records: list[tuple[Any, ...]]) -> None:
    if not records:
        return
    try:
        with connection.cursor() as cursor:
            cursor.executemany(UPSERT_SQL, records)
        connection.commit()
    except Exception:
        connection.rollback()
        raise


def main() -> int:
    load_local_env()
    args = parse_args()
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    connection = connect_database(args)
    try:
        trade_dates = load_trade_dates(connection, args.start_date, args.end_date)
        if not trade_dates:
            raise RuntimeError("指定范围内没有沪深A股日行情")
        LOGGER.info("开始回填每日市场成交额：交易日=%d，范围=%s至%s，dryRun=%s",
                    len(trade_dates), trade_dates[0], trade_dates[-1], args.dry_run)
        pending: list[tuple[Any, ...]] = []
        saved = 0
        for index, trade_date in enumerate(trade_dates, start=1):
            pending.append(build_record(trade_date, load_turnover_amounts(connection, trade_date)))
            if len(pending) >= args.batch_size:
                if not args.dry_run:
                    save_records(connection, pending)
                saved += len(pending)
                pending.clear()
            if index % args.progress_every == 0 or index == len(trade_dates):
                LOGGER.info("[%d/%d] 已处理至%s", index, len(trade_dates), trade_date)
        if not args.dry_run:
            save_records(connection, pending)
        saved += len(pending)
        LOGGER.info("每日市场成交额回填完成：处理=%d", saved)
        return 0
    except Exception:
        LOGGER.exception("每日市场成交额回填失败")
        return 1
    finally:
        connection.close()


if __name__ == "__main__":
    raise SystemExit(main())
