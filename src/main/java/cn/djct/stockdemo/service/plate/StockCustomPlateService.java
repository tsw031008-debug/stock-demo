package cn.djct.stockdemo.service.plate;

import cn.djct.stockdemo.pojo.dto.PageDto;
import cn.djct.stockdemo.pojo.dto.StockCustomCategoryDto;
import cn.djct.stockdemo.pojo.dto.StockCustomPlateDto;
import cn.djct.stockdemo.pojo.dto.StockCustomPlateMemberDto;
import cn.djct.stockdemo.pojo.dto.StockCustomPlateMemberRelationDto;
import cn.djct.stockdemo.pojo.dto.StockCustomPlateSaveDto;

import java.util.List;

/**
 * 四大类自定义子板块配置服务。
 */
public interface StockCustomPlateService {

    /**
     * 查询系统固定定义的四大类，返回顺序与枚举配置一致。
     */
    List<StockCustomCategoryDto> findCategories();

    /**
     * 分页查询指定大类下的有效子板块。
     *
     * @param categoryCode 四大类编码
     * @param pageNum 页码，从1开始
     * @param pageSize 每页数量，最大100
     * @return 有效子板块分页数据
     */
    PageDto<StockCustomPlateDto> findPlates(String categoryCode, int pageNum, int pageSize);

    /**
     * 分页查询有效子板块当前生效的成分股。
     *
     * @param plateId 子板块编号
     * @param pageNum 页码，从1开始
     * @param pageSize 每页数量，最大100
     * @return 成分股分页数据
     */
    PageDto<StockCustomPlateMemberDto> findMembers(Long plateId, int pageNum, int pageSize);

    /**
     * 新增子板块，并保存创建时提交的完整成分股列表。
     *
     * @param request 子板块及完整成分股请求参数
     * @return 新增后的子板块
     */
    StockCustomPlateDto create(StockCustomPlateSaveDto request);

    /**
     * 更新子板块，并以提交的股票代码全量替换原有成分股，而非增量追加。
     *
     * @param plateId 子板块编号
     * @param request 更新后的子板块及完整成分股请求参数
     * @return 更新后的子板块
     */
    StockCustomPlateDto update(Long plateId, StockCustomPlateSaveDto request);

    /**
     * 软删除子板块及其当前有效的成分关系，历史记录仍保留在数据库中。
     *
     * @param plateId 子板块编号
     */
    void delete(Long plateId);

    /**
     * 查询四大类成交额汇总所需的全部有效板块与成分股关系。
     *
     * @return 当前有效成分关系
     */
    List<StockCustomPlateMemberRelationDto> findActiveMemberRelations();
}
