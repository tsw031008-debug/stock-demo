package cn.djct.stockdemo.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 四大类自定义子板块实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockCustomPlate {

    private Long id;
    private String categoryCode;
    private String plateName;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
