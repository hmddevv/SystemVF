package com.hmdao.farm.identity.infrastructure;

import com.hmdao.farm.identity.application.port.CurrentUserProvider;
import com.hmdao.farm.identity.application.port.UserRepository;
import com.hmdao.farm.shared.domain.UnauthenticatedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Implementation cho MVP: đọc id chủ sở hữu từ header {@code X-User-Id}, mặc định là user
 * cấu hình sẵn. KHÔNG an toàn cho production — sẽ được thay bằng JwtCurrentUserProvider
 * ở Phase 5 mà không phải sửa service nào.
 */
@Component
class HeaderCurrentUserProvider implements CurrentUserProvider {

    static final String HEADER = "X-User-Id";

    private final UserRepository users;
    private final Long defaultUserId;

    HeaderCurrentUserProvider(UserRepository users, @Value("${farm.identity.default-user-id}") Long defaultUserId) {
        this.users = users;
        this.defaultUserId = defaultUserId;
    }

    @Override
    public Long currentUserId() {
        Long userId = resolveHeader();
        if (!users.existsById(userId)) {
            throw new UnauthenticatedException("Người dùng với id %d không tồn tại.".formatted(userId));
        }
        return userId;
    }

    private Long resolveHeader() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return defaultUserId;
        }
        String raw = attributes.getRequest().getHeader(HEADER);
        if (raw == null || raw.isBlank()) {
            return defaultUserId;
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException ex) {
            throw new UnauthenticatedException("Header %s phải là số nguyên, nhận được '%s'.".formatted(HEADER, raw));
        }
    }
}
