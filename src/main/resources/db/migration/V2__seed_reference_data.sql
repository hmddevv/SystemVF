-- Chỉ dữ liệu THAM CHIẾU đi vào đây: thứ mà mọi môi trường, kể cả production, đều cần.
-- Người dùng mẫu nằm ở classpath:db/demo (xem ADR-13) để không có tài khoản giả nào lọt vào
-- cơ sở dữ liệu thật.

-- Danh mục cây trồng phổ biến vùng Tây Nguyên / Đông Nam Bộ.
-- season_start_month là tháng bắt đầu niên vụ theo CHU KỲ SẢN XUẤT (chi phí chăm sóc đi trước
-- doanh thu thu hoạch), không phải niên vụ thương mại. Là giá trị mặc định — chỉnh được qua API.
--   Cà phê:    tưới ra hoa T2–3, bón phân T5–9, thu hoạch T11–T1   -> bắt đầu tháng 2
--   Hồ tiêu:   thu hoạch T2–4, chăm sóc sau thu hoạch từ T5         -> bắt đầu tháng 5
--   Sầu riêng: thu hoạch T7–9 (Đắk Lắk), chăm sóc lại từ T10        -> bắt đầu tháng 10
--   Điều:      thu hoạch T2–5, chăm sóc sau thu hoạch từ T6         -> bắt đầu tháng 6
--   Cao su:    nghỉ cạo mùa rụng lá T2–3, mở miệng cạo lại từ T3–4  -> bắt đầu tháng 3
INSERT INTO crop (name, variety, is_perennial, season_start_month) VALUES
    ('Cà phê',    'Robusta',    TRUE,  2),
    ('Cà phê',    'Arabica',    TRUE,  2),
    ('Hồ tiêu',   'Vĩnh Linh',  TRUE,  5),
    ('Sầu riêng', 'Ri6',        TRUE,  10),
    ('Điều',      'PN1',        TRUE,  6),
    ('Cao su',    'RRIM 600',   TRUE,  3),
    ('Ngô',       'LVN10',      FALSE, NULL);
