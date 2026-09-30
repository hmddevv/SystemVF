import { describe, expect, it } from 'vitest';
import { act, render, screen, waitFor } from '@testing-library/react';
import { QueryClientProvider } from '@tanstack/react-query';
import { http, HttpResponse } from 'msw';
import { createQueryClient } from '../services/queryClient';
import { api as client, isMockData } from '../services/api';
import * as mockData from '../services/mockData';
import { SessionProvider } from './useSession';
import {
  seasonOptions,
  useActivePlantings,
  useFarms,
  useMockActive,
  useRefetchMockOnReconnect,
} from './useFarmData';
import { api, server } from '../test/server';

describe('bộ lọc niên vụ', () => {
  it('lấy nguyên nhãn của API, cây ngắn ngày chỉ là "2025"', () => {
    expect(seasonOptions([{ year: 2025, label: '2025' }])).toEqual([{ year: 2025, label: '2025' }]);
  });

  it('cùng năm bắt đầu mà nhãn khác nhau thì hiện đủ, năm mới trước', () => {
    const options = seasonOptions([
      { year: 2024, label: '2024/2025' },
      { year: 2025, label: '2025' },
      { year: 2025, label: '2025/2026' },
      { year: 2025, label: '2025/2026' },
    ]);
    expect(options).toEqual([
      { year: 2025, label: '2025/2026 · 2025' },
      { year: 2024, label: '2024/2025' },
    ]);
  });
});

describe('dấu dữ liệu mẫu trong cache', () => {
  it('dữ liệu thật rồi mất kết nối: kết quả mẫu vẫn giữ dấu qua structural sharing', async () => {
    const queryClient = createQueryClient({ queries: { retry: false } });
    const query = { queryKey: ['farms', 1], queryFn: client.farms.list, staleTime: 0 };
    await queryClient.fetchQuery(query);
    server.use(http.get(api('/farms'), () => HttpResponse.error()));
    await queryClient.fetchQuery(query);
    expect(isMockData(queryClient.getQueryData(['farms', 1]))).toBe(true);
  });

  it('dữ liệu mẫu rồi dữ liệu thật trùng nội dung: không giữ lại object mẫu cũ', async () => {
    const queryClient = createQueryClient({ queries: { retry: false } });
    const query = { queryKey: ['farms', 1], queryFn: client.farms.list, staleTime: 0 };
    server.use(http.get(api('/farms'), () => HttpResponse.error()));
    await queryClient.fetchQuery(query);
    server.use(http.get(api('/farms'), () => HttpResponse.json(mockData.farms())));
    await queryClient.fetchQuery(query);
    expect(isMockData(queryClient.getQueryData(['farms', 1]))).toBe(false);
  });
});

function Probe() {
  useRefetchMockOnReconnect();
  useFarms();
  useActivePlantings(1);
  return <p>{useMockActive() ? 'đang có dữ liệu mẫu' : 'toàn dữ liệu thật'}</p>;
}

function renderProbe() {
  const queryClient = createQueryClient({ queries: { retry: false } });
  render(
    <QueryClientProvider client={queryClient}>
      <SessionProvider>
        <Probe />
      </SessionProvider>
    </QueryClientProvider>,
  );
  return queryClient;
}

describe('dải báo dữ liệu mẫu', () => {
  it('một truy vấn có lại dữ liệu thật không tắt dải báo khi truy vấn khác vẫn là dữ liệu mẫu', async () => {
    server.use(
      http.get(api('/farms'), () => HttpResponse.error()),
      http.get(api('/plantings'), () => HttpResponse.error()),
    );
    const queryClient = renderProbe();
    await screen.findByText('đang có dữ liệu mẫu');

    // Nông trại có lại dữ liệu thật; lứa trồng vẫn không tải được
    let farmCalls = 0;
    server.use(
      http.get(api('/farms'), () => {
        farmCalls += 1;
        return HttpResponse.json([]);
      }),
    );
    await act(() => queryClient.refetchQueries({ queryKey: ['farms'] }));
    await waitFor(() => expect(queryClient.isFetching()).toBe(0));
    expect(screen.getByText('đang có dữ liệu mẫu')).toBeInTheDocument();
    // Lần đọc vừa thành công không bị hủy rồi gửi lại khi báo có kết nối lại
    expect(farmCalls).toBe(1);

    // Backend chạy lại hẳn: lần đọc thành công kế tiếp tải lại mọi truy vấn còn giữ dữ liệu mẫu
    server.resetHandlers();
    await act(() => queryClient.refetchQueries({ queryKey: ['farms'] }));
    expect(await screen.findByText('toàn dữ liệu thật')).toBeInTheDocument();
  });
});
