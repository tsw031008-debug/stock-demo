package cn.djct.stockdemo.controller.marketlevel;

import cn.djct.stockdemo.common.Result;
import cn.djct.stockdemo.pojo.dto.MarketLevelDto;
import cn.djct.stockdemo.pojo.vo.MarketLevelRespVo;
import cn.djct.stockdemo.service.marketlevel.MarketLevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 市场水位接口。
 */
@Tag(name = "市场水位")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/marketLevel")
public class MarketLevelController {

    private final MarketLevelService marketLevelService;

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
}
