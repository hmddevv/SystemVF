import { QueryClient } from '@tanstack/react-query';

/*
 * Cấu hình TanStack Query dùng chung cho ứng dụng và test — test phải chạy đúng cấu hình thật,
 * nếu không thì test chống ghi trùng không chứng minh được gì.
 *
 * - Truy vấn đọc thử lại 1 lần.
 * - Thao tác ghi KHÔNG BAO GIỜ tự thử lại: backend chưa có idempotency key, thử lại một POST có
 *   thể sinh dòng nhật ký trùng (plan 6.2, ADR-16).
 * - networkMode 'always' cho thao tác ghi: mặc định (online) máy mất mạng thì mutation bị treo ở
 *   "đang gửi" rồi tự gửi khi có mạng lại mà người dùng không hay biết. Phải báo lỗi ngay để
 *   người dùng tự quyết định gửi lại.
 */
export function createQueryClient(overrides = {}) {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: 1, refetchOnWindowFocus: false, staleTime: 30_000, ...overrides.queries },
      mutations: { retry: 0, networkMode: 'always' },
    },
  });
}
