# 项目当前进度

最后更新：2026-08-20

## 当前阶段

项目处于第一阶段基础能力建设，采用 Spring Boot 模块化单体架构。当前已完成交易日历、股票表结构、新浪股票基础信息同步和腾讯股票日行情同步链路，下一步需要受控验证腾讯真实行情，再进行近两年历史日线回补。

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
- 两张股票表已经在本地MySQL创建，应用可正常查询 `stock_basic`。
- 确认腾讯行情字段33/41均为当日最高价，字段34/42均为当日最低价，表中不重复存储。
- 完成新浪 `hs_a` 全部A股代码、名称全量拉取及批量upsert链路，不做二次市场过滤。
- 新浪股票清单按每页100条串行获取，默认请求间隔1秒，任意分页不完整时失败且不写库。
- 股票基础信息同步支持交易日09:10执行，以及09:10之后启动应用时补采。
- 同一天已经同步时不再请求外部数据源，响应不完整时不写库。
- 提供股票基础信息手动同步接口 `POST /api/stockBasic/synchronize`，与定时任务复用同一同步逻辑。
- 2026-08-20已通过新浪真实接口完成5547只股票基础信息同步。
- 完成腾讯沪深京A股日行情解析，每批最多200只并按1秒间隔串行请求。
- 完成腾讯成交额、市值单位转换，以及重复成交量、最高价、最低价字段一致性校验。
- 股票日行情在全部采集成功后按500条分批事务upsert，支持同日幂等和残缺快照修复。
- 股票日行情支持交易日15:02执行，以及15:02之后启动应用时补采。
- 提供股票日行情手动同步接口 `POST /api/stockDailyQuote/synchronize`。

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

### 手动同步股票基础信息

```text
POST /api/stockBasic/synchronize
```

接口使用上海时区当天日期。非交易日或当天已经同步时，`savedCount` 返回0且不会请求新浪。

### 手动同步股票日行情

```text
POST /api/stockDailyQuote/synchronize
```

接口使用上海时区当天日期。非交易日或当天已经完整同步时，`savedCount` 返回0且不会请求腾讯。

## 验证状态

- Maven 完整测试已通过。
- 当前测试数量：53。
- 交易日历初始化接口已验证可生成并保存 3652 条自然日记录。

## 已知限制

- 当前交易日判断以国家节假日和周末为基础，尚未处理交易所临时休市。
- 当前初始化策略不支持对已有日期范围进行覆盖更新。
- Flyway 已引入但默认关闭，表结构暂时由人工维护。
- 2026-08-20新浪受控实测确认单页最多返回100条，分页修复后已完成5547只股票基础信息真实入库。
- 腾讯股票日行情同步尚未执行真实接口验证。
- 近两年股票历史日线尚未回补。
- 权限认证模块尚未实现，`401` 和 `403` 响应码目前仅完成常量约定。
- 国务院节假日通知没有稳定的官方 JSON API，节假日数据仍需按年度人工维护。

## 建议下一步

1. 为交易日查询补充数据库集成测试。
2. 受控验证腾讯真实行情采集、字段解析和首次落库。
3. 设计并执行近两年股票历史日线离线回补。

## 最近推进记录

- [2026-08-20-011-tencent-daily-quote-sync.md](2026-08-20-011-tencent-daily-quote-sync.md)
- [2026-08-20-010-sina-request-interval.md](2026-08-20-010-sina-request-interval.md)
- [2026-08-20-009-sina-stock-basic-pagination-fix.md](2026-08-20-009-sina-stock-basic-pagination-fix.md)
- [2026-08-20-008-stock-basic-manual-sync-api.md](2026-08-20-008-stock-basic-manual-sync-api.md)
- [2026-08-20-007-sina-stock-basic-source.md](2026-08-20-007-sina-stock-basic-source.md)
- [2026-08-20-006-stock-basic-sync.md](2026-08-20-006-stock-basic-sync.md)
- [2026-08-20-005-stock-tables.md](2026-08-20-005-stock-tables.md)
- [2026-08-20-004-api-path-camel-case.md](2026-08-20-004-api-path-camel-case.md)
- [2026-08-20-003-trading-day-offset-api.md](2026-08-20-003-trading-day-offset-api.md)
- [2026-08-20-002-trading-day-query.md](2026-08-20-002-trading-day-query.md)
- [2026-08-20-001-project-baseline.md](2026-08-20-001-project-baseline.md)
