package com.hmdao.farm.shared.domain;

/**
 * Dữ liệu đúng cú pháp nhưng vi phạm bất biến nghiệp vụ.
 */
public class BusinessRuleViolationException extends DomainException {

    public BusinessRuleViolationException(String ruleCode, String message) {
        super(ruleCode, message);
    }

    /** Ném lỗi nếu điều kiện không thỏa — giữ code kiểm tra bất biến trong entity ngắn gọn. */
    public static void require(boolean condition, String ruleCode, String message) {
        if (!condition) {
            throw new BusinessRuleViolationException(ruleCode, message);
        }
    }
}
