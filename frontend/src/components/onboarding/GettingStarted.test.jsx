import { describe, expect, it } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router';
import { QueryClientProvider, onlineManager } from '@tanstack/react-query';
import { http, HttpResponse } from 'msw';
import { createQueryClient } from '../../services/queryClient';
import { shiftDays, todayIso } from '../../services/format';
import { SessionProvider, useSession } from '../../hooks/useSession';
import { LogActivityProvider } from '../../hooks/useLogActivity';
import { DashboardPage } from '../../pages/DashboardPage';
import { api, problem, server } from '../../test/server';

/*
 * Bắt đầu lần đầu (plan 6.3). Render qua màn Tổng quan để kiểm luôn việc suy ra bước từ dữ liệu:
 * chưa có nông trại → 1, chưa có lô → 2, chưa có lứa đang trồng → 3, đủ cả → Tổng quan.
 * Backend giả giữ trạng thái trong bộ nhớ, nên tạo xong một bước thì lần đọc sau thấy kết quả.
 */

const CROPS = [
  {
    id: 1,
    name: 'Cà phê',
    variety: 'Robusta',
    displayName: 'Cà phê (Robusta)',
    perennial: true,
    seasonStartMonth: 2,
  },
  {
    id: 3,
    name: 'Hồ tiêu',
    variety: 'Vĩnh Linh',
    displayName: 'Hồ tiêu (Vĩnh Linh)',
    perennial: true,
    seasonStartMonth: 5,
  },
  {
    id: 7,
    name: 'Ngô',
    variety: 'LVN10',
    displayName: 'Ngô (LVN10)',
    perennial: false,
    seasonStartMonth: null,
  },
];

function fakeBackend({ farms = [], plots = [], plantings = [] } = {}) {
  const db = { farms: [...farms], plots: [...plots], plantings: [...plantings] };
  const posts = [];
  server.use(
    http.get(api('/farms'), () => HttpResponse.json(db.farms)),
    http.post(api('/farms'), async ({ request }) => {
      const body = await request.json();
      posts.push({ url: '/farms', body });
      const farm = { id: 5, plotCount: 0, totalAreaM2: 0, ...body };
      db.farms.push(farm);
      return HttpResponse.json(farm, { status: 201 });
    }),
    http.get(api('/farms/:farmId/plots'), ({ params }) =>
      HttpResponse.json(db.plots.filter((p) => p.farmId === Number(params.farmId))),
    ),
    http.post(api('/farms/:farmId/plots'), async ({ request, params }) => {
      const body = await request.json();
      posts.push({ url: `/farms/${params.farmId}/plots`, body });
      const plot = {
        id: 9,
        farmId: Number(params.farmId),
        areaHectares: body.areaM2 / 1e4,
        ...body,
      };
      db.plots.push(plot);
      return HttpResponse.json(plot, { status: 201 });
    }),
    http.get(api('/plantings'), () => HttpResponse.json(db.plantings)),
    http.get(api('/crops'), () => HttpResponse.json(CROPS)),
    http.post(api('/plots/:plotId/plantings'), async ({ request, params }) => {
      const body = await request.json();
      posts.push({ url: `/plots/${params.plotId}/plantings`, body });
      const plot = db.plots.find((p) => p.id === Number(params.plotId));
      const crop = CROPS.find((c) => c.id === body.cropId);
      const planting = {
        id: 40 + db.plantings.length,
        plotId: plot.id,
        plotName: plot.name,
        cropId: crop.id,
        cropName: crop.displayName,
        perennial: crop.perennial,
        plantingDate: body.plantingDate,
        treeCount: body.treeCount,
        status: body.alreadyProducing ? 'PRODUCING' : 'GROWING',
        ageMonths: 0,
        endDate: null,
        endReason: null,
        endNote: null,
      };
      db.plantings.push(planting);
      return HttpResponse.json(planting, { status: 201 });
    }),
    // Màn Tổng quan sau khi xong ba bước
    http.get(api('/reminders'), () => HttpResponse.json([])),
    http.get(api('/reports/profit-loss'), () =>
      HttpResponse.json({ groupBy: 'CROP', year: null, rows: [], total: null }),
    ),
    http.get(api('/plantings/:id/seasons'), () => HttpResponse.json([])),
  );
  return posts;
}

const FARM = { id: 5, name: 'Vườn nhà', location: null, plotCount: 0, totalAreaM2: 0 };
const PLOT = {
  id: 9,
  farmId: 5,
  name: 'Lô A1',
  areaM2: 15000,
  areaHectares: 1.5,
  soilType: null,
};

// Thay cho bộ chọn nông trại ở thanh trên (Topbar không nằm trong DashboardPage)
function FarmSwitch({ ids }) {
  const { setFarmId } = useSession();
  return ids.map((id) => (
    <button key={id} type="button" onClick={() => setFarmId(id)}>
      xem nông trại {id}
    </button>
  ));
}

function renderDashboard({ farmIds } = {}) {
  const user = userEvent.setup();
  render(
    <QueryClientProvider client={createQueryClient({ queries: { retry: false } })}>
      <SessionProvider>
        <LogActivityProvider>
          <MemoryRouter>
            {farmIds && <FarmSwitch ids={farmIds} />}
            <DashboardPage />
          </MemoryRouter>
        </LogActivityProvider>
      </SessionProvider>
    </QueryClientProvider>,
  );
  return user;
}

// Điền thẻ chi tiết của một cây đã chọn — nhãn ô mang tên cây để hai cây không trùng nhãn
async function fillRow(user, cropName, { date, trees, producing = false }) {
  await user.type(screen.getByLabelText(`Ngày trồng, ${cropName}`), date);
  await user.type(screen.getByLabelText(`Số cây, ${cropName}`), trees);
  if (producing) await user.click(screen.getByLabelText(`Đã cho thu hoạch, ${cropName}`));
}

const stepHeading = (name) => screen.findByRole('heading', { level: 2, name });

describe('Bắt đầu lần đầu', () => {
  it('chưa có nông trại: tạo nông trại xong thì sang ngay bước thêm lô, focus theo sang', async () => {
    const posts = fakeBackend();
    const user = renderDashboard();

    await stepHeading('Tạo nông trại');
    expect(screen.getByRole('listitem', { current: 'step' })).toHaveTextContent('Tạo nông trại');

    await user.type(screen.getByLabelText('Tên nông trại'), '  Vườn nhà  ');
    await user.click(screen.getByRole('button', { name: 'Tạo nông trại' }));

    const next = await stepHeading('Thêm lô đất');
    await waitFor(() => expect(next).toHaveFocus());
    expect(screen.getByRole('status')).toHaveTextContent('Đã tạo nông trại Vườn nhà.');
    // Bỏ khoảng trắng thừa; địa chỉ trống gửi null chứ không gửi chuỗi rỗng
    expect(posts).toEqual([{ url: '/farms', body: { name: 'Vườn nhà', location: null } }]);
  });

  it('thêm lô: diện tích gõ kiểu Việt, quy ra ha để đối chiếu, gửi lên số m²', async () => {
    const posts = fakeBackend({ farms: [FARM] });
    const user = renderDashboard();

    await stepHeading('Thêm lô đất');
    await user.type(screen.getByLabelText('Tên lô'), 'Lô A1');
    const area = screen.getByLabelText('Diện tích');
    await user.type(area, '15000');
    expect(area).toHaveValue('15.000');
    expect(screen.getByText('Bằng 1,5 ha')).toBeInTheDocument();
    await user.clear(area);
    await user.type(area, '30');
    // Lô nhỏ không bị làm tròn thành "0 ha"
    expect(screen.getByText('Bằng 0,003 ha')).toBeInTheDocument();
    await user.clear(area);
    await user.type(area, '15000');
    await user.click(screen.getByRole('button', { name: 'Thêm lô' }));

    await stepHeading('Trồng cây trên lô');
    expect(posts).toEqual([
      { url: '/farms/5/plots', body: { name: 'Lô A1', areaM2: 15000, soilType: null } },
    ]);
  });

  it('trồng cây: một lô thì chọn sẵn; xong ba bước thì vào Tổng quan', async () => {
    const posts = fakeBackend({ farms: [FARM], plots: [PLOT] });
    const user = renderDashboard();

    await stepHeading('Trồng cây trên lô');
    // Chỉ một lô: không bắt người dùng chọn
    expect(screen.queryByRole('group', { name: 'Lô' })).not.toBeInTheDocument();

    await user.click(await screen.findByRole('checkbox', { name: /^Cà phê/ }));
    await fillRow(user, 'Cà phê (Robusta)', {
      date: '2016-06-15',
      trees: '1100',
      producing: true,
    });
    await user.click(screen.getByRole('button', { name: 'Trồng cây' }));

    expect(await screen.findByRole('heading', { level: 1, name: 'Tổng quan' })).toBeInTheDocument();
    expect(posts).toEqual([
      {
        url: '/plots/9/plantings',
        body: { cropId: 1, plantingDate: '2016-06-15', treeCount: 1100, alreadyProducing: true },
      },
    ]);
  });

  /*
   * Xen canh (Epic B): một lô có nhiều lứa, mỗi cây khai ngày trồng và số cây riêng. Backend
   * nhận một lứa mỗi lần gọi, nên N cây là N lần POST — gửi lần lượt, đúng thông tin của từng cây.
   */
  it('trồng xen: chọn nhiều cây trên một lô, mỗi cây một lứa với ngày trồng và số cây riêng', async () => {
    const posts = fakeBackend({ farms: [FARM], plots: [PLOT] });
    const user = renderDashboard();

    await stepHeading('Trồng cây trên lô');
    await user.click(await screen.findByRole('checkbox', { name: /^Cà phê/ }));
    await user.click(screen.getByRole('checkbox', { name: /^Hồ tiêu/ }));
    await fillRow(user, 'Cà phê (Robusta)', { date: '2016-06-15', trees: '1100', producing: true });
    await fillRow(user, 'Hồ tiêu (Vĩnh Linh)', { date: '2020-05-10', trees: '400' });
    await user.click(screen.getByRole('button', { name: 'Trồng 2 loại cây' }));

    expect(await screen.findByRole('heading', { level: 1, name: 'Tổng quan' })).toBeInTheDocument();
    expect(posts).toEqual([
      {
        url: '/plots/9/plantings',
        body: { cropId: 1, plantingDate: '2016-06-15', treeCount: 1100, alreadyProducing: true },
      },
      {
        url: '/plots/9/plantings',
        body: { cropId: 3, plantingDate: '2020-05-10', treeCount: 400, alreadyProducing: false },
      },
    ]);
  });

  it('một cây lỗi: cây đã trồng khóa lại, "Gửi lại" chỉ gửi cây còn thiếu', async () => {
    const posts = fakeBackend({ farms: [FARM], plots: [PLOT] });
    let pepperFails = true;
    server.use(
      http.post(api('/plots/:plotId/plantings'), async ({ request }) => {
        const body = await request.clone().json();
        if (body.cropId === 3 && pepperFails) {
          pepperFails = false;
          return problem(422, { detail: 'Ngày trồng trước ngày tạo lô.', rule: 'BR-02' });
        }
        return undefined; // chuyển tiếp cho backend giả
      }),
    );
    const user = renderDashboard();

    await stepHeading('Trồng cây trên lô');
    await user.click(await screen.findByRole('checkbox', { name: /^Cà phê/ }));
    await user.click(screen.getByRole('checkbox', { name: /^Hồ tiêu/ }));
    await fillRow(user, 'Cà phê (Robusta)', { date: '2016-06-15', trees: '1100' });
    await fillRow(user, 'Hồ tiêu (Vĩnh Linh)', { date: '2020-05-10', trees: '400' });
    await user.click(screen.getByRole('button', { name: 'Trồng 2 loại cây' }));

    // Vẫn ở bước 3: lỗi hiện đúng trên thẻ hồ tiêu, cà phê đã trồng và bị khóa
    expect(await screen.findByText('Ngày trồng trước ngày tạo lô.')).toBeInTheDocument();
    expect(screen.getByText('Đã trồng')).toBeInTheDocument();
    expect(screen.getByLabelText('Số cây, Cà phê (Robusta)')).toBeDisabled();
    expect(screen.getByRole('checkbox', { name: /^Cà phê/ })).toBeDisabled();

    await user.click(screen.getByRole('button', { name: 'Trồng 1 cây còn lại' }));
    expect(await screen.findByRole('heading', { level: 1, name: 'Tổng quan' })).toBeInTheDocument();
    // Cà phê chỉ được tạo MỘT lần
    expect(posts.map((p) => p.body.cropId)).toEqual([1, 3]);
  });

  it('mất mạng giữa chừng: dừng ngay, không gửi tiếp cây sau, có mạng lại cũng không tự gửi', async () => {
    fakeBackend({ farms: [FARM], plots: [PLOT] });
    let calls = 0;
    server.use(
      http.post(api('/plots/:plotId/plantings'), () => {
        calls += 1;
        return HttpResponse.error();
      }),
    );
    const user = renderDashboard();

    await stepHeading('Trồng cây trên lô');
    await user.click(await screen.findByRole('checkbox', { name: /^Cà phê/ }));
    await user.click(screen.getByRole('checkbox', { name: /^Hồ tiêu/ }));
    await fillRow(user, 'Cà phê (Robusta)', { date: '2016-06-15', trees: '1100' });
    await fillRow(user, 'Hồ tiêu (Vĩnh Linh)', { date: '2020-05-10', trees: '400' });
    onlineManager.setOnline(false);
    await user.click(screen.getByRole('button', { name: 'Trồng 2 loại cây' }));

    expect(await screen.findByText('Chưa gửi được')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Gửi lại' })).toBeInTheDocument();
    expect(calls).toBe(1);
    onlineManager.setOnline(true);
    await new Promise((r) => setTimeout(r, 50));
    expect(calls).toBe(1);
  });

  /*
   * Quay lại một nông trại đã xem thì lô có sẵn trong cache, Tổng quan không qua màn "đang tải"
   * nên form không tự dựng lại. Nông trại chỉ có một lô thì ô chọn lô bị ẩn — nếu form giữ lô của
   * nông trại vừa xem, người dùng trồng nhầm chỗ mà không thấy gì trên màn hình.
   */
  it('đổi qua lại giữa hai nông trại ở bước 3 thì trồng đúng lô của nông trại đang xem', async () => {
    const farmB = { ...FARM, id: 6, name: 'Vườn rẫy' };
    const plotB = { ...PLOT, id: 20, farmId: 6, name: 'Lô R1' };
    const posts = fakeBackend({ farms: [FARM, farmB], plots: [PLOT, plotB] });
    const user = renderDashboard({ farmIds: [5, 6] });
    const stepOf = (name) =>
      screen.findByText(
        (_, el) => el?.tagName === 'P' && el.textContent === `Bước 3 trên 3, ${name}`,
      );

    await stepOf('Vườn nhà');
    await user.click(screen.getByRole('button', { name: 'xem nông trại 6' }));
    await stepOf('Vườn rẫy');
    await user.click(screen.getByRole('button', { name: 'xem nông trại 5' }));
    await stepOf('Vườn nhà');

    await user.click(await screen.findByRole('checkbox', { name: /^Ngô/ }));
    await fillRow(user, 'Ngô (LVN10)', { date: '2026-05-01', trees: '5000' });
    await user.click(screen.getByRole('button', { name: 'Trồng cây' }));

    await waitFor(() => expect(posts).toHaveLength(1));
    expect(posts[0].url).toBe('/plots/9/plantings');
  });

  it('chặn ngay ở máy: chưa chọn cây nào; ngày trồng ở tương lai, số cây 0', async () => {
    fakeBackend({ farms: [FARM], plots: [PLOT] });
    let calls = 0;
    server.use(
      http.post(api('/plots/:plotId/plantings'), () => {
        calls += 1;
        return HttpResponse.error();
      }),
    );
    const user = renderDashboard();

    await stepHeading('Trồng cây trên lô');
    await screen.findByRole('checkbox', { name: /^Cà phê/ });
    await user.click(screen.getByRole('button', { name: 'Trồng cây' }));
    expect(
      await screen.findByRole('group', { name: 'Cây trồng trên lô' }),
    ).toHaveAccessibleDescription(/Chọn ít nhất một loại cây/);

    await user.click(screen.getByRole('checkbox', { name: /^Cà phê/ }));
    await fillRow(user, 'Cà phê (Robusta)', { date: shiftDays(todayIso(), 1), trees: '0' });
    await user.click(screen.getByRole('button', { name: 'Trồng cây' }));

    expect(await screen.findByText('Ngày trồng không được ở tương lai')).toBeInTheDocument();
    expect(screen.getByText('Nhập số cây lớn hơn 0')).toBeInTheDocument();
    expect(screen.getByLabelText('Số cây, Cà phê (Robusta)')).toHaveAttribute(
      'aria-invalid',
      'true',
    );
    expect(calls).toBe(0);
  });

  it('BR-01 tên lô trùng: hiện nguyên detail của backend, giữ nội dung đã nhập', async () => {
    fakeBackend({ farms: [FARM] });
    server.use(
      http.post(api('/farms/:farmId/plots'), () =>
        problem(409, {
          type: 'urn:farm:problem:conflict',
          detail: 'Nông trại Vườn nhà đã có lô tên Lô A1.',
          rule: 'BR-01',
        }),
      ),
    );
    const user = renderDashboard();

    await stepHeading('Thêm lô đất');
    await user.type(screen.getByLabelText('Tên lô'), 'Lô A1');
    await user.type(screen.getByLabelText('Diện tích'), '5000');
    await user.click(screen.getByRole('button', { name: 'Thêm lô' }));

    expect(await screen.findByText('Nông trại Vườn nhà đã có lô tên Lô A1.')).toBeInTheDocument();
    expect(screen.getByLabelText('Tên lô')).toHaveValue('Lô A1');
    // Lỗi nghiệp vụ gửi lại y nguyên vẫn lỗi: không mời "Gửi lại"
    expect(screen.getByRole('button', { name: 'Thêm lô' })).toBeEnabled();
  });

  it('lỗi validation 400 của backend gắn vào đúng ô', async () => {
    fakeBackend({ farms: [FARM] });
    server.use(
      http.post(api('/farms/:farmId/plots'), () =>
        problem(400, {
          type: 'urn:farm:problem:validation',
          detail: 'Dữ liệu không hợp lệ',
          errors: [{ field: 'areaM2', message: 'Diện tích phải lớn hơn 0' }],
        }),
      ),
    );
    const user = renderDashboard();

    await stepHeading('Thêm lô đất');
    await user.type(screen.getByLabelText('Tên lô'), 'Lô A1');
    await user.type(screen.getByLabelText('Diện tích'), '5000');
    await user.click(screen.getByRole('button', { name: 'Thêm lô' }));

    await waitFor(() =>
      expect(screen.getByLabelText('Diện tích')).toHaveAttribute('aria-invalid', 'true'),
    );
    expect(screen.getByText('Diện tích phải lớn hơn 0')).toBeInTheDocument();
  });

  it('mất mạng: báo "Chưa gửi được", có mạng lại cũng KHÔNG tự tạo nông trại', async () => {
    fakeBackend();
    let calls = 0;
    server.use(
      http.post(api('/farms'), () => {
        calls += 1;
        return HttpResponse.error();
      }),
    );
    const user = renderDashboard();

    await stepHeading('Tạo nông trại');
    await user.type(screen.getByLabelText('Tên nông trại'), 'Vườn nhà');
    onlineManager.setOnline(false);
    await user.click(screen.getByRole('button', { name: 'Tạo nông trại' }));

    const alert = await screen.findByRole('alert');
    expect(within(alert).getByText('Chưa gửi được')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Gửi lại' })).toBeInTheDocument();
    expect(screen.getByLabelText('Tên nông trại')).toHaveValue('Vườn nhà');

    onlineManager.setOnline(true);
    await new Promise((r) => setTimeout(r, 50));
    expect(calls).toBe(1);
  });

  it('nông trại có lô và cây đang trồng thì không hiện màn Bắt đầu', async () => {
    fakeBackend({
      farms: [FARM],
      plots: [PLOT],
      plantings: [
        {
          id: 40,
          plotId: 9,
          plotName: 'Lô A1',
          cropId: 1,
          cropName: 'Cà phê (Robusta)',
          perennial: true,
          plantingDate: '2016-06-15',
          treeCount: 1100,
          status: 'PRODUCING',
          ageMonths: 123,
          endDate: null,
          endReason: null,
          endNote: null,
        },
      ],
    });
    renderDashboard();

    expect(await screen.findByRole('heading', { level: 1, name: 'Tổng quan' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Bắt đầu' })).not.toBeInTheDocument();
  });
});
