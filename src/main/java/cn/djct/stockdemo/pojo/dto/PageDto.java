package cn.djct.stockdemo.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页业务数据。
 *
 * @param <T> 记录类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageDto<T> {

    private int pageNum;
    private int pageSize;
    private long total;
    private List<T> records;
}
