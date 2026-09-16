package cn.djct.stockdemo.controller.indexstyle;

import cn.djct.stockdemo.common.Result;
import cn.djct.stockdemo.pojo.vo.StockIndexDifferenceRespVo;
import cn.djct.stockdemo.service.indexstyle.StockIndexDifferenceService;
import cn.djct.stockdemo.pojo.vo.IndexEtfComparisonRespVo;
import cn.djct.stockdemo.pojo.dto.IndexStyleComparisonDto;
import cn.djct.stockdemo.pojo.vo.IndexStyleComparisonRespVo;
import cn.djct.stockdemo.service.indexstyle.IndexStyleService;
import cn.djct.stockdemo.service.indexstyle.IndexEtfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 指数大小风格查询接口。
 */
@Tag(name = "指数大小风格")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/indexStyle")
public class IndexStyleController {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final IndexStyleService indexStyleService;
    private final IndexEtfService indexEtfService;
    private final StockIndexDifferenceService stockIndexDifferenceService;

    /** 按交易日返回个股相对沪深300的累计差异数量及文档口径饼图占比。 */
    @Operation(summary = "查询个股与沪深300差异分布", description = "tradeDate必填，格式yyyy-MM-dd。盘后按两交易日未复权收盘价计算；"
            + "统计查询日已落库的所有股票，不过滤ST，缺少有效价格时跳过并返回数量。"
            + "差异=股票收盘价比值-指数收盘价比值，严格比较±1%、±3%、±5%、±7%，累计计数允许重复。"
            + "饼图占比=本项数量/同侧四项累计数量之和×100，保留2位小数，分母为0时为null；"
            + "不代表互斥区间占比。沪深300任一天或股票整日行情缺失时提示错误，不回退日期。")
    @GetMapping("/stockDifference")
    public Result<StockIndexDifferenceRespVo> getStockDifference(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeDate) {
        return Result.success("操作成功", stockIndexDifferenceService.findByTradeDate(tradeDate));
    }

    /**
     * 查询四个固定指数最近五个完整交易日的强弱结果。
     *
     * @return 指数五日强弱比较
     */
    @Operation(summary = "查询四个指数的五日强弱")
    @GetMapping("/latest")
    public Result<IndexStyleComparisonRespVo> getLatest() {
        return Result.success("操作成功", toResponse(indexStyleService.getLatest()));
    }

    /**
     * 查询四只固定指数ETF最新完整交易日的5日涨幅。
     *
     * @return ETF 5日涨幅比较
     */
    @Operation(summary = "查询四只指数ETF的5日涨幅")
    @GetMapping("/etfChange")
    public Result<IndexEtfComparisonRespVo> getEtfChange() {
        return Result.success("操作成功", indexEtfService.getLatest());
    }

    private IndexStyleComparisonRespVo toResponse(IndexStyleComparisonDto comparison) {
        return IndexStyleComparisonRespVo.builder()
                .statisticsDate(comparison.getStatisticsDate())
                .tradeDates(comparison.getTradeDates().stream()
                        .map(DATE_FORMATTER::format)
                        .toList())
                .indices(comparison.getIndices())
                .build();
    }

}
