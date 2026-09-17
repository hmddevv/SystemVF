package com.hmdao.farm.identity.application.port;

/**
 * Rào cản trừu tượng giữa nghiệp vụ và cơ chế xác thực. Service chỉ hỏi "ai đang thao tác",
 * không biết câu trả lời đến từ HTTP header (MVP) hay JWT (Phase 5).
 */
public interface CurrentUserProvider {

    /**
     * @return id của người dùng hiện tại, đã được xác minh là tồn tại
     * @throws com.hmdao.farm.shared.domain.UnauthenticatedException nếu không xác định được
     */
    Long currentUserId();
}
