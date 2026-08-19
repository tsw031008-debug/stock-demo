package cn.djct.stockdemo.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 国家节假日实体。
 */
@TableName("national_holiday")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NationalHoliday {
    /**
     * 主键。
     */
    private Long id;

    /**
     * 节假日日期。
     */
    private LocalDate holidayDate;

    /**
     * 节假日名称。
     */
    private String holidayName;

    /**
     * 国务院通知标题。
     */
    private String sourceTitle;

    /**
     * 国务院通知地址。
     */
    private String sourceUrl;

    /**
     * 补充说明。
     */
    private String remark;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
