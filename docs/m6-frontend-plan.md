# M6 — Kế hoạch frontend

Trạng thái (23/09/2026): **đã chốt kỹ thuật và phạm vi** — năm quyết định ở mục 5 và các bổ
sung sau rà soát ở mục 6. **Hệ thống thiết kế ở mục 3 vẫn là đề xuất**: chốt sau khi chủ dự án
chọn một trong các mockup (bước đầu tiên của M6a, mục 7).

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

## 3. Hệ thống thiết kế (đề xuất — chốt sau khi chọn mockup, mục 7)

### Màu

| Token | Giá trị | Vai trò |
|---|---|---|
| `giay` | `#EFF1EC` | Nền xám pha xanh lá nhạt — đọc được dưới nắng gắt, không phải nền kem |
| `muc` | `#14201A` | Đen ngả xanh lá cà phê ướt, toàn bộ chữ |
| `ke` | `#C9CDBE` | Đường kẻ ngang của sổ, 1px |
| `dat` | `#8A3A1E` | Oxit đất bazan — lỗ, quá hạn |
| `la` | `#2F6B3C` | Lá — lãi, đã xong |
| `nang` | `#C98A12` | Chỉ làm nền đánh dấu. Tương phản 2,6:1, **không bao giờ làm màu chữ** |

Bốn token đầu chịu toàn bộ giao diện. `dat` và `la` chỉ xuất hiện ở chỗ nói về tiền và hạn
việc — nhìn vào là biết ngay đâu là thông tin có hệ quả.

### Chữ

- **Bricolage Grotesque** — tiêu đề và dải niên vụ.
- **IBM Plex Sans** — nội dung và số, bật `tnum` để cột kg và cột đồng thẳng hàng.

Ràng buộc cứng: **phải phủ đủ dấu tiếng Việt.** Kiểm tra `subset=vietnamese` trước khi dùng;
hụt thì lùi về Be Vietnam Pro.

### Bố cục: dòng sổ, không phải thẻ

Đơn vị cơ bản là một dòng kẻ ngang chạy hết chiều rộng. Ngày bên trái, việc ở giữa, tiền bên
phải căn phải như sổ kế toán. Không bo góc, không đổ bóng, không thẻ.

```
┌──────────────────────────────────────────────────────────────┐
│  Lô A2 · Cà phê Robusta              niên vụ 2025/2026        │
│  ╞═══╪════╪════╪════╪════╪════╪════╪════╪════╪════╪════╡     │
│   T2   T3   T4  │T5   T6   T7   T8   T9│ T10  T11  T12  T1    │
│    ·    ●        ░░░░ mùa mưa ░░░░░░░░        ▲    ▲          │
│                      ▲ hôm nay                thu hoạch       │
├──────────────────────────────────────────────────────────────┤
│  HÔM NAY                                                      │
│  quá hạn 9 ngày   Bón phân đợt 2 · Lô A2          [ Ghi ]     │
│  ─────────────────────────────────────────────────────────    │
│  còn 3 ngày       Tỉa cành sau thu · Lô B1        [ Ghi ]     │
├──────────────────────────────────────────────────────────────┤
│  15/11   Thu hoạch cà phê   3.200 kg         +76.800.000 đ    │
│  02/11   Bón phân NPK                           −10.000.000 đ │
│  28/10   Tưới đợt 3                              −1.800.000 đ │
└──────────────────────────────────────────────────────────────┘
```

**Thứ duy nhất được phép nổi bật là dải niên vụ.** Nó vẽ đúng khái niệm trung tâm của hệ
thống — niên vụ chứ không phải năm dương lịch — thành một vật thể nhìn thấy được: tháng chạy
ngang, mùa mưa tô nền, chi phí là vạch dưới đường kẻ, thu hoạch là vạch trên, hôm nay là mũi
tên. Nhìn một cái hiểu ngay "tiền bỏ ra trước, tiền thu về sau" — điều ba ô số không nói được.

Mọi thứ còn lại im lặng.

### Chuyển động: đúng một khoảnh khắc

Ghi xong một dòng nhật ký thì lời nhắc tương ứng gạch ngang rồi rời khỏi danh sách. Đó là
BR-18 hiện thành hình: không có nút "đã làm", chính dòng nhật ký làm lời nhắc biến mất.
Ngoài ra không có hiệu ứng trôi lên khi cuộn, không transition trên từng thẻ.

### Chữ nghĩa

Mọi lỗi đều có `detail` bằng tiếng Việt — đó là thông báo mặc định. `rule` chỉ có ở lỗi nghiệp
vụ (không có ở 400, `concurrent-update`, `data-integrity`; xem bảng lỗi trong `CLAUDE.md`). Khi có
`rule`, giao diện có thể nói rõ hơn phải làm gì:

> **BR-10** → "Không xóa được lô đất này: còn 3 lứa trồng đang canh tác. Kết thúc các lứa
> trồng trước đã."

Nút nói đúng việc nó làm: **Ghi hoạt động** → thông báo **Đã ghi**.

### Đã cân nhắc rồi bỏ

Bản nháp đầu: nền kem `#F4F1EA`, serif tiêu đề to, nhấn màu đất nung, mỗi nông trại một thẻ
bo góc đổ bóng, hàng KPI ở đầu trang. Bỏ vì hai lý do, lý do thứ hai mới là lý do thật:

1. Đó là bộ mặc định thấy ở mọi trang sinh bằng AI.
2. **Nó đặt sai việc.** Thẻ tách mỗi lô thành một hòn đảo, trong khi nhà nông cần so sánh các
   dòng với nhau theo ngày và theo tiền — việc của sổ kẻ dòng. Hàng KPI thì trả lời câu hỏi
   của buổi tối, đặt trước câu hỏi của buổi sáng.

Cũng bỏ: nền đen với xanh neon, nhãn IN HOA giãn chữ, mốc 01 / 02 / 03 (nhật ký là dòng thời
gian, không phải quy trình có bước).

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
| 1 | Chia M6? | **M6a** (nền tảng + Hôm nay + sổ nhật ký + bắt đầu lần đầu) và **M6b** (quản lý nông trại/lô/lứa trồng/danh mục + lãi/lỗ). Điểm dừng rơi đúng lúc giao diện đã dùng được thật, thay vì làm xong 6 màn mới biết hướng thiết kế có đúng không |
| 2 | Tailwind v4 hay v3? | **v4**, bản ổn định mới nhất (4.3.x lúc chốt), plugin `@tailwindcss/vite`. Bỏ `tailwind.config.js`, khai báo token bằng `@theme` trong CSS. Chấp nhận yêu cầu Safari 16.4+ / Chrome 111+ / Firefox 128+ |
| 3 | Client gọi API | **Sinh tự động** từ `docs/openapi.json` bằng `openapi-typescript` + `openapi-fetch` — backend đổi tên một trường là frontend đỏ ngay lúc biên dịch. Đây là lý do ADR-15 tồn tại |
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
| Nền | Vite + React 19 + TypeScript (strict) |
| Router | React Router |
| Form | react-hook-form + zod; `errors[]` gắn vào từng ô, lỗi khác hiện `detail` |
| Thành phần khó (dialog, toast) | Radix UI primitives, tự style theo token |
| Test | Vitest + Testing Library + MSW (giả lập API theo `openapi.json`) |
| Lint / định dạng | ESLint (flat config) + Prettier |
| Node | Ghim **Node 24 LTS** bằng `.nvmrc` và `engines`; CI dùng cùng bản |
| Font | Tự host bằng `@fontsource/*` (subset `vietnamese`), không gọi Google Fonts lúc chạy |
| Trạng thái màn hình | Mọi màn có đủ: đang tải, lỗi, trống; một Error Boundary ở gốc |
| Dung lượng | Tách code theo route; ngưỡng JS gzip cho màn đầu tiên ghi trong README frontend |
| CI | Job `frontend` trong `build.yml`: `npm ci` → sinh kiểu từ `openapi.json` → `git diff --exit-code` file kiểu → typecheck → lint → test → build |

### 6.8 Để sau, ghi nhận là giới hạn đã biết

- **PWA / cài ra màn hình chính** — làm cùng offline đầy đủ.
- **Ghi đè khi hai máy cùng sửa:** request PUT không mang `version`, nên hai thiết bị sửa cùng
  một dòng thì bản sau đè bản trước mà không báo. Hiếm với một hộ; xem lại khi có phân quyền công nhân.
- **Bộ chọn người dùng dev:** không có API liệt kê người dùng → ghi cứng user 1 và 2 (seed demo).
- **ADR-16** trong `architecture.md`: ghi các quyết định frontend ở mục 5–6.

---

## 7. Thứ tự làm và tiêu chí xong

### M6a

1. **Mockup 2–3 hướng thiết kế** (HTML tĩnh, dữ liệu tiếng Việt thật, xem được trên điện thoại)
   → chủ dự án chọn → chốt mục 3 → **dừng duyệt**.
2. Backend: `GET /plantings` (6.1).
3. Dựng `frontend/`: Vite, Tailwind v4 + token, client sinh từ `openapi.json`, TanStack Query,
   router, proxy, bộ chọn người dùng dev, job CI.
4. Màn **Hôm nay** + form ghi nhanh + bắt đầu lần đầu (6.2, 6.3, 6.6).
5. Màn **Sổ nhật ký** một lứa trồng + dải niên vụ (6.4, 6.5).
6. ADR-16, README frontend.

**Xong M6a khi:** chạy được trên điện thoại thật qua wifi (`vite --host`) và có ảnh chụp; điểm
Accessibility của Lighthouse ≥ 95 cho mọi màn; CI xanh cả backend lẫn frontend → **dừng duyệt**.

### M6b

Quản lý nông trại / lô / lứa trồng (sửa, xóa, kết thúc, bắt đầu cho thu hoạch), danh mục cây
trồng, màn lãi/lỗ. Cùng tiêu chí xong như M6a.

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
