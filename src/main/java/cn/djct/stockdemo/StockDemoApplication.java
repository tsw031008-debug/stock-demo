package cn.djct.stockdemo;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("cn.djct.stockdemo.mapper")
@EnableScheduling
public class StockDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockDemoApplication.class, args);
    }

}
