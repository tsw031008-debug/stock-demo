package cn.djct.stockdemo.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页响应。
 *
 * @param <T> 响应记录类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageRespVo<T> {

    // 当前页码
    private int pageNum;
    // 每页大小
    private int pageSize;
    // 总记录数
    private long total;
    // 记录列表
    private List<T> records;
}
