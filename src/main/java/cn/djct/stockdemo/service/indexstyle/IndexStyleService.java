package cn.djct.stockdemo.service.indexstyle;

import cn.djct.stockdemo.pojo.dto.IndexStyleComparisonDto;

/**
 * 指数大小风格查询服务。
 */
public interface IndexStyleService {

    /**
     * 查询最近五个完整交易日的指数强弱结果。
     *
     * @return 指数五日强弱比较
     */
    IndexStyleComparisonDto getLatest();
}
