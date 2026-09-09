CREATE TABLE index_etf_daily_quote
(
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    etf_code     VARCHAR(6)      NOT NULL COMMENT 'ETF代码',
    index_name   VARCHAR(64)     NOT NULL COMMENT '对应指数展示名称',
    trade_date   DATE            NOT NULL COMMENT '交易日期',
    quote_time   DATETIME                 DEFAULT NULL COMMENT '行情源返回时间，历史回补时可为空',
    close_price  DECIMAL(18, 4)  NOT NULL COMMENT '当日收盘价',
    data_source  VARCHAR(32)     NOT NULL COMMENT '数据来源，如TENCENT、AKSHARE_SINA',
    collected_at DATETIME        NOT NULL COMMENT '实际采集或导入时间',
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_index_etf_daily_quote_code_date (etf_code, trade_date),
    KEY idx_index_etf_daily_quote_date_code (trade_date, etf_code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '指数ETF日行情表';
