package cn.djct.stockdemo.service.indexdivergence;

import java.time.LocalDateTime;

/**
 * 指数分钟行情同步服务。
 */
public interface IndexMinuteQuoteSyncService {

    /**
     * 同步指定分钟的上证指数行情。
     *
     * @param triggerTime 任务触发时间
     * @return 保存记录数，非采集时间返回0
     */
    int synchronize(LocalDateTime triggerTime);
}
