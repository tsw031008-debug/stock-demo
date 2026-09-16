package cn.djct.stockdemo.service.indexdivergence;

import java.time.LocalDateTime;

/**
 * 指数MACD背离信号服务。
 */
public interface IndexDivergenceSignalService {

    /**
     * 计算并保存指定分钟新确认的背离信号。
     *
     * @param quoteTime 已入库的当前行情分钟
     * @return 当前分钟新确认的信号数量
     */
    int calculateAndSave(LocalDateTime quoteTime);

    /** 补录后幂等保存检查时刻之前的当日信号；午休和盘后分别截到11:30和15:00。 */
    int recoverAndSave(LocalDateTime checkTime);
}
