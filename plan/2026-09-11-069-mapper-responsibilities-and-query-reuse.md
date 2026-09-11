# 整理板块Mapper职责与重复行情查询

## 修改

- StockPlateMapper仅维护stock_plate，新增StockPlateMemberMapper维护stock_plate_member，迁移成员写入、失效、计数和查询共4个方法。
- StockCustomPlateMapper仅维护stock_custom_plate，新增StockCustomPlateMemberMapper维护stock_custom_plate_member，迁移成员计数、分页、失效、写入和关系查询共5个方法。
- 成员查询所需的板块或股票基础信息关联保留在成员Mapper中，跨表业务与事务继续由现有Service编排。
- 将selectTechnologyStockQuotes和selectQuotes统一为StockDailyQuoteMapper.selectByStockCodesAndTradeDates，参数统一stockCodes、tradeDates，返回StockDailyQuote。保留原股票代码、日期筛选和代码日期升序排序。
- 科技股排名、成交额异动及平台突破复用上述查询。计算组件及测试统一使用日线实体，删除不再使用的TechnologyStockQuoteDto，不改变对外VO或公式。
- 独立保留全市场收盘价、沪深成交额、指定成员成交额及含卖一量的板块行情查询，这些查询具有不同的范围或字段要求。
- IndexMinuteQuoteMapper合并单条及批量写入；实时采集以单元素列表调用upsertBatch，分钟恢复仍使用同一批量方法，删除重复upsert SQL和方法。
- 同步调用方、构造器注入、MVC测试Mock与相关集成测试。数据库表和迁移脚本不变。

## 验证

- 相关测试85项通过。
- 加强日线Mapper集成测试，验证统一查询返回开高低、完整状态、最早日期，以及股票代码过滤和交易日期升序。
- 对比Git基线中的XML，9条迁移成员SQL的结构、参数、筛选、写入规则均一致。
- src中无旧DTO、旧日线查询方法残留，git diff --check通过。
- 完整Maven回归305项通过，失败0、错误0、跳过0；数据库验证沿用项目H2测试环境，本次未修改真实数据库。
