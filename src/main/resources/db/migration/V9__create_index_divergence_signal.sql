CREATE TABLE index_divergence_signal
(
    id                           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    index_code                   VARCHAR(6)      NOT NULL COMMENT '指数代码，保留前导零',
    signal_type                  VARCHAR(16)     NOT NULL COMMENT '信号类型：MACD_TOP或MACD_BOTTOM',
    signal_time                  DATETIME        NOT NULL COMMENT '信号确认时间',
    previous_interval_start_time DATETIME        NOT NULL COMMENT '前一有效区间开始时间',
    previous_interval_end_time   DATETIME        NOT NULL COMMENT '前一有效区间结束时间',
    current_interval_start_time  DATETIME        NOT NULL COMMENT '当前有效区间开始时间',
    current_interval_end_time    DATETIME        NOT NULL COMMENT '当前有效区间结束时间',
    previous_price_extreme       DECIMAL(18, 4)  NOT NULL COMMENT '前一有效区间价格极值',
    current_price_extreme        DECIMAL(18, 4)  NOT NULL COMMENT '当前有效区间价格极值',
    previous_macd_extreme        DECIMAL(24, 8)  NOT NULL COMMENT '前一有效区间MACD极值',
    current_macd_extreme         DECIMAL(24, 8)  NOT NULL COMMENT '当前有效区间MACD极值',
    previous_dif_extreme         DECIMAL(24, 8)  NOT NULL COMMENT '前一有效区间DIF极值',
    current_dif_extreme          DECIMAL(24, 8)  NOT NULL COMMENT '当前有效区间DIF极值',
    created_at                   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at                   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_index_divergence_signal (index_code, signal_type, signal_time),
    KEY idx_index_divergence_signal_time (signal_time, index_code),
    CONSTRAINT chk_index_divergence_signal_type
        CHECK (signal_type IN ('MACD_TOP', 'MACD_BOTTOM'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '指数MACD背离信号表';
