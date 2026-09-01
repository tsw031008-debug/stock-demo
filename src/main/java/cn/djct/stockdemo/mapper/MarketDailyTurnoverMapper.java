package cn.djct.stockdemo.mapper;

import cn.djct.stockdemo.pojo.entity.MarketDailyTurnover;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日市场成交额数据访问接口。
 */
@Mapper
public interface MarketDailyTurnoverMapper {

    /**
     * 查询指定交易日的市场成交额汇总。
     *
     * @param tradeDate 交易日
     * @return 当日汇总；不存在时返回null
     */
    MarketDailyTurnover selectByTradeDate(@Param("tradeDate") LocalDate tradeDate);

    /**
     * 查询最新一条完整市场成交额汇总的交易日。
     *
     * @return 最新完整交易日；没有完整数据时返回null
     */
    LocalDate selectLatestCompleteTradeDate();

    /**
     * 按交易日升序查询指定日期范围内的完整市场成交额汇总。
     *
     * @param startDate 开始日期，包含
     * @param endDate   结束日期，包含
     * @return 完整市场成交额汇总列表
     */
    List<MarketDailyTurnover> selectCompleteByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 按交易日插入或更新市场成交额汇总。
     *
     * @param turnover 每日市场成交额汇总
     * @return 影响行数
     */
    int upsert(MarketDailyTurnover turnover);
}
