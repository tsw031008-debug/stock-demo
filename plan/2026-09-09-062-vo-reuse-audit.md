# 相同结果直接使用VO

## 规范与本次修改

- 用户明确要求：同一业务结果字段相同，不再为了分层机械复制DTO到VO。
- AGENTS.md已明确：字段、类型、业务含义一致时，Service及计算组件直接构造VO；有内部字段、脱敏、单位或结构变化才保留独立DTO。数据库实体不得直接作为公共API响应。
- 功能12和13共用排名明细，因此本次同步修改两者计算组件、Service接口及实现，直接使用已有响应VO。
- 删除TechnologyStockRankDto、TechnologyStockRankingDto、TechnologyStockTurnoverDto以及Controller的toRankResponses；Controller直接封装Service结果。
- 保留TechnologyStockQuoteDto：它是查询原始行情，包含成交额、涨幅、收盘价等计算字段，与仅返回排名和股票信息的VO不是同一结构。
- 不改变JSON字段、日期格式、筛选阈值、候选范围、统计日期、排名顺序；保留用户此前删除的日期降序校验修改。

## 其他模块检查结果 仅记录未修改

扫描DTO与同名RespVo的字段定义，并核对Controller中的映射，发现以下同形复制候选：

| 模块 | 重复对象或转换 |
| --- | --- |
| 市场水位 | MarketLevelDto → MarketLevelRespVo |
| 周月水位 | MarketPeriodItemDto → MarketPeriodItemRespVo及外层列表包装 |
| 功能11股票家数 | RecentStockRiseCountDto → RecentStockRiseCountRespVo |
| 指数ETF | IndexEtfChangeDto → IndexEtfChangeRespVo及外层包装 |
| 指数风格 | IndexDailyStyleDto → IndexDailyStyleRespVo |
| 指数背离 | IndexMinuteCurvePointDto、IndexDivergenceSignalDto → 对应RespVo |
| 四大类水位 | StockCategoryTurnoverItemDto → StockCategoryTurnoverItemRespVo |
| 自定义板块 | StockCustomCategoryDto、StockCustomPlateDto、StockCustomPlateMemberDto → 对应RespVo |
| 资金流向 | StockFundFlowDto → StockFundFlowRespVo |
| 开板提醒 | StockOpenBoardAlertDto → StockOpenBoardAlertRespVo |
| 板块涨停 | StockPlateLimitUpDto → StockPlateLimitUpRespVo |
| 公共分页 | PageDto与PageRespVo字段同形，但关联多模块及不同记录类型，需要单独评估整体影响 |

并非所有转换都应删除：涨速预警DTO包含不对外暴露的slopeDifference；指数风格外层把LocalDate列表格式化为字符串列表，存在实际类型变化。不能仅凭字段名相似合并。

上述其他模块此次仅审查，未跨模块批量重构；后续处理需核对各DTO内部用途、VO序列化注解及原有测试。

## 验证

- 功能12/13计算、Service与Controller相关27项测试通过。
- 完整运行 `mvnw.cmd test`：268项通过，失败0、错误0、跳过0。
