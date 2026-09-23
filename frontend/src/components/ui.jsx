import { cropColor } from './cropTone';

/*
 * Mảnh giao diện dùng lại ở mọi màn: khung panel, chấm màu cây, và ba trạng thái bắt buộc của
 * mọi khối dữ liệu — đang tải / lỗi / trống (plan 6.7).
 */

export function Panel({ title, aside, children, className = '', as: Tag = 'section', ...rest }) {
  return (
    <Tag className={`rounded-lg border border-line bg-panel ${className}`} {...rest}>
      {(title || aside) && (
        <header className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-2 px-4 pt-4 pb-3">
          {title && <h2 className="font-display text-lg font-semibold text-ink">{title}</h2>}
          {aside}
        </header>
      )}
      {children}
    </Tag>
  );
}

// Chấm màu cây luôn đi kèm tên cây ở cạnh — màu không bao giờ là kênh nhận diện duy nhất.
export function CropDot({ cropName, size = 10 }) {
  return (
    <span
      aria-hidden="true"
      className="inline-block shrink-0 rounded-full"
      style={{ width: size, height: size, background: cropColor(cropName) }}
    />
  );
}

export function LoadingBlock({ label = 'Đang tải…', className = '' }) {
  return (
    <div role="status" className={`px-4 py-6 text-muted ${className}`}>
      <span className="inline-block h-2 w-2 animate-pulse rounded-full bg-muted align-middle" />{' '}
      {label}
    </div>
  );
}

// Lỗi hiển thị nguyên `detail` của API (tiếng Việt) — không tự bịa câu khác.
export function ErrorBlock({ error, onRetry, className = '' }) {
  return (
    <div role="alert" className={`px-4 py-5 ${className}`}>
      <p className="text-clay">{error?.detail ?? error?.message ?? 'Có lỗi xảy ra.'}</p>
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="mt-3 min-h-11 rounded-md border border-line px-4 text-ink hover:border-muted"
        >
          Tải lại
        </button>
      )}
    </div>
  );
}

export function EmptyBlock({ children, className = '' }) {
  return <div className={`px-4 py-6 text-muted ${className}`}>{children}</div>;
}
