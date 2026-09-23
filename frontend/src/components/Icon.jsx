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
