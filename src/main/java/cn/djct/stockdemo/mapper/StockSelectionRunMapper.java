package cn.djct.stockdemo.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/** 选股任务完成记录。 */
@Mapper
public interface StockSelectionRunMapper {
    /** 查询是否存在成功完成的结果，成功零入选也返回true。 */
    boolean isCompleted(@Param("tradeDate") LocalDate tradeDate, @Param("strategyType") String strategyType);

    /** 在结果写入的同一事务中标记本次完成。 */
    int completeRun(@Param("tradeDate") LocalDate tradeDate, @Param("strategyType") String strategyType);
}
