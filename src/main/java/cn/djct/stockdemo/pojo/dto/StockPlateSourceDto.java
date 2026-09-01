package cn.djct.stockdemo.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 板块数据源快照。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockPlateSourceDto {

    private String plateName;
    private List<String> stockCodes;
}
