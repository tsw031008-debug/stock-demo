package cn.djct.stockdemo.service.plate;

import cn.djct.stockdemo.pojo.dto.StockPlateMemberDto;
import cn.djct.stockdemo.pojo.dto.StockPlateSourceDto;

import java.time.LocalDate;
import java.util.List;

/**
 * 股票板块持久化服务。
 */
public interface StockPlateService {

    /**
     * 判断指定交易日的板块快照是否已经同步。
     *
     * @param tradeDate 交易日
     * @return 是否已经同步
     */
    boolean hasSynchronized(LocalDate tradeDate);

    /**
     * 保存完整板块和成分股快照。
     *
     * @param tradeDate 交易日
     * @param plates    板块快照
     * @return 保存的板块数量
     */
    int saveSnapshot(LocalDate tradeDate, List<StockPlateSourceDto> plates);

    /**
     * 查询当前有效板块成分关系。
     *
     * @return 有效成分关系列表
     */
    List<StockPlateMemberDto> findActiveMembers();
}
