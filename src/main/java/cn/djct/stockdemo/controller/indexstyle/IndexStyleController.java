package cn.djct.stockdemo.controller.indexstyle;

import cn.djct.stockdemo.common.Result;
import cn.djct.stockdemo.pojo.dto.IndexDailyStyleDto;
import cn.djct.stockdemo.pojo.dto.IndexStyleComparisonDto;
import cn.djct.stockdemo.pojo.dto.IndexStyleItemDto;
import cn.djct.stockdemo.pojo.vo.IndexDailyStyleRespVo;
import cn.djct.stockdemo.pojo.vo.IndexStyleComparisonRespVo;
import cn.djct.stockdemo.pojo.vo.IndexStyleItemRespVo;
import cn.djct.stockdemo.service.indexstyle.IndexStyleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;

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

    private IndexStyleComparisonRespVo toResponse(IndexStyleComparisonDto comparison) {
        return IndexStyleComparisonRespVo.builder()
                .statisticsDate(comparison.getStatisticsDate())
                .tradeDates(comparison.getTradeDates().stream()
                        .map(DATE_FORMATTER::format)
                        .toList())
                .indices(comparison.getIndices().stream()
                        .map(this::toItemResponse)
                        .toList())
                .build();
    }

    private IndexStyleItemRespVo toItemResponse(IndexStyleItemDto item) {
        return IndexStyleItemRespVo.builder()
                .indexCode(item.getIndexCode())
                .indexName(item.getIndexName())
                .dailyStyles(item.getDailyStyles().stream()
                        .map(this::toDailyResponse)
                        .toList())
                .build();
    }

    private IndexDailyStyleRespVo toDailyResponse(IndexDailyStyleDto dailyStyle) {
        return IndexDailyStyleRespVo.builder()
                .tradeDate(dailyStyle.getTradeDate())
                .changePercent(dailyStyle.getChangePercent())
                .strengthType(dailyStyle.getStrengthType())
                .build();
    }
}
