#!/usr/bin/env python3
"""使用新浪财经单股历史接口回补最近121个交易日的资金流向。"""

from __future__ import annotations

import argparse
from dataclasses import dataclass
from datetime import date
from decimal import Decimal, InvalidOperation
import json
import logging
import os
from pathlib import Path
import time
from typing import Any, Callable

import pymysql
import requests


LOGGER = logging.getLogger("stock_fund_flow_backfill")
SOURCE_URL = (
    "https://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php/"
    "MoneyFlow.ssl_qsfx_lscjfb"
)
SOURCE_NAME = "SINA_MONEY_FLOW"
PERCENT_MULTIPLIER = Decimal("100")
RATIO_QUANTIZER = Decimal("0.0001")
BROWSER_USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/140.0.0.0 Safari/537.36"
)


@dataclass(frozen=True)
class Stock:
    """待回补的沪深A股。"""

    code: str
    name: str


class SourceRateLimitedError(RuntimeError):
    """新浪财经明确返回限流或拒绝访问。"""


class RateLimiter:
    """限制串行请求之间的最小时间间隔。"""

    def __init__(self, interval_seconds: float) -> None:
        self.interval_seconds = interval_seconds
        self.next_allowed_at = 0.0

    def wait(self) -> None:
        """等待到下一次允许请求的时刻。"""
        now = time.monotonic()
        remaining = self.next_allowed_at - now
        if remaining > 0:
            time.sleep(remaining)
        self.next_allowed_at = time.monotonic() + self.interval_seconds


def load_local_env(path: Path = Path(".env")) -> None:
    """加载本地未跟踪环境变量，且不覆盖进程已有配置。"""
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
    """解析并校验资金流向回补参数。"""
    parser = argparse.ArgumentParser(description="回补新浪财经最近121个交易日资金流向")
    parser.add_argument("--start-date", help="开始交易日；不传则取最近history-days个交易日")
    parser.add_argument("--end-date", default=date.today().isoformat(), help="结束交易日")
    parser.add_argument("--history-days", type=int, default=121, help="默认回补交易日数量")
    parser.add_argument("--limit-stocks", type=int, default=0, help="仅处理前N只股票，0表示全部")
    parser.add_argument("--request-interval", type=float, default=1.5, help="请求最小间隔秒数")
    parser.add_argument("--max-retries", type=int, default=2)
    parser.add_argument("--retry-backoff", type=float, default=60.0, help="重试退避基数秒数")
    parser.add_argument("--connect-timeout", type=float, default=5.0)
    parser.add_argument("--read-timeout", type=float, default=15.0)
    parser.add_argument("--batch-size", type=int, default=1000)
    parser.add_argument("--progress-every", type=int, default=20)
    parser.add_argument("--cache-dir", default=".cache/stock-fund-flow-backfill-sina")
    parser.add_argument("--db-host", default=os.getenv("DB_HOST", "127.0.0.1"))
    parser.add_argument("--db-port", type=int, default=int(os.getenv("DB_PORT", "3306")))
    parser.add_argument("--db-name", default=os.getenv("DB_NAME", "stock_demo"))
    parser.add_argument("--db-username", default=os.getenv("DB_USERNAME", "root"))
    args = parser.parse_args()

    args.start_date = date.fromisoformat(args.start_date) if args.start_date else None
    args.end_date = date.fromisoformat(args.end_date)
    args.cache_dir = Path(args.cache_dir)
    if args.start_date and args.start_date > args.end_date:
        parser.error("start-date不能晚于end-date")
    if args.history_days < 1 or args.limit_stocks < 0:
        parser.error("history-days必须大于0，limit-stocks不能为负数")
    if args.request_interval < 0 or args.retry_backoff < 0:
        parser.error("请求间隔和重试退避不能为负数")
    if args.max_retries < 0 or args.batch_size < 1 or args.progress_every < 1:
        parser.error("max-retries不能为负数，批量和进度数量必须大于0")
    if args.connect_timeout <= 0 or args.read_timeout <= 0:
        parser.error("连接和读取超时必须大于0")
    if not os.getenv("DB_PASSWORD"):
        parser.error("请通过环境变量DB_PASSWORD提供数据库密码")
    return args


def connect_database(args: argparse.Namespace) -> pymysql.Connection:
    """连接资金流向目标数据库。"""
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


def create_source_session() -> requests.Session:
    """创建不读取系统代理的新浪财经请求会话。"""
    session = requests.Session()
    session.trust_env = False
    return session


def resolve_date_range(
    connection: pymysql.Connection,
    start_date: date | None,
    end_date: date,
    history_days: int,
) -> tuple[date, date]:
    """根据交易日历确定实际回补日期范围。"""
    if start_date is not None:
        return start_date, end_date
    with connection.cursor() as cursor:
        cursor.execute(
            """
            SELECT trade_date
              FROM trade_calendar
             WHERE market_code = 'CN_A'
               AND is_trading_day = 1
               AND trade_date <= %s
             ORDER BY trade_date DESC
             LIMIT %s
            """,
            (end_date, history_days),
        )
        trade_dates = [row["trade_date"] for row in cursor.fetchall()]
    if not trade_dates:
        raise RuntimeError("交易日历中没有可回补日期")
    return min(trade_dates), max(trade_dates)


def load_stocks(connection: pymysql.Connection) -> list[Stock]:
    """读取当前股票清单并保留沪深A股。"""
    with connection.cursor() as cursor:
        cursor.execute("SELECT stock_code, stock_name FROM stock_basic ORDER BY stock_code")
        rows = cursor.fetchall()
    return [
        Stock(row["stock_code"], row["stock_name"])
        for row in rows
        if row["stock_code"].startswith(("0", "3", "6"))
    ]


def build_sina_symbol(stock_code: str) -> str:
    """把沪深股票代码转换为新浪财经证券标识。"""
    if stock_code.startswith("6"):
        return f"sh{stock_code}"
    if stock_code.startswith(("0", "3")):
        return f"sz{stock_code}"
    raise ValueError(f"不支持的沪深A股代码：{stock_code}")


def request_with_retry(
    operation: Callable[[], Any],
    limiter: RateLimiter,
    max_retries: int,
    retry_backoff: float,
    description: str,
) -> Any:
    """有限重试普通网络故障，遇到明确限流时立即停止。"""
    last_error: BaseException | None = None
    for attempt in range(max_retries + 1):
        limiter.wait()
        try:
            return operation()
        except SourceRateLimitedError:
            raise
        except Exception as error:
            last_error = error
            if attempt < max_retries:
                wait_seconds = retry_backoff * (attempt + 1)
                LOGGER.warning(
                    "%s失败，第%d次重试将在%.0f秒后执行：%s",
                    description,
                    attempt + 1,
                    wait_seconds,
                    error,
                )
                time.sleep(wait_seconds)
    assert last_error is not None
    raise last_error


def request_stock_history(
    session: requests.Session,
    stock: Stock,
    timeout: tuple[float, float],
) -> list[dict[str, Any]]:
    """请求一只股票最近可用的日级资金流向。"""
    response = session.get(
        SOURCE_URL,
        params={
            "page": 1,
            "num": 121,
            "sort": "opendate",
            "asc": 0,
            "daima": build_sina_symbol(stock.code),
        },
        headers={
            "Accept": "application/json, text/plain, */*",
            "Accept-Language": "zh-CN,zh;q=0.9",
            "Referer": "https://finance.sina.com.cn/",
            "User-Agent": BROWSER_USER_AGENT,
        },
        timeout=timeout,
    )
    if response.status_code in (403, 429):
        raise SourceRateLimitedError(
            f"新浪财经拒绝或限制访问，status={response.status_code}，stockCode={stock.code}"
        )
    response.raise_for_status()
    payload = response.json()
    validate_payload(payload, stock.code)
    return payload


def validate_payload(payload: list[dict[str, Any]], stock_code: str) -> None:
    """校验新浪财经历史资金流向响应结构。"""
    required_fields = {
        "opendate", "trade", "changeratio", "r0", "r1", "r2", "r3",
        "r0_net", "r1_net", "r2_net", "r3_net",
    }
    if not isinstance(payload, list) or not payload:
        raise RuntimeError(f"新浪财经历史资金流向为空，stockCode={stock_code}")
    if any(not isinstance(item, dict) or not required_fields.issubset(item) for item in payload):
        raise RuntimeError(f"新浪财经历史资金流向字段缺失，stockCode={stock_code}")


def load_or_fetch_payload(
    session: requests.Session,
    stock: Stock,
    cache_file: Path,
    limiter: RateLimiter,
    max_retries: int,
    retry_backoff: float,
    timeout: tuple[float, float],
) -> list[dict[str, Any]]:
    """优先读取已验证缓存，避免续跑时重复请求。"""
    if cache_file.exists():
        payload = json.loads(cache_file.read_text(encoding="utf-8"))
        validate_payload(payload, stock.code)
        return payload

    payload = request_with_retry(
        lambda: request_stock_history(session, stock, timeout),
        limiter,
        max_retries,
        retry_backoff,
        f"新浪财经 {stock.code} 历史资金流向",
    )
    cache_file.parent.mkdir(parents=True, exist_ok=True)
    cache_file.write_text(json.dumps(payload, ensure_ascii=False), encoding="utf-8")
    return payload


def decimal_or_none(value: str) -> Decimal | None:
    """把新浪财经数值转换为Decimal，空值保持NULL。"""
    if value in ("", "-", "--"):
        return None
    try:
        result = Decimal(value)
        return result if result.is_finite() else None
    except (InvalidOperation, ValueError):
        return None


def calculate_ratio(net_amount: Decimal | None, total_amount: Decimal | None) -> Decimal | None:
    """按净流入金额除以总成交额计算百分比。"""
    if net_amount is None or total_amount is None or total_amount == 0:
        return None
    return (net_amount / total_amount * PERCENT_MULTIPLIER).quantize(RATIO_QUANTIZER)


def build_records(
    stock: Stock,
    payload: list[dict[str, Any]],
    start_date: date,
    end_date: date,
) -> list[tuple[Any, ...]]:
    """解析并筛选一只股票指定范围内的历史资金流向。"""
    validate_payload(payload, stock.code)
    records: list[tuple[Any, ...]] = []
    seen_dates: set[date] = set()
    for item in payload:
        trade_date = date.fromisoformat(item["opendate"])
        if trade_date < start_date or trade_date > end_date:
            continue
        if trade_date in seen_dates:
            raise RuntimeError(f"新浪财经返回重复交易日，stockCode={stock.code}，tradeDate={trade_date}")
        seen_dates.add(trade_date)

        category_amounts = [decimal_or_none(item[field]) for field in ("r0", "r1", "r2", "r3")]
        total_amount = sum(category_amounts, Decimal("0")) if all(
            amount is not None for amount in category_amounts
        ) else None
        super_large_amount = decimal_or_none(item["r0_net"])
        large_amount = decimal_or_none(item["r1_net"])
        main_amount = (
            super_large_amount + large_amount
            if super_large_amount is not None and large_amount is not None
            else None
        )
        main_ratio = calculate_ratio(main_amount, total_amount)
        super_large_ratio = calculate_ratio(super_large_amount, total_amount)
        large_ratio = calculate_ratio(large_amount, total_amount)
        close_price = decimal_or_none(item["trade"])
        change_ratio = decimal_or_none(item["changeratio"])
        change_percent = (
            (change_ratio * PERCENT_MULTIPLIER).quantize(RATIO_QUANTIZER)
            if change_ratio is not None
            else None
        )
        complete = all(
            value is not None
            for value in (
                close_price,
                change_percent,
                main_amount,
                main_ratio,
                super_large_amount,
                super_large_ratio,
                large_amount,
                large_ratio,
            )
        )
        records.append(
            (
                stock.code,
                stock.name,
                trade_date,
                close_price,
                change_percent,
                main_amount,
                main_ratio,
                super_large_amount,
                super_large_ratio,
                large_amount,
                large_ratio,
                SOURCE_NAME,
                "COMPLETE" if complete else "PARTIAL",
            )
        )
    return records


UPSERT_SQL = """
    INSERT INTO stock_fund_flow (
        stock_code, stock_name, trade_date, latest_price, change_percent,
        main_net_inflow_yuan, main_net_inflow_ratio,
        super_large_net_inflow_yuan, super_large_net_inflow_ratio,
        large_net_inflow_yuan, large_net_inflow_ratio,
        data_source, data_status, collected_at
    ) VALUES (
        %s, %s, %s, %s, %s,
        %s, %s, %s, %s, %s, %s,
        %s, %s, NOW()
    )
    ON DUPLICATE KEY UPDATE
        stock_name=IF(data_source='EAST_MONEY', stock_name, VALUES(stock_name)),
        latest_price=IF(data_source='EAST_MONEY', latest_price, VALUES(latest_price)),
        change_percent=IF(data_source='EAST_MONEY', change_percent, VALUES(change_percent)),
        main_net_inflow_yuan=IF(data_source='EAST_MONEY', main_net_inflow_yuan, VALUES(main_net_inflow_yuan)),
        main_net_inflow_ratio=IF(data_source='EAST_MONEY', main_net_inflow_ratio, VALUES(main_net_inflow_ratio)),
        super_large_net_inflow_yuan=IF(data_source='EAST_MONEY', super_large_net_inflow_yuan, VALUES(super_large_net_inflow_yuan)),
        super_large_net_inflow_ratio=IF(data_source='EAST_MONEY', super_large_net_inflow_ratio, VALUES(super_large_net_inflow_ratio)),
        large_net_inflow_yuan=IF(data_source='EAST_MONEY', large_net_inflow_yuan, VALUES(large_net_inflow_yuan)),
        large_net_inflow_ratio=IF(data_source='EAST_MONEY', large_net_inflow_ratio, VALUES(large_net_inflow_ratio)),
        data_status=IF(data_source='EAST_MONEY', data_status, VALUES(data_status)),
        collected_at=IF(data_source='EAST_MONEY', collected_at, VALUES(collected_at)),
        data_source=IF(data_source='EAST_MONEY', data_source, VALUES(data_source)),
        updated_at=CURRENT_TIMESTAMP
"""


def save_records(connection: pymysql.Connection, records: list[tuple[Any, ...]]) -> None:
    """批量幂等保存资金流向，失败时回滚当前批次。"""
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
    """执行最近121个交易日的沪深A股资金流向回补。"""
    load_local_env()
    args = parse_args()
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    connection = connect_database(args)
    session = create_source_session()
    limiter = RateLimiter(args.request_interval)

    try:
        start_date, end_date = resolve_date_range(
            connection,
            args.start_date,
            args.end_date,
            args.history_days,
        )
        stocks = load_stocks(connection)
        if args.limit_stocks:
            stocks = stocks[: args.limit_stocks]
        if not stocks:
            raise RuntimeError("stock_basic中没有沪深A股数据")

        cache_root = args.cache_dir / end_date.isoformat()
        LOGGER.info(
            "开始回补资金流向：股票=%d，范围=%s至%s，请求间隔=%.1f秒",
            len(stocks),
            start_date,
            end_date,
            args.request_interval,
        )
        pending_records: list[tuple[Any, ...]] = []
        total_saved = 0
        for index, stock in enumerate(stocks, start=1):
            payload = load_or_fetch_payload(
                session,
                stock,
                cache_root / f"{stock.code}.json",
                limiter,
                args.max_retries,
                args.retry_backoff,
                (args.connect_timeout, args.read_timeout),
            )
            pending_records.extend(build_records(stock, payload, start_date, end_date))
            while len(pending_records) >= args.batch_size:
                batch = pending_records[: args.batch_size]
                save_records(connection, batch)
                total_saved += len(batch)
                del pending_records[: args.batch_size]
            if index % args.progress_every == 0 or index == len(stocks):
                LOGGER.info(
                    "[%d/%d] 已处理至%s，累计写入=%d，待写入=%d",
                    index,
                    len(stocks),
                    stock.code,
                    total_saved,
                    len(pending_records),
                )

        save_records(connection, pending_records)
        total_saved += len(pending_records)
        LOGGER.info("资金流向回补完成：股票=%d，累计写入=%d", len(stocks), total_saved)
        return 0
    except KeyboardInterrupt:
        LOGGER.warning("任务已中断；已缓存响应和已提交批次可以安全续跑")
        return 130
    except SourceRateLimitedError:
        LOGGER.exception("检测到新浪财经限流，任务立即停止，稍后可从缓存续跑")
        return 2
    except Exception:
        LOGGER.exception("历史资金流向回补失败")
        return 1
    finally:
        session.close()
        connection.close()


if __name__ == "__main__":
    raise SystemExit(main())
