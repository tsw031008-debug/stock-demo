# 简化平台突破任务锁与事务

- 按用户要求，synchronized移到PlatformBreakoutTask.selectOnSchedule定时入口。
- Service.selectStocks恢复@Transactional，直接执行原有计算和保存，删除TransactionTemplate、事务管理器注入和calculateAndSave包装方法。
- 定时任务通过Spring代理调用Service，Service事务提交或回滚后返回，任务入口随后释放Java锁。
- 锁仅保护当前单机定时入口；直接调用Service不再具有Java串行保护。当前未提供其他生产选股触发入口。
- 保留查询事务、选股条件和结果保存逻辑，不修改数据库迁移。
- 新增任务入口并发与异常后释放锁测试，Service保留事务回滚及失败后重新执行的集成测试。
- 相关测试19项通过，完整Maven回归305项通过，失败0、错误0、跳过0；git diff --check通过。
