# Frontend — Minh Đảo

Giao diện web cho hệ thống quản lý canh tác: nền tối, mở vào màn **Tổng quan**, dùng được trên
điện thoại ngoài vườn. Phạm vi, token màu và các quyết định đã chốt nằm ở
[`docs/m6-frontend-plan.md`](../docs/m6-frontend-plan.md); lý do kiến trúc ở ADR-16 trong
[`docs/architecture.md`](../docs/architecture.md).

## Chạy

Cần Node 24 LTS (`.nvmrc`) và backend đang chạy ở cổng 8080 (xem README gốc).

```bash
npm ci
npm run dev            # http://localhost:5173 — /api được proxy sang localhost:8080
npm run dev -- --host  # mở từ điện thoại cùng wifi, qua IP của máy
```

Backend tắt thì các màn đọc hiện **dữ liệu mẫu kèm dải báo màu vàng**; thao tác ghi sẽ báo
"Chưa gửi được", không bao giờ giả vờ thành công.

Chưa có đăng nhập: bộ chọn "Người dùng dev" trên thanh trên đổi header `X-User-Id` (1 hoặc 2)
để thử việc cô lập dữ liệu giữa hai chủ nông trại (BR-11). Phase 5 thay bằng JWT.

## Lệnh

| Lệnh | Làm gì |
|---|---|
| `npm run dev` | Máy chủ phát triển có proxy API |
| `npm test` | Vitest + Testing Library, API giả lập bằng MSW |
| `npm run lint` | ESLint |
| `npm run format` / `format:check` | Prettier sửa / chỉ kiểm |
| `npm run build` | Bản production vào `dist/` |

CI (`.github/workflows/build.yml`, job `frontend`) chạy lint, Prettier, test và build trên mọi
nhánh.

## Cấu trúc

```
src/
  services/    nơi DUY NHẤT gọi HTTP và biết tên trường API (api.js), dữ liệu mẫu,
               nhãn tiếng Việt của enum, định dạng tiền/ngày, cấu hình TanStack Query
  hooks/       bọc TanStack Query cho từng màn; phiên (người dùng, nông trại); mở form ghi
  components/  mảnh dùng lại: khung, panel, biểu đồ Tổng quan, form ghi hoạt động,
               màn Bắt đầu (onboarding/), phần báo lỗi dùng chung của mọi form ghi (form/)
  pages/       mỗi file một màn
  test/        cấu hình Vitest và máy chủ MSW
```

## Những luật dễ làm sai

- **Đổi tên trường ở backend không báo lỗi lúc biên dịch** (JavaScript, không sinh kiểu từ
  `openapi.json`). Hợp đồng API đổi thì rà `src/services/` bằng tay.
- **Thao tác ghi không tự gửi lại**, kể cả khi có mạng trở lại: backend chưa có idempotency key,
  gửi lại có thể sinh dòng nhật ký trùng. Cấu hình ở `services/queryClient.js`
  (`retry: 0`, `networkMode: 'always'`) có test hồi quy — đừng đổi.
- **Ghi theo lứa trồng, không theo lô.** Lô trồng xen có nhiều lứa.
- **Không tự tính niên vụ.** Lấy `label`, `startDate`, `endDate` từ API.
- Màu cây không bao giờ đứng một mình — luôn kèm tên cây. Màu cây (`crop-*`) chỉ để tô, không
  làm chữ; đổi màu thì chạy lại bộ kiểm tra mù màu cho cả bộ. `clay` không làm chữ trên `panel2`.
- "Hôm nay" theo giờ Việt Nam (`todayIso()`), không dùng `new Date().toISOString()`.

## Giới hạn đã biết

- Chưa đồng bộ offline: mất sóng thì người dùng tự bấm "Gửi lại"; nháp chỉ lưu trên một máy.
- Nông trại có lô mà mọi lứa đã kết thúc sẽ hiện lại màn Bắt đầu (bước 3) thay cho Tổng quan —
  xem lại ở M6b khi có màn kết thúc lứa.
- Lô đất, lứa trồng, nhật ký, nhắc việc, báo cáo chi tiết làm ở M6b–M6d (plan mục 7) — hiện là
  trang "sắp có".
