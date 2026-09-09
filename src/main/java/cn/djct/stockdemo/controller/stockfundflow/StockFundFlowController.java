package cn.djct.stockdemo.controller.stockfundflow;

import cn.djct.stockdemo.common.Result;
import cn.djct.stockdemo.pojo.vo.PageRespVo;
import cn.djct.stockdemo.pojo.vo.StockFundFlowRespVo;
import cn.djct.stockdemo.pojo.vo.StockFundFlowSynchronizeRespVo;
import cn.djct.stockdemo.service.stockfundflow.StockFundFlowService;
import cn.djct.stockdemo.service.stockfundflow.StockFundFlowSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    private final StockFundFlowService stockFundFlowService;
    private final StockFundFlowSyncService stockFundFlowSyncService;

    /**
     * 分页查询指定交易日的股票资金流向。
     *
     * @param tradeDate 交易日期
     * @param pageNum   页码，默认1
     * @param pageSize  每页数量，默认20，最大100
     * @return 股票资金流向分页响应
     */
    @Operation(summary = "按日期查询股票资金流向")
    @GetMapping
    public Result<PageRespVo<StockFundFlowRespVo>> findByTradeDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeDate,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return Result.success("操作成功", stockFundFlowService.findByTradeDate(tradeDate, pageNum, pageSize));
    }

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

    /**
     * 将资金流向业务数据转换为接口响应。
     *
     * @param fundFlow 资金流向业务数据
     * @return 资金流向接口响应
     */

}
