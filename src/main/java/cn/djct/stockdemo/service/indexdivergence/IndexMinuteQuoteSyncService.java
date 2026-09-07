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

    /**
     * 检查并补齐指定时间之前缺失的上证指数分钟行情。
     *
     * @param checkTime 完整性检查时间
     * @return 补录记录数，无缺口或不需要检查时返回0
     */
    int recoverMissingMinutes(LocalDateTime checkTime);
}
