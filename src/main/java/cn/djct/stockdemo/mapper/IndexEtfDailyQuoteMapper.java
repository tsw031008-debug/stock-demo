package cn.djct.stockdemo.mapper;

import cn.djct.stockdemo.pojo.entity.IndexEtfDailyQuote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 指数ETF日行情数据访问接口。
 */
@Mapper
public interface IndexEtfDailyQuoteMapper {

    /**
     * 批量幂等保存四只固定ETF日行情。
     *
     * @param quotes ETF日行情
     * @return 影响行数
     */
    int upsertBatch(@Param("list") List<IndexEtfDailyQuote> quotes);

    /**
     * 定位ETF日行情表中的最新交易日。
     *
     * @return 最新交易日，无数据时返回空
     */
    LocalDate selectLatestTradeDate();

    /**
     * 查询指定交易日和ETF代码的原始日行情。
     *
     * @param tradeDates 交易日列表
     * @param etfCodes ETF代码列表
     * @return ETF日行情
     */
    List<IndexEtfDailyQuote> selectByTradeDatesAndCodes(
            @Param("tradeDates") List<LocalDate> tradeDates,
            @Param("etfCodes") List<String> etfCodes
    );
}
