package com.hmdao.farm.support;

import org.testcontainers.DockerClientFactory;

/**
 * Điều kiện bật/tắt integration test. Không có Docker thì test được <b>bỏ qua kèm lý do</b>,
 * thay vì đổ ra một bức tường stack trace "Could not find a valid Docker environment" khiến
 * người đọc không phân biệt được "máy thiếu Docker" với "code hỏng".
 *
 * <p>CI luôn có Docker nên ở đó không bao giờ bỏ qua — máy cá nhân quên bật Docker cũng không
 * vì thế mà báo build đỏ oan.
 */
public final class DockerAvailable {

    public static final String REASON =
            "Bỏ qua integration test: không tìm thấy Docker. Hãy bật Docker Desktop rồi chạy lại `mvnw verify`.";

    private static final boolean AVAILABLE = probe();

    private DockerAvailable() {
    }

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    private static boolean probe() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
