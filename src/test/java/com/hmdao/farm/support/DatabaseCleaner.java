package com.hmdao.farm.support;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Trả database về trạng thái vừa migrate xong trước mỗi test.
 *
 * <p>Mọi integration test dùng chung một container để khỏi trả giá khởi động nhiều lần, nên
 * nếu không dọn thì dữ liệu của lớp test này chảy sang lớp test kia: kết quả phụ thuộc thứ tự
 * chạy, và một test đỏ có thể do test chạy trước nó gây ra. {@code TRUNCATE} rẻ hơn nhiều so
 * với dựng lại container, còn {@code RESTART IDENTITY} khiến id sinh ra lặp lại được — test
 * chẩn đoán dễ hơn hẳn.
 *
 * <p>Chỉ dọn bảng giao dịch. {@code app_user} và {@code crop} là dữ liệu tham chiếu do Flyway
 * nạp; xóa đi thì phải nạp lại trước mỗi test mà chẳng được gì.
 *
 * <p>Đồng hồ cũng được trả về mặc định ở đây, vì cùng một lý do: test đổi "hôm nay" mà không
 * hoàn lại thì test sau thừa hưởng ngày sai.
 */
public class DatabaseCleaner implements BeforeEachCallback {

    private static final String TRUNCATE = """
            TRUNCATE TABLE harvest, activity, season, planting, plot, farm RESTART IDENTITY CASCADE""";

    @Override
    public void beforeEach(ExtensionContext context) throws SQLException {
        var applicationContext = SpringExtension.getApplicationContext(context);
        applicationContext.getBean(MutableTestClock.class).reset();
        try (Connection connection = applicationContext.getBean(DataSource.class).getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute(TRUNCATE);
        }
    }
}
