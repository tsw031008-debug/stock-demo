package cn.djct.stockdemo.controller.plate;

import cn.djct.stockdemo.common.Result;
import cn.djct.stockdemo.pojo.dto.PageDto;
import cn.djct.stockdemo.pojo.dto.StockCategoryTurnoverComparisonDto;
import cn.djct.stockdemo.pojo.dto.StockCategoryTurnoverItemDto;
import cn.djct.stockdemo.pojo.dto.StockCustomCategoryDto;
import cn.djct.stockdemo.pojo.dto.StockCustomPlateDto;
import cn.djct.stockdemo.pojo.dto.StockCustomPlateMemberDto;
import cn.djct.stockdemo.pojo.dto.StockCustomPlateSaveDto;
import cn.djct.stockdemo.pojo.vo.PageRespVo;
import cn.djct.stockdemo.pojo.vo.StockCategoryTurnoverComparisonRespVo;
import cn.djct.stockdemo.pojo.vo.StockCategoryTurnoverItemRespVo;
import cn.djct.stockdemo.pojo.vo.StockCustomCategoryRespVo;
import cn.djct.stockdemo.pojo.vo.StockCustomPlateMemberRespVo;
import cn.djct.stockdemo.pojo.vo.StockCustomPlateRespVo;
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
        List<StockCustomCategoryRespVo> response = stockCustomPlateService.findCategories()
                .stream()
                .map(this::toCategoryResponse)
                .toList();
        return Result.success("操作成功", response);
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
        return Result.success(
                "操作成功",
                toPlatePage(stockCustomPlateService.findPlates(categoryCode, pageNum, pageSize))
        );
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
        return Result.success(
                "操作成功",
                toMemberPage(stockCustomPlateService.findMembers(plateId, pageNum, pageSize))
        );
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
        StockCustomPlateDto plate = stockCustomPlateService.create(request);
        return Result.success("操作成功", toPlateResponse(plate));
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
        StockCustomPlateDto plate = stockCustomPlateService.update(plateId, request);
        return Result.success("操作成功", toPlateResponse(plate));
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
        return Result.success("操作成功", toComparisonResponse(comparisonService.getCurrent()));
    }

    private StockCustomCategoryRespVo toCategoryResponse(StockCustomCategoryDto category) {
        return StockCustomCategoryRespVo.builder()
                .categoryCode(category.getCategoryCode())
                .categoryName(category.getCategoryName())
                .build();
    }

    private StockCustomPlateRespVo toPlateResponse(StockCustomPlateDto plate) {
        return StockCustomPlateRespVo.builder()
                .id(plate.getId())
                .categoryCode(plate.getCategoryCode())
                .categoryName(plate.getCategoryName())
                .plateName(plate.getPlateName())
                .build();
    }

    private PageRespVo<StockCustomPlateRespVo> toPlatePage(PageDto<StockCustomPlateDto> page) {
        return PageRespVo.<StockCustomPlateRespVo>builder()
                .pageNum(page.getPageNum())
                .pageSize(page.getPageSize())
                .total(page.getTotal())
                .records(page.getRecords().stream().map(this::toPlateResponse).toList())
                .build();
    }

    private PageRespVo<StockCustomPlateMemberRespVo> toMemberPage(
            PageDto<StockCustomPlateMemberDto> page
    ) {
        return PageRespVo.<StockCustomPlateMemberRespVo>builder()
                .pageNum(page.getPageNum())
                .pageSize(page.getPageSize())
                .total(page.getTotal())
                .records(page.getRecords().stream()
                        .map(member -> StockCustomPlateMemberRespVo.builder()
                                .stockCode(member.getStockCode())
                                .stockName(member.getStockName())
                                .build())
                        .toList())
                .build();
    }

    private StockCategoryTurnoverComparisonRespVo toComparisonResponse(
            StockCategoryTurnoverComparisonDto comparison
    ) {
        return StockCategoryTurnoverComparisonRespVo.builder()
                .statisticsDate(comparison.getStatisticsDate())
                .previousTradeDate(comparison.getPreviousTradeDate())
                .categories(comparison.getCategories().stream()
                        .map(this::toTurnoverItemResponse)
                        .toList())
                .build();
    }

    private StockCategoryTurnoverItemRespVo toTurnoverItemResponse(
            StockCategoryTurnoverItemDto item
    ) {
        return StockCategoryTurnoverItemRespVo.builder()
                .categoryCode(item.getCategoryCode())
                .categoryName(item.getCategoryName())
                .stockCount(item.getStockCount())
                .currentTurnoverYi(item.getCurrentTurnoverYi())
                .previousTurnoverYi(item.getPreviousTurnoverYi())
                .build();
    }
}
