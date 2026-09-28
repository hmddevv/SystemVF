# M6 — Kế hoạch frontend

Trạng thái (23/09/2026): **đã chốt — phương án 2.** Chủ dự án chọn giao diện nền tối mở vào màn
Tổng quan (dashboard) theo đặc tả của mình, bỏ mọi phần phải bịa dữ liệu. Mục 0 ghi những gì
đổi so với bản trước; mục 3 là hệ thống thiết kế hiện hành. Các mục còn lại giữ nguyên hiệu lực
trừ chỗ mục 0 nói khác.

Mở rộng (28/09/2026, chủ dự án duyệt): phần còn lại của M6 chia thành **M6b / M6c / M6d**, phủ
đủ Epic A–F bằng API đã có; điều hướng thành 7 mục; thêm `GET /activities` ở M6c (mục 7).

---

## 0. Điều chỉnh theo phương án 2 (23/09/2026)

| Hạng mục | Trước | Nay |
|---|---|---|
| Màn mở đầu | "Hôm nay" (ghi nhật ký trong 10 giây) | **Tổng quan** (dashboard). Nút "Ghi hoạt động" luôn ở thanh trên, lời nhắc nằm ngay trên màn Tổng quan nên việc ghi vẫn cách một chạm |
| Nền | Sáng, đọc được ngoài nắng | **Tối** (mục 3). Hệ quả chấp nhận: khó đọc dưới nắng gắt |
| Ngôn ngữ | TypeScript strict | **JavaScript (JSX)** |
| Client API | Sinh từ `openapi.json` (`openapi-typescript` + `openapi-fetch`) | **axios** trong `src/services/api.js`, tiền tố `/api/v1` qua proxy Vite. Hệ quả: backend đổi tên trường thì frontend **không** báo lỗi lúc biên dịch — lớp `services/` là nơi duy nhất biết tên trường của API |
| Dữ liệu mẫu | Không có | Chỉ cho **truy vấn đọc** khi không kết nối được backend, luôn kèm dải báo "Đang hiện dữ liệu mẫu". Thao tác ghi không bao giờ giả lập thành công |
| Mockup chọn hướng | Bước đầu của M6a | Bỏ — hướng thiết kế đã do chủ dự án chỉ định |
| Cấu trúc | — | `frontend/src/{components,pages,services,hooks}` |

Lấy từ đặc tả của chủ dự án nhưng **không làm vì phải bịa dữ liệu** (backend không có):

- Bản đồ lô đất theo vị trí thật — `Plot` không có toạ độ. Thay bằng **sơ đồ tỉ lệ diện tích**:
  mỗi lô là một khối có diện tích tỉ lệ `areaM2`, tô theo cây đang trồng, ghi rõ "sơ đồ theo diện
  tích, không phải bản đồ".
- Cảnh báo sâu bệnh — thay bằng lời nhắc chăm sóc thật (`GET /reminders`).
- Ô tìm kiếm, chuông thông báo, đăng xuất — chưa có API/đăng nhập. Chỗ avatar là **bộ chọn người
  dùng dev** (`X-User-Id`).
- Panel "Thiết bị & cảm biến" và "Quản lý vật tư" (Phase 5) — ngoài phạm vi M6.

Giữ nguyên hiệu lực: Tailwind v4 (token khai báo bằng `@theme` trong CSS — cùng bộ token của
đặc tả, chỉ khác chỗ đặt), React Router, TanStack Query v5, react-hook-form + zod, mục 6.1–6.6
(endpoint `GET /plantings`, không retry thao tác ghi, bắt đầu lần đầu, `size=200`, không tự tính
niên vụ, nhập liệu trên điện thoại), công cụ kiểm thử và CI ở 6.7 (trừ bước sinh kiểu).

---

## 1. Phạm vi thật của M6

`design.md` chỉ có đúng một dòng về frontend — *"React + Tailwind CSS"* — không mô tả màn
hình nào. Nên phạm vi M6 do ta định nghĩa, và nguồn sự thật đang có là hợp đồng API chốt ở
M5: **20 đường dẫn, 37 endpoint** trong `docs/openapi.json`.

| Màn hình | Endpoint dùng | Epic |
|---|---|---|
| Hôm nay — nhắc việc + ghi nhanh | `GET /reminders`, `POST /plantings/{id}/activities`, **`GET /plantings` (mới, mục 6.1)** | F |
| Bắt đầu lần đầu (khi chưa có dữ liệu) | `POST /farms`, `POST /farms/{id}/plots`, `POST /plots/{id}/plantings`, `GET /crops` | A, B |
| Sổ nhật ký một lứa trồng | `GET /plantings/{id}/seasons`, `GET /seasons/{id}/activities`, `/harvests`, PUT/DELETE | D, E |
| Nông trại & lô đất | `/farms`, `/farms/{id}/plots`, `/plots/{id}` | A |
| Lứa trồng trên một lô (xen canh) | `/plots/{id}/plantings`, `/plantings/{id}`, `/termination`, `/production-start` | B, C |
| Lãi/lỗ | `GET /reports/profit-loss` | E (Phase 2) |
| Danh mục cây trồng | `/crops` | — |

---

## 2. Người dùng và việc cần làm

Chủ nông hộ 35–60 tuổi ở Đắk Lắk, vài ha cà phê xen tiêu và sầu riêng.

Dùng ở hai nơi khác hẳn nhau: **điện thoại, ngoài vườn, giữa nắng, tay bẩn**; và **laptop cũ
ở nhà, buổi tối**.

Hai việc chính, đúng theo thứ tự này:

1. Ghi một dòng nhật ký trong mười giây khi vừa tưới xong.
2. Tối về xem lô nào đang lời, cây nào đang lỗ.

Thứ tự đó quyết định toàn bộ thiết kế: **đây là cuốn sổ ghi ngoài đồng, không phải dashboard.**
Mở app ra mà thấy ba ô số to "Tổng doanh thu / Tổng chi phí / Lợi nhuận" là đã đặt việc của
tối thứ Bảy lên trước việc của chín giờ sáng.

---

## 3. Hệ thống thiết kế — nền tối (chốt 23/09/2026)

### Màu

Token khai báo bằng `@theme` trong `frontend/src/index.css`. Tương phản đo theo WCAG trên `bg` / `panel` / `panel2`.

| Token | Giá trị | Vai trò | Tương phản chữ |
|---|---|---|---|
| `bg` | `#10170F` | Nền trang | — |
| `panel` | `#19231A` | Nền khối | — |
| `panel2` | `#1F2B1E` | Nền khối lồng, ô nhập | — |
| `line` | `#2E3A2B` | Viền mảnh (thay đổ bóng) | chỉ viền |
| `ink` | `#E9EEE3` | Chữ chính | 15.5 / 13.7 / 12.5 |
| `muted` | `#93A288` | Chữ phụ | 6.7 / 6.0 / 5.5 |
| `leaf` | `#6FBE4F` | Nhấn chính, hành động chính; chữ trên nút là `bg` (7.9) | 7.9 / 7.1 / 6.4 |
| `harvest` | `#E3A930` | Mùa vụ, thu hoạch, doanh thu | 8.7 / 7.7 / 7.0 |
| `clay` | `#D9663D` | Cảnh báo, quá hạn, lỗ | 5.1 / 4.6 / **4.2 — không làm chữ trên `panel2`** |
| `sky` | `#5BA7C6` | Nước (tưới) | 6.8 / 6.0 / 5.5 |

**Màu theo loại cây** — nhất quán ở mọi nơi (chấm tròn, sơ đồ lô, biểu đồ), ánh xạ theo
`Crop.name` ở một file duy nhất:

| Cây | Token | Màu | Gợi từ |
|---|---|---|---|
| Cà phê | `crop-coffee` | `#008151` | tán lá cà phê |
| Hồ tiêu | `crop-pepper` | `#CD4C96` | chùm tiêu chín đỏ tía |
| Sầu riêng | `crop-durian` | `#BD8A12` | cơm sầu riêng vàng hổ phách |
| Cao su | `crop-rubber` | `#3782E5` | lam, tách hẳn khỏi nhóm màu ấm |
| Điều | `crop-cashew` | `#C6420F` | trái điều chín cam cháy |
| Cây khác (vd. Ngô) | `crop-other` | `#93A288` | xám lục, cố ý không mang sắc độ |

Bảng màu cây **tách riêng** khỏi màu trạng thái (`leaf`, `harvest`, `clay`) và thay bộ màu đầu
tiên (dùng lại `leafdeep`/`pepper`/`harvest`/`leaf`/`clay`) — bộ đó trượt kiểm tra mù màu: cao su
và sầu riêng gần như trùng với người mù màu đỏ-lục (ΔE 2.6). Bộ mới được tìm trong không gian
OKLCH, mỗi cây giữ vùng sắc độ có nghĩa, rồi chạy `validate_palette` của skill dataviz trên nền
`panel`, so **mọi cặp** vì sơ đồ lô là dạng bản đồ: mù màu ΔE 9.4 (mục tiêu ≥ 8), mắt thường
15.8 (sàn 15), tương phản ≥ 3:1. Vàng chỉ đạt tới hổ phách vì dải sáng trên nền tối dừng ở
L 0.67 — vàng tươi hơn sẽ chói và lệch dải. Màu cây chỉ dùng để tô, không làm chữ.

Màu không bao giờ là kênh thông tin duy nhất: chấm màu luôn đi kèm tên cây, lãi/lỗ luôn có dấu
`+` / `−`.

### Chữ

- **Bitter** (slab serif) — tiêu đề và số liệu, `tabular-nums`.
- **IBM Plex Sans** — chữ giao diện.
- Cả hai có subset `vietnamese` (đã kiểm tra), tự host bằng `@fontsource`.
- Chữ thường tự nhiên, **không in hoa nhãn**.

### Bố cục

```
┌──┬──────────────────────────────────────────────────────────────┐
│  │ Minh Đảo      [nông trại ▾]          [+ Ghi hoạt động] [U1▾]  │
│▣ ├────────────────────────────────────────┬─────────────────────┤
│▤ │ Sơ đồ lô theo diện tích (2/3)           │ Cảnh báo (quá hạn)  │
│▥ │ ┌──────────┬──────┬───┐                 │ Sắp đến hạn         │
│▦ │ │ Lô A2    │ B1   │A1 │  ● cà phê ...   │                     │
│▧ │ └──────────┴──────┴───┘                 │                     │
│  ├────────────────────────────────────────┴─────────────────────┤
│  │ Giám sát mùa vụ: chi phí theo tháng │ Lãi/lỗ theo cây          │
│  │ + tiến độ niên vụ từng lứa           │ thanh ngang, ±           │
└──┴──────────────────────────────────────────────────────────────┘
```

- Thanh icon dọc bên trái, 7 mục (28/09/2026): Tổng quan · Nông trại & lô · Lứa trồng ·
  Nhật ký · Nhắc việc · Báo cáo · Danh mục cây. Trên điện thoại thanh dưới giữ 4 mục dùng nhiều
  nhất quanh nút "Ghi", các mục còn lại vào mục "Thêm" *(đề xuất, chốt khi duyệt M6b)*.
- Viền mảnh `line`, không đổ bóng. Màu nhấn `leaf` dồn cho hành động chính.
- Focus bàn phím: viền `leaf` 2px, lệch 2px.
- Responsive: dưới 1024px hàng hero thành một cột; dưới 640px thanh bên thành thanh dưới.

### Chữ nghĩa

Mọi lỗi đều có `detail` bằng tiếng Việt — đó là thông báo mặc định. `rule` chỉ có ở lỗi nghiệp
vụ. Nút nói đúng việc nó làm: **Ghi hoạt động** → thông báo **Đã ghi**.

### Phương án đã cân nhắc rồi bỏ

Nền sáng "sổ ghi ngoài đồng" mở vào màn "Hôm nay" (bản trước của mục này): tối ưu cho việc ghi
ngoài nắng. Chủ dự án chọn nền tối và màn Tổng quan. Hai mockup của hướng sáng còn trong
`docs/m6-mockups/` để tham khảo.

---

## 4. Ghi chú kỹ thuật

- **Backend chỉ thêm đúng một endpoint** (mục 6.1). Dev dùng proxy của Vite
  (`/api` → `localhost:8080`), M7 dùng nginx cùng origin. Không cần bật CORS.
- **Chưa có đăng nhập** → cần bộ chọn người dùng ở góc màn hình gửi header `X-User-Id`, đánh
  dấu rõ là công cụ dev, Phase 5 thay bằng JWT.
- **Vị trí:** `frontend/` ngay trong repo này, để M7 đóng gói cả hai bằng một `docker compose`.
- **Định dạng:** `Intl.NumberFormat('vi-VN')` cho tiền; ngày theo giờ `Asia/Ho_Chi_Minh` —
  cùng múi giờ với `Clock` của backend, nếu không thì "hôm nay" của hai bên lệch nhau.
- **Lỗi validation** trả về mảng `errors[{field, message}]` → gắn thẳng vào từng ô nhập. Các lỗi
  còn lại hiển thị `detail` (tiếng Việt); `rule` chỉ có ở lỗi nghiệp vụ.

---

## 5. Năm quyết định — đã chốt 23/09/2026

| # | Câu hỏi | Chốt |
|---|---|---|
| 1 | Chia M6? | **M6a** (nền tảng + Tổng quan + ghi hoạt động + bắt đầu lần đầu), rồi **M6b** (đất & cây), **M6c** (nhật ký & thu hoạch), **M6d** (báo cáo & phân tích) — chi tiết ở mục 7. Mỗi mốc dừng duyệt: biết sớm hướng thiết kế có đúng không thay vì làm xong mọi màn mới xem. *(28/09/2026: thay cách chia M6a/M6b ban đầu; sổ nhật ký chuyển từ M6a sang M6c)* |
| 2 | Tailwind v4 hay v3? | **v4**, bản ổn định mới nhất (4.3.x lúc chốt), plugin `@tailwindcss/vite`. Bỏ `tailwind.config.js`, khai báo token bằng `@theme` trong CSS. Chấp nhận yêu cầu Safari 16.4+ / Chrome 111+ / Firefox 128+ |
| 3 | Client gọi API | *(Thay bằng axios ở mục 0.)* ~~**Sinh tự động** từ `docs/openapi.json` bằng `openapi-typescript` + `openapi-fetch` — backend đổi tên một trường là frontend đỏ ngay lúc biên dịch. Đây là lý do ADR-15 tồn tại~~ |
| 4 | Quản lý trạng thái server | **TanStack Query v5** — cache, invalidate sau khi ghi. Retry chỉ cho truy vấn đọc; **thao tác ghi không tự thử lại** (mục 6.2) |
| 5 | Biểu đồ màn lãi/lỗ | Dải niên vụ tự vẽ SVG; biểu đồ luỹ kế theo hướng dẫn của skill `dataviz` |

Nguyên tắc phiên bản: mọi thư viện dùng bản ổn định mới nhất (tag `latest` trên npm), không
beta/canary; ghim bằng `package-lock.json`.

---

## 6. Bổ sung sau rà soát — đã chốt 23/09/2026

Đối chiếu kế hoạch với `docs/openapi.json` lộ ra những chỗ mà mục 1–5 chưa trả lời.

### 6.1 Endpoint mới: `GET /api/v1/plantings?farmId=&activeOnly=true`

API hiện chỉ liệt kê lứa trồng theo từng lô (`/plots/{id}/plantings`). Ghi nhanh một dòng nhật
ký không xuất phát từ lời nhắc sẽ buộc frontend gọi farms → từng lô → từng lứa: N+1 qua mạng,
đúng lúc sóng ngoài vườn yếu nhất. Lời nhắc đã mang `plantingId` nên luồng từ lời nhắc không
bị ảnh hưởng.

- Làm như một phần backend nhỏ ở đầu M6a: `farmId` tùy chọn (bỏ trống = mọi nông trại của chủ
  sở hữu), nông trại của người khác trả 404 (BR-11, ADR-14), truy vấn cố định không N+1.
- Có unit test + `*IT`, cập nhật và commit `docs/openapi.json`.

### 6.2 Mất mạng ngoài vườn — mức tối thiểu

Offline đồng bộ đầy đủ để giai đoạn sau. M6a làm mức tối thiểu:

- Gửi lỗi thì báo rõ "Chưa gửi được", **giữ nguyên nội dung vừa nhập**, có nút "Gửi lại".
- Bản nháp của form ghi nhật ký lưu tạm ở máy (localStorage, bọc try/catch) để lỡ tắt app không mất.
- **Không tự động thử lại thao tác ghi.** Backend chưa có khóa chống ghi trùng (idempotency
  key); retry một POST mà request trước thực ra đã tới server sẽ sinh **dòng nhật ký trùng**.
- Ghi thành ADR-16 cùng các quyết định frontend khác.

### 6.3 Bắt đầu lần đầu

Lời nhắc chỉ xuất hiện khi đã có nông trại → lô → lứa trồng. Người dùng mới mở app sẽ thấy màn
"Hôm nay" trống trơn. Khi chưa có dữ liệu, màn này hiện ba bước: **tạo nông trại → thêm lô →
trồng cây**, mỗi bước một form tối thiểu. Vì vậy ba form tạo này thuộc M6a; phần sửa/xóa/quản lý
đầy đủ vẫn ở M6b.

### 6.4 Sổ nhật ký trộn hai nguồn có phân trang

Hoạt động và thu hoạch nằm ở hai endpoint phân trang riêng; trộn hai luồng phân trang theo ngày
cho đúng là chỗ dễ sai. Chốt: **mỗi niên vụ tải một lần với `size=200`** (tối đa API cho phép)
rồi trộn ở client. Giả định: một niên vụ của một lứa trồng không quá 200 dòng mỗi loại. Nếu
`totalPages > 1` thì hiện dòng "Còn dữ liệu cũ hơn — tải thêm" chứ không cắt im lặng.

### 6.5 Frontend không tự tính niên vụ

Dải niên vụ và mọi chỗ nói tới niên vụ lấy `startDate` / `endDate` / `label` từ API. **Không viết
lại BR-05a bằng TypeScript** — hai nơi cùng tính một thứ thì sớm muộn sẽ lệch. Mùa mưa tô nền
trên dải (tháng 5–9) là hằng số hiển thị, khớp CARE-02.

### 6.6 Nhập liệu trên điện thoại

- **Tiền:** ô chữ với `inputmode="numeric"`, gõ tới đâu định dạng `76.800.000` tới đó, gửi lên
  số nguyên. Không dùng `type="number"` (không hiện dấu chấm nghìn, cuộn chuột đổi giá trị).
- **Ngày:** bộ chọn ngày gốc của trình duyệt, mặc định hôm nay theo `Asia/Ho_Chi_Minh`, `max` =
  hôm nay (BR-07).
- **Loại hoạt động:** 6 nút lớn có biểu tượng + chữ, không dùng dropdown. `OTHER` thì ô ghi chú
  thành bắt buộc (BR-12).

### 6.7 Công cụ và chất lượng

| Hạng mục | Chốt |
|---|---|
| Nền | Vite + React 19 + JavaScript (JSX) — *mục 0* |
| Router | React Router |
| Form | react-hook-form + zod; `errors[]` gắn vào từng ô, lỗi khác hiện `detail` |
| Thành phần khó (dialog, toast) | `<dialog>` gốc của trình duyệt (ADR-16), tự style theo token; không thêm Radix |
| Test | Vitest + Testing Library + MSW (giả lập API theo `openapi.json`) |
| Lint / định dạng | ESLint (flat config) + Prettier |
| Node | Ghim **Node 24 LTS** bằng `.nvmrc` và `engines`; CI dùng cùng bản |
| Font | Tự host bằng `@fontsource/*` (subset `vietnamese`), không gọi Google Fonts lúc chạy |
| Trạng thái màn hình | Mọi màn có đủ: đang tải, lỗi, trống; một Error Boundary ở gốc |
| Dung lượng | Tách code theo route; ngưỡng JS gzip cho màn đầu tiên ghi trong README frontend |
| CI | Job `frontend` trong `build.yml`: `npm ci` → lint → Prettier → test → build (không sinh kiểu — mục 0) |

### 6.8 Để sau, ghi nhận là giới hạn đã biết

- **PWA / cài ra màn hình chính** — làm cùng offline đầy đủ.
- **Ghi đè khi hai máy cùng sửa:** request PUT không mang `version`, nên hai thiết bị sửa cùng
  một dòng thì bản sau đè bản trước mà không báo. Hiếm với một hộ; xem lại khi có phân quyền công nhân.
- **Bộ chọn người dùng dev:** không có API liệt kê người dùng → ghi cứng user 1 và 2 (seed demo).
- **ADR-16** trong `architecture.md`: ghi các quyết định frontend ở mục 5–6.

---

## 7. Thứ tự làm và tiêu chí xong

### M6a

1. Dựng `frontend/`: Vite + React + Tailwind v4 (token mục 3), React Router, TanStack Query,
   `services/api.js` (axios, proxy `/api/v1`, `X-User-Id`, dữ liệu mẫu cho truy vấn đọc), khung
   giao diện (thanh icon, thanh trên, bộ chọn người dùng dev).
2. Màn **Tổng quan**: sơ đồ lô theo diện tích, cảnh báo + sắp đến hạn, giám sát mùa vụ, lãi/lỗ
   theo cây. Trạng thái trống dẫn qua ba bước bắt đầu (6.3).
3. Backend: `GET /plantings` (6.1); chuyển Tổng quan và form ghi sang dùng nó. **Xong** — một câu
   JPQL `join fetch`, IT đếm ≤ 3 câu SQL; Tổng quan còn 2 lần gọi cố định thay cho N+1.
4. **Form ghi hoạt động** dạng slide-over — chọn **lứa trồng** (không phải lô, vì một lô trồng
   xen có nhiều lứa), loại việc, ngày, chi phí, ghi chú (6.2, 6.6). **Xong** — `<dialog>` gốc
   (phải trên desktop, dưới lên trên điện thoại), mở từ thanh trên, nút "Ghi" giữa thanh điều
   hướng dưới và nút "Ghi" của từng lời nhắc (điền sẵn `suggestedActivity`). Đã chạy với backend
   thật: ghi thành công báo niên vụ do backend xếp, lời nhắc tự biến mất (BR-18), lỗi 422 hiện
   `detail`, offline báo "Chưa gửi được" và giữ nháp. Lighthouse Accessibility 100 (có và không
   mở form).
5. Job CI frontend, ADR-16, README frontend. **Xong** — job `frontend` trong `build.yml` (lint,
   Prettier, Vitest, build); 18 test gồm lớp gọi API và form ghi, trong đó có test hồi quy
   chống tự gửi lại khi mất mạng (đã kiểm: bỏ `networkMode: 'always'` thì test đỏ). Cấu hình
   TanStack Query tách ra `services/queryClient.js` để test chạy đúng cấu hình thật.
6. **Bắt đầu lần đầu** (6.3) — ba form tạo tối thiểu: nông trại (`POST /farms`) → lô
   (`POST /farms/{id}/plots`) → lứa trồng (`POST /plots/{id}/plantings`, cây lấy từ `GET /crops`,
   danh mục có sẵn từ `V2`). Bước hiện tại suy ra từ dữ liệu (chưa có nông trại → 1, có nông
   trại chưa có lô → 2, có lô chưa có lứa đang trồng → 3), không lưu cờ riêng. Xử lý lỗi dùng
   chung với form ghi (`errors[]` vào ô, `detail`, "Chưa gửi được" + "Gửi lại"). *Bổ sung
   28/09/2026: mục 6.3 xếp phần này vào M6a nhưng danh sách bước ở đây từng bỏ sót.*
   **Xong** — `components/onboarding/GettingStarted.jsx`; phần báo lỗi tách ra
   `components/form/` và form ghi hoạt động dùng lại. Tạo xong mỗi bước thì ghi thẳng kết quả vào
   cache nên bước sau hiện ngay, focus chuyển lên tiêu đề bước mới. Nút "Ghi" ẩn tới khi có lứa
   đang trồng. Đã chạy trọn ba bước với backend thật (user 2, xóa lại sau khi thử); axe-core
   không có vi phạm ở cả bước đầu, bước cuối và khi đang báo lỗi. 27 test (thêm 9, gồm đổi nông trại giữa chừng không trồng nhầm lô). Sửa kèm hai
   lỗi Tổng quan lộ ra với nông trại mới: nhãn trục "500 nghìn" bị cắt, biểu đồ chi phí toàn số
   0 vẽ đường phẳng thay vì nói "chưa ghi khoản chi nào".
   *Giới hạn:* nông trại có lô nhưng mọi lứa đều đã kết thúc cũng rơi vào bước 3, che mất lãi/lỗ
   lịch sử trên Tổng quan. Chưa xảy ra được ở M6a (chưa có màn kết thúc lứa) — xem lại ở M6b.

**Xong M6a khi:** chạy được với backend thật, trên điện thoại thật qua wifi (`vite --host`) và có
ảnh chụp; điểm Accessibility của Lighthouse ≥ 95; CI xanh → **dừng duyệt**.

### M6b — Đất & cây (Epic A, B, C)

1. **Nông trại**: danh sách (số lô, tổng diện tích), tạo / sửa / xóa. Xóa bị chặn (409, BR-10)
   thì hiện nguyên `detail` kèm việc cần làm trước.
2. **Lô đất**: danh sách theo nông trại + chi tiết lô — các lứa đang trồng xen, **lịch sử sử
   dụng đất** (dải thời gian các lứa đã kết thúc kèm lý do), nút "Trồng cây mới", sửa / xóa.
3. **Hồ sơ lứa trồng** (màn trung tâm): cây, giống, tuổi, số cây; thanh vòng đời
   *Kiến thiết → Kinh doanh → Đã kết thúc*; chuyển sang kinh doanh (`production-start`); kết
   thúc lứa (`termination`, bắt buộc lý do trong 5 lý do); sửa ngày trồng / số cây.
4. **Danh mục cây**: thêm / sửa cây và giống (lâu năm hay ngắn ngày, tháng bắt đầu niên vụ).
5. Điều hướng 7 mục (mục 3). Nhật ký, Nhắc việc, Báo cáo còn là trang "sắp có" tới mốc của nó.

### M6c — Nhật ký & thu hoạch (Epic D, E, F)

1. **Backend `GET /api/v1/activities?farmId=&from=&to=&type=`** (phân trang): nhật ký toàn nông
   trại một lần gọi, thay cho farms → lứa → niên vụ (N+1). Làm như `GET /plantings`: unit test,
   `*IT` đếm câu SQL, cập nhật `openapi.json`, BR-11 trả 404. Cần thì thêm `GET /harvests` tương tự.
2. **Sổ nhật ký một lứa** trong hồ sơ lứa trồng: dải niên vụ (nhãn, ngày từ API — 6.5), chọn
   niên vụ xem tổng chi / thu / sản lượng / lãi, danh sách trộn hoạt động + thu hoạch (6.4),
   sửa / xóa từng dòng.
3. **Form ghi thu hoạch**: sản lượng kg, doanh thu (bỏ trống = chưa bán); giá/kg lấy từ response.
   Thu hoạch đầu tiên tự chuyển lứa sang kinh doanh (BR-09) — báo lại cho người dùng.
4. **Trang Nhật ký** toàn nông trại: lọc theo loại việc, lô, khoảng ngày.
5. **Trang Nhắc việc**: đủ lời nhắc, nhóm theo mức độ, ghi luật sinh ra (CARE-01…04), nút "Ghi".

### M6d — Báo cáo & phân tích (Epic E Phase 2)

1. **Báo cáo** ba chiều `groupBy` = cây / lô / lứa, lọc niên vụ; bảng có chỉ số chuẩn hoá
   (BR-15), cờ "số liệu ước lượng" cho lô trồng xen (BR-16).
2. Theo lứa: **luỹ kế lãi/lỗ và niên vụ hoàn vốn** (BR-17) — biểu đồ theo skill `dataviz`.
3. Xuất CSV từ đúng dữ liệu đang xem.
4. Tổng quan thêm: tổng chi / thu / lãi niên vụ hiện tại, hoạt động gần đây (dùng API của M6c).

**Mỗi mốc M6b–M6d xong khi** đạt cùng tiêu chí M6a: chạy với backend thật, ảnh trên điện thoại
thật, Lighthouse Accessibility ≥ 95, CI xanh → dừng duyệt.

**Không làm trong M6** (`design.md` §8, backend chưa có dữ liệu): vật tư đầu vào, giá thị trường,
ảnh truy xuất nguồn gốc, cảm biến, phân quyền công nhân.

---

## 8. Công cụ nên dùng trong M6

| Công cụ | Dùng ở đâu | Vì sao |
|---|---|---|
| **context7** (MCP) | Trước khi viết cấu hình Tailwind, Vite, TanStack Query | Có hai bộ tài liệu tách biệt cho Tailwind v3 và v4. v4 đổi cách cấu hình tận gốc — đúng loại việc mà trí nhớ mô hình dễ lỗi thời |
| **chrome-devtools** (MCP + skill) | Suốt quá trình dựng | Tự chụp màn hình để tự phê bình thiết kế, đọc console và network. Bạn chỉ chụp ở điểm dừng để duyệt |
| **a11y-debugging** (skill) | Cuối mỗi màn | Dùng ngoài nắng, tay bẩn: tương phản màu và vùng chạm ≥44px là điều kiện dùng được, không phải chuyện làm màu |
| **dataviz** (skill) | Chỉ màn lãi/lỗ | Nạp *trước* dòng code biểu đồ đầu tiên |
| **ide getDiagnostics** | Sau mỗi lần sửa file | Bắt lỗi TypeScript ngay, không cần chạy build |
| **fewer-permission-prompts** | Đầu M6 | M6 chạy rất nhiều lệnh `npm`; allowlist để đỡ bị hỏi quyền |
| **/code-review**, **/security-review** | Cuối M6 | Frontend mở bề mặt mới: XSS, xử lý header danh tính, dữ liệu trong localStorage. **Do bạn gõ**, Claude không tự chạy được |
| **Playwright** (MCP) | Để sau | Dựng E2E lúc giao diện còn đổi thì sửa test nhiều hơn viết tính năng |

**Không dùng:** claude-in-chrome và Playwright *song song* với chrome-devtools (ba công cụ
trình duyệt chồng chức năng — chọn một); Artifact (giao diện phải sống trong repo, chạy được
thật); Claude Docs / Google Drive / docx (không sinh tài liệu rời); subagent (mỗi lần gọi là
dựng lại ngữ cảnh, trong khi phiên hiện tại đang nắm toàn bộ thiết kế API và bảng quy tắc).
