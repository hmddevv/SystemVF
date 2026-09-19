package com.hmdao.farm.reminder.config;

import com.hmdao.farm.reminder.domain.CareRule;
import com.hmdao.farm.reminder.domain.DrySeasonIrrigationRule;
import com.hmdao.farm.reminder.domain.PostHarvestPruningRule;
import com.hmdao.farm.reminder.domain.RainySeasonFertilizingRule;
import com.hmdao.farm.reminder.domain.YoungOrchardCheckRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Nơi duy nhất biết danh sách luật nhắc việc. Luật là logic nghiệp vụ thuần nên nằm ở
 * {@code domain} và không mang annotation Spring (ArchUnit chặn); thêm luật mới chỉ cần thêm
 * một class và một {@code @Bean}, {@code CareReminderService} không đổi.
 */
@Configuration(proxyBeanMethods = false)
public class ReminderConfig {

    @Bean
    CareRule drySeasonIrrigationRule() {
        return new DrySeasonIrrigationRule();
    }

    @Bean
    CareRule rainySeasonFertilizingRule() {
        return new RainySeasonFertilizingRule();
    }

    @Bean
    CareRule postHarvestPruningRule() {
        return new PostHarvestPruningRule();
    }

    @Bean
    CareRule youngOrchardCheckRule() {
        return new YoungOrchardCheckRule();
    }
}
