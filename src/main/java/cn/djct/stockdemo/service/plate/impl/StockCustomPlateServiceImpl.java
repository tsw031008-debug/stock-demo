package cn.djct.stockdemo.service.plate.impl;

import cn.djct.stockdemo.constant.StockCustomCategory;
import cn.djct.stockdemo.mapper.StockBasicMapper;
import cn.djct.stockdemo.mapper.StockCustomPlateMapper;
import cn.djct.stockdemo.pojo.dto.PageDto;
import cn.djct.stockdemo.pojo.dto.StockCustomCategoryDto;
import cn.djct.stockdemo.pojo.dto.StockCustomPlateDto;
import cn.djct.stockdemo.pojo.dto.StockCustomPlateMemberDto;
import cn.djct.stockdemo.pojo.dto.StockCustomPlateMemberRelationDto;
import cn.djct.stockdemo.pojo.dto.StockCustomPlateSaveDto;
import cn.djct.stockdemo.pojo.entity.StockBasic;
import cn.djct.stockdemo.pojo.entity.StockCustomPlate;
import cn.djct.stockdemo.pojo.entity.StockCustomPlateMember;
import cn.djct.stockdemo.service.plate.StockCustomPlateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 四大类自定义子板块配置服务实现。
 */
@Service
@RequiredArgsConstructor
public class StockCustomPlateServiceImpl implements StockCustomPlateService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MEMBER_BATCH_SIZE = 500;

    private final StockCustomPlateMapper stockCustomPlateMapper;
    private final StockBasicMapper stockBasicMapper;

    /**
     * 按产品固定顺序返回四大类，不从数据库动态生成大类。
     *
     * @return 固定顺序的四大类
     */
    @Override
    public List<StockCustomCategoryDto> findCategories() {
        return Arrays.stream(StockCustomCategory.values())
                .map(category -> StockCustomCategoryDto.builder()
                        .categoryCode(category.name())
                        .categoryName(category.getDisplayName())
                        .build())
                .toList();
    }

    /**
     * 分页查询指定大类下未被软删除的子板块。
     *
     * @param categoryCode 四大类编码
     * @param pageNum 页码，从1开始
     * @param pageSize 每页数量，最大100
     * @return 有效子板块分页数据
     */
    @Override
    public PageDto<StockCustomPlateDto> findPlates(String categoryCode, int pageNum, int pageSize) {
        // 校验四大板块代码
        StockCustomCategory category = StockCustomCategory.fromCode(categoryCode);
        validatePage(pageNum, pageSize);
        // 查询有效子板块数量
        int total = stockCustomPlateMapper.countActiveByCategory(category.name());
        int offset = (pageNum - 1) * pageSize;
        List<StockCustomPlateDto> records = stockCustomPlateMapper
                .selectActiveByCategory(category.name(), offset, pageSize)
                .stream()
                .map(this::toDto)
                .toList();
        return PageDto.<StockCustomPlateDto>builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .total(total)
                .records(records)
                .build();
    }

    /**
     * 分页查询指定有效子板块当前生效的成分股及股票名称。
     *
     * @param plateId 子板块编号
     * @param pageNum 页码，从1开始
     * @param pageSize 每页数量，最大100
     * @return 成分股分页数据
     */
    @Override
    public PageDto<StockCustomPlateMemberDto> findMembers(Long plateId, int pageNum, int pageSize) {
        // 校验子板块存在且未被软删除
        requireActivePlate(plateId);
        // 校验页码和页大小
        validatePage(pageNum, pageSize);
        // 查询有效成分股数量
        int total = stockCustomPlateMapper.countActiveMembers(plateId);
        int offset = (pageNum - 1) * pageSize;
        return PageDto.<StockCustomPlateMemberDto>builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .total(total)
                .records(stockCustomPlateMapper.selectActiveMembers(plateId, offset, pageSize))
                .build();
    }

    /**
     * 新增子板块及其完整成分股列表。
     *
     * @param request 子板块及完整成分股请求参数
     * @return 新增后的子板块
     */
    @Override
    @Transactional
    public StockCustomPlateDto create(StockCustomPlateSaveDto request) {
        //校验四大板块代码
        StockCustomCategory category = StockCustomCategory.fromCode(request.getCategoryCode());
        //校验股票代码
        List<String> actualCodes = validateAndNormalizeStockCodes(request.getStockCodes());
        //校验板块名称
        String actualName = normalizePlateName(request.getPlateName());
        //新增时校验同一类下是否存在同名的子版块，不排除任何板块id
        validateUniqueName(category, actualName, null);
        StockCustomPlate plate = StockCustomPlate.builder()
                .categoryCode(category.name())
                .plateName(actualName)
                .active(true)
                .build();
        // 插入新子板块
        stockCustomPlateMapper.insert(plate);
        // 插入成分股关系
        saveMembers(plate.getId(), actualCodes);
        return toDto(plate);
    }

    /**
     * 更新子板块，并用本次提交的股票代码全量替换原有成分股。
     *
     * @param plateId 子板块编号
     * @param request 更新后的子板块及完整成分股请求参数
     * @return 更新后的子板块
     */
    @Override
    @Transactional
    public StockCustomPlateDto update(
            Long plateId,
            StockCustomPlateSaveDto request
    ) {
        // 校验子板块存在且未被软删除
        requireActivePlate(plateId);
        // 校验四大板块代码
        StockCustomCategory category = StockCustomCategory.fromCode(request.getCategoryCode());
        // 校验股票代码
        List<String> actualCodes = validateAndNormalizeStockCodes(request.getStockCodes());
        // 校验板块名称
        String actualName = normalizePlateName(request.getPlateName());
        // 校验板块名称唯一性
        validateUniqueName(category, actualName, plateId);
        StockCustomPlate plate = StockCustomPlate.builder()
                .id(plateId)
                .categoryCode(category.name())
                .plateName(actualName)
                .active(true)
                .build();
        // 更新子板块
        if (stockCustomPlateMapper.update(plate) != 1) {
            throw new IllegalStateException("自定义子板块更新失败，plateId=" + plateId);
        }
        // 本接口是全量更新：先失效原有关系，再写入本次提交的完整成员列表
        stockCustomPlateMapper.deactivateMembers(plateId);
        saveMembers(plateId, actualCodes);
        return toDto(plate);
    }

    /**
     * 软删除子板块及其有效成分关系，保留历史数据以便审计。
     *
     * @param plateId 子板块编号
     */
    @Override
    @Transactional
    public void delete(Long plateId) {
        // 校验子板块存在且未被软删除
        requireActivePlate(plateId);
        // 删除成分股关系
        stockCustomPlateMapper.deactivateMembers(plateId);
        // 软删除子板块
        if (stockCustomPlateMapper.deactivate(plateId) != 1) {
            throw new IllegalStateException("自定义子板块删除失败，plateId=" + plateId);
        }
    }

    /**
     * 查询板块成交额计算所需的全部有效大类、子板块和股票关系。
     *
     * @return 当前有效成分关系
     */
    @Override
    public List<StockCustomPlateMemberRelationDto> findActiveMemberRelations() {
        return stockCustomPlateMapper.selectActiveMemberRelations();
    }

    /**
     * 规范化并校验成分股代码：去除首尾空格、保持首次出现顺序去重，并核对最新股票基础信息快照。
     */
    private List<String> validateAndNormalizeStockCodes(List<String> stockCodes) {
        if (stockCodes == null) {
            throw new IllegalArgumentException("成分股列表不能为空");
        }
        // 去重，保持首次出现顺序
        Set<String> codes = new LinkedHashSet<>();
        for (String stockCode : stockCodes) {
            String actualCode = stockCode == null ? "" : stockCode.trim();
            if (!actualCode.matches("\\d{6}")) {
                throw new IllegalArgumentException("股票代码必须为6位数字：" + stockCode);
            }
            codes.add(actualCode);
        }
        if (codes.isEmpty()) {
            return List.of();
        }
        List<String> actualCodes = new ArrayList<>(codes);
        // 校验股票代码存在于最新基础信息快照
        Set<String> existingCodes = stockBasicMapper.selectLatestByCodes(actualCodes).stream()
                .map(StockBasic::getStockCode)
                .collect(Collectors.toSet());
        // 过滤掉不存在于最新基础信息快照的股票代码
        List<String> missingCodes = actualCodes.stream()
                .filter(code -> !existingCodes.contains(code))
                .toList();
        // 如果存在不存在于最新基础信息快照的股票代码，则抛出异常
        if (!missingCodes.isEmpty()) {
            throw new IllegalArgumentException("股票代码不存在于最新基础信息快照：" + missingCodes);
        }
        return actualCodes;
    }

    /**
     * 分批写入或恢复成分关系，避免默认板块的大量成分股形成过长的单条SQL。
     * 500一批
     */
    private void saveMembers(Long plateId, List<String> stockCodes) {
        for (int start = 0; start < stockCodes.size(); start += MEMBER_BATCH_SIZE) {
            int end = Math.min(start + MEMBER_BATCH_SIZE, stockCodes.size());
            // 创建成员关系列表
            List<StockCustomPlateMember> members = stockCodes.subList(start, end).stream()
                    .map(stockCode -> StockCustomPlateMember.builder()
                            .customPlateId(plateId)
                            .stockCode(stockCode)
                            .active(true)
                            .build())
                    .toList();
            stockCustomPlateMapper.upsertMemberBatch(members);
        }
    }

    /**
     * 获取有效子板块，不存在或已被软删除时统一拒绝后续操作。
     */
    private StockCustomPlate requireActivePlate(Long plateId) {
        if (plateId == null) {
            throw new IllegalArgumentException("子板块编号不能为空");
        }
        StockCustomPlate plate = stockCustomPlateMapper.selectActiveById(plateId);
        if (plate == null) {
            throw new IllegalArgumentException("自定义子板块不存在，plateId=" + plateId);
        }
        return plate;
    }

    private String normalizePlateName(String plateName) {
        String actualName = plateName == null ? "" : plateName.trim();
        if (actualName.isEmpty() || actualName.length() > 128) {
            throw new IllegalArgumentException("子板块名称长度必须在1到128之间");
        }
        return actualName;
    }

    private void validatePage(int pageNum, int pageSize) {
        if (pageNum < 1) {
            throw new IllegalArgumentException("页码必须大于等于1");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("每页数量必须在1到100之间");
        }
    }

    private void validateUniqueName(
            StockCustomCategory category,
            String plateName,
            Long excludeId
    ) {
        // 更新时排除当前板块自身；新增时excludeId为空，不排除任何记录。
        if (stockCustomPlateMapper.countActiveByName(category.name(), plateName, excludeId) > 0) {
            throw new IllegalArgumentException("同一大类下已存在同名子板块：" + plateName);
        }
    }

    private StockCustomPlateDto toDto(StockCustomPlate plate) {
        //获取板块类别
        StockCustomCategory category = StockCustomCategory.fromCode(plate.getCategoryCode());
        return StockCustomPlateDto.builder()
                .id(plate.getId())
                .categoryCode(category.name())
                .categoryName(category.getDisplayName())
                .plateName(plate.getPlateName())
                .build();
    }
}
