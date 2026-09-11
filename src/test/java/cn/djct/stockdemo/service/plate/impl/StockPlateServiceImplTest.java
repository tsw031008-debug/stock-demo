package cn.djct.stockdemo.service.plate.impl;

import cn.djct.stockdemo.mapper.StockPlateMapper;
import cn.djct.stockdemo.mapper.StockPlateMemberMapper;
import cn.djct.stockdemo.pojo.dto.StockPlateSourceDto;
import cn.djct.stockdemo.pojo.entity.StockPlate;
import cn.djct.stockdemo.pojo.entity.StockPlateMember;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockPlateServiceImplTest {

    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 8, 26);

    @Mock
    private StockPlateMapper stockPlateMapper;
    @Mock
    private StockPlateMemberMapper stockPlateMemberMapper;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void shouldSavePlateAndDeduplicatedMembers() {
        List<StockPlateSourceDto> source = List.of(
                StockPlateSourceDto.builder()
                        .plateName("科技-概")
                        .stockCodes(List.of("000001", "000001", "000002"))
                        .build(),
                StockPlateSourceDto.builder()
                        .plateName("金融-概")
                        .stockCodes(List.of("000002"))
                        .build()
        );
        when(stockPlateMapper.selectByDataSource(StockPlateServiceImpl.DATA_SOURCE)).thenReturn(List.of(
                StockPlate.builder().id(1L).plateName("科技-概").build(),
                StockPlate.builder().id(2L).plateName("金融-概").build()
        ));
        when(stockPlateMapper.countActivePlates(StockPlateServiceImpl.DATA_SOURCE, TRADE_DATE))
                .thenReturn(2);
        when(stockPlateMemberMapper.countActiveMembers(StockPlateServiceImpl.DATA_SOURCE, TRADE_DATE))
                .thenReturn(3);
        StockPlateServiceImpl service = new StockPlateServiceImpl(stockPlateMapper, stockPlateMemberMapper);

        int result = service.saveSnapshot(TRADE_DATE, source);

        assertEquals(2, result);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StockPlateMember>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockPlateMemberMapper, atLeastOnce()).upsertMemberBatch(captor.capture());
        assertEquals(3, captor.getAllValues().stream().mapToInt(List::size).sum());
    }
}
