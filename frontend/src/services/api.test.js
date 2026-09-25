import { describe, expect, it } from 'vitest';
import { http, HttpResponse } from 'msw';
import { api, mockStatus, setUserId } from './api';
import { api as path, problem, server } from '../test/server';

describe('truy vấn đọc', () => {
  it('gửi X-User-Id của người dùng đang chọn', async () => {
    let header;
    server.use(
      http.get(path('/farms'), ({ request }) => {
        header = request.headers.get('X-User-Id');
        return HttpResponse.json([]);
      }),
    );
    setUserId(2);
    await api.farms.list();
    expect(header).toBe('2');
  });

  it('mất kết nối thì lấy dữ liệu mẫu và bật cờ để giao diện báo rõ', async () => {
    server.use(http.get(path('/farms'), () => HttpResponse.error()));
    const farms = await api.farms.list();
    expect(farms.length).toBeGreaterThan(0);
    expect(mockStatus.get()).toBe(true);

    // Có kết nối lại thì tắt cờ
    server.use(http.get(path('/farms'), () => HttpResponse.json([])));
    await api.farms.list();
    expect(mockStatus.get()).toBe(false);
  });

  it('lỗi nghiệp vụ thật của backend không bị che bằng dữ liệu mẫu', async () => {
    server.use(
      http.get(path('/reminders'), () =>
        problem(404, { type: 'urn:farm:problem:not-found', detail: 'Không tìm thấy nông trại 9.' }),
      ),
    );
    await expect(api.reminders.list(9)).rejects.toMatchObject({
      status: 404,
      detail: 'Không tìm thấy nông trại 9.',
      unreachable: false,
    });
  });
});

describe('thao tác ghi', () => {
  it('không có dữ liệu mẫu và không tự thử lại khi máy chủ không phản hồi', async () => {
    let calls = 0;
    server.use(
      http.post(path('/plantings/1/activities'), () => {
        calls += 1;
        // Proxy của Vite trả 502 không kèm ProblemDetail khi backend tắt
        return new HttpResponse(null, { status: 502 });
      }),
    );
    await expect(api.activities.log(1, { type: 'WEEDING' })).rejects.toMatchObject({
      unreachable: true,
    });
    expect(calls).toBe(1);
  });

  it('giữ nguyên detail, rule và errors của ProblemDetail', async () => {
    server.use(
      http.post(path('/plantings/1/activities'), () =>
        problem(422, { detail: 'Ngày làm trước ngày trồng.', rule: 'BR-07' }),
      ),
    );
    await expect(api.activities.log(1, {})).rejects.toMatchObject({
      status: 422,
      detail: 'Ngày làm trước ngày trồng.',
      rule: 'BR-07',
      errors: [],
    });
  });
});
