package cn.djct.stockdemo.controller;

import cn.djct.stockdemo.common.Result;
import cn.djct.stockdemo.pojo.vo.StockFundFlowSynchronizeRespVo;
import cn.djct.stockdemo.service.StockFundFlowSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 股票资金流向接口。
 */
@Tag(name = "股票资金流向")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stockFundFlow")
public class StockFundFlowController {

    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");

    private final StockFundFlowSyncService stockFundFlowSyncService;

    @Operation(summary = "手动同步当天股票资金流向")
    @PostMapping("/synchronize")
    public Result<StockFundFlowSynchronizeRespVo> synchronize() {
        LocalDate tradeDate = LocalDate.now(SHANGHAI_ZONE);
        int savedCount = stockFundFlowSyncService.synchronize(tradeDate);
        StockFundFlowSynchronizeRespVo response = StockFundFlowSynchronizeRespVo.builder()
                .tradeDate(tradeDate)
                .savedCount(savedCount)
                .build();
        return Result.success("操作成功", response);
    }
}
