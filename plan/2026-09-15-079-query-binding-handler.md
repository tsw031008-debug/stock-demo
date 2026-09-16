# 查询参数绑定错误统一处理

日期：2026-09-15

- 按用户要求移除功能15 Controller中的BindingResult参数、手动检查及异常抛出，保留DTO接收参数。
- GlobalExceptionHandler统一处理BindException；LocalDate转换错误保持“日期格式必须为yyyy-MM-dd：tradeDate”，其他字段绑定错误返回简洁提示。
- 原MethodArgumentNotValidException处理保持不变，业务日期校验仍由Service负责。
- 既有5项接口测试通过，新增全局处理器单元测试覆盖日期转换、其他错误及无字段错误。`mvnw.cmd test`全量382项通过，失败0、错误0、跳过0。
