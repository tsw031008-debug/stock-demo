# 按职责拆分选股Mapper

- 按用户确认的方案，移除混合承担完成记录、选股结果与日线查询的LeftSideStockMapper及对应XML。
- StockSelectionRunMapper负责isCompleted、completeRun，只访问stock_selection_run。
- StockSelectionResultMapper负责deleteResults、insertResults、countResults、selectPage。分页展示需要的当日日线左关联保留在该Mapper。
- selectQuotes、selectFirstQuoteDate及原SQL移回StockDailyQuoteMapper，负责独立股票日线查询。
- PlatformBreakoutServiceImpl明确注入并调用三种Mapper，事务边界、任务锁、指标规则、SQL筛选与排序、分页返回保持不变。
- 同步集成测试和MVC测试Mapper注入。没有新建数据表或修改迁移脚本。
- 清理旧编译产物后相关测试27项通过，完整Maven回归305项通过，失败0、错误0、跳过0；src中无旧Mapper引用，git diff --check通过。
