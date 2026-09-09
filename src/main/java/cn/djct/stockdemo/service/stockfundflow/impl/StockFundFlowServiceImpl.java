package cn.djct.stockdemo.service.stockfundflow.impl;

import cn.djct.stockdemo.mapper.StockFundFlowMapper;
import cn.djct.stockdemo.pojo.vo.PageRespVo;
import cn.djct.stockdemo.pojo.vo.StockFundFlowRespVo;
import cn.djct.stockdemo.pojo.entity.StockFundFlow;
import cn.djct.stockdemo.service.stockfundflow.StockFundFlowService;
import cn.djct.stockdemo.service.tradecalendar.TradeCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 股票资金流向服务实现。
 */
@Service
@RequiredArgsConstructor
public class StockFundFlowServiceImpl implements StockFundFlowService {

    private static final int SAVE_BATCH_SIZE = 500;
    private static final int MAX_PAGE_SIZE = 100;

    private final StockFundFlowMapper stockFundFlowMapper;
    private final TradeCalendarService tradeCalendarService;

    /**
     * 分页查询指定交易日的资金流向。
     *
     * @param tradeDate 交易日期
     * @param pageNum   页码
     * @param pageSize  每页数量
     * @return 资金流向分页数据
     */
    @Override
    public PageRespVo<StockFundFlowRespVo> findByTradeDate(
            LocalDate tradeDate,
            int pageNum,
            int pageSize
    ) {
        // 日期和分页参数必须在访问数据库前完成校验
        if (tradeDate == null) {
            throw new IllegalArgumentException("交易日期不能为空");
        }
        validatePage(pageNum, pageSize);
        // 所有业务日期均以交易日历为最终判断依据
        if (!tradeCalendarService.isTradingDay(tradeDate)) {
            throw new IllegalArgumentException("查询日期不是交易日");
        }

        int total = stockFundFlowMapper.countByTradeDate(tradeDate);
        long offset = (long) (pageNum - 1) * pageSize;
        // 无数据或页码超出范围时不执行无意义的分页查询
        List<StockFundFlowRespVo> records = total == 0 || offset >= total
                ? List.of()
                : stockFundFlowMapper.selectPageByTradeDate(tradeDate, offset, pageSize);
        return PageRespVo.<StockFundFlowRespVo>builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .total(total)
                .records(records)
                .build();
    }

    @Override
    public int countByTradeDate(LocalDate tradeDate) {
        Objects.requireNonNull(tradeDate, "交易日期不能为空");
        return stockFundFlowMapper.countByTradeDate(tradeDate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveSnapshot(List<StockFundFlow> fundFlows) {
        Objects.requireNonNull(fundFlows, "股票资金流向不能为空");
        if (fundFlows.isEmpty()) {
            throw new IllegalArgumentException("股票资金流向不能为空");
        }
        LocalDate tradeDate = fundFlows.get(0).getTradeDate();
        if (fundFlows.stream().anyMatch(fundFlow -> !tradeDate.equals(fundFlow.getTradeDate()))) {
            throw new IllegalArgumentException("股票资金流向必须属于同一交易日");
        }

        for (int startIndex = 0; startIndex < fundFlows.size(); startIndex += SAVE_BATCH_SIZE) {
            int endIndex = Math.min(startIndex + SAVE_BATCH_SIZE, fundFlows.size());
            stockFundFlowMapper.upsertBatch(
                    new ArrayList<>(fundFlows.subList(startIndex, endIndex))
            );
        }
        int savedCount = stockFundFlowMapper.countByTradeDate(tradeDate);
        if (savedCount != fundFlows.size()) {
            throw new IllegalStateException("股票资金流向落库数量不一致，expected="
                    + fundFlows.size() + "，actual=" + savedCount);
        }
        return fundFlows.size();
    }

    /**
     * 校验分页参数，限制单次查询返回数量。
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     */
    private void validatePage(int pageNum, int pageSize) {
        if (pageNum < 1) {
            throw new IllegalArgumentException("页码必须大于等于1");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("每页数量必须在1到100之间");
        }
    }
}
