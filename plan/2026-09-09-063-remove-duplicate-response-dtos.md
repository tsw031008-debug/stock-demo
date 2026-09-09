# 合并其他模块同形结果对象

## 范围与规则

用户授权在不影响功能的前提下调整062记录中的重复转换。逐一比较DTO与VO的字段名、类型和用途，仅合并同一业务结果；不改变业务公式、筛选条件、统计日期、排序、分页边界或数据源。

## 修改

- 删除20份重复DTO，使用现有VO及其JSON日期格式、Schema注解。
- 涉及市场水位、周月对比、近期股票家数、ETF、指数风格明细、指数背离、四大类水位、自定义板块、资金流向、开板提醒、板块涨停及分页包装。
- 同步Service、计算组件、Mapper接口和测试类型。资金流向与自定义板块成员的MyBatis XML只更改resultType，不改SQL字段或查询条件。
- Controller移除同形逐字段复制与列表映射，直接返回结果。
- 分页统一PageRespVo，分页算法未改。涨速预警仍需将内部记录转换为公开记录，因此保留这一处分页记录转换。

删除的DTO：

IndexDailyStyleDto、IndexDivergenceSignalDto、IndexMinuteCurvePointDto、IndexEtfChangeDto、IndexEtfComparisonDto、MarketLevelDto、MarketPeriodItemDto、MarketPeriodComparisonDto、RecentStockRiseCountDto、StockCategoryTurnoverItemDto、StockCategoryTurnoverComparisonDto、StockCustomCategoryDto、StockCustomPlateDto、StockCustomPlateMemberDto、StockFundFlowDto、StockOpenBoardAlertDto、StockPlateLimitUpDto、IndexStyleItemDto、PageDto、IndexDivergenceOverviewDto。

这些类型的字段保留在对应VO中；本次无数据库删除或数据迁移。

## 明确保留

- StockSpeedAlertDto：包含不应对外返回的slopeDifference，继续保留DTO至VO转换。
- IndexStyleComparisonDto：内部tradeDates是LocalDate列表，接口为格式化字符串列表；仅明细直接复用VO，外层日期转换保留。
- 原始行情、请求DTO、实体至VO转换：有独立用途，不进行机械合并。
- 功能12和13的已确认业务规则及用户此前手工修改保持不变。

## 验证

- 已检查src中删除类型无残留引用。
- 原有计算、Service、Controller和Mapper集成测试同步使用VO，保留原断言。
- 加强周月水位的起止日期、交易日数、可用标识以及背离信号时间的响应断言。
- 最终完整运行 `mvnw.cmd test`：268项通过，失败0、错误0、跳过0。
