CREATE TABLE trade_calendar
(
    id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    market_code        VARCHAR(16)     NOT NULL DEFAULT 'CN_A' COMMENT '市场代码，CN_A表示沪深京A股',
    trade_date         DATE            NOT NULL COMMENT '自然日期',
    is_trading_day     TINYINT UNSIGNED NOT NULL COMMENT '是否交易日：0否，1是',
    day_type           VARCHAR(32)     NOT NULL COMMENT '日期类型：TRADING_DAY、WEEKEND、NATIONAL_HOLIDAY、EXCHANGE_CLOSED',
    holiday_name       VARCHAR(32)              DEFAULT NULL COMMENT '节假日名称',
    source_title       VARCHAR(128)             DEFAULT NULL COMMENT '数据来源标题',
    source_url         VARCHAR(512)             DEFAULT NULL COMMENT '数据来源地址',
    is_manual_adjusted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否人工修正：0否，1是',
    remark             VARCHAR(255)             DEFAULT NULL COMMENT '补充说明',
    created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_trade_calendar_market_date (market_code, trade_date),
    KEY idx_trade_calendar_trading_date (market_code, is_trading_day, trade_date),
    CONSTRAINT chk_trade_calendar_is_trading_day CHECK (is_trading_day IN (0, 1)),
    CONSTRAINT chk_trade_calendar_is_manual_adjusted CHECK (is_manual_adjusted IN (0, 1)),
    CONSTRAINT chk_trade_calendar_day_type CHECK (
        day_type IN ('TRADING_DAY', 'WEEKEND', 'NATIONAL_HOLIDAY', 'EXCHANGE_CLOSED')
    )
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = 'A股交易日历表';
