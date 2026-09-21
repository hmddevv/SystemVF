package com.hmdao.farm.support;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Hai thứ mà mọi integration test dùng chung: PostgreSQL thật và một đồng hồ chỉnh được.
 *
 * <p>PostgreSQL cùng phiên bản với môi trường chạy, để Flyway migration, ràng buộc CHECK/UNIQUE
 * và JPQL được kiểm chứng đúng như production — H2 sẽ nuốt mất chính những thứ đó.
 *
 * <p>Container chỉ khởi động một lần cho cả bộ test: mọi lớp test đều khai báo qua
 * {@link IntegrationTest} nên dùng chung một application context, và Spring cache context lại.
 */
@TestConfiguration(proxyBeanMethods = false)
public class IntegrationTestConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:17-alpine");
    }

    /** Thay {@code Clock.system()} của production; {@link DatabaseCleaner} trả về mặc định sau mỗi test. */
    @Bean
    MutableTestClock testClock(@Value("${farm.time-zone}") String zone) {
        return new MutableTestClock(ZoneId.of(zone));
    }

    /** Để service nào inject {@link Clock} cũng nhận đúng đồng hồ test. */
    @Bean
    @Primary
    Clock clockForTests(MutableTestClock testClock) {
        return testClock;
    }
}
