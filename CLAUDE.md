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
| `docs/m6-frontend-plan.md` | Bản nháp thiết kế frontend (**chưa duyệt**) |

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

`detail` luôn là tiếng Việt, người dùng đọc được. **Bản nháp M6 viết "API trả `rule` cho mọi lỗi"
là sai.** Frontend phải hiển thị `detail` làm mặc định, chỉ dùng `rule` / `errors` khi có.

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

## Frontend (M6) — đang ở giai đoạn kế hoạch

**Trạng thái:** chỉ có `docs/m6-frontend-plan.md` (bản nháp). Chưa có thư mục `frontend/`.
**Chưa code gì cho đến khi chủ dự án chốt nốt các quyết định ở §5 của bản kế hoạch**:

1. Chia M6a / M6b — *chưa chốt*.
2. **Tailwind v4 — đã chốt (23/09/2026).** Dùng bản ổn định mới nhất (4.3.x lúc chốt), plugin
   `@tailwindcss/vite`, token khai báo bằng `@theme` trong CSS, không có `tailwind.config.js`.
   Hệ quả chấp nhận: cần Safari 16.4+ / Chrome 111+ / Firefox 128+.
3. Sinh client từ `docs/openapi.json` (`openapi-typescript`) — *chưa chốt*.
4. TanStack Query — *chưa chốt*.
5. Cách vẽ biểu đồ lãi/lỗ — *chưa chốt*.

Nguyên tắc chung về phiên bản: dùng bản ổn định mới nhất (tag `latest` trên npm), không dùng
bản beta / canary. Ghim phiên bản bằng `package-lock.json`.

Nguyên tắc đã rõ, áp dụng khi bắt đầu code:

- Vị trí: `frontend/` trong repo này, để M7 đóng gói chung bằng Docker Compose.
- **Kiểu dữ liệu sinh từ file `docs/openapi.json` đã commit**, không lấy từ `/v3/api-docs`
  (prod tắt). Không viết tay interface cho DTO.
- Dev: proxy Vite `/api` → `http://localhost:8080`. Không bật CORS ở backend.
- Header `X-User-Id` đặt ở một chỗ duy nhất trong API client. Bộ chọn người dùng đánh dấu rõ là
  công cụ dev.
- Tiền định dạng `Intl.NumberFormat('vi-VN')` (vd. `76.800.000 đ`). Ngày tính theo
  `Asia/Ho_Chi_Minh`, khớp `Clock` của backend.
- Mọi chữ trên giao diện là tiếng Việt có dấu. Font phải có subset `vietnamese`.
- Người dùng dùng điện thoại ngoài nắng: tương phản chữ ≥ 4.5:1, vùng chạm ≥ 44px, mobile-first.
- Việc quan trọng nhất là **ghi nhật ký trong 10 giây**, không phải dashboard. Không đặt hàng
  KPI ở đầu trang.
- Enum hiển thị bằng nhãn tiếng Việt ở một file ánh xạ duy nhất: `ActivityType`, `PlantingStatus`,
  `EndReason`, `ReminderSeverity` (`OVERDUE`, `DUE_SOON`), `groupBy` (`CROP`, `PLOT`, `PLANTING`).
- Trước khi cấu hình Tailwind / Vite / TanStack Query thì tra tài liệu bản mới qua **context7**.
  Kiểm tra giao diện bằng **chrome-devtools** (chụp màn hình, console) và **a11y-debugging**.
  Màn lãi/lỗ phải nạp skill **dataviz** trước khi viết biểu đồ.

## Lộ trình còn lại

| Mốc | Nội dung |
|---|---|
| M6 | Phase 4: frontend React + Tailwind (đang lập kế hoạch) |
| M7 | Docker Compose cho cả hệ thống (backend + frontend + PostgreSQL), cấu hình triển khai |
| Phase 5+ | JWT, phân quyền công nhân, giá thị trường, truy xuất nguồn gốc (`design.md` §8) |
