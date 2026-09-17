package com.hmdao.farm.catalog.domain;

import static com.hmdao.farm.shared.domain.BusinessRuleViolationException.require;

import com.hmdao.farm.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Month;

/**
 * Loại cây trồng trong danh mục (cà phê Robusta, hồ tiêu Vĩnh Linh…).
 *
 * <p>Cây lâu năm có {@code seasonStartMonth}: tháng bắt đầu niên vụ theo chu kỳ sản xuất,
 * dùng để tự gán hoạt động/thu hoạch vào đúng niên vụ (BR-05a). Cây ngắn ngày không có
 * giá trị này — mỗi lứa trồng là một niên vụ.
 */
@Entity
@Table(name = "crop")
public class Crop extends BaseEntity {

    public static final int NAME_MAX = 80;
    public static final int VARIETY_MAX = 80;

    @Column(nullable = false, length = NAME_MAX)
    private String name;

    @Column(length = VARIETY_MAX)
    private String variety;

    @Column(name = "is_perennial", nullable = false)
    private boolean perennial;

    @Column(name = "season_start_month")
    private Integer seasonStartMonth;

    protected Crop() {
    }

    public static Crop create(String name, String variety, boolean perennial, Integer seasonStartMonth) {
        Crop crop = new Crop();
        crop.update(name, variety, perennial, seasonStartMonth);
        return crop;
    }

    public void update(String name, String variety, boolean perennial, Integer seasonStartMonth) {
        require(name != null && !name.isBlank(), "BR-01", "Tên cây trồng không được để trống.");
        if (perennial) {
            require(seasonStartMonth != null && seasonStartMonth >= 1 && seasonStartMonth <= 12, "BR-05a",
                    "Cây lâu năm phải có tháng bắt đầu niên vụ từ 1 đến 12.");
        } else {
            require(seasonStartMonth == null, "BR-05a",
                    "Cây ngắn ngày không dùng tháng bắt đầu niên vụ — mỗi lứa trồng là một niên vụ. Hãy bỏ trống trường này.");
        }
        this.name = name.strip();
        this.variety = variety == null || variety.isBlank() ? null : variety.strip();
        this.perennial = perennial;
        this.seasonStartMonth = seasonStartMonth;
    }

    /** "Cà phê (Robusta)" hoặc "Ngô" nếu không có giống. */
    public String getDisplayName() {
        return variety == null ? name : "%s (%s)".formatted(name, variety);
    }

    public Month getSeasonStart() {
        return seasonStartMonth == null ? null : Month.of(seasonStartMonth);
    }

    public String getName() {
        return name;
    }

    public String getVariety() {
        return variety;
    }

    public boolean isPerennial() {
        return perennial;
    }

    public Integer getSeasonStartMonth() {
        return seasonStartMonth;
    }
}
