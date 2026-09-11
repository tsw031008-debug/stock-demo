# 简化平台突破查询传参

- 按用户要求，平台突破查询改为直接接收tradeDate、pageNum、pageSize，与当前控制器其他查询接口保持一致。
- 删除LeftSideStockQueryDto、ModelAttribute、ParameterObject、Valid和BindingResult，以及Controller中的手动绑定错误解析。
- Service同步改为三个明确参数，保留日期、交易日和分页范围校验；日期及数值格式错误使用现有全局异常处理。
- 日期仍必填，pageNum默认1，pageSize默认20且最大100。请求路径、返回结构、选股规则及盘后任务不变。
- 缺少日期提示调整为现有统一提示“请求参数不能为空：tradeDate”；日期格式错误提示“日期格式必须为yyyy-MM-dd：tradeDate”。非法分页范围使用Service原有统一提示。
- 同步OpenAPI描述、Controller测试和Service集成测试，新增显式分页参数传递与校验错误响应测试。
- 相关测试24项通过；补充显式分页测试后完整Maven回归303项通过，失败0、错误0、跳过0。

请求示例：`GET /api/stockAlert/platformBreakout?tradeDate=2026-09-10&pageNum=1&pageSize=20`。
