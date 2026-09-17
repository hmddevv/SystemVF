package com.hmdao.farm.cultivation.domain;

import static com.hmdao.farm.shared.domain.BusinessRuleViolationException.require;

import com.hmdao.farm.catalog.domain.Crop;
import com.hmdao.farm.land.domain.Plot;
import com.hmdao.farm.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

/**
 * Lứa trồng: một loại cây trên một lô đất, bắt đầu từ một ngày trồng. Một lô có nhiều lứa
 * trồng cùng lúc (xen canh) và nối tiếp nhau theo thời gian (lịch sử sử dụng đất).
 *
 * <p>Vòng đời thay đổi bằng thuộc tính {@code status}, {@code endDate}, {@code endReason}
 * — không xóa bản ghi — để giữ lịch sử và lý do chuyển đổi cây trồng.
 */
@Entity
@Table(name = "planting")
public class Planting extends BaseEntity {

    public static final int END_NOTE_MAX = 255;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plot_id", nullable = false, updatable = false)
    private Plot plot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crop_id", nullable = false, updatable = false)
    private Crop crop;

    @Column(name = "planting_date", nullable = false)
    private LocalDate plantingDate;

    @Column(name = "tree_count", nullable = false)
    private int treeCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlantingStatus status;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "end_reason", length = 20)
    private EndReason endReason;

    @Column(name = "end_note", length = END_NOTE_MAX)
    private String endNote;

    protected Planting() {
    }

    /**
     * Trồng một loại cây lên lô đất.
     *
     * @param alreadyProducing {@code true} khi số hóa một vườn đã cho thu hoạch từ trước
     *                         (vd. vườn cà phê 10 năm tuổi), để không phải gọi thêm bước chuyển trạng thái
     */
    public static Planting plant(Plot plot, Crop crop, LocalDate plantingDate, int treeCount,
            boolean alreadyProducing, LocalDate today) {
        Objects.requireNonNull(plot, "plot");
        Objects.requireNonNull(crop, "crop");
        Planting planting = new Planting();
        planting.plot = plot;
        planting.crop = crop;
        planting.status = alreadyProducing ? PlantingStatus.PRODUCING : PlantingStatus.GROWING;
        planting.correct(plantingDate, treeCount, today);
        return planting;
    }

    /** Sửa dữ liệu nhập sai. Cây trồng và lô đất không đổi được — đổi cây là một lứa trồng mới. */
    public void correct(LocalDate plantingDate, int treeCount, LocalDate today) {
        require(plantingDate != null, "BR-02", "Ngày trồng không được để trống.");
        require(!plantingDate.isAfter(today), "BR-02",
                "Ngày trồng %s ở tương lai (hôm nay là %s).".formatted(plantingDate, today));
        require(treeCount > 0, "BR-02", "Số cây phải lớn hơn 0.");
        require(endDate == null || !endDate.isBefore(plantingDate), "BR-04",
                "Ngày trồng %s không được sau ngày kết thúc %s của lứa trồng.".formatted(plantingDate, endDate));
        this.plantingDate = plantingDate;
        this.treeCount = treeCount;
    }

    /** Chuyển từ kiến thiết cơ bản sang kinh doanh (BR-03). */
    public void startProducing() {
        requireTransition(PlantingStatus.PRODUCING);
        this.status = PlantingStatus.PRODUCING;
    }

    /** Cưa bỏ / chặt bỏ lứa trồng (BR-03, BR-04). */
    public void terminate(LocalDate endDate, EndReason reason, String note, LocalDate today) {
        requireTransition(PlantingStatus.TERMINATED);
        require(reason != null, "BR-04", "Phải chọn lý do kết thúc lứa trồng.");
        require(endDate != null, "BR-04", "Ngày kết thúc không được để trống.");
        require(!endDate.isBefore(plantingDate), "BR-04",
                "Ngày kết thúc %s không được trước ngày trồng %s.".formatted(endDate, plantingDate));
        require(!endDate.isAfter(today), "BR-04",
                "Ngày kết thúc %s ở tương lai (hôm nay là %s).".formatted(endDate, today));
        this.status = PlantingStatus.TERMINATED;
        this.endDate = endDate;
        this.endReason = reason;
        this.endNote = note == null || note.isBlank() ? null : note.strip();
    }

    public boolean isActive() {
        return status != PlantingStatus.TERMINATED;
    }

    /** Tuổi vườn tính đến ngày kết thúc (nếu đã kết thúc) hoặc hôm nay. */
    public int ageInMonths(LocalDate today) {
        LocalDate until = endDate != null ? endDate : today;
        return until.isBefore(plantingDate) ? 0 : (int) Period.between(plantingDate, until).toTotalMonths();
    }

    private void requireTransition(PlantingStatus target) {
        require(status != PlantingStatus.TERMINATED, "BR-03",
                "Lứa trồng đã kết thúc ngày %s. Đây là trạng thái cuối, không thể thay đổi.".formatted(endDate));
        require(status.canTransitionTo(target), "BR-03",
                "Không thể chuyển lứa trồng từ %s sang %s.".formatted(status, target));
    }

    public Plot getPlot() {
        return plot;
    }

    public Crop getCrop() {
        return crop;
    }

    public LocalDate getPlantingDate() {
        return plantingDate;
    }

    public int getTreeCount() {
        return treeCount;
    }

    public PlantingStatus getStatus() {
        return status;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public EndReason getEndReason() {
        return endReason;
    }

    public String getEndNote() {
        return endNote;
    }
}
