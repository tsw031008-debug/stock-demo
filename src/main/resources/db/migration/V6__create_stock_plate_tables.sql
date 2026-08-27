CREATE TABLE stock_plate
(
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    plate_name           VARCHAR(128)    NOT NULL COMMENT '板块名称',
    data_source          VARCHAR(32)     NOT NULL COMMENT '数据来源',
    last_seen_trade_date DATE            NOT NULL COMMENT '最近一次完整同步交易日',
    active               TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否为当前有效板块',
    created_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_plate_source_name (data_source, plate_name),
    KEY idx_stock_plate_active_date (active, last_seen_trade_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '股票板块表';

CREATE TABLE stock_plate_member
(
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    plate_id             BIGINT UNSIGNED NOT NULL COMMENT '板块ID',
    stock_code           VARCHAR(6)      NOT NULL COMMENT '股票代码，保留前导零',
    last_seen_trade_date DATE            NOT NULL COMMENT '最近一次完整同步交易日',
    active               TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否为当前有效成分股',
    created_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_plate_member (plate_id, stock_code),
    KEY idx_stock_plate_member_code (stock_code, active),
    KEY idx_stock_plate_member_date (last_seen_trade_date, active)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '股票板块成分股表';

CREATE TABLE stock_plate_daily_quote
(
    id                       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    plate_id                 BIGINT UNSIGNED NOT NULL COMMENT '板块ID',
    plate_name               VARCHAR(128)    NOT NULL COMMENT '板块名称快照',
    trade_date               DATE            NOT NULL COMMENT '交易日期',
    open_price               DECIMAL(18, 4)  NOT NULL COMMENT '板块开盘值',
    close_price              DECIMAL(18, 4)  NOT NULL COMMENT '板块收盘值',
    change_percent           DECIMAL(10, 4)  NOT NULL COMMENT '成分股平均涨幅，单位：百分比',
    turnover_amount_yuan     DECIMAL(24, 2)  NOT NULL COMMENT '成分股成交额总和，单位：元',
    stock_count              INT UNSIGNED    NOT NULL COMMENT '参与计算的成分股数量',
    data_source              VARCHAR(32)     NOT NULL COMMENT '数据来源',
    data_status              VARCHAR(16)     NOT NULL DEFAULT 'COMPLETE' COMMENT '数据状态：COMPLETE或PARTIAL',
    created_at               DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at               DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_plate_daily_quote (plate_id, trade_date),
    KEY idx_stock_plate_daily_quote_date (trade_date, plate_id),
    CONSTRAINT chk_stock_plate_daily_status CHECK (data_status IN ('COMPLETE', 'PARTIAL'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '股票板块日线表';
