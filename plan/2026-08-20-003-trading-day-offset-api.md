# 前后第N个交易日查询接口

日期：2026-08-20  
状态：已完成

## 完成内容

- 新增前第 N 个交易日查询接口。
- 新增后第 N 个交易日查询接口。
- 两个接口共用前后交易日查询响应对象。
- 复用现有 Service 和 MyBatis XML 查询，没有修改数据库结构。

## 接口

```text
GET /api/trade-calendar/previous-trading-day?date=2023-06-26&offset=2
GET /api/trade-calendar/next-trading-day?date=2023-06-26&offset=2
```

示例结果分别为 `2023-06-20` 和 `2023-06-28`。

## 验证结果

- Maven 完整测试通过。
- 共执行 17 个测试，失败 0，错误 0。

