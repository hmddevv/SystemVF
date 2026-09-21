package com.hmdao.farm.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Khai báo dùng chung cho mọi integration test đi qua HTTP thật tới PostgreSQL thật.
 *
 * <p>Gom vào một annotation không chỉ để gõ ít hơn: Spring cache application context theo bộ
 * khai báo của lớp test, nên chỉ cần một lớp lệch một property là sinh thêm một context và
 * <b>thêm một container PostgreSQL</b>. Mọi lớp dùng chung annotation này thì cả bộ test chỉ
 * khởi động một container duy nhất.
 *
 * <p>{@code generate_statistics} bật sẵn để test nào cần cũng đếm được số câu truy vấn —
 * đó là cách duy nhất chứng minh một endpoint không rơi vào N+1.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(IntegrationTestConfiguration.class)
@ExtendWith(DatabaseCleaner.class)
@EnabledIf(value = "com.hmdao.farm.support.DockerAvailable#isAvailable", disabledReason = DockerAvailable.REASON)
public @interface IntegrationTest {
}
