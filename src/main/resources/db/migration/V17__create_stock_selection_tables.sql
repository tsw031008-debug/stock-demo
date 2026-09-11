CREATE TABLE stock_selection_run
(
    trade_date    DATE        NOT NULL COMMENT '选股交易日',
    strategy_type VARCHAR(32) NOT NULL COMMENT '策略类型，PLATFORM_BREAKOUT平台突破',
    completed     TINYINT     NOT NULL DEFAULT 0 COMMENT '成功完成标记，零入选也标记为1',
    completed_at  DATETIME             DEFAULT NULL COMMENT '最后成功完成时间',
    PRIMARY KEY (trade_date, strategy_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '选股完成记录及同日策略事务锁';

CREATE TABLE stock_selection_result
(
    trade_date    DATE        NOT NULL COMMENT '选股交易日',
    strategy_type VARCHAR(32) NOT NULL COMMENT '策略类型',
    stock_code    VARCHAR(6)  NOT NULL COMMENT '入选股票代码',
    stock_name    VARCHAR(64) NOT NULL COMMENT '入选时股票名称',
    PRIMARY KEY (trade_date, strategy_type, stock_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '每日左侧选股结果';
