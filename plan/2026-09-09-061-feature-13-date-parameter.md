# 功能13简化日期入参

- 按用户明确要求，单日期接口不再使用请求DTO，改为RequestParam与DateTimeFormat直接接收LocalDate。
- 删除不再使用的TechnologyStockTurnoverQueryDto和BindingResult，请求URL、响应及筛选逻辑不变。
- 全局异常处理补充必填参数缺失、参数类型转换失败，避免直接日期绑定失败落入系统异常；其他直接参数接口同类错误也返回业务参数错误。
- Service仍保留日期非空、未来日期与交易日校验，不增加日期回退或筛选规则。
- Controller测试覆盖正常日期、格式错误、未传日期、空日期，以及全局非日期参数转换错误分支。
- 完整运行 `mvnw.cmd test`：268项通过，失败0、错误0、跳过0。
