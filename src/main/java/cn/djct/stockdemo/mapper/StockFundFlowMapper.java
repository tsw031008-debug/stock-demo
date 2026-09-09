package cn.djct.stockdemo.mapper;

import cn.djct.stockdemo.pojo.vo.StockFundFlowRespVo;
import cn.djct.stockdemo.pojo.entity.StockFundFlow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 股票资金流向数据访问接口。
 */
@Mapper
public interface StockFundFlowMapper {

    /**
     * 根据交易日查询资金流向记录数。
     *
     * @param tradeDate 交易日
     * @return 记录数
     */
    int countByTradeDate(@Param("tradeDate") LocalDate tradeDate);

    /**
     * 分页查询指定交易日的资金流向原始数据。
     *
     * @param tradeDate 交易日
     * @param offset    起始位置
     * @param limit     返回数量
     * @return 资金流向记录
     */
    List<StockFundFlowRespVo> selectPageByTradeDate(
            @Param("tradeDate") LocalDate tradeDate,
            @Param("offset") long offset,
            @Param("limit") int limit
    );

    /**
     * 批量插入或更新资金流向。
     *
     * @param fundFlows 资金流向列表
     * @return 影响行数
     */
    int upsertBatch(@Param("list") List<StockFundFlow> fundFlows);
}
