CREATE TABLE national_holiday
(
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    holiday_date DATE            NOT NULL COMMENT '节假日日期',
    holiday_name VARCHAR(32)     NOT NULL COMMENT '节假日名称',
    source_title VARCHAR(128)    NOT NULL COMMENT '国务院通知标题',
    source_url   VARCHAR(512)    NOT NULL COMMENT '国务院通知地址',
    remark       VARCHAR(255)             DEFAULT NULL COMMENT '补充说明',
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_national_holiday_date (holiday_date),
    KEY idx_national_holiday_name_date (holiday_name, holiday_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '国家节假日日期表';
