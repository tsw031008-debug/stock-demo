package cn.djct.stockdemo.mapper;

import cn.djct.stockdemo.pojo.entity.TradeCalendar;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 交易日历数据访问接口。
 */
@Mapper
public interface TradeCalendarMapper {

    /**
     * 查询指定市场某一天的交易日历记录。
     *
     * @param marketCode 市场代码
     * @param tradeDate  查询日期
     * @return 交易日历记录；不存在时返回null
     */
    TradeCalendar selectByMarketCodeAndTradeDate(
            @Param("marketCode") String marketCode,
            @Param("tradeDate") LocalDate tradeDate
    );

    /**
     * 查询指定日期之前的第N个交易日。
     *
     * @param marketCode 市场代码
     * @param tradeDate  基准日期，不包含
     * @param offset     SQL偏移量，从0开始，0表示上一个交易日
     * @return 目标交易日
     */
    LocalDate selectPreviousTradingDate(
            @Param("marketCode") String marketCode,
            @Param("tradeDate") LocalDate tradeDate,
            @Param("offset") int offset
    );

    /**
     * 查询指定日期之后的第N个交易日。
     *
     * @param marketCode 市场代码
     * @param tradeDate  基准日期，不包含
     * @param offset     SQL偏移量，从0开始，0表示下一个交易日
     * @return 目标交易日
     */
    LocalDate selectNextTradingDate(
            @Param("marketCode") String marketCode,
            @Param("tradeDate") LocalDate tradeDate,
            @Param("offset") int offset
    );

    /**
     * 批量新增交易日历。
     *
     * @param calendars 交易日历列表
     * @return 影响行数
     */
    int insertBatch(
            @Param("list") List<TradeCalendar> calendars
    );

    /**
     * 统计指定市场、日期范围内已有的数据量。
     *
     * @param marketCode 市场代码
     * @param startDate  开始日期，包含
     * @param endDate    结束日期，包含
     * @return 已有记录数量
     */
    int countByMarketCodeAndDateRange(
            @Param("marketCode") String marketCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 查询指定范围内的交易日。
     *
     * @param marketCode 市场代码
     * @param startDate  开始日期，包含
     * @param endDate    结束日期，包含
     * @return 交易日列表
     */
    List<LocalDate> selectTradingDates(
            @Param("marketCode") String marketCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
