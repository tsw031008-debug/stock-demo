CREATE TABLE stock_basic
(
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    stock_code           VARCHAR(6)      NOT NULL COMMENT '股票代码，保留前导零',
    stock_name           VARCHAR(64)     NOT NULL COMMENT '股票名称',
    last_seen_trade_date DATE            NOT NULL COMMENT '最近一次在完整股票清单中出现的交易日',
    created_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_basic_code (stock_code),
    KEY idx_stock_basic_last_seen_date (last_seen_trade_date, stock_code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '沪深京A股基础信息表';

CREATE TABLE stock_daily_quote
(
    id                           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    stock_code                   VARCHAR(6)      NOT NULL COMMENT '股票代码，保留前导零',
    stock_name                   VARCHAR(64)     NOT NULL COMMENT '股票名称快照',
    trade_date                   DATE            NOT NULL COMMENT '交易日期',
    quote_time                   DATETIME                 DEFAULT NULL COMMENT '行情源返回时间，历史回补时可为空',
    close_price                  DECIMAL(18, 4)           DEFAULT NULL COMMENT '收盘价，对应收盘后腾讯当前价',
    previous_close_price         DECIMAL(18, 4)           DEFAULT NULL COMMENT '昨收价',
    open_price                   DECIMAL(18, 4)           DEFAULT NULL COMMENT '开盘价',
    high_price                   DECIMAL(18, 4)           DEFAULT NULL COMMENT '当日最高价，腾讯字段33和41含义相同，仅存一份',
    low_price                    DECIMAL(18, 4)           DEFAULT NULL COMMENT '当日最低价，腾讯字段34和42含义相同，仅存一份',
    volume_hand                  BIGINT UNSIGNED           DEFAULT NULL COMMENT '成交量，单位：手',
    turnover_amount_yuan         DECIMAL(24, 2)           DEFAULT NULL COMMENT '成交额，统一换算为元',
    bid1_price                   DECIMAL(18, 4)           DEFAULT NULL COMMENT '买一价',
    bid1_volume_hand             BIGINT UNSIGNED           DEFAULT NULL COMMENT '买一量，单位：手',
    ask1_price                   DECIMAL(18, 4)           DEFAULT NULL COMMENT '卖一价',
    ask1_volume_hand             BIGINT UNSIGNED           DEFAULT NULL COMMENT '卖一量，单位：手',
    change_percent               DECIMAL(10, 4)           DEFAULT NULL COMMENT '涨跌幅，单位：百分比',
    amplitude_percent            DECIMAL(10, 4)           DEFAULT NULL COMMENT '振幅，单位：百分比',
    turnover_rate                DECIMAL(10, 4)           DEFAULT NULL COMMENT '换手率，单位：百分比',
    pe_ratio                     DECIMAL(18, 4)           DEFAULT NULL COMMENT '市盈率',
    pb_ratio                     DECIMAL(18, 4)           DEFAULT NULL COMMENT '市净率',
    circulating_market_cap_yuan  DECIMAL(24, 2)           DEFAULT NULL COMMENT '流通市值，统一换算为元',
    total_market_cap_yuan        DECIMAL(24, 2)           DEFAULT NULL COMMENT '总市值，统一换算为元',
    data_source                  VARCHAR(32)      NOT NULL COMMENT '数据来源，如TENCENT、AKSHARE',
    data_status                  VARCHAR(16)      NOT NULL DEFAULT 'COMPLETE' COMMENT '数据状态：COMPLETE完整、PARTIAL部分字段缺失',
    collected_at                 DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '采集或导入时间',
    created_at                   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at                   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_daily_quote_code_date (stock_code, trade_date),
    KEY idx_stock_daily_quote_date_code (trade_date, stock_code),
    CONSTRAINT chk_stock_daily_quote_data_status CHECK (data_status IN ('COMPLETE', 'PARTIAL'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '沪深京A股日行情表';
