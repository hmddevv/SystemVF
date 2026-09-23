import { useId } from 'react';
import { DEV_USERS } from '../../services/api';
import { useSession } from '../../hooks/useSession';
import { useCurrentFarm } from '../../hooks/useFarmData';

/*
 * Thanh trên: tên ứng dụng, nông trại đang xem, bộ chọn người dùng dev.
 * Không có ô tìm kiếm, chuông thông báo, đăng xuất — backend chưa có API tương ứng (plan mục 0).
 */
function FarmPicker() {
  const { setFarmId } = useSession();
  const { farms, farm } = useCurrentFarm();
  const id = useId();
  if (!farm) return null;
  if (farms.length === 1) {
    return (
      <p className="min-w-0 truncate text-sm text-muted">
        <span className="text-ink">{farm.name}</span>
        {farm.location ? `, ${farm.location}` : ''}
      </p>
    );
  }
  return (
    <div className="flex min-w-0 items-center gap-2">
      <label htmlFor={id} className="sr-only">
        Nông trại đang xem
      </label>
      <select
        id={id}
        value={farm.id}
        onChange={(e) => setFarmId(Number(e.target.value))}
        className="min-h-11 max-w-56 truncate rounded-md border border-line bg-panel px-3 text-ink"
      >
        {farms.map((f) => (
          <option key={f.id} value={f.id}>
            {f.name}
          </option>
        ))}
      </select>
    </div>
  );
}

// Thay cho avatar: chọn người dùng gửi trong header X-User-Id. Công cụ dev, Phase 5 thay bằng JWT.
function DevUserSwitcher() {
  const { userId, changeUser } = useSession();
  const id = useId();
  return (
    <div className="flex items-center gap-2">
      <span
        aria-hidden="true"
        className="grid h-9 w-9 place-items-center rounded-full border border-line bg-panel2 font-display text-sm text-ink"
      >
        {userId}
      </span>
      <label htmlFor={id} className="text-xs text-muted">
        Người dùng <span className="rounded bg-panel2 px-1 text-harvest">dev</span>
      </label>
      <select
        id={id}
        value={userId}
        onChange={(e) => changeUser(Number(e.target.value))}
        className="min-h-11 rounded-md border border-line bg-panel px-2 text-sm text-ink"
      >
        {DEV_USERS.map((u) => (
          <option key={u.id} value={u.id}>
            {u.id}: {u.name}
          </option>
        ))}
      </select>
    </div>
  );
}

export function Topbar() {
  return (
    <header className="z-10 md:sticky md:top-0 flex flex-wrap items-center gap-x-6 gap-y-2 border-b border-line bg-bg/95 px-4 py-3 backdrop-blur md:px-6">
      <p className="font-display text-xl font-semibold tracking-tight text-ink">Minh Đảo</p>
      <div className="min-w-0 flex-1">
        <FarmPicker />
      </div>
      <DevUserSwitcher />
    </header>
  );
}
