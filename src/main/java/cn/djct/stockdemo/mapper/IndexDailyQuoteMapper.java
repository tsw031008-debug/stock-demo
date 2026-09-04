package cn.djct.stockdemo.mapper;

import cn.djct.stockdemo.pojo.entity.IndexDailyQuote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 指数日行情数据访问接口。
 */
@Mapper
public interface IndexDailyQuoteMapper {

    /**
     * 批量插入或更新指数日行情。
     *
     * @param quotes 指数日行情
     * @return 影响行数
     */
    int upsertBatch(@Param("list") List<IndexDailyQuote> quotes);

    /**
     * 查询指数日行情表中的最新交易日。
     *
     * @return 最新交易日，无数据时返回空
     */
    LocalDate selectLatestTradeDate();

    /**
     * 查询指定交易日和指数的日行情原始数据。
     *
     * @param tradeDates 交易日列表
     * @param indexCodes 指数代码列表
     * @return 按交易日和固定指数顺序排列的日行情
     */
    List<IndexDailyQuote> selectByTradeDatesAndCodes(
            @Param("tradeDates") List<LocalDate> tradeDates,
            @Param("indexCodes") List<String> indexCodes
    );
}
