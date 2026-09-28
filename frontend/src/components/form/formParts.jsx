import { groupDigits } from '../../services/format';

/*
 * Mảnh dùng chung cho mọi form ghi (plan 6.2). Một chỗ quyết định cách báo lỗi, để form ghi hoạt
 * động và các form tạo mới nói cùng một giọng:
 * - Lỗi validation 400 có `errors[{field, message}]` → gắn vào đúng ô theo tên trường của API.
 * - Lỗi khác → hiện nguyên `detail` tiếng Việt của backend.
 * - Không kết nối được → "Chưa gửi được", nội dung còn nguyên, người dùng tự bấm "Gửi lại".
 */

export const inputClass =
  'min-h-12 w-full rounded-md border border-line bg-panel px-3 text-ink placeholder:text-muted/60 aria-invalid:border-clay';

export function FieldError({ id, message }) {
  if (!message) return null;
  return (
    <p id={id} className="mt-2 text-sm text-clay">
      {message}
    </p>
  );
}

/*
 * Lỗi gửi. `retryHint` nói người dùng cần xem lại gì trước khi gửi lại: request có thể đã tới
 * máy chủ trước khi mất sóng, mà backend chưa có khóa chống ghi trùng.
 */
export function SubmitError({ error, retryHint }) {
  if (!error) return null;
  if (error.unreachable) {
    return (
      <div role="alert" className="mb-3 rounded-md border border-clay/60 px-3 py-2 text-sm">
        <p className="font-semibold text-clay">Chưa gửi được</p>
        <p className="mt-0.5 text-muted">
          Không kết nối được máy chủ. Nội dung bạn nhập vẫn còn nguyên.
          {retryHint ? ` ${retryHint}` : ''}
        </p>
      </div>
    );
  }
  if (error.status === 400 && error.errors?.length) {
    return (
      <p role="alert" className="mb-3 text-sm text-clay">
        Kiểm tra lại các ô được đánh dấu.
      </p>
    );
  }
  return (
    <p role="alert" className="mb-3 rounded-md border border-clay/60 px-3 py-2 text-sm text-ink">
      {error.detail}
    </p>
  );
}

/*
 * Một ô nhập có nhãn hiện rõ, gợi ý và lỗi. `children` nhận { id, describedBy, invalid } để gắn
 * vào input — nhãn luôn trỏ đúng ô, trình đọc màn hình đọc cả gợi ý lẫn lỗi.
 */
export function Field({ id, label, hint, optional, error, children }) {
  const hintId = hint ? `${id}-hint` : null;
  const errorId = `${id}-error`;
  const describedBy = [hintId, error ? errorId : null].filter(Boolean).join(' ') || undefined;
  return (
    <div>
      <label htmlFor={id} className="mb-1.5 flex items-baseline justify-between gap-3">
        <span className="font-medium text-ink">{label}</span>
        {optional && <span className="text-sm text-muted">Không bắt buộc</span>}
      </label>
      {children({ id, describedBy, invalid: error ? true : undefined })}
      {hint && (
        <p id={hintId} className="mt-1.5 text-sm text-muted">
          {hint}
        </p>
      )}
      <FieldError id={errorId} message={error} />
    </div>
  );
}

/*
 * Ô số nguyên cho điện thoại (tiền, diện tích, số cây): bàn phím số, gõ tới đâu ngăn nghìn tới đó
 * kiểu Việt ("1.250.000"). Không dùng type="number" — không hiện dấu chấm nghìn, cuộn chuột đổi
 * giá trị (plan 6.6). Giá trị trong form là chuỗi đã định dạng; lúc gửi dùng digitsOnly().
 */
export function DigitsInput({ value, onValueChange, suffix, className = '', ...rest }) {
  return (
    <div className="relative">
      <input
        type="text"
        inputMode="numeric"
        autoComplete="off"
        value={value}
        onChange={(e) => onValueChange(groupDigits(e.target.value))}
        className={`num ${inputClass} ${suffix ? 'pr-12' : ''} ${className}`}
        {...rest}
      />
      {suffix && (
        <span
          aria-hidden="true"
          className="pointer-events-none absolute top-1/2 right-3 -translate-y-1/2 text-muted"
        >
          {suffix}
        </span>
      )}
    </div>
  );
}
