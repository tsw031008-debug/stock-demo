package cn.djct.stockdemo.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 自定义子板块业务数据。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockCustomPlateDto {
    private Long id;
    private String categoryCode;
    private String categoryName;
    private String plateName;
}
