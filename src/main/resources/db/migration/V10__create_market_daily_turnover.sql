CREATE TABLE market_daily_turnover
(
    id                     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    trade_date             DATE            NOT NULL COMMENT '交易日期',
    turnover_amount_yuan   DECIMAL(24, 2)  NOT NULL COMMENT '沪深A股成交额合计，单位：元',
    stock_count            INT UNSIGNED    NOT NULL COMMENT '参与汇总的股票数量',
    amount_record_count    INT UNSIGNED    NOT NULL COMMENT '成交额非空记录数量',
    data_source            VARCHAR(32)     NOT NULL COMMENT '数据来源',
    data_status            VARCHAR(16)     NOT NULL DEFAULT 'COMPLETE' COMMENT '数据状态',
    calculated_at          DATETIME        NOT NULL COMMENT '计算时间',
    created_at             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_market_daily_turnover_date (trade_date),
    KEY idx_market_daily_turnover_status_date (data_status, trade_date),
    CONSTRAINT chk_market_daily_turnover_status CHECK (data_status IN ('COMPLETE', 'PARTIAL'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '每日沪深A股成交额汇总表';
