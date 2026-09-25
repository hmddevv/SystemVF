# CLAUDE.md — Hệ thống Quản lý Canh tác Nông trại

Hướng dẫn cho Claude Code khi làm việc trong repo này. Đọc hết trước khi sửa code.

## Làm việc với chủ dự án

- **Trả lời bằng tiếng Việt.** Tên lớp, biến, endpoint, lệnh giữ nguyên tiếng Anh.
- Dự án chia theo mốc (M0…M7, xem `docs/architecture.md` §10). **Hết mỗi mốc thì dừng lại**
  để chủ dự án chụp màn hình và duyệt, không tự chạy sang mốc sau.
- **Chỉ commit / push khi được yêu cầu.** Mỗi mốc một nhánh `feature/mN-<chủ-đề>`, tách từ
  nhánh của mốc trước. Không làm thẳng trên `main`.
- Commit message viết bằng tiếng Việt, theo mẫu đã dùng:
  ```
  M6: <tóm tắt một dòng>

  <nhóm thay đổi>
  - <ý>: <vì sao>, không chỉ <làm gì>.
  ```
- Quyết định kiến trúc mới thì ghi thành ADR (đánh số tiếp) trong `docs/architecture.md`;
  quy tắc nghiệp vụ mới thì thêm mã BR-xx vào bảng §6 **và** viết test cho nó.

## Tài liệu là nguồn sự thật

| File | Dùng khi |
|---|---|
| `docs/design.md` | Nghiệp vụ gốc, user story (Epic A–F), ERD, từ điển dữ liệu |
| `docs/architecture.md` | Module, Ports & Adapters, **bảng quy tắc BR-01…BR-18, CARE-01…04 (§6)**, API (§7), ADR-1…16 (§9), lộ trình (§10) |
| `docs/openapi.json` | Hợp đồng API — 21 đường dẫn, 38 endpoint. Frontend lấy kiểu dữ liệu từ đây |
| `docs/m6-frontend-plan.md` | Kế hoạch frontend M6: phạm vi, quyết định đã chốt, thứ tự làm |

Không chép lại nội dung các file này vào code hay tài liệu khác, chỉ trỏ tới chúng.

## Khái niệm nghiệp vụ không được làm sai

- **Niên vụ ≠ năm dương lịch.** Niên vụ cà phê 2025/2026 chạy từ 1/2/2025 đến 31/1/2026.
  `SEASON.year` là năm *bắt đầu*, hiển thị dạng `2025/2026`. Báo cáo gom theo niên vụ (BR-13).
- **Niên vụ do hệ thống tự gán (BR-05a, ADR-7).** Người dùng chỉ khai "lứa trồng nào, ngày nào".
  Không có POST/PUT niên vụ. Ghi hoạt động/thu hoạch vào `/plantings/{id}/…`, đọc theo
  `/seasons/{id}/…`. **Không bao giờ bắt người dùng chọn niên vụ.**
- **Xen canh:** một lô có nhiều lứa trồng cùng lúc. Lô đất ≠ lứa trồng.
- **Vòng đời lứa trồng:** `GROWING → PRODUCING → TERMINATED` (cuối). Muốn "bỏ" thì kết thúc
  (`/termination`, bắt buộc lý do), không xóa. Thu hoạch đầu tiên tự chuyển sang PRODUCING (BR-09).
- **Nhắc việc không lưu trạng thái (BR-18, ADR-12).** Lời nhắc biến mất khi dòng nhật ký tương
  ứng được ghi. **Không có nút "đã làm".**
- **"Hôm nay" theo giờ Việt Nam** (`farm.time-zone=Asia/Ho_Chi_Minh`). Ngày trong tương lai bị
  từ chối (BR-02, BR-07).
- **Xóa bị chặn (409) khi còn dữ liệu con (BR-10).** Không cascade, không xóa mềm (ADR-6).
- **Dữ liệu của người khác trả 404**, không trả 403 hay danh sách rỗng (BR-11, ADR-14).
- Tiền: `BigDecimal` / `NUMERIC(15,2)`. Chi phí bỏ trống = 0. Sản lượng > 0 kg.

## Backend

Java 21 · **Spring Boot 4.1** · **Jackson 3 (`tools.jackson.*`, không phải `com.fasterxml`)** ·
PostgreSQL 17 · Flyway · springdoc · JUnit 5 + Mockito · Testcontainers · ArchUnit · JaCoCo.

Package gốc `com.hmdao.farm`. Modular monolith gồm các module `shared`, `identity`, `land`,
`catalog`, `cultivation`, `analytics`, `reminder`. Mỗi module theo Ports & Adapters:
`domain` ← `application` (`port/in` = `*UseCase`, `port/out` = `*Repository`/`*QueryPort`,
`dto`, `service`) ← `web` / `infrastructure`. Phụ thuộc chỉ đi vào trong.

### Luật mà ArchUnit sẽ đánh đỏ build (`ArchitectureTest`, 13 luật)

- `domain` không import Spring, Hibernate, `web`, `infrastructure`. Chỉ được dùng `jakarta.persistence`.
- `application` không biết `org.springframework.data`. Phân trang dùng `shared.application.Page` /
  `PageRequest` (ADR-8).
- Không có chu trình giữa các module. Module cấp thấp hỏi ngược lên thì qua port (`PlotUsagePort`, `CropUsagePort`).
- **Không field injection**, chỉ constructor injection.
- **Không gọi `LocalDate.now()` / `LocalDateTime.now()` không tham số.** Dùng `LocalDate.now(clock)`
  với bean `Clock` được inject.
- Tiền là `BigDecimal`. Mọi entity kế thừa `BaseEntity`, không có setter public, trạng thái đổi
  qua method nghiệp vụ.
- `SeasonPolicy`, `CareRule` nằm ở `domain`, không có annotation Spring. Khai báo bean trong
  `config/` của module.

### Lỗi API (RFC 9457 ProblemDetail, `shared/web/GlobalExceptionHandler`)

| HTTP | `type` (`urn:farm:problem:…`) | Trường thêm |
|---|---|---|
| 400 | `validation` | `errors[{field, message}]` **chỉ** khi lỗi Bean Validation; sai kiểu tham số / JSON hỏng thì chỉ có `detail` |
| 401 | `unauthenticated` | `rule` |
| 404 | `not-found` | `rule` |
| 409 | `conflict` | `rule` |
| 409 | `concurrent-update`, `data-integrity` | **không có `rule`** |
| 422 | `business-rule` | `rule` |

`detail` luôn là tiếng Việt, người dùng đọc được. **`rule` không có ở mọi lỗi** — frontend hiển
thị `detail` làm mặc định, chỉ dùng `rule` / `errors` khi có.

### Định danh (tạm thời, đến Phase 5)

Chưa có đăng nhập. Chủ sở hữu lấy từ header `X-User-Id`; bỏ trống thì mặc định là user 1
(`HeaderCurrentUserProvider`). Seed demo `db/demo/V900__demo_users.sql` tạo hai chủ nông trại
để kiểm BR-11. Phase 5 thay bằng `JwtCurrentUserProvider` mà không sửa service nào.

### Database

- Migration ở `src/main/resources/db/migration` (`V1`…`V4`). **Không bao giờ sửa migration đã
  có**, luôn thêm `V5__…`. `ddl-auto=validate`.
- Dữ liệu demo **chỉ** ở `db/demo`, nạp ở profile `dev` và integration test, không bao giờ ở `prod` (ADR-13).
- Có index cho mọi FK. Có `@Version` trên `Planting`, `Season`. `open-in-view=false`.
- Báo cáo và nhắc việc có số câu truy vấn cố định (3 câu, `architecture.md` §8). Integration
  test đếm số câu này, **không được thêm N+1**.

## Lệnh

Chạy trong `Farm Management System/`. Trên Windows dùng `./mvnw` qua Git Bash, hoặc `mvnw.cmd`.

```bash
./mvnw spring-boot:run   # profile mặc định dev; cần PostgreSQL ở cổng 5433
./mvnw test              # unit test (*Test), không cần Docker, ~30 giây
./mvnw verify            # + integration (*IT, Testcontainers) + ArchUnit + JaCoCo
```

- PostgreSQL cho dev: `docker-compose.yml` và `.env` nằm ở thư mục cha `D:\PMQLVF`, **ngoài repo**.
  Không đọc hay chép giá trị trong `.env` vào bất kỳ file nào.
- Swagger UI: http://localhost:8080/swagger-ui.html (chỉ có ở dev; `prod` tắt springdoc).
- Health check: `/actuator/health`.
- Thiếu Docker thì `*IT` bị **bỏ qua kèm lý do**, không phải lỗi. CI (`.github/workflows/build.yml`)
  luôn chạy đủ.

### Những thứ dễ làm đỏ build

- **Hợp đồng API:** `OpenApiContractIT` sinh lại `docs/openapi.json` (khóa sắp xếp, xuống dòng LF)
  rồi so với bản đã commit. Đổi DTO/endpoint thì file này đổi theo, **phải commit file đó**.
  CI chạy `git diff --exit-code docs/openapi.json`. Đổi tên trường là thay đổi phá vỡ frontend.
- **Truy vết quy tắc:** `BusinessRuleCoverageTest` kiểm cả hai chiều. Mã BR/CARE mới trong
  `architecture.md` phải có test, và test không được dùng mã không có trong tài liệu.
- **Độ phủ:** JaCoCo yêu cầu `domain` + `application.service` đạt ≥ 80% lệnh và ≥ 70% nhánh.
- Integration test mới: dùng `@IntegrationTest` (`src/test/.../support`) để chung một context
  và một container. Ngày tháng trong test dùng `MutableTestClock`.

## Frontend (M6) — phương án 2, nền tối, mở vào Tổng quan

**Nguồn sự thật: `docs/m6-frontend-plan.md`** — mục 0 (điều chỉnh phương án 2), mục 3 (token
màu, font, bố cục), mục 6 (luật đã chốt), mục 7 (thứ tự làm). Code nằm ở `frontend/`.

**Stack:** Vite · React 19 · **JavaScript (JSX)** · Tailwind v4 (`@tailwindcss/vite`, token bằng
`@theme` trong `src/index.css`, không có `tailwind.config.js`) · React Router · TanStack Query v5
· axios · react-hook-form + zod · Vitest + Testing Library + MSW · ESLint + Prettier ·
Node 24 LTS (`.nvmrc`). Mọi thư viện dùng bản ổn định mới nhất; ghim bằng `package-lock.json`.
Tra cấu hình bản mới qua **context7** trước khi viết.

Cấu trúc: `src/components/` (tái sử dụng) · `src/pages/` (một file một màn) · `src/services/`
(**nơi duy nhất gọi HTTP và biết tên trường API**) · `src/hooks/` (bọc TanStack Query).

Luật khi code frontend:

- **Không bịa dữ liệu.** Chỉ hiển thị thứ backend có. Không bản đồ toạ độ, không cảm biến, không
  vật tư, không tìm kiếm/thông báo/đăng xuất cho tới khi có API tương ứng.
- Gọi API qua `services/api.js`: axios `baseURL: '/api/v1'`, proxy Vite → `http://localhost:8080`.
  Không gọi thẳng `localhost:8080` (backend không bật CORS).
- Đổi tên trường ở backend thì **không có gì báo lỗi lúc biên dịch** → sửa `docs/openapi.json`
  xong phải rà `services/` bằng tay.
- Dữ liệu mẫu chỉ cho truy vấn đọc khi không kết nối được backend, luôn kèm dải báo "Đang hiện dữ
  liệu mẫu". **Thao tác ghi không bao giờ giả lập thành công.**
- **Thao tác ghi (POST/PUT/DELETE) không tự thử lại** — backend chưa có idempotency key. Gửi lỗi
  thì giữ nội dung form, hiện "Chưa gửi được" + nút "Gửi lại". Mutation phải có
  `networkMode: 'always'` (đã đặt mặc định trong `main.jsx`): chế độ mặc định của TanStack Query
  *treo* mutation khi offline rồi **tự gửi khi có mạng lại** — đã tái hiện được lỗi này.
- Form ghi dùng chung một bảng `<dialog>` (`hooks/useLogActivity.jsx` + `components/log/`), mở
  bằng `useLogActivity().open({ plantingId, type })`. Lời nhắc truyền `suggestedActivity` của
  backend — không tự suy loại việc từ mã CARE-0x.
- **Ghi hoạt động/thu hoạch theo lứa trồng**, không theo lô: lô trồng xen có nhiều lứa.
- **Không tự tính niên vụ.** Lấy `startDate` / `endDate` / `label` từ API.
- Sổ nhật ký: mỗi niên vụ tải với `size=200` rồi trộn theo ngày; còn trang sau thì hiện "tải thêm".
- Lỗi: gắn `errors[]` vào từng ô; còn lại hiện `detail`.
- Header `X-User-Id` đặt ở một chỗ trong `services/api.js`. Bộ chọn người dùng (user 1, 2) ở chỗ
  avatar, đánh dấu rõ là công cụ dev.
- Tiền `Intl.NumberFormat('vi-VN')`; ô nhập tiền là text `inputmode="numeric"`. Ngày theo
  `Asia/Ho_Chi_Minh`, `max` = hôm nay.
- Màu theo loại cây và nhãn tiếng Việt của enum: mỗi thứ **một** file ánh xạ. Màu không bao giờ là
  kênh thông tin duy nhất (kèm tên cây, dấu `+`/`−`).
- Màu cây là token `crop-*` riêng, chỉ để tô, không làm chữ. Đổi màu nào thì chạy lại
  `validate_palette` (skill dataviz) cho cả bộ, so mọi cặp. `clay` không làm chữ trên `panel2`
  (tương phản < 4.5).
- Chữ thường, không in hoa nhãn. Viền mảnh, không đổ bóng. Vùng chạm ≥ 44px. Focus rõ.
- Kiểm tra giao diện bằng **chrome-devtools** và **a11y-debugging**. Biểu đồ phải nạp skill
  **dataviz** trước khi viết.

## Lộ trình còn lại

| Mốc | Nội dung |
|---|---|
| M6 | Phase 4: frontend. **M6a**: dựng `frontend/` → Tổng quan → `GET /plantings` → form ghi hoạt động → CI. **M6b**: lô đất, thu hoạch, sổ nhật ký, báo cáo |
| M7 | Docker Compose cho cả hệ thống (backend + frontend + PostgreSQL), cấu hình triển khai |
| Phase 5+ | JWT, phân quyền công nhân, giá thị trường, truy xuất nguồn gốc (`design.md` §8) |
