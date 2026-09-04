package cn.djct.stockdemo.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 自定义子板块成分股实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockCustomPlateMember {

    private Long id;
    private Long customPlateId;
    private String stockCode;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
