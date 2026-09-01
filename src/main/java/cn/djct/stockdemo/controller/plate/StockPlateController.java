package cn.djct.stockdemo.controller.plate;

import cn.djct.stockdemo.common.Result;
import cn.djct.stockdemo.pojo.dto.PageDto;
import cn.djct.stockdemo.pojo.dto.StockPlateLimitUpDto;
import cn.djct.stockdemo.pojo.vo.PageRespVo;
import cn.djct.stockdemo.pojo.vo.StockPlateLimitUpRespVo;
import cn.djct.stockdemo.pojo.vo.StockPlateOperationRespVo;
import cn.djct.stockdemo.service.plate.StockPlateDailyQuoteService;
import cn.djct.stockdemo.service.plate.StockPlateQueryService;
import cn.djct.stockdemo.service.plate.StockPlateSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * 股票板块接口。
 */
@Tag(name = "股票板块")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/plate")
public class StockPlateController {

    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");

    private final StockPlateQueryService stockPlateQueryService;
    private final StockPlateSyncService stockPlateSyncService;
    private final StockPlateDailyQuoteService stockPlateDailyQuoteService;

    /**
     * 分页查询当天最新板块涨停统计。
     *
     * @param pageNum  页码，默认1
     * @param pageSize 每页数量，默认20，最大100
     * @return 板块涨停统计分页响应
     */
    @Operation(summary = "查询当天板块涨停统计")
    @GetMapping("/limitUpStatistics")
    public Result<PageRespVo<StockPlateLimitUpRespVo>> findLimitUpStatistics(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        // 查询分页业务数据并只转换文档明确要求的五个响应字段
        PageDto<StockPlateLimitUpDto> page = stockPlateQueryService.findLimitUpStatistics(
                pageNum,
                pageSize
        );
        List<StockPlateLimitUpRespVo> records = page.getRecords().stream()
                .map(this::toResponse)
                .toList();
        PageRespVo<StockPlateLimitUpRespVo> response = PageRespVo.<StockPlateLimitUpRespVo>builder()
                .pageNum(page.getPageNum())
                .pageSize(page.getPageSize())
                .total(page.getTotal())
                .records(records)
                .build();
        return Result.success("操作成功", response);
    }

    /**
     * 手动同步当天板块和成分股快照。
     *
     * @return 同步结果
     */
    @Operation(summary = "手动同步当天板块和成分股")
    @PostMapping("/synchronize")
    public Result<StockPlateOperationRespVo> synchronize() {
        LocalDate tradeDate = LocalDate.now(SHANGHAI_ZONE);
        int savedCount = stockPlateSyncService.synchronize(tradeDate);
        return Result.success("操作成功", operationResponse(tradeDate, savedCount));
    }

    /**
     * 手动计算并保存当天板块日线。
     *
     * @return 同步结果
     */
    @Operation(summary = "手动同步当天板块日线")
    @PostMapping("/synchronizeDailyQuote")
    public Result<StockPlateOperationRespVo> synchronizeDailyQuote() {
        LocalDate tradeDate = LocalDate.now(SHANGHAI_ZONE);
        int savedCount = stockPlateDailyQuoteService.synchronize(tradeDate);
        return Result.success("操作成功", operationResponse(tradeDate, savedCount));
    }

//    历史板块日线当前不在功能5的查询范围，且PARTIAL回补需要先增加COMPLETE数据保护，暂不开放接口。
//    @Operation(summary = "回补当年板块日线")
//    @PostMapping("/backfillDailyQuotes")
//    public Result<StockPlateOperationRespVo> backfillDailyQuotes(
//            @RequestParam(required = false) LocalDate endDate
//    ) {
//        LocalDate actualEndDate = endDate == null ? LocalDate.now(SHANGHAI_ZONE) : endDate;
//        int savedCount = stockPlateDailyQuoteService.backfillCurrentYear(actualEndDate);
//        return Result.success("操作成功", operationResponse(actualEndDate, savedCount));
//    }

    /**
     * 将板块涨停业务数据转换为接口响应。
     */
    private StockPlateLimitUpRespVo toResponse(StockPlateLimitUpDto statistic) {
        return StockPlateLimitUpRespVo.builder()
                .plateName(statistic.getPlateName())
                .totalStockCount(statistic.getTotalStockCount())
                .plateChangePercent(statistic.getPlateChangePercent())
                .limitUpStockCount(statistic.getLimitUpStockCount())
                .limitUpRatio(statistic.getLimitUpRatio())
                .build();
    }

    /**
     * 构建板块同步或回补操作响应。
     */
    private StockPlateOperationRespVo operationResponse(LocalDate tradeDate, int savedCount) {
        return StockPlateOperationRespVo.builder()
                .tradeDate(tradeDate)
                .savedCount(savedCount)
                .build();
    }
}
