package cn.djct.stockdemo.service.plate;

import cn.djct.stockdemo.pojo.dto.StockPlateSourceDto;

import java.util.List;

/**
 * 股票板块数据源服务。
 */
public interface StockPlateSourceService {

    /**
     * 获取完整板块和成分股快照。
     *
     * @return 板块快照列表
     */
    List<StockPlateSourceDto> fetchAll();
}
