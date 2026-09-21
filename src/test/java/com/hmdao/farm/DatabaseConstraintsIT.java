package com.hmdao.farm;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hmdao.farm.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Ràng buộc trong database là lớp phòng thủ cuối, và lớp phòng thủ cuối thì chỉ đáng tin khi
 * đã được thử.
 *
 * <p>Mọi test khác đi qua API nên luôn có domain chắn trước: chúng chứng minh domain từ chối
 * dữ liệu sai, chứ không chứng minh được database cũng từ chối. Test này cố tình <b>đi vòng
 * qua domain</b> bằng SQL thô — đúng như một script sửa dữ liệu vội vàng, một lần import, hay
 * một bug tương lai sẽ làm.
 */
@IntegrationTest
class DatabaseConstraintsIT {

    /** Cà phê Robusta trong dữ liệu tham chiếu: niên vụ bắt đầu tháng 2. */
    private static final long COFFEE = 1L;

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    private long plotId;
    private long plantingId;
    private long seasonId;

    @BeforeEach
    void oneOrchardWithOneSeasonAlreadyOpened() throws Exception {
        long farmId = created("/api/v1/farms", """
                {"name": "Nông trại kiểm ràng buộc"}
                """);
        plotId = created("/api/v1/farms/%d/plots".formatted(farmId), """
                {"name": "Lô A2", "areaM2": 10000}
                """);
        plantingId = created("/api/v1/plots/%d/plantings".formatted(plotId), """
                {"cropId": %d, "plantingDate": "2020-01-15", "treeCount": 500, "alreadyProducing": true}
                """.formatted(COFFEE));
        created("/api/v1/plantings/%d/activities".formatted(plantingId), """
                {"type": "FERTILIZING", "activityDate": "2025-03-15", "cost": 10000000}
                """);
        seasonId = jdbc.queryForObject(
                "select id from season where planting_id = ?", Long.class, plantingId);
    }

    @Test
    void br08_aNegativeCostCannotBeWrittenEvenBySql() {
        assertThatThrownBy(() -> jdbc.update("""
                insert into activity (season_id, type, activity_date, cost) values (?, 'WATERING', ?, -1)
                """, seasonId, java.sql.Date.valueOf("2025-04-01")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_activity_cost_not_negative");
    }

    @Test
    void br12_anOtherEntryWithoutANoteIsRefusedByTheDatabaseToo() {
        assertThatThrownBy(() -> jdbc.update("""
                insert into activity (season_id, type, activity_date, cost) values (?, 'OTHER', ?, 100)
                """, seasonId, java.sql.Date.valueOf("2025-04-01")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_activity_other_has_note");
    }

    @Test
    void br08_aHarvestOfZeroKilogramsIsNotAHarvest() {
        assertThatThrownBy(() -> jdbc.update("""
                insert into harvest (season_id, harvest_date, quantity_kg, revenue) values (?, ?, 0, 1000)
                """, seasonId, java.sql.Date.valueOf("2025-11-20")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_harvest_quantity_positive");
    }

    @Test
    void anUnknownActivityTypeCannotSneakInThroughSql() {
        assertThatThrownBy(() -> jdbc.update("""
                insert into activity (season_id, type, activity_date, cost) values (?, 'DANCING', ?, 0)
                """, seasonId, java.sql.Date.valueOf("2025-04-01")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_activity_type");
    }

    /**
     * Chính ràng buộc mà {@code SeasonAssigner} dựa vào: hai request ghi đồng thời vào một niên
     * vụ chưa tồn tại thì cả hai cùng thấy "chưa có" và cùng tạo. Một trong hai phải thất bại.
     */
    @Test
    void br05_aPlantingCannotEndUpWithTwoSeasonsForTheSameYear() {
        assertThatThrownBy(() -> jdbc.update("""
                insert into season (planting_id, year, start_date) values (?, 2025, ?)
                """, plantingId, java.sql.Date.valueOf("2025-02-01")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_season_planting_year");
    }

    @Test
    void ck_seasonCannotEndBeforeItStarts() {
        assertThatThrownBy(() -> jdbc.update("""
                insert into season (planting_id, year, start_date, end_date) values (?, 2031, ?, ?)
                """, plantingId, java.sql.Date.valueOf("2031-02-01"), java.sql.Date.valueOf("2030-12-31")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_season_dates");
    }

    /**
     * BR-10 ở tầng ứng dụng trả 409 kèm lời giải thích. Nếu ai đó lách qua tầng ấy, khóa ngoại
     * {@code ON DELETE RESTRICT} vẫn giữ lịch sử canh tác lại — không có lô đất nào biến mất
     * cùng toàn bộ dữ liệu của nó.
     */
    @Test
    void br10_deletingAPlotThatStillHasPlantingsIsRefusedByTheForeignKey() {
        assertThatThrownBy(() -> jdbc.update("delete from plot where id = ?", plotId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_planting_plot");
    }

    private long created(String path, String body) throws Exception {
        String response = mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.<Number>read(response, "$.id").longValue();
    }
}
