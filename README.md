# Hệ thống quản lý canh tác nông nghiệp

Phần mềm ghi chép và phân tích canh tác cho nông hộ vùng Tây Nguyên / Đông Nam Bộ: quản lý
nông trại và lô đất, theo dõi từng lứa trồng qua cả vòng đời, ghi nhật ký chăm sóc và thu
hoạch theo **niên vụ**, rồi trả lời câu hỏi thật sự quan trọng — *cây nào, lô nào đang có lãi*.

Điểm khác biệt so với một CRUD thông thường nằm ở ba chỗ:

- **Niên vụ do hệ thống tự gán.** Nhà nông chỉ khai "cây gì, ngày nào"; chi phí tháng 3 và
  doanh thu tháng 1 năm sau của cùng một chu kỳ cà phê tự nằm chung một vụ. Gom theo năm
  dương lịch sẽ tách chúng ra và báo cáo sai.
- **Trồng xen là chuyện bình thường.** Một lô có thể có nhiều lứa trồng cùng lúc; lô đất và
  lứa trồng là hai khái niệm tách bạch.
- **Nhắc việc không lưu trạng thái.** Lời nhắc biến mất khi dòng nhật ký tương ứng được ghi —
  không có nút "đã làm", vì nhà nông vẫn phải ghi nhật ký.

## Chạy thử

Cần JDK 21+ và Docker (cho PostgreSQL).

```bash
# PostgreSQL 17 ở cổng 5433 (Docker Compose của cả hệ thống sẽ được đóng gói ở M7)
docker run -d --name farm-postgres -p 5433:5432 -e POSTGRES_DB=farm_management -e POSTGRES_PASSWORD=postgres postgres:17-alpine

./mvnw spring-boot:run         # profile mặc định: dev
```

Mở http://localhost:8080/swagger-ui.html. Chưa có đăng nhập: chủ nông trại lấy từ header
`X-User-Id` (bỏ trống = user 1). Đổi sang `2` để thấy dữ liệu được cô lập giữa hai chủ nông
trại (BR-11).

Dữ liệu tham chiếu (danh mục cây trồng kèm tháng bắt đầu niên vụ) do Flyway nạp sẵn. Hai tài
khoản demo chỉ có ở profile `dev` — `db/demo` không bao giờ chạy ở production.

### Giao diện web

Cần Node 24 LTS. Chạy song song với backend ở trên:

```bash
cd frontend && npm ci && npm run dev   # http://localhost:5173
```

Chi tiết ở [`frontend/README.md`](frontend/README.md).

## Kiểm thử

```bash
./mvnw test      # 209 test đơn vị, không cần Docker, ~30 giây
./mvnw verify    # thêm integration test trên PostgreSQL thật + coverage
```

Frontend: `cd frontend && npm test` (Vitest + Testing Library, API giả lập bằng MSW).

Thiếu Docker thì integration test được **bỏ qua kèm lý do**, không báo đỏ oan. CI
(`.github/workflows/build.yml`) luôn có Docker nên ở đó chúng luôn chạy.

| Loại | Chạy ở đâu | Kiểm cái gì |
|---|---|---|
| Domain | `*Test` trong `domain` | Bất biến nghiệp vụ, không cần Spring |
| Service | `*Test` trong `application.service` | Luồng use case với port giả lập |
| Web slice | `*ControllerTest` | Mã HTTP, validation, hình dạng ProblemDetail |
| Integration | `*IT` | HTTP thật → PostgreSQL thật, gồm cả số câu truy vấn |
| Kiến trúc | `ArchitectureTest` | 13 luật phụ thuộc — vi phạm là build đỏ |
| Truy vết | `BusinessRuleCoverageTest` | Mọi quy tắc trong tài liệu đều có test |

## Tài liệu

- [`docs/architecture.md`](docs/architecture.md) — thiết kế, bảng quy tắc nghiệp vụ (BR-01…BR-18,
  CARE-01…CARE-04), các quyết định kiến trúc (ADR) và lộ trình.
- [`docs/design.md`](docs/design.md) — nghiệp vụ gốc và tiêu chí chấp nhận.
- [`docs/openapi.json`](docs/openapi.json) — hợp đồng API được version hoá, sinh tự động và
  đối chiếu trong mỗi lần build.

## Kiến trúc trong một đoạn

Modular monolith, mỗi module nghiệp vụ (`land`, `catalog`, `cultivation`, `analytics`,
`reminder`, `identity`) tổ chức theo Ports & Adapters: `domain` ở lõi không biết framework,
`application` điều phối use case qua các interface port, `web` và `infrastructure` là adapter
ở ngoài cùng. Phụ thuộc chỉ đi vào trong; các luật ArchUnit canh đúng điều đó trong mỗi lần
build. Chi tiết và lý do từng lựa chọn nằm trong `docs/architecture.md`.
