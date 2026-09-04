# 功能8四大类板块成交额对比实现

## 目标

实现试用期文档第8点：维护周期、金融、科技、消费四大类及其自定义子板块，返回当前交易日与上一交易日的四大类成交额，供前端绘制双柱图。

## 已实现

- 新增 `stock_custom_plate` 和 `stock_custom_plate_member`，支持子板块及成分股的新增、查询、更新和软删除。
- 预置15个已确认的子板块及1084条具体成分股关系，全部由V12直接插入；运行时不再依赖来源板块或执行二次初始化。
- 四大类固定为 `CYCLE`、`FINANCE`、`TECHNOLOGY`、`CONSUMPTION`，接口展示顺序固定为周期、金融、科技、消费。
- 同一大类的多个子板块按股票代码去重，不同大类之间允许重复。
- 当前和上一交易日均使用查询时的同一份子板块配置，在Service汇总后转换为亿元并保留2位小数。
- 当前交易日只向腾讯行情源请求四大类涉及的成分股，跨大类重复代码也只请求一次，不创建全市场快照。
- 上一交易日由交易日历确定，成交额读取 `stock_daily_quote.turnover_amount_yuan`，不重复请求历史行情。
- 接口仅允许在交易日09:30后查询；实时行情时间必须属于当天。任一大类无有效成分股、任一成员成交额缺失或为空时直接失败，不以0冒充真实值。

## 接口

```text
GET    /api/plate/customCategories
GET    /api/plate/customPlates?categoryCode=TECHNOLOGY&pageNum=1&pageSize=20
GET    /api/plate/customPlates/{plateId}/members?pageNum=1&pageSize=20
POST   /api/plate/customPlates
PUT    /api/plate/customPlates/{plateId}
DELETE /api/plate/customPlates/{plateId}
GET    /api/plate/turnoverComparison
```

新增和更新接口中的 `stockCodes` 表示完整成分股列表；更新时替换原有有效成员。股票代码必须存在于最新股票基础信息快照。

## 默认子板块

- 周期：煤炭开采加工、钢铁、工业金属、石油加工贸易。
- 金融：银行、证券、保险及其他。
- 科技：半导体及元件、国产软件、通信设备、计算机设备。
- 消费：食品加工制造、饮料制造、白色家电、零售。

## 验证

- 单元测试覆盖定向实时查询、同类股票去重、元转亿元、实时行情日期守门、缺失成交额守门、成员代码去重和非法代码拒绝。
- Mapper集成测试覆盖15个默认子板块及1084条固定成分关系、成员幂等写入、软删除后同名重建、最新股票快照及两交易日成交额读取。
- MVC测试覆盖成交额响应结构和自定义子板块创建请求。
- 完整Maven测试通过：193个测试，失败0、错误0、跳过0。

## 部署验证

1. 在目标MySQL依次应用V11、V12、V13、V14迁移。
2. 确认15个默认子板块和1055条有效成分关系已经落库。
3. 确认默认成员中不再存在北交所旧代码，21个对应成员已经更新为920号段。
4. 在交易日09:30后调用 `GET /api/plate/turnoverComparison`，核对四大类均返回当日实时累计成交额和上一交易日成交额。
