package cn.djct.stockdemo.mapper;

import cn.djct.stockdemo.pojo.entity.StockCustomPlate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 自定义子板块数据访问接口。
 */
@Mapper
public interface StockCustomPlateMapper {

    /**
     * 按主键查询未被软删除的子板块。
     *
     * @param id 子板块编号
     * @return 有效子板块；不存在或已失效时返回null
     */
    StockCustomPlate selectActiveById(@Param("id") Long id);

    /**
     * 统计指定大类下未被软删除的子板块数量。
     *
     * @param categoryCode 四大类编码
     * @return 有效子板块数量
     */
    int countActiveByCategory(@Param("categoryCode") String categoryCode);

    /**
     * 统计同一大类下的有效同名子板块，用于新增和更新前的唯一性校验。
     *
     * @param categoryCode 四大类编码
     * @param plateName 子板块名称
     * @param excludeId 更新时排除的当前板块编号；新增时传null
     * @return 符合条件的记录数量
     */
    int countActiveByName(
            @Param("categoryCode") String categoryCode,
            @Param("plateName") String plateName,
            @Param("excludeId") Long excludeId
    );

    /**
     * 按创建顺序分页查询指定大类下的有效子板块。
     *
     * @param categoryCode 四大类编码
     * @param offset 分页偏移量
     * @param limit 本次最多返回数量
     * @return 有效子板块列表
     */
    List<StockCustomPlate> selectActiveByCategory(
            @Param("categoryCode") String categoryCode,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    /**
     * 新增自定义子板块并回填自增主键。
     *
     * @param plate 待新增子板块
     * @return 受影响行数
     */
    int insert(StockCustomPlate plate);

    /**
     * 按主键更新未被软删除的子板块。
     *
     * @param plate 更新后的子板块数据
     * @return 受影响行数；目标不存在或已失效时为0
     */
    int update(StockCustomPlate plate);

    /**
     * 将有效子板块标记为失效。
     *
     * @param id 子板块编号
     * @return 受影响行数；目标不存在或已失效时为0
     */
    int deactivate(@Param("id") Long id);

}
