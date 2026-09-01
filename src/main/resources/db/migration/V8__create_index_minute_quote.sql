CREATE TABLE index_minute_quote
(
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    index_code           VARCHAR(6)      NOT NULL COMMENT '指数代码，保留前导零',
    index_name           VARCHAR(64)     NOT NULL COMMENT '指数名称快照',
    trade_date           DATE            NOT NULL COMMENT '交易日期',
    quote_time           DATETIME        NOT NULL COMMENT '分钟行情时间，秒固定为00',
    current_price        DECIMAL(18, 4)  NOT NULL COMMENT '当前价格',
    previous_close_price DECIMAL(18, 4)  NOT NULL COMMENT '昨收价',
    data_source          VARCHAR(32)     NOT NULL COMMENT '数据来源，如TENCENT',
    collected_at         DATETIME        NOT NULL COMMENT '实际采集时间',
    created_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_index_minute_quote_code_time (index_code, quote_time),
    KEY idx_index_minute_quote_date_code_time (trade_date, index_code, quote_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '指数分钟行情表';
