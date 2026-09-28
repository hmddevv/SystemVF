// Logic dùng chung của form ghi; phần hiển thị ở formParts.jsx.

/**
 * Gắn lỗi validation của backend vào ô cùng tên. Trường backend báo mà form không có ô tương ứng
 * thì bỏ qua ở đây — SubmitError vẫn nhắc người dùng kiểm tra lại.
 */
export function applyServerErrors(error, setError, fieldNames) {
  error?.errors?.forEach(({ field, message }) => {
    if (fieldNames.includes(field)) setError(field, { type: 'server', message });
  });
}

/**
 * Nhãn nút gửi. Chỉ đổi thành "Gửi lại" khi không kết nối được: lỗi 4xx gửi lại y nguyên vẫn lỗi,
 * gọi là "Gửi lại" sẽ làm người dùng bấm mãi.
 */
export function submitLabel(mutation, idle) {
  if (mutation.isPending) return 'Đang gửi…';
  if (mutation.error?.unreachable) return 'Gửi lại';
  return idle;
}
