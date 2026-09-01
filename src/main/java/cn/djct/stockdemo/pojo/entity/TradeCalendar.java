package cn.djct.stockdemo.pojo.entity;

import cn.djct.stockdemo.constant.TradeDayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A股交易日历实体。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradeCalendar {

    private Long id;

    /**
     * 市场代码，例如CN_A。
     */
    private String marketCode;

    /**
     * 自然日期。
     */
    private LocalDate tradeDate;

    /**
     * 是否为交易日。
     */
    private Boolean isTradingDay;

    /**
     * 日期类型。
     */
    private TradeDayType dayType;

    /**
     * 节假日名称
     */
    private String holidayName;

    /**
     * 来源标题
     */
    private String sourceTitle;

    /**
     * 来源地址
     */
    private String sourceUrl;

    /**
     * 是否经过人工修正。
     */
    private Boolean isManualAdjusted;

    /**
     * 补充说明
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
