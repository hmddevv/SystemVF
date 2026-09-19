package com.hmdao.farm.reminder.domain;

/**
 * Mức độ của một lời nhắc. Không có mức "chưa tới hạn": danh sách chỉ chứa việc đã quá hạn
 * hoặc sắp tới hạn, để nhà nông mở ra là làm được ngay.
 */
public enum ReminderSeverity {
    /** Đã qua ngày đến hạn. */
    OVERDUE,
    /** Đến hạn trong vòng một tuần. */
    DUE_SOON
}
