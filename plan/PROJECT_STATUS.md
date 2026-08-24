# 项目当前进度

最后更新：2026-08-24

## 当前阶段

项目处于第一阶段基础能力建设，采用 Spring Boot 模块化单体架构。当前已完成交易日历、股票基础信息、股票日行情、近两年历史日线回补、东方财富沪深A股资金流向每日同步链路，以及市场水位查询接口；下一步进行真实运行验证。

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
- 新增Tushare日线与ZZShare估值离线回补脚本，按交易日获取全市场数据，不逐只股票请求。
- 历史回补支持限频、有限重试、按日原始数据缓存、批量幂等写入和腾讯记录保护。
- 历史盘口字段保持NULL，成交额和市值统一换算为元，回补记录标记为 `PARTIAL`。
- 已完成2024-08-20至2026-08-20共485个交易日的真实历史回补，数据库最终写入2,617,326条近两年行情。
- 近两年交易日覆盖完整，行情核心字段缺失0条，重复记录0条，原有2026-08-20腾讯完整行情5,547条保持不变。
- 股票基础信息和日行情采集的最小股票数量统一固定为3000，不再从配置文件或环境变量读取。
- 新增 `stock_fund_flow` 沪深A股每日资金流向表，已在本地MySQL完成建表。
- 完成东方财富资金流向字段解析，保存最新价、涨跌幅、主力、超大单和大单净流入金额及占比。
- 资金流向按每页100条、1秒间隔串行获取；分页或最终数量不完整时失败且不写库，第三方空值保存为NULL并标记为 `PARTIAL`。
- 资金流向使用固定完整Chrome请求头和正确Referer，失败后等待30秒才进行第二次尝试，不随机切换User-Agent。
- 资金流向支持交易日15:05执行，以及15:05之后启动应用时补采；当日数据完整时不会重复请求。
- 启动补采按股票基础信息、股票日行情、股票资金流向的顺序执行。
- 提供资金流向手动同步接口 `POST /api/stockFundFlow/synchronize`。
- 完成试用期文档“市场水位”业务分析，明确实时成交额来源、历史聚合范围、计算公式、最小实现边界和待确认口径。
- 完成市场水位查询接口，支持腾讯实时两市成交额、历史前 3/5 个交易日聚合、市场风格和量比计算。
- 市场水位风格阈值为小于8000亿元、8000至10000亿元（过渡期，包含边界）、大于10000亿元。
- 市场水位接口金额和量比统一保留2位小数，响应只包含需求明确的4个字段。
- 市场水位历史成交额SQL仅查询交易日和原始成交额，按日求和、记录计数及完整性统计已移至Service。
- 市场水位真实接口已验证通过，Controller、交易日历、历史行情、Service汇总、腾讯实时行情和计算组件完整链路运行正常。
- Controller和Service已按交易日历、股票基础信息、股票日行情、股票资金流向、市场水位5个业务模块拆分子包，测试包同步镜像调整。
- 市场水位新增方法已补充Javadoc、参数返回说明和关键业务步骤注释。
- 已完成试用期文档功能3“各种”的业务分析，已确认涨幅使用`T-1/T-2`、共享60秒实时快照、交易时段提示、当前涨幅排序、卖一量筛选及两个接口的最小返回字段；剩余分页精度和股票范围待确认。

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

### 手动同步股票资金流向

```text
POST /api/stockFundFlow/synchronize
```

接口使用上海时区当天日期。非交易日或当天沪深A股资金流向已经完整同步时，`savedCount` 返回0且不会请求东方财富。

### 查询最新市场水位

```text
GET /api/marketLevel/latest
```

当前成交额读取腾讯上证指数和深证综指实时成交额；历史均值聚合日线表中代码以0、3、6开头的沪深A股。返回市场风格、前3个交易日成交额均值、当前成交额和量比，成交额单位为亿元。

## 验证状态

- Maven 完整测试已通过。
- 当前测试数量：94。
- 市场水位计算、腾讯解析、Service历史汇总、统计日选择和接口相关测试22个全部通过。
- 2026-08-24真实调用市场水位接口成功：当前成交额20074.56亿元、前3日均值21559.74亿元、偏机构风格、量比0.89。
- Controller和Service模块包调整后已执行Maven clean及完整测试，Spring扫描、导包和原有接口均验证通过。
- 历史回补Python固定样本测试4个全部通过。
- 交易日历初始化接口已验证可生成并保存 3652 条自然日记录。
- 数据库预检确认当前股票5547只、近两年交易日485个、行情表已有2026-08-20腾讯数据5547条。
- ZZShare匿名受控请求2026-08-20估值成功，返回5211条。
- 近两年真实回补已完成：485个交易日、2,617,326条行情，日期范围2024-08-20至2026-08-20。
- 每个交易日5,286至5,547条，平均5,396.55条；交易日缺口0、行情核心字段缺失0、唯一键重复0。
- 数据来源分布：TUSHARE_ZZSHARE/PARTIAL 2,476,799条，TUSHARE/PARTIAL 134,980条，TENCENT/COMPLETE 5,547条。
- 东方财富资金流向字段映射和分页固定样本验证通过，开发测试未请求真实网站。
- 本地MySQL `stock_fund_flow` 建表完成：17列、唯一键正确、当前0条记录。
- 完整浏览器请求头的东方财富单页受控验证成功：`rc=0`、总数5552、第一页100条；未继续访问后续分页。
- 东方财富 `ulist.np/get` 已进行一次3只沪深A股的受控验证，请求仍被服务器提前断开；未重试、未写库，临时改造已撤销，回退后相关测试11个通过。

## 已知限制

- 当前交易日判断以国家节假日和周末为基础，尚未处理交易所临时休市。
- 当前初始化策略不支持对已有日期范围进行覆盖更新。
- Flyway 已引入但默认关闭，表结构暂时由人工维护。
- 2026-08-20新浪受控实测确认单页最多返回100条，分页修复后已完成5547只股票基础信息真实入库。
- 腾讯股票日行情同步已保留2026-08-20真实完整行情5,547条；日常定时增量链路仍需持续运行验证。
- ZZShare不覆盖北交所历史估值，北交所134,865条历史记录的换手率、PE、PB、总市值和流通市值保持NULL。
- `302132`（中航成飞）在2024-08-20至2025-02-14的115条记录未匹配到ZZShare估值，从2025-02-17开始正常。
- 历史日线无法恢复买一、卖一和精确行情采集时间，这些字段保持NULL。
- 权限认证模块尚未实现，`401` 和 `403` 响应码目前仅完成常量约定。
- 国务院节假日通知没有稳定的官方 JSON API，节假日数据仍需按年度人工维护。
- 2026-08-21 15:05资金流向首次任务因东方财富单页100条限制被完整性校验终止，数据库未写入残缺数据；分页修复后需重启应用再补采验证。
- 2026-08-21 15:11启动补采的首个请求被服务器提前断开；改用完整浏览器请求头后单页受控请求成功，但全量56页稳定性仍待验证。
- 按股票代码批量查询的 `ulist.np/get` 在当前网络环境下同样发生响应提前断开，不能作为无需代理的直接替代方案。

## 建议下一步

1. 确认功能3剩余的排序字段、开板定义、卖一量、分页与小数位、非交易时段提示和股票范围。
2. 在当日股票基础信息就绪后，验证15:05东方财富资金流向任务并核对入库完整性。
3. 验证下一个交易日15:02腾讯日行情增量任务，并核对与历史数据的幂等衔接。

## 最近推进记录

- [2026-08-24-023-feature-3-alerts-business-analysis.md](2026-08-24-023-feature-3-alerts-business-analysis.md)
- [2026-08-24-022-market-level-live-verification.md](2026-08-24-022-market-level-live-verification.md)
- [2026-08-24-021-market-level-service-aggregation.md](2026-08-24-021-market-level-service-aggregation.md)
- [2026-08-24-020-module-package-refactor.md](2026-08-24-020-module-package-refactor.md)
- [2026-08-24-019-market-level-implementation.md](2026-08-24-019-market-level-implementation.md)
- [2026-08-24-018-market-level-business-analysis.md](2026-08-24-018-market-level-business-analysis.md)
- [2026-08-21-017-eastmoney-ulist-probe.md](2026-08-21-017-eastmoney-ulist-probe.md)
- [2026-08-21-016-eastmoney-browser-header.md](2026-08-21-016-eastmoney-browser-header.md)
- [2026-08-21-015-stock-fund-flow-pagination-fix.md](2026-08-21-015-stock-fund-flow-pagination-fix.md)
- [2026-08-21-014-stock-fund-flow-sync.md](2026-08-21-014-stock-fund-flow-sync.md)
- [2026-08-21-013-fixed-minimum-stock-count.md](2026-08-21-013-fixed-minimum-stock-count.md)
- [2026-08-21-012-stock-history-backfill.md](2026-08-21-012-stock-history-backfill.md)
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
