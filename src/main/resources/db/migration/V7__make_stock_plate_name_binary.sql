ALTER TABLE stock_plate
    MODIFY COLUMN plate_name VARCHAR(128)
        CHARACTER SET utf8mb4
        COLLATE utf8mb4_bin
        NOT NULL COMMENT '板块名称';
