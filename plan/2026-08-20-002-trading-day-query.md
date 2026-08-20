# 交易日查询功能

日期：2026-08-20  
状态：已完成

## 本次目标

提供指定日期的交易日判断接口，并验证指定日期之前第 N 个交易日的查询能力。

## 完成内容

- 新增 `GET /api/trade-calendar/trading-day` 接口。
- 接口接收 `date` 参数，返回查询日期和是否为交易日。
- 交易日判断以 `trade_calendar` 表为最终依据，该表初始化规则为非国家节假日且非周六、周日。
- 保留 Service 中前第 N 个和后第 N 个交易日查询方法。
- 验证 `2023-06-26` 之前第 2 个交易日为 `2023-06-20`。

## 节假日数据来源

- 国务院每年发布节假日安排通知，但未发现稳定的官方 JSON API。
- 当前继续人工维护 `national_holiday` 表，历史数据和当年数据使用国务院通知核对。
- 项目已有的节假日种子数据覆盖 2017—2026 年，并保存对应官方来源 URL。
- 不接入第三方节假日 API，避免核心交易日判断依赖不稳定的外部服务。

## 涉及文件

- `src/main/java/cn/djct/stockdemo/controller/TradeCalendarController.java`
- `src/main/java/cn/djct/stockdemo/pojo/vo/TradingDayRespVo.java`
- `src/test/java/cn/djct/stockdemo/controller/TradeCalendarControllerTest.java`
- `src/test/java/cn/djct/stockdemo/service/TradeCalendarServiceTest.java`

## 验证结果

- Maven 完整测试通过。
- 共执行 14 个测试，失败 0，错误 0。

## 后续维护

每年国务院发布下一年度节假日安排后，补充 `national_holiday` 数据，再初始化对应年度的 `trade_calendar` 数据。

