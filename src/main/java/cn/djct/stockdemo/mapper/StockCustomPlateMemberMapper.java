package cn.djct.stockdemo.mapper;

import cn.djct.stockdemo.pojo.dto.StockCustomPlateMemberRelationDto;
import cn.djct.stockdemo.pojo.entity.StockCustomPlateMember;
import cn.djct.stockdemo.pojo.vo.StockCustomPlateMemberRespVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 自定义子板块成分关系数据访问。 */
@Mapper
public interface StockCustomPlateMemberMapper {

    /**
     * 统计指定子板块当前有效的成分股数量。
     *
     * @param customPlateId 子板块编号
     * @return 有效成分股数量
     */
    int countActiveMembers(@Param("customPlateId") Long customPlateId);

    /**
     * 分页查询子板块有效成分股，并关联最新股票基础信息快照取得股票名称。
     *
     * @param customPlateId 子板块编号
     * @param offset 分页偏移量
     * @param limit 本次最多返回数量
     * @return 成分股代码及名称
     */
    List<StockCustomPlateMemberRespVo> selectActiveMembers(
            @Param("customPlateId") Long customPlateId,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    /**
     * 将指定子板块当前有效的全部成分关系标记为失效。
     *
     * @param customPlateId 子板块编号
     * @return 受影响行数
     */
    int deactivateMembers(@Param("customPlateId") Long customPlateId);

    /**
     * 批量插入成分关系；唯一键已存在时恢复为有效状态，保证重复保存幂等。
     *
     * @param members 待写入的成分关系
     * @return 受影响行数
     */
    int upsertMemberBatch(@Param("list") List<StockCustomPlateMember> members);

    /**
     * 查询成交额汇总使用的全部有效大类、子板块和股票关系。
     *
     * @return 当前有效成分关系
     */
    List<StockCustomPlateMemberRelationDto> selectActiveMemberRelations();
}
