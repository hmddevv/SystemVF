/*
 * Định dạng số, tiền, ngày theo tiếng Việt. "Hôm nay" tính theo giờ Việt Nam để khớp Clock của
 * backend — lệch múi giờ thì hai bên hiểu "hôm nay" khác nhau.
 */

export const TIME_ZONE = 'Asia/Ho_Chi_Minh';

const number = new Intl.NumberFormat('vi-VN');
const compact = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 1 });

export function formatMoney(value) {
  return `${number.format(Math.round(value ?? 0))} đ`;
}

// Tiền có dấu tường minh: lãi/lỗ không bao giờ chỉ dựa vào màu.
export function formatSignedMoney(value) {
  const v = Math.round(value ?? 0);
  if (v === 0) return '0 đ';
  return `${v > 0 ? '+' : '−'}${number.format(Math.abs(v))} đ`;
}

// Rút gọn cho trục biểu đồ: 12.400.000 → "12,4 tr"; 1.250.000.000 → "1,3 tỷ".
export function formatMoneyShort(value) {
  const v = Math.abs(value ?? 0);
  const sign = value < 0 ? '−' : '';
  if (v >= 1e9) return `${sign}${compact.format(v / 1e9)} tỷ`;
  if (v >= 1e6) return `${sign}${compact.format(v / 1e6)} tr`;
  if (v >= 1e3) return `${sign}${compact.format(v / 1e3)} nghìn`;
  return `${sign}${number.format(v)}`;
}

export function formatNumber(value) {
  return number.format(value ?? 0);
}

export function formatArea(m2) {
  if (m2 >= 10000) return `${compact.format(m2 / 10000)} ha`;
  return `${number.format(m2)} m²`;
}

// yyyy-mm-dd của hôm nay theo giờ Việt Nam.
export function todayIso() {
  return new Intl.DateTimeFormat('en-CA', { timeZone: TIME_ZONE }).format(new Date());
}

export function formatDayMonth(iso) {
  const [, m, d] = iso.split('-');
  return `${d}/${m}`;
}

export function formatLongToday() {
  return new Intl.DateTimeFormat('vi-VN', {
    timeZone: TIME_ZONE,
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  }).format(new Date());
}

export function daysBetween(fromIso, toIso) {
  const a = Date.UTC(...fromIso.split('-').map((n, i) => (i === 1 ? n - 1 : +n)));
  const b = Date.UTC(...toIso.split('-').map((n, i) => (i === 1 ? n - 1 : +n)));
  return Math.round((b - a) / 864e5);
}
