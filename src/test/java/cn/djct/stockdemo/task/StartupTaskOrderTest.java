package cn.djct.stockdemo.task;

import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.Order;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StartupTaskOrderTest {

    @Test
    void shouldRunStartupCatchUpInDependencyOrder() throws NoSuchMethodException {
        assertEquals(10, orderOf(StockBasicTask.class));
        assertEquals(20, orderOf(StockDailyQuoteTask.class));
        assertEquals(25, orderOf(StockPlateTask.class));
        assertEquals(30, orderOf(StockFundFlowTask.class));
    }

    private int orderOf(Class<?> taskClass) throws NoSuchMethodException {
        return taskClass.getMethod("synchronizeAfterStartup")
                .getAnnotation(Order.class)
                .value();
    }
}
