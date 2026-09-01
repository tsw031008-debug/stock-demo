package cn.djct.stockdemo.mapper;

import cn.djct.stockdemo.pojo.entity.IndexDivergenceSignal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 指数MACD背离信号数据访问接口。
 */
@Mapper
public interface IndexDivergenceSignalMapper {

    /**
     * 批量插入或更新背离信号。
     *
     * @param signals 背离信号列表
     * @return 影响行数
     */
    int upsertBatch(@Param("list") List<IndexDivergenceSignal> signals);

    /**
     * 按确认时间升序查询指定范围的背离信号。
     *
     * @param indexCode 指数代码
     * @param startTime 开始时间，包含
     * @param endTime   结束时间，不包含
     * @return 背离信号列表
     */
    List<IndexDivergenceSignal> selectByIndexCodeAndSignalTimeRange(
            @Param("indexCode") String indexCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
