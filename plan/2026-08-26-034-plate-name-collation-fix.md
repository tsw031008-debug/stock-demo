# 板块名称排序规则修复

## 问题

手动调用 `POST /api/plate/synchronize` 时，来源接口解析得到538个板块，但数据库完整性校验只有535个有效板块：

```text
板块ID读取不完整，expected=538，actual=535
```

## 原因

来源数据中存在3组仅全角、半角括号不同的板块名称：

```text
东数西算(算力)-概 / 东数西算（算力）-概
跨境支付(CIPS)-概 / 跨境支付（CIPS）-概
先进封装(Chiplet)-概 / 先进封装（Chiplet）-概
```

`stock_plate.plate_name` 原来继承表级 `utf8mb4_0900_ai_ci` 排序规则，唯一键比较时把每组名称视为相同，因此538个来源板块只得到535条数据库记录。

## 修复

- 新增 `V7__make_stock_plate_name_binary.sql`，将 `plate_name` 改为 `utf8mb4_bin`，按实际字符区分全角和半角括号。
- 不修改已经执行的V6迁移。
- 新增固定样本测试，确认解析层保留全角和半角两个独立板块名称。
- 已对本地MySQL执行相同V7变更，字段排序规则由 `utf8mb4_0900_ai_ci` 变为 `utf8mb4_bin`。

## 验证

- 本地真实调用 `POST /api/plate/synchronize` 成功，返回 `savedCount=538`、`code=200`。
- Maven完整测试通过：133个测试，0失败、0错误、0跳过。
- `git diff --check` 通过。

## 自动同步说明

板块同步在交易日09:15自动执行；应用在09:15之后启动会补执行。手动接口仅用于首次初始化、故障补采和测试，不需要每天人工调用。
