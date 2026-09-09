package cn.djct.stockdemo.pojo.dto;

import cn.djct.stockdemo.pojo.vo.IndexStyleItemRespVo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 四个固定指数的五日强弱比较结果。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IndexStyleComparisonDto {

    private LocalDate statisticsDate;
    private List<LocalDate> tradeDates;
    private List<IndexStyleItemRespVo> indices;
}
