package cn.djct.stockdemo.controller.marketlevel;

import cn.djct.stockdemo.common.Result;
import cn.djct.stockdemo.pojo.vo.MarketLevelRespVo;
import cn.djct.stockdemo.pojo.vo.MarketPeriodComparisonRespVo;
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
        return Result.success("操作成功", marketLevelService.getLatest());
    }

    /**
     * 查询两市周月平均成交额同比环比。
     *
     * @return 周月平均成交额同比环比
     */
    @Operation(summary = "查询两市周月平均成交额同比环比")
    @GetMapping("/periodComparison")
    public Result<MarketPeriodComparisonRespVo> getPeriodComparison() {
        return Result.success("操作成功", marketPeriodComparisonService.getLatest());
    }

    /**
     * 查询近期股票涨幅家数。
     *
     * @return 最近10个交易日的5日和10日涨幅家数
     */
    @Operation(summary = "查询近期股票涨幅家数")
    @GetMapping("/recentStockCounts")
    public Result<List<RecentStockRiseCountRespVo>> getRecentStockCounts() {
        return Result.success("操作成功", recentStockRiseCountService.getLatest());
    }

}
