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
     * 插入或更新一条指数分钟行情。
     *
     * @param quote 指数分钟行情
     * @return 影响行数
     */
    int upsert(@Param("quote") IndexMinuteQuote quote);

    /**
     * 按时间升序查询指定范围的指数分钟行情。
     */
    List<IndexMinuteQuote> selectByIndexCodeAndQuoteTimeRange(
            @Param("indexCode") String indexCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
