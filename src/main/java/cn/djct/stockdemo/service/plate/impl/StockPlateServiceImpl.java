package cn.djct.stockdemo.service.plate.impl;

import cn.djct.stockdemo.mapper.StockPlateMapper;
import cn.djct.stockdemo.mapper.StockPlateMemberMapper;
import cn.djct.stockdemo.pojo.dto.StockPlateMemberDto;
import cn.djct.stockdemo.pojo.dto.StockPlateSourceDto;
import cn.djct.stockdemo.pojo.entity.StockPlate;
import cn.djct.stockdemo.pojo.entity.StockPlateMember;
import cn.djct.stockdemo.service.plate.StockPlateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 股票板块持久化服务实现。
 */
@Service
@RequiredArgsConstructor
public class StockPlateServiceImpl implements StockPlateService {

    public static final String DATA_SOURCE = "THS_GN_3_LEVEL";
    private static final int SAVE_BATCH_SIZE = 500;

    private final StockPlateMapper stockPlateMapper;
    private final StockPlateMemberMapper stockPlateMemberMapper;

    /**
     * 判断指定交易日的板块快照是否已经同步。
     *
     * @param tradeDate 交易日
     * @return 是否已经同步
     */
    @Override
    public boolean hasSynchronized(LocalDate tradeDate) {
        Objects.requireNonNull(tradeDate, "交易日期不能为空");
        return stockPlateMapper.countActivePlates(DATA_SOURCE, tradeDate) > 0;
    }

    /**
     * 事务保存完整板块和成分股快照。
     *
     * @param tradeDate 交易日
     * @param plates    板块快照
     * @return 保存的板块数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveSnapshot(LocalDate tradeDate, List<StockPlateSourceDto> plates) {
        // 校验完整快照，并禁止重复板块名称导致覆盖
        Objects.requireNonNull(tradeDate, "交易日期不能为空");
        Objects.requireNonNull(plates, "板块快照不能为空");
        if (plates.isEmpty()) {
            throw new IllegalArgumentException("板块快照不能为空");
        }
        Set<String> plateNames = plates.stream()
                .map(StockPlateSourceDto::getPlateName)
                .collect(Collectors.toSet());
        if (plateNames.size() != plates.size() || plateNames.contains(null)) {
            throw new IllegalArgumentException("板块快照存在重复或空板块名称");
        }

        // 先保存板块，再查询数据库生成成分关系需要的板块ID
        List<StockPlate> plateEntities = plates.stream()
                .map(plate -> StockPlate.builder()
                        .plateName(plate.getPlateName())
                        .dataSource(DATA_SOURCE)
                        .lastSeenTradeDate(tradeDate)
                        .active(true)
                        .build())
                .toList();
        savePlateBatches(plateEntities);
        Map<String, Long> plateIds = loadPlateIds(plateNames);

        // 按板块和股票代码生成去重后的成分关系
        List<StockPlateMember> members = new ArrayList<>();
        for (StockPlateSourceDto plate : plates) {
            Objects.requireNonNull(plate.getStockCodes(), "板块成分股不能为空");
            Set<String> stockCodes = Set.copyOf(plate.getStockCodes());
            if (stockCodes.isEmpty()) {
                throw new IllegalArgumentException("板块成分股不能为空，plateName=" + plate.getPlateName());
            }
            for (String stockCode : stockCodes) {
                members.add(StockPlateMember.builder()
                        .plateId(plateIds.get(plate.getPlateName()))
                        .stockCode(stockCode)
                        .lastSeenTradeDate(tradeDate)
                        .active(true)
                        .build());
            }
        }
        saveMemberBatches(members);

        // 只有完整快照成功保存后才失效未出现的旧板块和旧成分关系
        stockPlateMemberMapper.deactivateMembersNotSeen(DATA_SOURCE, tradeDate);
        stockPlateMapper.deactivatePlatesNotSeen(DATA_SOURCE, tradeDate);
        validateSavedCounts(tradeDate, plates.size(), members.size());
        return plates.size();
    }

    /**
     * 查询当前有效板块成分关系。
     *
     * @return 有效成分关系列表
     */
    @Override
    public List<StockPlateMemberDto> findActiveMembers() {
        //同花顺三级概念版块
        return stockPlateMemberMapper.selectActiveMembers(DATA_SOURCE);
    }

    /**
     * 分批保存板块，避免单条SQL参数过多。
     */
    private void savePlateBatches(List<StockPlate> plates) {
        for (int startIndex = 0; startIndex < plates.size(); startIndex += SAVE_BATCH_SIZE) {
            int endIndex = Math.min(startIndex + SAVE_BATCH_SIZE, plates.size());
            stockPlateMapper.upsertPlateBatch(new ArrayList<>(plates.subList(startIndex, endIndex)));
        }
    }

    /**
     * 查询并校验本次快照中全部板块的数据库ID。
     */
    private Map<String, Long> loadPlateIds(Set<String> expectedNames) {
        Map<String, Long> plateIds = new HashMap<>();
        for (StockPlate plate : stockPlateMapper.selectByDataSource(DATA_SOURCE)) {
            if (expectedNames.contains(plate.getPlateName())) {
                plateIds.put(plate.getPlateName(), plate.getId());
            }
        }
        if (plateIds.size() != expectedNames.size()) {
            throw new IllegalStateException("板块ID读取不完整，expected="
                    + expectedNames.size() + "，actual=" + plateIds.size());
        }
        return plateIds;
    }

    /**
     * 分批保存成分关系，避免单条SQL参数过多。
     */
    private void saveMemberBatches(List<StockPlateMember> members) {
        for (int startIndex = 0; startIndex < members.size(); startIndex += SAVE_BATCH_SIZE) {
            int endIndex = Math.min(startIndex + SAVE_BATCH_SIZE, members.size());
            stockPlateMemberMapper.upsertMemberBatch(new ArrayList<>(members.subList(startIndex, endIndex)));
        }
    }

    /**
     * 校验事务保存后的有效板块和成分关系数量。
     */
    private void validateSavedCounts(LocalDate tradeDate, int expectedPlates, int expectedMembers) {
        int actualPlates = stockPlateMapper.countActivePlates(DATA_SOURCE, tradeDate);
        int actualMembers = stockPlateMemberMapper.countActiveMembers(DATA_SOURCE, tradeDate);
        if (actualPlates != expectedPlates || actualMembers != expectedMembers) {
            throw new IllegalStateException("板块快照落库数量不一致，expectedPlates="
                    + expectedPlates + "，actualPlates=" + actualPlates
                    + "，expectedMembers=" + expectedMembers + "，actualMembers=" + actualMembers);
        }
    }
}
