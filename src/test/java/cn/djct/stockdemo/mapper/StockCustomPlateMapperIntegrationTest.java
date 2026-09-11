package cn.djct.stockdemo.mapper;

import cn.djct.stockdemo.pojo.dto.StockCustomPlateMemberRelationDto;
import cn.djct.stockdemo.pojo.entity.StockCustomPlate;
import cn.djct.stockdemo.pojo.entity.StockCustomPlateMember;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:stock_custom_plate_mapper;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@Sql(scripts = {
        "classpath:db/migration/V4__create_stock_tables.sql",
        "classpath:db/migration/V11__create_stock_custom_plate_tables.sql",
        "classpath:db/migration/V12__seed_default_stock_custom_plates.sql",
        "classpath:db/migration/V13__migrate_bse_custom_plate_member_codes.sql",
        "classpath:db/migration/V14__deactivate_invalid_default_custom_plate_members.sql"
})
class StockCustomPlateMapperIntegrationTest {

    private static final Set<String> LEGACY_BSE_STOCK_CODES = Set.of(
            "871634", "430139", "831641", "835179", "837821", "871981", "830799",
            "834415", "831961", "835640", "430090", "430198", "836395", "872190",
            "872808", "831726", "833429", "836826", "839273", "430718", "831768"
    );

    private static final Set<String> CURRENT_BSE_STOCK_CODES = Set.of(
            "920634", "920139", "920641", "920179", "920821", "920981", "920799",
            "920415", "920961", "920640", "920090", "920198", "920395", "920190",
            "920808", "920726", "920429", "920826", "920273", "920718", "920768"
    );

    private static final Set<String> INVALID_DEFAULT_MEMBER_RELATIONS = Set.of(
            "4:600387", "6:600837",
            "7:000416", "7:000616", "7:000627", "7:000666", "7:300309", "7:600705",
            "8:000806", "8:002288",
            "9:000851", "9:002308", "9:002417", "9:300330", "9:300344", "9:300379",
            "9:600355", "9:688555",
            "10:000836", "10:000851", "10:600355",
            "11:002308", "11:300330", "11:688086",
            "13:600696",
            "15:000861", "15:002336", "15:600122", "15:600306"
    );

    @Autowired
    private StockCustomPlateMapper stockCustomPlateMapper;
    @Autowired
    private StockCustomPlateMemberMapper stockCustomPlateMemberMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldSeedDefaultsAndUpsertCustomMembersIdempotently() {
        List<StockCustomPlateMemberRelationDto> defaultMembers =
                stockCustomPlateMemberMapper.selectActiveMemberRelations();
        assertEquals(4, stockCustomPlateMapper.countActiveByCategory("CYCLE"));
        assertEquals(1055, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM stock_custom_plate_member WHERE active = 1",
                Integer.class
        ));
        assertEquals(1055, defaultMembers.size());
        assertEquals(0, countInvalidDefaultMembers(defaultMembers));
        assertEquals(0, countMembersByCodes(defaultMembers, LEGACY_BSE_STOCK_CODES));
        assertEquals(21, countMembersByCodes(defaultMembers, CURRENT_BSE_STOCK_CODES));

        jdbcTemplate.update(
                "INSERT INTO stock_basic (stock_code, stock_name, last_seen_trade_date) VALUES (?, ?, ?)",
                "600000",
                "浦发银行",
                LocalDate.of(2026, 8, 28)
        );
        StockCustomPlate plate = StockCustomPlate.builder()
                .categoryCode("FINANCE")
                .plateName("核心金融")
                .active(true)
                .build();
        stockCustomPlateMapper.insert(plate);
        StockCustomPlateMember member = StockCustomPlateMember.builder()
                .customPlateId(plate.getId())
                .stockCode("600000")
                .active(true)
                .build();

        stockCustomPlateMemberMapper.upsertMemberBatch(List.of(member));
        stockCustomPlateMemberMapper.upsertMemberBatch(List.of(member));

        assertEquals(1, stockCustomPlateMemberMapper.countActiveMembers(plate.getId()));
        assertEquals("浦发银行", stockCustomPlateMemberMapper
                .selectActiveMembers(plate.getId(), 0, 20)
                .get(0)
                .getStockName());
        assertEquals(1056, stockCustomPlateMemberMapper.selectActiveMemberRelations().size());

        stockCustomPlateMemberMapper.deactivateMembers(plate.getId());
        stockCustomPlateMapper.deactivate(plate.getId());
        StockCustomPlate recreated = StockCustomPlate.builder()
                .categoryCode("FINANCE")
                .plateName("核心金融")
                .active(true)
                .build();
        stockCustomPlateMapper.insert(recreated);
        assertEquals(1, stockCustomPlateMapper.countActiveByName(
                "FINANCE",
                "核心金融",
                null
        ));
    }

    /**
     * 统计默认子板块中指定代码的有效成员数量，用于校验北交所代码迁移结果。
     */
    private long countMembersByCodes(
            List<StockCustomPlateMemberRelationDto> members,
            Set<String> stockCodes
    ) {
        return members.stream()
                .map(StockCustomPlateMemberRelationDto::getStockCode)
                .filter(stockCodes::contains)
                .count();
    }

    /**
     * 统计仍然有效的错误预置关系，确保同一代码位于不同子板块时也能分别校验。
     */
    private long countInvalidDefaultMembers(List<StockCustomPlateMemberRelationDto> members) {
        return members.stream()
                .map(member -> member.getCustomPlateId() + ":" + member.getStockCode())
                .filter(INVALID_DEFAULT_MEMBER_RELATIONS::contains)
                .count();
    }

}
