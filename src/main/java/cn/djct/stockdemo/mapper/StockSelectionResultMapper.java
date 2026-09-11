package cn.djct.stockdemo.mapper;

import cn.djct.stockdemo.pojo.entity.StockBasic;
import cn.djct.stockdemo.pojo.vo.LeftSideStockRespVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/** 选股结果保存和查询。 */
@Mapper
public interface StockSelectionResultMapper {
    /** 清除该日该策略的旧结果，供事务内全量替换。 */
    int deleteResults(@Param("tradeDate") LocalDate tradeDate, @Param("strategyType") String strategyType);

    /** 批量保存入选时的代码和名称。 */
    int insertResults(@Param("tradeDate") LocalDate tradeDate, @Param("strategyType") String strategyType,
                      @Param("stocks") List<StockBasic> stocks);

    /** 统计指定日期及策略的入选数量。 */
    long countResults(@Param("tradeDate") LocalDate tradeDate, @Param("strategyType") String strategyType);

    /** 按股票代码升序分页返回指定日期结果及当日行情，不关联最新基础快照。 */
    List<LeftSideStockRespVo> selectPage(@Param("tradeDate") LocalDate tradeDate,
                                        @Param("strategyType") String strategyType,
                                        @Param("offset") long offset, @Param("limit") int limit);
}
