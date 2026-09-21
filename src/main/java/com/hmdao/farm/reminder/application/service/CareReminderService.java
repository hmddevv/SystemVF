package com.hmdao.farm.reminder.application.service;

import com.hmdao.farm.cultivation.domain.ActivityType;
import com.hmdao.farm.identity.application.port.CurrentUserProvider;
import com.hmdao.farm.shared.domain.ResourceNotFoundException;
import com.hmdao.farm.reminder.application.dto.LastActivityDate;
import com.hmdao.farm.reminder.application.dto.LastHarvestDate;
import com.hmdao.farm.reminder.application.dto.PlantingSnapshot;
import com.hmdao.farm.reminder.application.dto.ReminderView;
import com.hmdao.farm.reminder.application.port.in.CareReminderUseCase;
import com.hmdao.farm.reminder.application.port.out.CareContextQueryPort;
import com.hmdao.farm.reminder.domain.CareContext;
import com.hmdao.farm.reminder.domain.CareRule;
import com.hmdao.farm.reminder.domain.Reminder;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Engine nhắc việc (Epic F, Phase 2).
 *
 * <p>Ba truy vấn dựng {@link CareContext} cho mọi lứa đang canh tác, rồi chạy từng
 * {@link CareRule} trên bộ nhớ. Luật không được chạm database: N lứa trồng × M luật mà mỗi
 * luật tự truy vấn sẽ thành N×M lần gọi, và luật cũng hết test được nếu không có DB.
 *
 * <p>Không có bảng nào, không có trạng thái "đã làm" — ghi nhật ký tưới thì lời nhắc tưới
 * biến mất ở lần gọi sau (BR-18, ADR-12).
 */
@Service
@Transactional(readOnly = true)
class CareReminderService implements CareReminderUseCase {

    private final CareContextQueryPort query;
    private final List<CareRule> rules;
    private final CurrentUserProvider currentUser;
    private final Clock clock;

    CareReminderService(CareContextQueryPort query, List<CareRule> rules, CurrentUserProvider currentUser,
            Clock clock) {
        this.query = query;
        this.rules = List.copyOf(rules);
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Override
    public List<ReminderView> list(Long farmId) {
        Long ownerId = currentUser.currentUserId();
        if (farmId != null && !query.farmBelongsToOwner(ownerId, farmId)) {
            throw new ResourceNotFoundException("nông trại", farmId);
        }
        List<PlantingSnapshot> plantings = query.findActivePlantings(ownerId, farmId);
        if (plantings.isEmpty()) {
            return List.of();
        }
        List<Long> ids = plantings.stream().map(PlantingSnapshot::plantingId).toList();
        Map<Long, Map<ActivityType, LocalDate>> lastActivity = groupLastActivity(query.lastActivityDates(ids));
        Map<Long, LocalDate> lastHarvest = query.lastHarvestDates(ids).stream()
                .collect(Collectors.toMap(LastHarvestDate::plantingId, LastHarvestDate::lastDate));

        LocalDate today = LocalDate.now(clock);
        return plantings.stream()
                .map(planting -> context(planting, lastActivity, lastHarvest, today))
                .flatMap(context -> rules.stream().flatMap(rule -> rule.evaluate(context).stream()))
                .sorted(Comparator.comparing(Reminder::severity)
                        .thenComparing(Reminder::dueDate)
                        .thenComparing(Reminder::plotName)
                        .thenComparing(Reminder::ruleCode))
                .map(ReminderView::of)
                .toList();
    }

    private static CareContext context(PlantingSnapshot planting,
            Map<Long, Map<ActivityType, LocalDate>> lastActivity, Map<Long, LocalDate> lastHarvest,
            LocalDate today) {
        return new CareContext(
                planting.plantingId(),
                planting.cropDisplayName(),
                planting.plotName(),
                Boolean.TRUE.equals(planting.perennial()),
                planting.plantingDate(),
                planting.status(),
                ageInMonths(planting.plantingDate(), today),
                planting.treeCount() == null ? 0 : planting.treeCount(),
                lastActivity.getOrDefault(planting.plantingId(), Map.of()),
                lastHarvest.get(planting.plantingId()),
                today);
    }

    private static Map<Long, Map<ActivityType, LocalDate>> groupLastActivity(List<LastActivityDate> rows) {
        Map<Long, Map<ActivityType, LocalDate>> grouped = new HashMap<>();
        for (LastActivityDate row : rows) {
            grouped.computeIfAbsent(row.plantingId(), id -> new EnumMap<>(ActivityType.class))
                    .put(row.type(), row.lastDate());
        }
        return grouped;
    }

    private static int ageInMonths(LocalDate plantingDate, LocalDate today) {
        return today.isBefore(plantingDate) ? 0 : (int) Period.between(plantingDate, today).toTotalMonths();
    }
}
