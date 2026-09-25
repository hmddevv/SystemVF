import { describe, expect, it } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClientProvider, onlineManager } from '@tanstack/react-query';
import { http, HttpResponse } from 'msw';
import { createQueryClient } from '../../services/queryClient';
import { todayIso } from '../../services/format';
import { SessionProvider } from '../../hooks/useSession';
import { LogActivityProvider, useLogActivity } from '../../hooks/useLogActivity';
import { api, problem, server } from '../../test/server';

/*
 * Form ghi hoạt động chạy với cấu hình QueryClient THẬT (createQueryClient) — test chống ghi trùng
 * chỉ có nghĩa khi retry và networkMode giống hệt ứng dụng.
 */

function Opener({ prefill }) {
  const { open } = useLogActivity();
  return (
    <button type="button" onClick={() => open(prefill)}>
      mở form
    </button>
  );
}

async function openSheet(prefill) {
  const user = userEvent.setup();
  render(
    <QueryClientProvider client={createQueryClient({ queries: { retry: false } })}>
      <SessionProvider>
        <LogActivityProvider>
          <Opener prefill={prefill} />
        </LogActivityProvider>
      </SessionProvider>
    </QueryClientProvider>,
  );
  await user.click(screen.getByRole('button', { name: 'mở form' }));
  const sheet = await screen.findByRole('dialog', { name: 'Ghi hoạt động' });
  // Danh sách lứa trồng đã tải
  await within(sheet).findByRole('radio', { name: /Cà phê/ });
  return { user, sheet };
}

function captureLog() {
  const requests = [];
  server.use(
    http.post(api('/plantings/:id/activities'), async ({ request, params }) => {
      const body = await request.json();
      requests.push({ plantingId: params.id, body });
      return HttpResponse.json(
        {
          id: 31,
          seasonId: 7,
          seasonLabel: '2026/2027',
          type: body.type,
          activityDate: body.activityDate,
          cost: body.cost ?? 0,
          note: body.note,
        },
        { status: 201 },
      );
    }),
  );
  return requests;
}

describe('Ghi hoạt động', () => {
  it('ghi vào LỨA TRỒNG, gửi tiền dạng số và báo lại niên vụ backend đã xếp', async () => {
    const requests = captureLog();
    const { user, sheet } = await openSheet();

    await user.click(within(sheet).getByRole('radio', { name: /Cà phê/ }));
    await user.click(within(sheet).getByRole('radio', { name: 'Bón phân' }));
    await user.type(within(sheet).getByLabelText('Chi phí, đồng'), '1250000');
    expect(within(sheet).getByLabelText('Chi phí, đồng')).toHaveValue('1.250.000');
    await user.click(within(sheet).getByRole('button', { name: 'Ghi hoạt động' }));

    expect(await within(sheet).findByText(/Đã ghi bón phân/)).toBeInTheDocument();
    expect(within(sheet).getByText('2026/2027')).toBeInTheDocument();
    expect(requests).toEqual([
      {
        plantingId: '1',
        body: { type: 'FERTILIZING', activityDate: todayIso(), cost: 1250000, note: null },
      },
    ]);
  });

  it('mở từ lời nhắc thì điền sẵn lứa trồng và việc gợi ý của backend', async () => {
    const { sheet } = await openSheet({ plantingId: 3, type: 'FERTILIZING' });
    expect(within(sheet).getByRole('radio', { name: /Sầu riêng/ })).toBeChecked();
    expect(within(sheet).getByRole('radio', { name: 'Bón phân' })).toBeChecked();
  });

  it('BR-12: "Việc khác" phải có ghi chú, chặn ngay ở máy không gửi đi', async () => {
    // Không đăng ký handler POST: nếu form vẫn gửi, MSW báo request lạ và test đỏ
    const { user, sheet } = await openSheet();
    await user.click(within(sheet).getByRole('radio', { name: /Cà phê/ }));
    await user.click(within(sheet).getByRole('radio', { name: 'Việc khác' }));
    await user.click(within(sheet).getByRole('button', { name: 'Ghi hoạt động' }));

    expect(await within(sheet).findByText('Việc khác thì ghi rõ là việc gì')).toBeInTheDocument();
  });

  it('lỗi validation 400 gắn vào đúng ô theo tên trường', async () => {
    server.use(
      http.post(api('/plantings/:id/activities'), () =>
        problem(400, {
          type: 'urn:farm:problem:validation',
          detail: 'Dữ liệu không hợp lệ',
          errors: [{ field: 'cost', message: 'Chi phí tối đa 13 chữ số' }],
        }),
      ),
    );
    const { user, sheet } = await openSheet({ plantingId: 1, type: 'WEEDING' });
    await user.click(within(sheet).getByRole('button', { name: 'Ghi hoạt động' }));

    const cost = within(sheet).getByLabelText('Chi phí, đồng');
    await waitFor(() => expect(cost).toHaveAttribute('aria-invalid', 'true'));
    expect(within(sheet).getByText('Chi phí tối đa 13 chữ số')).toBeInTheDocument();
  });

  it('lỗi nghiệp vụ 422 hiện nguyên detail, giữ nút "Ghi" vì gửi lại y nguyên vẫn lỗi', async () => {
    server.use(
      http.post(api('/plantings/:id/activities'), () =>
        problem(422, { detail: 'Ngày 2010-03-01 trước ngày trồng 2016-06-15.', rule: 'BR-07' }),
      ),
    );
    const { user, sheet } = await openSheet({ plantingId: 1, type: 'WEEDING' });
    await user.click(within(sheet).getByRole('button', { name: 'Ghi hoạt động' }));

    expect(
      await within(sheet).findByText('Ngày 2010-03-01 trước ngày trồng 2016-06-15.'),
    ).toBeInTheDocument();
    expect(within(sheet).getByRole('button', { name: 'Ghi hoạt động' })).toBeEnabled();
  });

  /*
   * Hồi quy: networkMode mặc định của TanStack Query treo mutation khi offline rồi TỰ GỬI khi có
   * mạng lại — đã tái hiện được với backend thật (M6a). Backend chưa có idempotency key nên đó là
   * đường dẫn tới dòng nhật ký trùng. Yêu cầu: báo lỗi ngay, giữ nội dung, chỉ gửi khi người
   * dùng bấm "Gửi lại".
   */
  it('mất mạng: báo "Chưa gửi được" ngay, giữ nội dung, có mạng lại cũng KHÔNG tự gửi', async () => {
    let calls = 0;
    server.use(
      http.post(api('/plantings/:id/activities'), () => {
        calls += 1;
        return HttpResponse.error();
      }),
    );
    const { user, sheet } = await openSheet({ plantingId: 1, type: 'WEEDING' });
    onlineManager.setOnline(false);
    await user.click(within(sheet).getByRole('button', { name: 'Ghi hoạt động' }));

    expect(await within(sheet).findByText('Chưa gửi được')).toBeInTheDocument();
    expect(within(sheet).getByRole('radio', { name: 'Làm cỏ' })).toBeChecked();
    expect(within(sheet).getByRole('button', { name: 'Gửi lại' })).toBeInTheDocument();
    expect(calls).toBe(1);

    onlineManager.setOnline(true);
    await new Promise((r) => setTimeout(r, 50));
    expect(calls).toBe(1);
  });

  it('bản nháp: bỏ lứa trồng không còn trong danh sách và đưa ngày về hôm nay', async () => {
    localStorage.setItem(
      'farm.logDraft.1',
      JSON.stringify({
        plantingId: '999',
        type: 'PRUNING',
        activityDate: '2026-01-02',
        cost: '50.000',
        note: 'tỉa chồi vượt',
      }),
    );
    const { sheet } = await openSheet();

    await waitFor(() =>
      within(sheet)
        .getAllByRole('radio', { name: /Cà phê|Sầu riêng/ })
        .forEach((r) => expect(r).not.toBeChecked()),
    );
    expect(within(sheet).getByRole('radio', { name: 'Tỉa cành' })).toBeChecked();
    expect(within(sheet).getByLabelText('Chọn ngày khác')).toHaveValue(todayIso());
    expect(within(sheet).getByLabelText('Chi phí, đồng')).toHaveValue('50.000');
  });
});
