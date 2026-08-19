package cn.djct.stockdemo;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("cn.djct.stockdemo.mapper")
public class StockDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockDemoApplication.class, args);
    }

}
