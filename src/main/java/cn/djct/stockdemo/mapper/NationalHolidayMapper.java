package cn.djct.stockdemo.mapper;

import cn.djct.stockdemo.pojo.entity.NationalHoliday;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 国家节假日数据访问接口。
 */
@Mapper
public interface NationalHolidayMapper {

    /**
     * 查询指定日期范围内的国家节假日。
     *
     * @param startDate 开始日期，包含
     * @param endDate   结束日期，包含
     * @return 国家节假日列表
     */
    List<NationalHoliday> selectByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
