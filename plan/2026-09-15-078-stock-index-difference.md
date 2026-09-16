# 功能15 个股与沪深300差异分布

日期：2026-09-15

## 需求及口径

- GET /api/indexStyle/stockDifference?tradeDate=2026-09-14，日期必填，返回Result包装的统计结果。
- T为指定交易日，P通过交易日历获取前一交易日；当天15:00前拒绝查询，未来日期及非交易日拒绝查询。
- 指数使用000300沪深300的T、P两日收盘价，不使用ETF或实时行情；股票沿用未复权日线收盘价。
- 差异=股票T收盘价/股票P收盘价-指数T收盘价/指数P收盘价。
- 强势严格大于1%、3%、5%、7%，弱势严格小于-1%、-3%、-5%、-7%；累计数量允许重叠，等于阈值不计入对应项。
- 按用户确认的文档样图，饼图占比为本项数量/同侧四项累计数量之和×100；保留2位小数，分母为0返回null。不是互斥区间占比。
- 以T日已落库股票为候选池，不额外过滤ST、北交所或停牌股；两天均有正收盘价即参与。
- 单只股票缺失或无效价格跳过；totalStockCount、validStockCount、skippedStockCount公开实际处理数量。totalStockCount不证明全市场完整性。
- 股票任一整日无记录或指数任一日收盘价无效，提示无法计算，不回退日期。

## 实现

- 复用StockDailyQuoteMapper.selectClosePricesByTradeDates和IndexDailyQuoteMapper.selectByTradeDatesAndCodes，只读取两日指定数据。
- Service校验日期并编排查询，独立计算组件使用BigDecimal交叉相乘判断边界，避免除法精度误判。
- 新增请求DTO和响应VO，Service直接返回VO；同步维护OpenAPI说明。
- 不新增表、SQL、统计定时任务或外部网络请求，不恢复已取消的沪深300独立查询接口。

## 返回说明

- tradeDate、previousTradeDate：格式yyyy-MM-dd。
- indexCode：000300。
- totalStockCount、validStockCount、skippedStockCount：当日候选、有效、跳过数量。
- strong、weak：各四项，thresholdPercent为有符号百分点，count为累计数量，piePercent为饼图百分数。
- 因各项独立保留两位小数，占比总和可能存在0.01个百分点左右的舍入差异。

## 验证

定向30项通过；`mvnw.cmd test`全量379项通过，失败0、错误0、跳过0。覆盖严格阈值、累计重复、文档饼图分母、空分母、缺失或无效价格、日期参数及跨周末/跨月/跨年日历取数。未连接真实数据库验证数据覆盖；查询历史日期需已有沪深300两日收盘行情，本次不回补数据。
