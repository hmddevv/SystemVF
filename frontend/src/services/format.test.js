import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  digitsOnly,
  formatSignedMoney,
  formatWeekdayDate,
  groupDigits,
  shiftDays,
  todayIso,
} from './format';

describe('ô nhập tiền', () => {
  it('chỉ giữ chữ số — dấu chấm là ngăn nghìn kiểu Việt, không phải dấu thập phân', () => {
    expect(digitsOnly('12.500.000 đ')).toBe('12500000');
    expect(digitsOnly('')).toBe('');
  });

  it('ngăn nghìn khi đang gõ và bỏ số 0 thừa ở đầu', () => {
    expect(groupDigits('1250000')).toBe('1.250.000');
    expect(groupDigits('0012')).toBe('12');
    expect(groupDigits('1.2a3')).toBe('123');
  });
});

describe('ngày theo giờ Việt Nam', () => {
  afterEach(() => vi.useRealTimers());

  it('"hôm nay" là ngày ở Việt Nam, không phải ngày UTC của máy', () => {
    // 25/9 17:30 UTC = 26/9 00:30 ở Việt Nam — lệch múi giờ thì backend coi ngày này là tương lai
    vi.useFakeTimers({ now: new Date('2026-09-25T17:30:00Z') });
    expect(todayIso()).toBe('2026-09-26');
  });

  it('lùi ngày qua ranh giới tháng và năm', () => {
    expect(shiftDays('2026-03-01', -1)).toBe('2026-02-28');
    expect(shiftDays('2026-01-01', -1)).toBe('2025-12-31');
  });

  it('ghi kèm năm khi không phải năm nay', () => {
    vi.useFakeTimers({ now: new Date('2026-09-26T03:00:00Z') });
    expect(formatWeekdayDate('2026-09-26')).toBe('Thứ Bảy, 26/9');
    expect(formatWeekdayDate('2010-03-01')).toBe('Thứ Hai, 1/3/2010');
  });
});

it('lãi/lỗ luôn có dấu tường minh, không chỉ dựa vào màu', () => {
  expect(formatSignedMoney(8_500_000)).toBe('+8.500.000 đ');
  expect(formatSignedMoney(-8_500_000)).toBe('−8.500.000 đ');
  expect(formatSignedMoney(0)).toBe('0 đ');
});
