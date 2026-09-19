package com.hmdao.farm.cultivation.domain;

import static com.hmdao.farm.shared.domain.BusinessRuleViolationException.require;

import com.hmdao.farm.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Một dòng nhật ký canh tác: tưới, bón phân, phun thuốc, làm cỏ, tỉa cành… kèm ngày và chi phí.
 *
 * <p>Hoạt động gắn vào niên vụ chứa ngày thực hiện, nên chi phí chăm sóc và doanh thu thu hoạch
 * của cùng một chu kỳ sản xuất luôn nằm cùng một chỗ khi tính lãi/lỗ.
 */
@Entity
@Table(name = "activity")
public class Activity extends BaseEntity {

    public static final int NOTE_MAX = 255;
    /** Tiền lưu ở NUMERIC(15,2); đầu vào lệch scale được làm tròn về 2 chữ số thập phân. */
    public static final int MONEY_SCALE = 2;

    /**
     * Không phải {@code updatable = false}: sửa ngày hoạt động sang chu kỳ khác thì bản ghi
     * chuyển sang niên vụ tương ứng (BR-05a).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityType type;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(nullable = false, precision = 15, scale = MONEY_SCALE)
    private BigDecimal cost;

    @Column(length = NOTE_MAX)
    private String note;

    protected Activity() {
    }

    /**
     * Ghi một hoạt động vào niên vụ đã được {@code SeasonAssigner} xác định từ chính
     * {@code activityDate}.
     *
     * @param cost bỏ trống nghĩa là 0 — việc tự làm, không tốn chi phí (BR-08)
     */
    public static Activity log(Season season, ActivityType type, LocalDate activityDate, BigDecimal cost,
            String note) {
        Activity activity = new Activity();
        activity.apply(season, type, activityDate, cost, note);
        return activity;
    }

    /** Sửa dữ liệu nhập sai; niên vụ do service tính lại vì ngày mới có thể thuộc chu kỳ khác. */
    public void correct(Season season, ActivityType type, LocalDate activityDate, BigDecimal cost, String note) {
        apply(season, type, activityDate, cost, note);
    }

    private void apply(Season season, ActivityType type, LocalDate activityDate, BigDecimal cost, String note) {
        Objects.requireNonNull(season, "season");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(activityDate, "activityDate");
        require(season.contains(activityDate), "BR-07",
                "Ngày %s không nằm trong niên vụ %s (%s – %s).".formatted(activityDate, season.getLabel(),
                        season.getStartDate(), season.getEndDate()));
        BigDecimal amount = (cost == null ? BigDecimal.ZERO : cost).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        require(amount.signum() >= 0, "BR-08", "Chi phí không được âm (nhận %s).".formatted(amount));
        String trimmed = note == null || note.isBlank() ? null : note.strip();
        require(type != ActivityType.OTHER || trimmed != null, "BR-12",
                "Hoạt động loại OTHER phải có ghi chú, nếu không dòng nhật ký không còn ý nghĩa.");
        this.season = season;
        this.type = type;
        this.activityDate = activityDate;
        this.cost = amount;
        this.note = trimmed;
    }

    public Season getSeason() {
        return season;
    }

    public ActivityType getType() {
        return type;
    }

    public LocalDate getActivityDate() {
        return activityDate;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public String getNote() {
        return note;
    }
}
