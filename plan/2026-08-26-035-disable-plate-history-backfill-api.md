# 暂停板块历史日线回补接口

## 原因

功能5当天板块涨停统计不读取历史板块日线，日常板块日线已经由交易日15:03任务增量生成。当前历史回补使用现有板块成分关系回算，存在成分股前视偏差；现有upsert还可能把 `COMPLETE` 正式日线覆盖为 `PARTIAL`。

## 调整

- 暂时注释 `POST /api/plate/backfillDailyQuotes` 的Controller映射和方法，不再对外注册，也不再出现在OpenAPI接口中。
- 保留Service中的回补实现，后续明确需要历史板块日线并增加 `COMPLETE` 数据保护后再恢复。
- 保留 `POST /api/plate/synchronizeDailyQuote` 和交易日15:03自动日线任务，不影响从当前交易日开始记录板块日线。

## 验证

- Controller测试确认回补路径返回404，且没有调用板块日线Service。
- 当天板块涨停查询、板块成分同步和当天板块日线同步接口保持不变。
- Maven完整测试通过：133个测试，0失败、0错误、0跳过。
- `git diff --check` 通过。
