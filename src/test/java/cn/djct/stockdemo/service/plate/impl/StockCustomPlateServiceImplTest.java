package cn.djct.stockdemo.service.plate.impl;

import cn.djct.stockdemo.mapper.StockBasicMapper;
import cn.djct.stockdemo.mapper.StockCustomPlateMapper;
import cn.djct.stockdemo.pojo.dto.StockCustomPlateDto;
import cn.djct.stockdemo.pojo.dto.StockCustomPlateSaveDto;
import cn.djct.stockdemo.pojo.entity.StockBasic;
import cn.djct.stockdemo.pojo.entity.StockCustomPlate;
import cn.djct.stockdemo.pojo.entity.StockCustomPlateMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockCustomPlateServiceImplTest {

    @Mock
    private StockCustomPlateMapper stockCustomPlateMapper;
    @Mock
    private StockBasicMapper stockBasicMapper;

    private StockCustomPlateServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StockCustomPlateServiceImpl(
                stockCustomPlateMapper,
                stockBasicMapper
        );
    }

    @Test
    void shouldCreatePlateWithNormalizedDistinctExistingStockCodes() {
        when(stockBasicMapper.selectLatestByCodes(List.of("000001", "600000"))).thenReturn(List.of(
                StockBasic.builder().stockCode("000001").build(),
                StockBasic.builder().stockCode("600000").build()
        ));
        when(stockCustomPlateMapper.insert(any())).thenAnswer(invocation -> {
            StockCustomPlate plate = invocation.getArgument(0);
            plate.setId(16L);
            return 1;
        });

        StockCustomPlateDto result = service.create(StockCustomPlateSaveDto.builder()
                .categoryCode("FINANCE")
                .plateName(" 核心金融 ")
                .stockCodes(List.of("000001", "600000", "000001"))
                .build());

        assertEquals(16L, result.getId());
        assertEquals("核心金融", result.getPlateName());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StockCustomPlateMember>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockCustomPlateMapper).upsertMemberBatch(captor.capture());
        assertEquals(List.of("000001", "600000"), captor.getValue().stream()
                .map(StockCustomPlateMember::getStockCode)
                .toList());
    }

    @Test
    void shouldRejectUnknownStockCodeBeforeCreatingPlate() {
        when(stockBasicMapper.selectLatestByCodes(List.of("999999"))).thenReturn(List.of());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(StockCustomPlateSaveDto.builder()
                        .categoryCode("CYCLE")
                        .plateName("测试")
                        .stockCodes(List.of("999999"))
                        .build())
        );

        verify(stockCustomPlateMapper, never()).insert(any());
    }
}
