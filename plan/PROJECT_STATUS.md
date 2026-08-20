# 项目当前进度

最后更新：2026-08-20

## 当前阶段

项目处于第一阶段基础能力建设，采用 Spring Boot 模块化单体架构。当前已完成交易日历初始化链路以及股票基础信息表、股票日行情表设计，下一步可实现股票清单采集和存储链路。

## 已完成

- 完成 Spring Boot 3.0.2、JDK 17、MyBatis-Plus、MySQL、Redis 等基础配置。
- 明确所有 SQL 写在 MyBatis XML 中，不使用 SQL 注解。
- 完成国家节假日读取和指定日期范围的交易日历生成。
- 完成交易日历分批保存、重复范围校验和 Service 事务控制。
- 提供交易日历初始化接口 `POST /api/tradeCalendar/initialize`。
- 提供指定日期交易日判断接口 `GET /api/tradeCalendar/tradingDay`。
- 支持查询指定日期之前或之后的第 N 个交易日。
- 提供前第 N 个和后第 N 个交易日查询接口。
- 引入统一接口响应 `Result<T>` 和响应码常量。
- 完成参数错误、重复初始化和系统异常的统一处理。
- Flyway 默认关闭，当前数据库表结构采用手动维护方式。
- Redis 健康检查支持通过配置开关控制。
- 建立根目录 `plan` 进度文档机制，并写入 `AGENTS.md` 工作流程。
- 新增 `stock_basic` 股票基础信息表，支持按股票代码保存最新沪深京A股清单。
- 新增 `stock_daily_quote` 股票日行情表，支持按股票代码和交易日期幂等保存行情。
- 确认腾讯行情字段33/41均为当日最高价，字段34/42均为当日最低价，表中不重复存储。

## 当前接口

### 初始化交易日历

```text
POST /api/tradeCalendar/initialize
```

请求体：

```json
{
  "startDate": "2017-01-01",
  "endDate": "2026-12-31"
}
```

成功响应中的 `code` 为 `200`，操作错误为 `1000`，系统错误为 `500`。

### 判断是否为交易日

```text
GET /api/tradeCalendar/tradingDay?date=2023-06-26
```

返回查询日期以及 `tradingDay` 判断结果。

### 查询前第 N 个交易日

```text
GET /api/tradeCalendar/previousTradingDay?date=2023-06-26&offset=2
```

### 查询后第 N 个交易日

```text
GET /api/tradeCalendar/nextTradingDay?date=2023-06-26&offset=2
```

## 验证状态

- Maven 完整测试已通过。
- 当前测试数量：17。
- 交易日历初始化接口已验证可生成并保存 3652 条自然日记录。

## 已知限制

- 当前交易日判断以国家节假日和周末为基础，尚未处理交易所临时休市。
- 当前初始化策略不支持对已有日期范围进行覆盖更新。
- Flyway 已引入但默认关闭，表结构暂时由人工维护。
- 股票表迁移脚本已经创建，但尚未连接本地 MySQL 实际执行。
- 权限认证模块尚未实现，`401` 和 `403` 响应码目前仅完成常量约定。
- 国务院节假日通知没有稳定的官方 JSON API，节假日数据仍需按年度人工维护。

## 建议下一步

1. 为交易日查询补充数据库集成测试。
2. 实现每日股票清单采集、完整性校验和批量保存。

## 最近推进记录

- [2026-08-20-005-stock-tables.md](2026-08-20-005-stock-tables.md)
- [2026-08-20-004-api-path-camel-case.md](2026-08-20-004-api-path-camel-case.md)
- [2026-08-20-003-trading-day-offset-api.md](2026-08-20-003-trading-day-offset-api.md)
- [2026-08-20-002-trading-day-query.md](2026-08-20-002-trading-day-query.md)
- [2026-08-20-001-project-baseline.md](2026-08-20-001-project-baseline.md)
