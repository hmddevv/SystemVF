/*
 * Bộ icon nét mảnh, vẽ tay bằng SVG — không kéo thêm thư viện icon cho vài hình.
 * Luôn đi kèm chữ, nên đặt aria-hidden.
 */
const PATHS = {
  overview: (
    <>
      <path d="M4 4h7v7H4zM13 4h7v4h-7zM13 10h7v10h-7zM4 13h7v7H4z" />
    </>
  ),
  plots: (
    <>
      <path d="M3 7l6-3 6 3 6-3v13l-6 3-6-3-6 3z" />
      <path d="M9 4v13M15 7v13" />
    </>
  ),
  crops: (
    <>
      <path d="M12 21V10" />
      <path d="M12 14c-4 0-7-3-7-8 4 0 7 3 7 8zM12 11c0-4 3-7 7-7 0 4-3 7-7 7z" />
    </>
  ),
  journal: (
    <>
      <path d="M6 3h11a1 1 0 0 1 1 1v17H7a1 1 0 0 1-1-1z" />
      <path d="M9 8h6M9 12h6M9 16h4" />
    </>
  ),
  reports: (
    <>
      <path d="M4 20V4M4 20h16" />
      <path d="M8 16v-4M12 16V8M16 16v-6" />
    </>
  ),
  // Sáu loại việc chăm sóc — khớp enum ActivityType của backend.
  WATERING: (
    <path d="M12 3.5c3.4 4.3 6 7.6 6 10.8a6 6 0 0 1-12 0c0-3.2 2.6-6.5 6-10.8zM9.5 15a2.6 2.6 0 0 0 2.5 2.5" />
  ),
  FERTILIZING: (
    <>
      <path d="M8.5 4h7l-1.2 2.6c2 1.3 3.7 4.3 3.7 8.4 0 3.2-1.6 5-6 5s-6-1.8-6-5c0-4.1 1.7-7.1 3.7-8.4z" />
      <path d="M9.7 6.6h4.6" />
      <circle cx="10.5" cy="13" r="0.9" fill="currentColor" stroke="none" />
      <circle cx="13.6" cy="15.2" r="0.9" fill="currentColor" stroke="none" />
      <circle cx="11.2" cy="16.9" r="0.9" fill="currentColor" stroke="none" />
    </>
  ),
  SPRAYING: (
    <>
      <path d="M7 10h6.5v9.5a1.5 1.5 0 0 1-1.5 1.5H8.5A1.5 1.5 0 0 1 7 19.5zM9 10V7h2.5v3M11.5 7.5h3.2l1.3-1.5" />
      <path d="M18.5 5.5h.01M20.5 8h.01M18.5 10h.01M20.5 3.5h.01" strokeWidth="2.2" />
    </>
  ),
  WEEDING: (
    <path d="M3 20.5h18M6.5 20.5c0-4-1.2-7-3.5-9M10.5 20.5c0-5.2 1-9.2 3-12.5M14.5 20.5c0-3.4 2-6.4 5.5-7.8M10.3 13.2C9 11.5 7.5 10.5 5.8 10" />
  ),
  PRUNING: (
    <>
      <circle cx="6" cy="6.5" r="2.6" />
      <circle cx="6" cy="17.5" r="2.6" />
      <path d="M8.2 8l11.3 9.5M8.2 16L19.5 6.5" />
    </>
  ),
  OTHER: (
    <>
      <circle cx="6" cy="12" r="1.3" fill="currentColor" stroke="none" />
      <circle cx="12" cy="12" r="1.3" fill="currentColor" stroke="none" />
      <circle cx="18" cy="12" r="1.3" fill="currentColor" stroke="none" />
    </>
  ),
  plus: <path d="M12 5v14M5 12h14" />,
  close: <path d="M6.5 6.5l11 11M17.5 6.5l-11 11" />,
  check: <path d="M5 12.5l4.5 4.5L19 7.5" />,
};

export function Icon({ name, size = 22, className = '' }) {
  return (
    <svg
      viewBox="0 0 24 24"
      width={size}
      height={size}
      className={className}
      aria-hidden="true"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      {PATHS[name]}
    </svg>
  );
}
