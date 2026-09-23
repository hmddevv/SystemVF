import { NavLink, Outlet } from 'react-router';
import { Icon } from '../Icon';
import { Topbar } from './Topbar';
import { useMockActive } from '../../hooks/useFarmData';

const NAV = [
  { to: '/', label: 'Tổng quan', icon: 'overview', end: true },
  { to: '/lo-dat', label: 'Lô đất', icon: 'plots' },
  { to: '/cay-trong', label: 'Cây trồng', icon: 'crops' },
  { to: '/nhat-ky', label: 'Nhật ký', icon: 'journal' },
  { to: '/bao-cao', label: 'Báo cáo', icon: 'reports' },
];

// Mục điều hướng: icon kèm chữ (không để icon đứng một mình), mục đang mở có vạch leaf.
function NavItem({ to, label, icon, end }) {
  return (
    <NavLink
      to={to}
      end={end}
      className={({ isActive }) =>
        `relative flex min-h-14 flex-1 flex-col items-center justify-center gap-1 rounded-md px-1 text-xs font-medium md:flex-none md:py-3 ${
          isActive ? 'text-ink' : 'text-muted hover:text-ink'
        }`
      }
    >
      {({ isActive }) => (
        <>
          {isActive && (
            <span
              aria-hidden="true"
              className="absolute top-0 left-1/2 h-0.5 w-8 -translate-x-1/2 rounded-full bg-leaf md:top-1/2 md:left-0 md:h-8 md:w-0.5 md:-translate-x-0 md:-translate-y-1/2"
            />
          )}
          <Icon name={icon} className={isActive ? 'text-leaf' : ''} />
          <span>{label}</span>
        </>
      )}
    </NavLink>
  );
}

function MockBanner() {
  const active = useMockActive();
  if (!active) return null;
  return (
    <div
      role="status"
      className="border-b border-harvest/40 bg-panel px-4 py-2 text-sm text-harvest"
    >
      Không kết nối được máy chủ — đang hiện <strong>dữ liệu mẫu</strong>, không phải dữ liệu của
      bạn. Mọi thao tác ghi sẽ không được lưu.
    </div>
  );
}

export function AppShell() {
  return (
    <div className="min-h-dvh md:grid md:grid-cols-[88px_minmax(0,1fr)]">
      <nav
        aria-label="Điều hướng chính"
        className="fixed inset-x-0 bottom-0 z-20 flex border-t border-line bg-bg px-1 pb-[env(safe-area-inset-bottom)] md:sticky md:top-0 md:h-dvh md:flex-col md:gap-1 md:border-t-0 md:border-r md:px-2 md:pt-20"
      >
        {NAV.map((item) => (
          <NavItem key={item.to} {...item} />
        ))}
      </nav>
      <div className="flex min-w-0 flex-col pb-20 md:pb-0">
        <Topbar />
        <MockBanner />
        <main className="flex-1 px-4 py-5 md:px-6 md:py-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
