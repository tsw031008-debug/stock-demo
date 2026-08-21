package cn.djct.stockdemo.controller;

import cn.djct.stockdemo.common.Result;
import cn.djct.stockdemo.pojo.vo.StockDailyQuoteSynchronizeRespVo;
import cn.djct.stockdemo.service.StockDailyQuoteSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 股票日行情接口。
 */
@Tag(name = "股票日行情")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stockDailyQuote")
public class StockDailyQuoteController {

    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");

    private final StockDailyQuoteSyncService stockDailyQuoteSyncService;

    /**
     * 手动同步当天股票日行情。
     * @return 操作结果
     */
    @Operation(summary = "手动同步当天股票日行情")
    @PostMapping("/synchronize")
    public Result<StockDailyQuoteSynchronizeRespVo> synchronize() {
        //获得当前交易日
        LocalDate tradeDate = LocalDate.now(SHANGHAI_ZONE);
        //同步数据
        int savedCount = stockDailyQuoteSyncService.synchronize(tradeDate);
        //构建响应
        StockDailyQuoteSynchronizeRespVo response = StockDailyQuoteSynchronizeRespVo.builder()
                .tradeDate(tradeDate)
                .savedCount(savedCount)
                .build();
        return Result.success("操作成功", response);
    }
}
