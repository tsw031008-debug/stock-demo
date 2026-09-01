CREATE TABLE stock_fund_flow
(
    id                                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    stock_code                        VARCHAR(6)      NOT NULL COMMENT '股票代码，保留前导零',
    stock_name                        VARCHAR(64)     NOT NULL COMMENT '股票名称快照',
    trade_date                        DATE            NOT NULL COMMENT '交易日期',
    latest_price                      DECIMAL(18, 4)           DEFAULT NULL COMMENT '最新价',
    change_percent                    DECIMAL(10, 4)           DEFAULT NULL COMMENT '今日涨跌幅，单位：百分比',
    main_net_inflow_yuan              DECIMAL(24, 2)           DEFAULT NULL COMMENT '今日主力净流入金额，单位：元',
    main_net_inflow_ratio             DECIMAL(10, 4)           DEFAULT NULL COMMENT '今日主力净流入占比，单位：百分比',
    super_large_net_inflow_yuan       DECIMAL(24, 2)           DEFAULT NULL COMMENT '今日超大单净流入金额，单位：元',
    super_large_net_inflow_ratio      DECIMAL(10, 4)           DEFAULT NULL COMMENT '今日超大单净流入占比，单位：百分比',
    large_net_inflow_yuan             DECIMAL(24, 2)           DEFAULT NULL COMMENT '今日大单净流入金额，单位：元',
    large_net_inflow_ratio            DECIMAL(10, 4)           DEFAULT NULL COMMENT '今日大单净流入占比，单位：百分比',
    data_source                       VARCHAR(32)      NOT NULL COMMENT '数据来源',
    data_status                       VARCHAR(16)      NOT NULL COMMENT '数据状态：COMPLETE完整、PARTIAL部分字段缺失',
    collected_at                      DATETIME         NOT NULL COMMENT '采集时间',
    created_at                        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at                        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_fund_flow_code_date (stock_code, trade_date),
    KEY idx_stock_fund_flow_date_code (trade_date, stock_code),
    CONSTRAINT chk_stock_fund_flow_data_status CHECK (data_status IN ('COMPLETE', 'PARTIAL'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '沪深A股每日资金流向表';
