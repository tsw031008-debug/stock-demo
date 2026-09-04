CREATE TABLE stock_custom_plate
(
    id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    category_code         VARCHAR(32)     NOT NULL COMMENT '所属大类：CYCLE、FINANCE、TECHNOLOGY、CONSUMPTION',
    plate_name            VARCHAR(128)    NOT NULL COMMENT '自定义子板块名称',
    active                TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否有效',
    active_plate_name     VARCHAR(128) GENERATED ALWAYS AS (
        CASE WHEN active = 1 THEN plate_name ELSE NULL END
    ) COMMENT '仅有效记录参与名称唯一约束',
    created_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_custom_plate_category_name (category_code, active_plate_name),
    KEY idx_stock_custom_plate_category_active (category_code, active, id),
    CONSTRAINT chk_stock_custom_plate_category CHECK (
        category_code IN ('CYCLE', 'FINANCE', 'TECHNOLOGY', 'CONSUMPTION')
    )
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin
  COMMENT = '四大类自定义子板块表';

CREATE TABLE stock_custom_plate_member
(
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    custom_plate_id BIGINT UNSIGNED NOT NULL COMMENT '自定义子板块ID',
    stock_code      VARCHAR(6)      NOT NULL COMMENT '股票代码，保留前导零',
    active          TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否有效',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_custom_plate_member (custom_plate_id, stock_code),
    KEY idx_stock_custom_plate_member_code (stock_code, active),
    KEY idx_stock_custom_plate_member_plate (custom_plate_id, active, stock_code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '自定义子板块成分股表';
