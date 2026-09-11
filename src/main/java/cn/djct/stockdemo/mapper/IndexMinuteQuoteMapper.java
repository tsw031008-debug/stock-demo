package cn.djct.stockdemo.mapper;

import cn.djct.stockdemo.pojo.entity.IndexMinuteQuote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 指数分钟行情数据访问接口。
 */
@Mapper
public interface IndexMinuteQuoteMapper {

    /**
     * 批量插入或更新指数分钟行情。
     *
     * @param quotes 指数分钟行情列表
     * @return 影响行数
     */
    int upsertBatch(@Param("list") List<IndexMinuteQuote> quotes);

    /**
     * 按时间升序查询指定范围的指数分钟行情。
     *
     * @param indexCode 指数代码
     * @param startTime 开始时间，包含
     * @param endTime   结束时间，包含
     * @return 指数分钟行情列表
     */
    List<IndexMinuteQuote> selectByIndexCodeAndQuoteTimeRange(
            @Param("indexCode") String indexCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
