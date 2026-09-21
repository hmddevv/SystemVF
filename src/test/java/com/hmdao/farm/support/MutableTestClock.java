package com.hmdao.farm.support;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Đồng hồ do test điều khiển, thay cho {@code Clock.system()} của production.
 *
 * <p>Vì sao cần: nhắc việc và niên vụ đều hỏi "hôm nay là ngày nào". Nếu test dùng đồng hồ
 * thật thì luật theo mùa (tưới mùa khô tháng 12–4, bón mùa mưa tháng 5–9) chỉ chạy vào đúng
 * mùa — test sẽ xanh hay đỏ tùy tháng chạy, nên trước M5 hai luật ấy không được integration
 * test nào phủ. Có đồng hồ chỉnh được thì tháng nào chạy cũng cho một kết quả, và "đang là
 * mùa khô" trở thành một điều kiện khai báo được.
 *
 * <p>Giờ trong ngày cố định 08:00 giờ Việt Nam: đủ xa nửa đêm để không có test nào đổi kết
 * quả khi chạy vắt qua 00:00.
 */
public final class MutableTestClock extends Clock {

    /** Ngoài mùa khô (12–4) lẫn mùa mưa (5–9): mặc định không luật mùa nào chen vào kết quả. */
    public static final LocalDate DEFAULT_TODAY = LocalDate.of(2026, 10, 15);

    private static final LocalTime WORKING_HOUR = LocalTime.of(8, 0);

    private final ZoneId zone;
    private volatile Instant instant;

    public MutableTestClock(ZoneId zone) {
        this.zone = zone;
        this.instant = instantOf(DEFAULT_TODAY);
    }

    /** Đặt "hôm nay" cho mọi service đọc giờ qua {@code Clock}. */
    public void setToday(LocalDate today) {
        this.instant = instantOf(today);
    }

    public void reset() {
        setToday(DEFAULT_TODAY);
    }

    public LocalDate today() {
        return LocalDate.ofInstant(instant, zone);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId otherZone) {
        return new MutableTestClock(otherZone);
    }

    @Override
    public Instant instant() {
        return instant;
    }

    private Instant instantOf(LocalDate date) {
        return date.atTime(WORKING_HOUR).atZone(zone).toInstant();
    }
}
