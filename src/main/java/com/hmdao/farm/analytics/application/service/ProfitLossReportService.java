package com.hmdao.farm.analytics.application.service;

import com.hmdao.farm.analytics.application.dto.PlantingProfile;
import com.hmdao.farm.analytics.application.dto.ProfitLossCriteria;
import com.hmdao.farm.analytics.application.dto.ProfitLossReport;
import com.hmdao.farm.analytics.application.dto.ProfitLossRow;
import com.hmdao.farm.analytics.application.dto.SeasonCost;
import com.hmdao.farm.analytics.application.dto.SeasonYield;
import com.hmdao.farm.analytics.application.port.in.ProfitLossReportUseCase;
import com.hmdao.farm.analytics.application.port.out.ProfitLossQueryPort;
import com.hmdao.farm.analytics.domain.ProfitLossGrouping;
import com.hmdao.farm.identity.application.port.CurrentUserProvider;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Báo cáo lãi/lỗ (Epic E, Phase 2).
 *
 * <p>Ba truy vấn, phần còn lại làm trên bộ nhớ: hồ sơ lứa trồng, chi phí theo (lứa, niên vụ),
 * thu hoạch theo (lứa, niên vụ). Số lô, số lứa hay số năm dữ liệu tăng lên cũng không thêm
 * truy vấn nào.
 *
 * <p>Hai chỗ dễ sai mà báo cáo này xử lý tường minh:
 * <ul>
 *   <li><b>Niên vụ, không phải năm dương lịch</b> (BR-13) — gom theo {@code SEASON.year} nên
 *       doanh thu cà phê tháng 1 nằm cùng vụ với chi phí tháng 3 năm trước.</li>
 *   <li><b>Luỹ kế tính trên toàn bộ niên vụ</b> (BR-17) — bộ lọc {@code year} chỉ ảnh hưởng
 *       phần hiển thị. Cà phê lỗ ba năm kiến thiết cơ bản là bình thường; nhìn một năm rồi
 *       kết luận "cây này lỗ" là sai.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
class ProfitLossReportService implements ProfitLossReportUseCase {

    private final ProfitLossQueryPort query;
    private final CurrentUserProvider currentUser;

    ProfitLossReportService(ProfitLossQueryPort query, CurrentUserProvider currentUser) {
        this.query = query;
        this.currentUser = currentUser;
    }

    @Override
    public ProfitLossReport report(ProfitLossCriteria criteria) {
        List<PlantingProfile> profiles = query.findPlantings(currentUser.currentUserId(), criteria.farmId());
        if (profiles.isEmpty()) {
            return ProfitLossReport.empty(criteria);
        }
        List<Long> plantingIds = profiles.stream().map(PlantingProfile::plantingId).toList();
        List<SeasonFact> facts = seasonFacts(
                query.costsByPlantingAndSeason(plantingIds), query.yieldsByPlantingAndSeason(plantingIds));
        Set<Long> sharedPlots = plotsWithMoreThanOnePlanting(profiles);

        Map<Long, Long> groupOfPlanting = new HashMap<>();
        Map<Long, Bucket> buckets = new LinkedHashMap<>();
        Bucket total = new Bucket(null, "Tổng");
        for (PlantingProfile profile : profiles) {
            Long groupId = groupId(profile, criteria.groupBy());
            groupOfPlanting.put(profile.plantingId(), groupId);
            buckets.computeIfAbsent(groupId, id -> new Bucket(id, label(profile, criteria.groupBy())))
                    .include(profile, sharedPlots.contains(profile.plotId()));
            total.include(profile, sharedPlots.contains(profile.plotId()));
        }
        for (SeasonFact fact : facts) {
            Bucket bucket = buckets.get(groupOfPlanting.get(fact.plantingId()));
            // Luỹ kế và năm hoàn vốn nhìn mọi niên vụ; bộ lọc year chỉ cắt phần hiển thị (BR-17).
            bucket.accumulateLifetime(fact);
            total.accumulateLifetime(fact);
            if (criteria.year() == null || criteria.year().equals(fact.year())) {
                bucket.accumulateShown(fact);
                total.accumulateShown(fact);
            }
        }
        boolean payback = criteria.groupBy() == ProfitLossGrouping.PLANTING;
        List<ProfitLossRow> rows = buckets.values().stream()
                .map(bucket -> bucket.toRow(payback))
                .sorted(Comparator.comparing(ProfitLossRow::netProfit).reversed()
                        .thenComparing(ProfitLossRow::label))
                .toList();
        return new ProfitLossReport(criteria.groupBy(), criteria.year(), rows, total.toRow(false));
    }

    /**
     * Ghép hai kết quả {@code GROUP BY} theo khóa (lứa trồng, niên vụ). Gộp chúng vào một câu
     * SQL sẽ nhân chéo dòng: một niên vụ có 12 hoạt động và 3 lần thu hoạch sẽ cho 36 dòng,
     * chi phí bị cộng ba lần.
     */
    private static List<SeasonFact> seasonFacts(List<SeasonCost> costs, List<SeasonYield> yields) {
        Map<SeasonKey, SeasonFact> merged = new LinkedHashMap<>();
        for (SeasonCost cost : costs) {
            merged.computeIfAbsent(new SeasonKey(cost.plantingId(), cost.year()), SeasonFact::empty)
                    .applyCost(cost);
        }
        for (SeasonYield yield : yields) {
            merged.computeIfAbsent(new SeasonKey(yield.plantingId(), yield.year()), SeasonFact::empty)
                    .applyYield(yield);
        }
        return List.copyOf(merged.values());
    }

    /** Lô trồng xen: số theo đơn vị diện tích của nhóm chỉ là ước lượng (BR-16). */
    private static Set<Long> plotsWithMoreThanOnePlanting(List<PlantingProfile> profiles) {
        Set<Long> seen = new HashSet<>();
        Set<Long> shared = new HashSet<>();
        for (PlantingProfile profile : profiles) {
            if (!seen.add(profile.plotId())) {
                shared.add(profile.plotId());
            }
        }
        return shared;
    }

    private static Long groupId(PlantingProfile profile, ProfitLossGrouping groupBy) {
        return switch (groupBy) {
            case CROP -> profile.cropId();
            case PLOT -> profile.plotId();
            case PLANTING -> profile.plantingId();
        };
    }

    private static String label(PlantingProfile profile, ProfitLossGrouping groupBy) {
        return switch (groupBy) {
            case CROP -> profile.cropDisplayName();
            case PLOT -> profile.plotName();
            case PLANTING -> "%s — %s".formatted(profile.cropDisplayName(), profile.plotName());
        };
    }

    private record SeasonKey(Long plantingId, Integer year) {
    }

    /** Số liệu đã tổng hợp của một niên vụ thuộc một lứa trồng. */
    private static final class SeasonFact {

        private final Long plantingId;
        private final Integer year;
        private long activityCount;
        private BigDecimal cost = BigDecimal.ZERO;
        private long harvestCount;
        private double quantityKg;
        private BigDecimal revenue = BigDecimal.ZERO;

        private SeasonFact(SeasonKey key) {
            this.plantingId = key.plantingId();
            this.year = key.year();
        }

        static SeasonFact empty(SeasonKey key) {
            return new SeasonFact(key);
        }

        void applyCost(SeasonCost source) {
            activityCount = source.entries();
            cost = source.totalCost();
        }

        void applyYield(SeasonYield source) {
            harvestCount = source.entries();
            quantityKg = source.quantityKg();
            revenue = source.revenue();
        }

        Long plantingId() {
            return plantingId;
        }

        Integer year() {
            return year;
        }

        BigDecimal netProfit() {
            return revenue.subtract(cost);
        }
    }

    /** Gom nhiều lứa trồng và niên vụ thành một dòng báo cáo. */
    private static final class Bucket {

        private final Long id;
        private final String label;
        /** Theo id lô để lô trồng xen không bị cộng diện tích hai lần. */
        private final Map<Long, Double> plotAreas = new HashMap<>();
        private final TreeMap<Integer, BigDecimal> lifetimeNetByYear = new TreeMap<>();
        private int trees;
        private boolean sharedPlot;
        private int seasonCount;
        private long activityCount;
        private BigDecimal cost = BigDecimal.ZERO;
        private long harvestCount;
        private double quantityKg;
        private BigDecimal revenue = BigDecimal.ZERO;
        private BigDecimal lifetimeNet = BigDecimal.ZERO;

        Bucket(Long id, String label) {
            this.id = id;
            this.label = label;
        }

        void include(PlantingProfile profile, boolean plotIsShared) {
            plotAreas.putIfAbsent(profile.plotId(), profile.plotAreaM2());
            trees += profile.trees();
            sharedPlot |= plotIsShared;
        }

        void accumulateLifetime(SeasonFact fact) {
            lifetimeNet = lifetimeNet.add(fact.netProfit());
            lifetimeNetByYear.merge(fact.year(), fact.netProfit(), BigDecimal::add);
        }

        void accumulateShown(SeasonFact fact) {
            seasonCount++;
            activityCount += fact.activityCount;
            cost = cost.add(fact.cost);
            harvestCount += fact.harvestCount;
            quantityKg += fact.quantityKg;
            revenue = revenue.add(fact.revenue);
        }

        ProfitLossRow toRow(boolean withPayback) {
            BigDecimal net = revenue.subtract(cost);
            double area = plotAreas.values().stream().filter(java.util.Objects::nonNull)
                    .mapToDouble(Double::doubleValue).sum();
            return new ProfitLossRow(
                    id,
                    label,
                    seasonCount,
                    activityCount,
                    cost,
                    harvestCount,
                    quantityKg,
                    revenue,
                    net,
                    area > 0 ? area : null,
                    trees > 0 ? trees : null,
                    ProfitLossRow.per(net, area / 1000d),
                    ProfitLossRow.per(net, trees),
                    trees > 0 ? quantityKg / trees : null,
                    sharedPlot,
                    lifetimeNet,
                    withPayback ? paybackYear() : null);
        }

        /** Niên vụ đầu tiên mà luỹ kế từ đầu lứa trồng hòa vốn; {@code null} = chưa hoàn vốn. */
        private Integer paybackYear() {
            BigDecimal running = BigDecimal.ZERO;
            for (Map.Entry<Integer, BigDecimal> entry : lifetimeNetByYear.entrySet()) {
                running = running.add(entry.getValue());
                if (running.signum() >= 0) {
                    return entry.getKey();
                }
            }
            return null;
        }
    }
}
