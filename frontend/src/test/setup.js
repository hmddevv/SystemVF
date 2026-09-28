import '@testing-library/jest-dom/vitest';
import { afterAll, afterEach, beforeAll } from 'vitest';
import { cleanup, configure } from '@testing-library/react';
import { onlineManager } from '@tanstack/react-query';
import { server } from './server';

/*
 * MSW chặn request ở tầng mạng: axios, interceptor X-User-Id và cách đổi lỗi sang ApiError đều
 * chạy như thật. Request không có handler thì test đỏ — không để lọt gọi mạng thật.
 */
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));

/*
 * findBy… và waitFor mặc định chờ 1 giây. Máy nguội (lần chạy đầu, CI mới dựng) từng làm test form
 * ghi đỏ chập chờn dù chạy lại thì xanh — test đầu tiên đã mất ~0,9 giây khi máy ấm. 3 giây chỉ
 * nới thời gian CHỜ thứ đáng xuất hiện; thứ không bao giờ xuất hiện vẫn làm test đỏ.
 */
configure({ asyncUtilTimeout: 3000 });
afterEach(() => {
  server.resetHandlers();
  cleanup();
  onlineManager.setOnline(true);
  try {
    localStorage.clear();
  } catch {
    // jsdom luôn có localStorage; phòng hờ
  }
});
afterAll(() => server.close());

// jsdom chưa hỗ trợ đủ <dialog>: bổ sung showModal/close tối thiểu để form mở được trong test.
if (typeof HTMLDialogElement !== 'undefined') {
  const proto = HTMLDialogElement.prototype;
  if (!proto.showModal) {
    proto.showModal = function showModal() {
      this.setAttribute('open', '');
    };
  }
  if (!proto.close) {
    proto.close = function close() {
      this.removeAttribute('open');
      this.dispatchEvent(new Event('close'));
    };
  }
}
