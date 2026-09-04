package cn.djct.stockdemo.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 自定义子板块与成分股关系。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockCustomPlateMemberRelationDto {

    private String categoryCode;
    private Long customPlateId;
    private String plateName;
    private String stockCode;
}
