package com.hmdao.farm.shared.domain;

/**
 * Gốc của mọi lỗi nghiệp vụ. Mang mã quy tắc (vd. {@code BR-01}) để truy vết tới tài liệu
 * kiến trúc và test. Domain không biết HTTP — việc chọn mã HTTP thuộc về tầng web.
 */
public abstract class DomainException extends RuntimeException {

    private final String ruleCode;

    protected DomainException(String ruleCode, String message) {
        super(message);
        this.ruleCode = ruleCode;
    }

    public String getRuleCode() {
        return ruleCode;
    }
}
