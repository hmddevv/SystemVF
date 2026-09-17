package com.hmdao.farm.shared.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Nghiệp vụ lấy "hôm nay" qua {@link Clock} thay vì gọi {@code LocalDate.now()} trực tiếp,
 * để test có thể thay bằng {@code Clock.fixed(...)}.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    @Bean
    Clock clock(@Value("${farm.time-zone}") ZoneId zone) {
        return Clock.system(zone);
    }
}
