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
| `docs/architecture.md` | Module, Ports & Adapters, **bảng quy tắc BR-01…BR-18, CARE-01…04 (§6)**, API (§7), ADR-1…15 (§9), lộ trình (§10) |
| `docs/openapi.json` | Hợp đồng API — 20 đường dẫn, 37 endpoint. Frontend lấy kiểu dữ liệu từ đây |
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

## Frontend (M6) — kế hoạch đã chốt, chưa code

**Nguồn sự thật: `docs/m6-frontend-plan.md`.** Mục 5–6 đã chốt ngày 23/09/2026; mục 3 (màu,
font, bố cục) chỉ là đề xuất, chốt sau khi chủ dự án chọn mockup. Thứ tự làm và tiêu chí xong ở
mục 7. Chưa có thư mục `frontend/`.

**Stack:** Vite · React 19 · TypeScript strict · Tailwind v4 (`@tailwindcss/vite`, token bằng
`@theme`, không có `tailwind.config.js`) · React Router · TanStack Query v5 · `openapi-typescript`
+ `openapi-fetch` · react-hook-form + zod · Radix UI primitives · Vitest + Testing Library + MSW ·
ESLint + Prettier · Node 24 LTS (`.nvmrc`). Mọi thư viện dùng bản ổn định mới nhất (tag `latest`),
không beta; ghim bằng `package-lock.json`. Tra cấu hình bản mới qua **context7** trước khi viết.

Luật khi code frontend:

- Vị trí `frontend/` trong repo này. Dev: proxy Vite `/api` → `http://localhost:8080`, không CORS.
- **Kiểu dữ liệu sinh từ `docs/openapi.json` đã commit**, không lấy từ `/v3/api-docs` (prod tắt),
  không viết tay interface cho DTO.
- **Không tự tính niên vụ.** Lấy `startDate` / `endDate` / `label` từ API; không viết lại BR-05a
  bằng TypeScript.
- **Thao tác ghi (POST/PUT/DELETE) không tự thử lại** — backend chưa có idempotency key, retry
  sinh dòng trùng. Gửi lỗi thì giữ nội dung form, hiện "Chưa gửi được" + nút "Gửi lại".
- Sổ nhật ký: mỗi niên vụ tải hoạt động và thu hoạch với `size=200` rồi trộn theo ngày ở client;
  còn trang sau thì hiện "tải thêm", không cắt im lặng.
- Lỗi: gắn `errors[]` vào từng ô; còn lại hiện `detail`. Không tự bịa câu lỗi thay cho `detail`.
- Header `X-User-Id` đặt ở một chỗ duy nhất trong API client. Bộ chọn người dùng (ghi cứng
  user 1, 2) đánh dấu rõ là công cụ dev.
- Tiền: hiển thị `Intl.NumberFormat('vi-VN')` (`76.800.000 đ`); ô nhập là text
  `inputmode="numeric"`, không dùng `type="number"`. Ngày theo `Asia/Ho_Chi_Minh`, `max` = hôm nay.
- Loại hoạt động là nút lớn, không dropdown. `OTHER` bắt buộc ghi chú (BR-12).
- Nhãn tiếng Việt của enum ở **một** file ánh xạ: `ActivityType`, `PlantingStatus`, `EndReason`,
  `ReminderSeverity` (`OVERDUE`, `DUE_SOON`), `groupBy` (`CROP`, `PLOT`, `PLANTING`).
- Mọi chữ là tiếng Việt có dấu. Font tự host bằng `@fontsource/*`, subset `vietnamese`.
- Mobile-first, dùng ngoài nắng: tương phản chữ ≥ 4.5:1, vùng chạm ≥ 44px. Việc quan trọng nhất
  là **ghi nhật ký trong 10 giây** — không có hàng KPI ở đầu trang.
- Mọi màn có đủ trạng thái đang tải / lỗi / trống. Màn "Hôm nay" khi chưa có dữ liệu dẫn qua ba
  bước: tạo nông trại → thêm lô → trồng cây.
- Kiểm tra giao diện bằng **chrome-devtools** (chụp màn hình, console) và **a11y-debugging**.
  Màn lãi/lỗ phải nạp skill **dataviz** trước khi viết biểu đồ.
- CI có job `frontend`: file kiểu sinh ra phải khớp `openapi.json` (`git diff --exit-code`),
  typecheck, lint, test, build đều xanh.

## Lộ trình còn lại

| Mốc | Nội dung |
|---|---|
| M6 | Phase 4: frontend. **M6a**: mockup → `GET /plantings` → dựng `frontend/` → Hôm nay + bắt đầu lần đầu → Sổ nhật ký. **M6b**: quản lý + lãi/lỗ |
| M7 | Docker Compose cho cả hệ thống (backend + frontend + PostgreSQL), cấu hình triển khai |
| Phase 5+ | JWT, phân quyền công nhân, giá thị trường, truy xuất nguồn gốc (`design.md` §8) |
