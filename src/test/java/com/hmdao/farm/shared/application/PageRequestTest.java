package com.hmdao.farm.shared.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tham số phân trang đến từ URL nên người gọi gửi gì cũng được. Quy ước ở đây là <b>kẹp lại,
 * không báo lỗi</b>: {@code ?size=1000000} là gõ nhầm chứ không phải tấn công, trả về 400 chỉ
 * làm khó người dùng; nhưng để nguyên thì một request kéo cả trăm nghìn dòng nhật ký về.
 */
class PageRequestTest {

    @ParameterizedTest(name = "page={0} -> {1}")
    @CsvSource({"-1, 0", "0, 0", "5, 5"})
    void aNegativePageBecomesTheFirstPage(int requested, int expected) {
        assertThat(PageRequest.of(requested, 20).page()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "size={0} -> {1}")
    @CsvSource({"0, 1", "-10, 1", "50, 50", "1000000, 200"})
    void sizeIsClampedIntoTheAllowedRange(int requested, int expected) {
        assertThat(PageRequest.of(0, requested).size()).isEqualTo(expected);
    }

    @Test
    void theDefaultPageIsTheFirstOneWithTheDefaultSize() {
        PageRequest first = PageRequest.first();

        assertThat(first.page()).isZero();
        assertThat(first.size()).isEqualTo(PageRequest.DEFAULT_SIZE);
        assertThat(first.offset()).isZero();
    }

    @Test
    void offsetSkipsEveryRowOfTheEarlierPages() {
        assertThat(PageRequest.of(3, 25).offset()).isEqualTo(75);
    }

    /** Trang cuối của một tập lớn vẫn phải tính đúng, không tràn int. */
    @Test
    void aFarPageStillComputesAValidOffset() {
        assertThat(PageRequest.of(10_000, PageRequest.MAX_SIZE).offset()).isEqualTo(2_000_000);
    }
}
