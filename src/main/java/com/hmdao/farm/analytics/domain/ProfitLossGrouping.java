package com.hmdao.farm.analytics.domain;

/**
 * Chiều gom nhóm của báo cáo lãi/lỗ. Ba câu hỏi khác nhau của nhà nông:
 *
 * <ul>
 *   <li>{@code CROP} — trồng cây gì thì có lãi? (quyết định tái canh, chuyển đổi cây)</li>
 *   <li>{@code PLOT} — mảnh đất nào làm ăn được? (quyết định đầu tư, thuê thêm, bỏ bớt)</li>
 *   <li>{@code PLANTING} — từng lứa đã hoàn vốn chưa? (theo dõi vòng đời một vườn cụ thể)</li>
 * </ul>
 */
public enum ProfitLossGrouping {
    CROP,
    PLOT,
    PLANTING
}
