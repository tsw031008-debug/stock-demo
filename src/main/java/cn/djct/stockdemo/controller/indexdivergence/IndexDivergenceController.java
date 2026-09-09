package cn.djct.stockdemo.controller.indexdivergence;

import cn.djct.stockdemo.common.Result;
import cn.djct.stockdemo.pojo.vo.IndexDivergenceLatestRespVo;
import cn.djct.stockdemo.service.indexdivergence.IndexDivergenceQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 指数背离查询接口。
 */
@Tag(name = "指数背离")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/indexDivergence")
public class IndexDivergenceController {

    private final IndexDivergenceQueryService indexDivergenceQueryService;

    /**
     * 查询最新交易日的上证指数分钟曲线、昨收和背离信号。
     */
    @Operation(summary = "查询最新上证指数曲线和背离信号")
    @GetMapping("/latest")
    public Result<IndexDivergenceLatestRespVo> getLatest() {
        return Result.success("操作成功", indexDivergenceQueryService.getLatest());
    }

}
