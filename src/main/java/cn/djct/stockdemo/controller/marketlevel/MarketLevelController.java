package cn.djct.stockdemo.controller.marketlevel;

import cn.djct.stockdemo.common.Result;
import cn.djct.stockdemo.pojo.dto.MarketLevelDto;
import cn.djct.stockdemo.pojo.dto.MarketPeriodComparisonDto;
import cn.djct.stockdemo.pojo.dto.MarketPeriodItemDto;
import cn.djct.stockdemo.pojo.dto.RecentStockRiseCountDto;
import cn.djct.stockdemo.pojo.vo.MarketLevelRespVo;
import cn.djct.stockdemo.pojo.vo.MarketPeriodComparisonRespVo;
import cn.djct.stockdemo.pojo.vo.MarketPeriodItemRespVo;
import cn.djct.stockdemo.pojo.vo.RecentStockRiseCountRespVo;
import cn.djct.stockdemo.service.marketlevel.MarketLevelService;
import cn.djct.stockdemo.service.marketlevel.MarketPeriodComparisonService;
import cn.djct.stockdemo.service.marketlevel.RecentStockRiseCountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 市场水位接口。
 */
@Tag(name = "市场水位")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/marketLevel")
public class MarketLevelController {

    private final MarketLevelService marketLevelService;
    private final MarketPeriodComparisonService marketPeriodComparisonService;
    private final RecentStockRiseCountService recentStockRiseCountService;

    /**
     * 查询最新市场水位。
     *
     * @return 最新市场水位
     */
    @Operation(summary = "查询最新市场水位")
    @GetMapping("/latest")
    public Result<MarketLevelRespVo> getLatest() {
        // 查询市场水位计算结果
        MarketLevelDto marketLevel = marketLevelService.getLatest();
        // 转换为响应对象
        MarketLevelRespVo response = MarketLevelRespVo.builder()
                .style(marketLevel.getStyle())
                .previousThreeDayAverageTurnoverYi(marketLevel.getPreviousThreeDayAverageTurnoverYi())
                .currentTurnoverYi(marketLevel.getCurrentTurnoverYi())
                .volumeRatio(marketLevel.getVolumeRatio())
                .build();
        return Result.success("操作成功", response);
    }

    /**
     * 查询两市周月平均成交额同比环比。
     *
     * @return 周月平均成交额同比环比
     */
    @Operation(summary = "查询两市周月平均成交额同比环比")
    @GetMapping("/periodComparison")
    public Result<MarketPeriodComparisonRespVo> getPeriodComparison() {
        // 查询市场水位计算结果
        MarketPeriodComparisonDto comparison = marketPeriodComparisonService.getLatest();
        MarketPeriodComparisonRespVo response = MarketPeriodComparisonRespVo.builder()
                .statisticsTradeDate(comparison.getStatisticsTradeDate())
                .weekly(comparison.getWeekly().stream().map(this::toResponse).toList())
                .monthly(comparison.getMonthly().stream().map(this::toResponse).toList())
                .build();
        return Result.success("操作成功", response);
    }

    /**
     * 查询近期股票涨幅家数。
     *
     * @return 最近10个交易日的5日和10日涨幅家数
     */
    @Operation(summary = "查询近期股票涨幅家数")
    @GetMapping("/recentStockCounts")
    public Result<List<RecentStockRiseCountRespVo>> getRecentStockCounts() {
        List<RecentStockRiseCountRespVo> response = recentStockRiseCountService.getLatest()
                .stream()
                .map(this::toResponse)
                .toList();
        return Result.success("操作成功", response);
    }

    private MarketPeriodItemRespVo toResponse(MarketPeriodItemDto item) {
        return MarketPeriodItemRespVo.builder()
                .comparisonType(item.getComparisonType())
                .periodLabel(item.getPeriodLabel())
                .startDate(item.getStartDate())
                .endDate(item.getEndDate())
                .tradingDayCount(item.getTradingDayCount())
                .averageTurnoverYi(item.getAverageTurnoverYi())
                .available(item.getAvailable())
                .build();
    }

    private RecentStockRiseCountRespVo toResponse(RecentStockRiseCountDto item) {
        return RecentStockRiseCountRespVo.builder()
                .tradeDate(item.getTradeDate())
                .fiveDayRiseCount(item.getFiveDayRiseCount())
                .tenDayRiseCount(item.getTenDayRiseCount())
                .build();
    }
}
