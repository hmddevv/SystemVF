/*
 * Nhãn tiếng Việt cho mọi enum của API — file ánh xạ DUY NHẤT (CLAUDE.md).
 * Thêm giá trị enum ở backend thì thêm ở đây; thiếu nhãn sẽ hiện nguyên mã để dễ phát hiện.
 */

export const ACTIVITY_TYPE = {
  WATERING: 'Tưới nước',
  FERTILIZING: 'Bón phân',
  SPRAYING: 'Phun thuốc',
  WEEDING: 'Làm cỏ',
  PRUNING: 'Tỉa cành',
  OTHER: 'Việc khác',
};

export const PLANTING_STATUS = {
  GROWING: 'Đang trồng',
  PRODUCING: 'Đang cho thu',
  TERMINATED: 'Đã kết thúc',
};

export const END_REASON = {
  MARKET: 'Giá thị trường',
  PEST_DISEASE: 'Sâu bệnh',
  WEATHER: 'Thời tiết',
  OLD_AGE: 'Già cỗi',
  OTHER: 'Lý do khác',
};

export const REMINDER_SEVERITY = {
  OVERDUE: 'Quá hạn',
  DUE_SOON: 'Sắp đến hạn',
};

export const GROUP_BY = {
  CROP: 'Theo cây',
  PLOT: 'Theo lô',
  PLANTING: 'Theo lứa trồng',
};

export function label(map, code) {
  return map[code] ?? code;
}
