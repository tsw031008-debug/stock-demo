package cn.djct.stockdemo.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockMarketCodeUtilTest {

    @Test
    void shouldIdentifyShanghaiAndShenzhenAStocks() {
        assertTrue(StockMarketCodeUtil.isShanghaiOrShenzhenAStock("600000"));
        assertTrue(StockMarketCodeUtil.isShanghaiOrShenzhenAStock("000001"));
        assertTrue(StockMarketCodeUtil.isShanghaiOrShenzhenAStock("300001"));
        assertFalse(StockMarketCodeUtil.isShanghaiOrShenzhenAStock("920001"));
        assertFalse(StockMarketCodeUtil.isShanghaiOrShenzhenAStock(null));
    }

    @Test
    void shouldConvertStockCodeToTencentSymbol() {
        assertEquals("sh600519", StockMarketCodeUtil.toTencentSymbol("600519"));
        assertEquals("sz000001", StockMarketCodeUtil.toTencentSymbol("000001"));
        assertEquals("sz300750", StockMarketCodeUtil.toTencentSymbol("300750"));
        assertEquals("bj920001", StockMarketCodeUtil.toTencentSymbol("920001"));
        assertEquals("bj430001", StockMarketCodeUtil.toTencentSymbol("430001"));
        assertEquals("bj830001", StockMarketCodeUtil.toTencentSymbol("830001"));
    }

    @Test
    void shouldRejectUnknownMarket() {
        assertThrows(IllegalArgumentException.class,
                () -> StockMarketCodeUtil.toTencentSymbol("200001"));
    }
}
