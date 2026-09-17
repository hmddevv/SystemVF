package com.hmdao.farm.shared.domain;

/**
 * Thao tác xung đột với trạng thái hiện có: trùng tên, hoặc xóa khi còn dữ liệu con (BR-10).
 */
public class ResourceConflictException extends DomainException {

    public ResourceConflictException(String ruleCode, String message) {
        super(ruleCode, message);
    }
}
