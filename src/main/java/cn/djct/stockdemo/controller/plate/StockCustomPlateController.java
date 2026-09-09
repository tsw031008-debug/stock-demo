package cn.djct.stockdemo.controller.plate;

import cn.djct.stockdemo.common.Result;
import cn.djct.stockdemo.pojo.vo.PageRespVo;
import cn.djct.stockdemo.pojo.vo.StockCategoryTurnoverComparisonRespVo;
import cn.djct.stockdemo.pojo.vo.StockCustomCategoryRespVo;
import cn.djct.stockdemo.pojo.vo.StockCustomPlateRespVo;
import cn.djct.stockdemo.pojo.vo.StockCustomPlateMemberRespVo;
import cn.djct.stockdemo.pojo.dto.StockCustomPlateSaveDto;
import cn.djct.stockdemo.service.plate.StockCategoryTurnoverComparisonService;
import cn.djct.stockdemo.service.plate.StockCustomPlateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 四大类自定义子板块及成交额对比接口。
 */
@Tag(name = "四大类板块水位")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/plate")
public class StockCustomPlateController {

    private final StockCustomPlateService stockCustomPlateService;
    private final StockCategoryTurnoverComparisonService comparisonService;

    /**
     * 查询产品固定定义的四大类及其展示名称。
     *
     * @return 固定四大类
     */
    @Operation(summary = "查询固定四大类")
    @GetMapping("/customCategories")
    public Result<List<StockCustomCategoryRespVo>> findCategories() {
        return Result.success("操作成功", stockCustomPlateService.findCategories());
    }

    /**
     * 分页查询指定大类下当前有效的自定义子板块。
     *
     * @param categoryCode 四大类编码
     * @param pageNum 页码，从1开始
     * @param pageSize 每页数量，最大100
     * @return 子板块分页响应
     */
    @Operation(summary = "分页查询自定义子板块")
    @GetMapping("/customPlates")
    public Result<PageRespVo<StockCustomPlateRespVo>> findPlates(
            @RequestParam String categoryCode,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return Result.success("操作成功", stockCustomPlateService.findPlates(categoryCode, pageNum, pageSize));
    }

    /**
     * 分页查询指定子板块当前有效的成分股。
     *
     * @param plateId 子板块编号
     * @param pageNum 页码，从1开始
     * @param pageSize 每页数量，最大100
     * @return 成分股分页响应
     */
    @Operation(summary = "分页查询自定义子板块成分股")
    @GetMapping("/customPlates/{plateId}/members")
    public Result<PageRespVo<StockCustomPlateMemberRespVo>> findMembers(
            @PathVariable Long plateId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return Result.success("操作成功", stockCustomPlateService.findMembers(plateId, pageNum, pageSize));
    }

    /**
     * 新增子板块，提交的股票代码作为该板块的完整成分股列表。
     *
     * @param request 子板块及完整成分股列表
     * @return 新增后的子板块
     */
    @Operation(summary = "新增自定义子板块及成分股")
    @PostMapping("/customPlates")
    public Result<StockCustomPlateRespVo> create(@Valid @RequestBody StockCustomPlateSaveDto request) {
        return Result.success("操作成功", stockCustomPlateService.create(request));
    }

    /**
     * 更新子板块基本信息，并用提交的股票代码全量替换原有成分股。
     *
     * @param plateId 子板块编号
     * @param request 更新后的子板块及完整成分股列表
     * @return 更新后的子板块
     */
    @Operation(summary = "更新自定义子板块及完整成分股列表")
    @PutMapping("/customPlates/{plateId}")
    public Result<StockCustomPlateRespVo> update(
            @PathVariable Long plateId,
            @Valid @RequestBody StockCustomPlateSaveDto request
    ) {
        return Result.success("操作成功", stockCustomPlateService.update(plateId, request));
    }

    /**
     * 软删除子板块及其有效成分关系。
     *
     * @param plateId 子板块编号
     * @return 空响应
     */
    @Operation(summary = "删除自定义子板块")
    @DeleteMapping("/customPlates/{plateId}")
    public Result<Void> delete(@PathVariable Long plateId) {
        stockCustomPlateService.delete(plateId);
        return Result.success("操作成功", null);
    }

    /**
     * 查询当前交易日与上一交易日的四大类成交额对比，金额单位为亿元。
     *
     * @return 四大类成交额对比
     */
    @Operation(summary = "查询四大类当前与上一交易日成交额")
    @GetMapping("/turnoverComparison")
    public Result<StockCategoryTurnoverComparisonRespVo> getTurnoverComparison() {
        return Result.success("操作成功", comparisonService.getCurrent());
    }

}
