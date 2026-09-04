package cn.djct.stockdemo.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 一个固定指数的五日强弱结果。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IndexStyleItemDto {

    private String indexCode;
    private String indexName;
    private List<IndexDailyStyleDto> dailyStyles;
}
