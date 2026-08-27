package cn.djct.stockdemo.mapper;

import cn.djct.stockdemo.pojo.dto.StockPlateMemberDto;
import cn.djct.stockdemo.pojo.entity.StockPlate;
import cn.djct.stockdemo.pojo.entity.StockPlateMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 股票板块数据访问接口。
 */
@Mapper
public interface StockPlateMapper {

    /**
     * 批量插入或更新板块。
     *
     * @param plates 板块列表
     * @return 影响行数
     */
    int upsertPlateBatch(@Param("list") List<StockPlate> plates);

    /**
     * 查询指定来源的全部板块。
     *
     * @param dataSource 数据来源
     * @return 板块列表
     */
    List<StockPlate> selectByDataSource(@Param("dataSource") String dataSource);

    /**
     * 将本次完整快照未出现的板块标记为失效。
     *
     * @param dataSource 数据来源
     * @param tradeDate  同步交易日
     * @return 影响行数
     */
    int deactivatePlatesNotSeen(
            @Param("dataSource") String dataSource,
            @Param("tradeDate") LocalDate tradeDate
    );

    /**
     * 批量插入或更新板块成分股。
     *
     * @param members 成分股列表
     * @return 影响行数
     */
    int upsertMemberBatch(@Param("list") List<StockPlateMember> members);

    /**
     * 将本次完整快照未出现的成分关系标记为失效。
     *
     * @param dataSource 数据来源
     * @param tradeDate  同步交易日
     * @return 影响行数
     */
    int deactivateMembersNotSeen(
            @Param("dataSource") String dataSource,
            @Param("tradeDate") LocalDate tradeDate
    );

    /**
     * 统计指定交易日同步的有效板块数量。
     *
     * @param dataSource 数据来源
     * @param tradeDate  同步交易日
     * @return 有效板块数量
     */
    int countActivePlates(
            @Param("dataSource") String dataSource,
            @Param("tradeDate") LocalDate tradeDate
    );

    /**
     * 统计指定交易日同步的有效成分关系数量。
     *
     * @param dataSource 数据来源
     * @param tradeDate  同步交易日
     * @return 有效成分关系数量
     */
    int countActiveMembers(
            @Param("dataSource") String dataSource,
            @Param("tradeDate") LocalDate tradeDate
    );

    /**
     * 查询当前有效板块和成分股原始数据。
     *
     * @param dataSource 数据来源
     * @return 有效成分股列表
     */
    List<StockPlateMemberDto> selectActiveMembers(@Param("dataSource") String dataSource);
}
