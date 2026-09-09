package cn.djct.stockdemo.service.stockalert;

import cn.djct.stockdemo.pojo.vo.PageRespVo;
import cn.djct.stockdemo.pojo.vo.StockOpenBoardAlertRespVo;
import cn.djct.stockdemo.pojo.dto.StockSpeedAlertDto;

/**
 * 股票预警服务。
 */
public interface StockAlertService {

    /**
     * 分页查询涨速预警股票。
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 涨速预警分页数据
     */
    PageRespVo<StockSpeedAlertDto> findSpeedAlerts(int pageNum, int pageSize);

    /**
     * 分页查询开板提醒股票。
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 开板提醒分页数据
     */
    PageRespVo<StockOpenBoardAlertRespVo> findOpenBoardAlerts(int pageNum, int pageSize);
}
