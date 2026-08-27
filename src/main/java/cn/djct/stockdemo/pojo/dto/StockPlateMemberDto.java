package cn.djct.stockdemo.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 板块有效成分股原始数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockPlateMemberDto {

    private Long plateId;
    private String plateName;
    private String stockCode;
}
