package cn.djct.stockdemo.controller.tradecalendar;

import cn.djct.stockdemo.common.Result;
import cn.djct.stockdemo.pojo.vo.TradingDayOffsetRespVo;
import cn.djct.stockdemo.pojo.vo.TradingDayRespVo;
import cn.djct.stockdemo.pojo.vo.TradeCalendarInitializeRespVo;
import cn.djct.stockdemo.pojo.vo.TradeCalendarInitializeVo;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 交易日历接口。
 */
@Tag(name = "交易日历")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tradeCalendar")
public class TradeCalendarController {

    private final TradeCalendarService tradeCalendarService;

    /**
     * 判断指定日期是否为交易日。
      * @param date 指定日期
     * @return 是否为交易日
     */
    @Operation(summary = "判断指定日期是否为交易日")
    @GetMapping("/tradingDay")
    public Result<TradingDayRespVo> isTradingDay(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        //创建响应实体
        TradingDayRespVo response = TradingDayRespVo.builder()
                .date(date)
                .tradingDay(tradeCalendarService.isTradingDay(date))
                .build();

        return Result.success("操作成功", response);
    }

    /**
     * 查询指定日期之前的第N个交易日。
     * @param date 指定日期
     * @param offset 第N个交易日的偏移量
     */
    @Operation(summary = "查询指定日期之前的第N个交易日")
    @GetMapping("/previousTradingDay")
    public Result<TradingDayOffsetRespVo> getPreviousTradingDay(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam int offset
    ) {
        TradingDayOffsetRespVo response = TradingDayOffsetRespVo.builder()
                .baseDate(date)
                .offset(offset)
                .tradingDate(tradeCalendarService.getPreviousTradingDay(date, offset))
                .build();

        return Result.success("操作成功", response);
    }

    /**
     * 查询指定日期之后的第N个交易日。
     * @param date 指定日期
     * @param offset 第N个交易日的偏移量
     */
    @Operation(summary = "查询指定日期之后的第N个交易日")
    @GetMapping("/nextTradingDay")
    public Result<TradingDayOffsetRespVo> getNextTradingDay(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam int offset
    ) {
        TradingDayOffsetRespVo response = TradingDayOffsetRespVo.builder()
                .baseDate(date)
                .offset(offset)
                .tradingDate(tradeCalendarService.getNextTradingDay(date, offset))
                .build();

        return Result.success("操作成功", response);
    }

    /**
     * 初始化指定日期范围内的交易日历。
     * @param request 交易日历初始化请求参数
     * @return 初始化结果
     */
    @Operation(summary = "初始化指定日期范围内的交易日历")
    @PostMapping("/initialize")
    public Result<TradeCalendarInitializeRespVo> initialize(
            @Valid @RequestBody TradeCalendarInitializeVo request
    ) {
        //指定起始日期和结束日期，返回新增数量
        int insertedCount = tradeCalendarService.initialize(
                request.getStartDate(),
                request.getEndDate()
        );

        TradeCalendarInitializeRespVo response = TradeCalendarInitializeRespVo.builder()
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .insertedCount(insertedCount)
                .build();

        return Result.success("操作成功", response);
    }
}
