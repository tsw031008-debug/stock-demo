package cn.djct.stockdemo.controller;

import cn.djct.stockdemo.common.Result;
import cn.djct.stockdemo.pojo.vo.StockBasicSynchronizeRespVo;
import cn.djct.stockdemo.service.StockBasicSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 股票基础信息接口。
 */
@Tag(name = "股票基础信息")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stockBasic")
public class StockBasicController {

    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");

    private final StockBasicSyncService stockBasicSyncService;

    @Operation(summary = "手动同步当天股票基础信息")
    @PostMapping("/synchronize")
    public Result<StockBasicSynchronizeRespVo> synchronize() {
        LocalDate tradeDate = LocalDate.now(SHANGHAI_ZONE);
        int savedCount = stockBasicSyncService.synchronize(tradeDate);
        StockBasicSynchronizeRespVo response = StockBasicSynchronizeRespVo.builder()
                .tradeDate(tradeDate)
                .savedCount(savedCount)
                .build();
        return Result.success("操作成功", response);
    }
}
