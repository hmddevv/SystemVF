-- Dữ liệu demo, KHÔNG thuộc production (ADR-13).
-- Chỉ chạy khi cấu hình nạp thêm location classpath:db/demo — profile `dev` và integration test.
-- Đánh số 900 để luôn chạy sau mọi migration cấu trúc, dù sau này V5, V6... được thêm vào.

-- User 1 là chủ nông trại mặc định khi request không gửi header X-User-Id.
-- User 2 tồn tại để kiểm chứng việc cô lập dữ liệu giữa hai chủ nông trại (BR-11).
-- ON CONFLICT: chạy lại trên một database đã có sẵn hai tài khoản này (ví dụ database dev
-- đang dùng dở trước khi seed được tách ra) thì không lỗi, và không nhân đôi dữ liệu.
INSERT INTO app_user (name, email) VALUES
    ('Chủ nông trại mẫu', 'owner@farm.local'),
    ('Chủ nông trại thứ hai', 'owner2@farm.local')
ON CONFLICT (email) DO NOTHING;
