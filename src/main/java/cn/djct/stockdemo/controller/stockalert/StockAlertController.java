package cn.djct.stockdemo.controller.stockalert;

import cn.djct.stockdemo.common.Result;
import cn.djct.stockdemo.pojo.dto.PageDto;
import cn.djct.stockdemo.pojo.dto.StockOpenBoardAlertDto;
import cn.djct.stockdemo.pojo.dto.StockSpeedAlertDto;
import cn.djct.stockdemo.pojo.vo.PageRespVo;
import cn.djct.stockdemo.pojo.vo.StockOpenBoardAlertRespVo;
import cn.djct.stockdemo.pojo.vo.StockSpeedAlertRespVo;
import cn.djct.stockdemo.service.stockalert.StockAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 股票预警接口。
 */
@Tag(name = "股票预警")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stockAlert")
public class StockAlertController {

    private final StockAlertService stockAlertService;

    /**
     * 分页查询涨速预警股票。
     *
     * @param pageNum  页码，默认1
     * @param pageSize 每页数量，默认20，最大100
     * @return 涨速预警分页响应
     */
    @Operation(summary = "查询涨速预警股票")
    @GetMapping("/speed")
    public Result<PageRespVo<StockSpeedAlertRespVo>> findSpeedAlerts(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        // 查询涨速预警分页业务数据
        PageDto<StockSpeedAlertDto> page = stockAlertService.findSpeedAlerts(pageNum, pageSize);
        // 只转换接口需要展示的字段，斜率差不对外返回
        List<StockSpeedAlertRespVo> records = page.getRecords().stream()
                .map(alert -> StockSpeedAlertRespVo.builder()
                        .stockCode(alert.getStockCode())
                        .stockName(alert.getStockName())
                        .currentPrice(alert.getCurrentPrice())
                        .currentChangePercent(alert.getCurrentChangePercent())
                        .build())
                .toList();
        // 封装统一成功响应
        return Result.success("操作成功", toPageResponse(page, records));
    }

    /**
     * 分页查询开板提醒股票。
     *
     * @param pageNum  页码，默认1
     * @param pageSize 每页数量，默认20，最大100
     * @return 开板提醒分页响应
     */
    @Operation(summary = "查询开板提醒股票")
    @GetMapping("/openBoard")
    public Result<PageRespVo<StockOpenBoardAlertRespVo>> findOpenBoardAlerts(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        // 查询开板提醒分页业务数据
        PageDto<StockOpenBoardAlertDto> page = stockAlertService.findOpenBoardAlerts(pageNum, pageSize);
        // 转换接口展示字段，卖一量只参与筛选不对外返回
        List<StockOpenBoardAlertRespVo> records = page.getRecords().stream()
                .map(alert -> StockOpenBoardAlertRespVo.builder()
                        .stockCode(alert.getStockCode())
                        .stockName(alert.getStockName())
                        .currentPrice(alert.getCurrentPrice())
                        .currentChangePercent(alert.getCurrentChangePercent())
                        .turnoverYi(alert.getTurnoverYi())
                        .build())
                .toList();
        // 封装统一成功响应
        return Result.success("操作成功", toPageResponse(page, records));
    }

    /**
     * 将业务分页数据转换为接口分页响应。
     */
    private <S, T> PageRespVo<T> toPageResponse(PageDto<S> page, List<T> records) {
        // 保留Service分页信息并替换为接口响应记录
        return PageRespVo.<T>builder()
                .pageNum(page.getPageNum())
                .pageSize(page.getPageSize())
                .total(page.getTotal())
                .records(records)
                .build();
    }
}
