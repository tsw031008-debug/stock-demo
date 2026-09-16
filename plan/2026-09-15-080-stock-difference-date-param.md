# 功能15直接接收日期参数

- 按用户要求，stockDifference接口改为@RequestParam和@DateTimeFormat直接接收LocalDate，删除仅含日期的StockIndexDifferenceQueryDto。
- 日期格式错误复用现有MethodArgumentTypeMismatchException处理；缺参由Spring拦截，提示“请求参数不能为空：tradeDate”，不调用Service。
- 撤销079中仅为此DTO添加的BindException处理及对应测试；保留原全局异常处理逻辑。
- 接口路径、日期格式、业务计算及返回结构不变；Controller测试同步验证缺参不进入Service。
- 定向5项通过；mvnw.cmd clean test全量379项通过，失败0、错误0、跳过0。
