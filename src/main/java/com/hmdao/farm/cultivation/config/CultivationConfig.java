package com.hmdao.farm.cultivation.config;

import com.hmdao.farm.cultivation.domain.AnnualSeasonPolicy;
import com.hmdao.farm.cultivation.domain.PerennialSeasonPolicy;
import com.hmdao.farm.cultivation.domain.SeasonPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Chính sách niên vụ là logic nghiệp vụ thuần nên nằm ở {@code domain} và không mang annotation
 * Spring (ArchUnit chặn). Nơi duy nhất biết danh sách chúng là đây — {@code SeasonAssigner}
 * nhận {@code List<SeasonPolicy>} và không cần sửa khi có chu kỳ sản xuất mới.
 */
@Configuration(proxyBeanMethods = false)
public class CultivationConfig {

    @Bean
    SeasonPolicy perennialSeasonPolicy() {
        return new PerennialSeasonPolicy();
    }

    @Bean
    SeasonPolicy annualSeasonPolicy() {
        return new AnnualSeasonPolicy();
    }
}
