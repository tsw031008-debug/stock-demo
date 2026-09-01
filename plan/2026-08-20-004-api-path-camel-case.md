# 接口路径统一使用驼峰命名

日期：2026-08-20  
状态：已完成

## 完成内容

- REST 接口路径统一调整为斜杆加 lowerCamelCase 格式。
- 更新交易日历 Controller、接口测试和当前接口总览。
- 在 `AGENTS.md` 中加入接口路径命名约束。

## 路径调整

```text
/api/trade-calendar              -> /api/tradeCalendar
/trading-day                     -> /tradingDay
/previous-trading-day            -> /previousTradingDay
/next-trading-day                -> /nextTradingDay
```

## 验证

- Maven 完整测试通过。
- 共执行 17 个测试，失败 0，错误 0。
