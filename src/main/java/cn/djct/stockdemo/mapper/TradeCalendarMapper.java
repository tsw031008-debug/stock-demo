package cn.djct.stockdemo.mapper;

import cn.djct.stockdemo.pojo.entity.TradeCalendar;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 交易日历数据访问接口。
 *
 * TODO 在对应的 Mapper XML 中编写全部 SQL。
 */
@Mapper
public interface TradeCalendarMapper {

    /**
     * 查询指定市场某一天的是否为交易日。
     */
    TradeCalendar selectByMarketCodeAndTradeDate(
            @Param("marketCode") String marketCode,
            @Param("tradeDate") LocalDate tradeDate
    );

    /**
     * 查询指定日期之前的第N个交易日。
     *
     * @param offset 从0开始，0表示上一个交易日
     */
    LocalDate selectPreviousTradingDate(
            @Param("marketCode") String marketCode,
            @Param("tradeDate") LocalDate tradeDate,
            @Param("offset") int offset
    );

    /**
     * 查询指定日期之后的第N个交易日。
     *
     * @param offset 从0开始，0表示下一个交易日
     */
    LocalDate selectNextTradingDate(
            @Param("marketCode") String marketCode,
            @Param("tradeDate") LocalDate tradeDate,
            @Param("offset") int offset
    );

    /**
     * 批量新增或更新交易日历。
     */
    int insertBatch(
            @Param("list") List<TradeCalendar> calendars
    );
}
