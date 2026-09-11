package cn.djct.stockdemo.mapper;

import cn.djct.stockdemo.pojo.dto.StockPlateMemberDto;
import cn.djct.stockdemo.pojo.entity.StockPlateMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/** 板块成分关系数据访问。 */
@Mapper
public interface StockPlateMemberMapper {

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
