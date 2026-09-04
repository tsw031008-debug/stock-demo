package cn.djct.stockdemo.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 四大类展示数据。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockCustomCategoryDto {
    private String categoryCode;
    private String categoryName;
}
