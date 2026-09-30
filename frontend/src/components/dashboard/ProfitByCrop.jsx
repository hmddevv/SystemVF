import { useId, useState } from 'react';
import { CropDot, EmptyBlock, ErrorBlock, LoadingBlock, Panel } from '../ui';
import { cropColor } from '../cropTone';
import { formatMoney, formatSignedMoney } from '../../services/format';

/*
 * Lãi/lỗ theo cây — GET /reports/profit-loss?groupBy=CROP. Tính năng lõi của hệ thống.
 * Thanh ngang lệch hai phía quanh trục 0: lãi sang phải, lỗ sang trái. Màu thanh là màu của cây
 * (nhận diện), còn lãi/lỗ đọc bằng hướng thanh và dấu +/− — không dựa vào màu.
 * Báo cáo gom theo niên vụ, không theo năm dương lịch (BR-13).
 */

function Bar({ row, domain, active, onActive }) {
  const [min, max] = domain;
  const span = max - min || 1;
  const zero = (-min / span) * 100;
  const width = (Math.abs(row.netProfit) / span) * 100;
  const left = row.netProfit >= 0 ? zero : zero - width;
  const radius = row.netProfit >= 0 ? '0 4px 4px 0' : '4px 0 0 4px';
  return (
    <li
      tabIndex={0}
      onMouseEnter={onActive}
      onFocus={onActive}
      aria-label={
        row.netProfit === 0
          ? `${row.label}: chưa lãi chưa lỗ`
          : `${row.label}: ${row.netProfit > 0 ? 'lãi' : 'lỗ'} ${formatMoney(Math.abs(row.netProfit))}`
      }
      className={`grid grid-cols-[minmax(0,8rem)_minmax(0,1fr)_auto] items-center gap-3 rounded-md px-2 py-2 sm:grid-cols-[minmax(0,11rem)_minmax(0,1fr)_auto] ${
        active ? 'bg-panel2' : ''
      }`}
    >
      <span className="flex min-w-0 items-center gap-1.5 text-sm text-ink">
        <CropDot cropName={row.label} />
        <span className="truncate">{row.label}</span>
      </span>
      <span className="relative block h-7">
        <span
          aria-hidden="true"
          className="absolute inset-y-0 w-px bg-muted"
          style={{ left: `${zero}%` }}
        />
        <span
          aria-hidden="true"
          className="absolute top-1 h-5"
          style={{
            left: `${left}%`,
            width: `${Math.max(width, 0.5)}%`,
            background: cropColor(row.label),
            borderRadius: radius,
          }}
        />
      </span>
      <span className="num min-w-[8.5rem] text-right text-sm text-ink">
        {formatSignedMoney(row.netProfit)}
      </span>
    </li>
  );
}

function Detail({ row, year }) {
  if (!row) return null;
  return (
    <div className="border-t border-line px-4 py-3">
      <p className="mb-2 flex items-center gap-1.5 text-sm text-ink">
        <CropDot cropName={row.label} size={8} />
        Chi tiết {row.label}
      </p>
      <dl className="num grid grid-cols-2 gap-x-4 gap-y-1 text-sm sm:grid-cols-4">
        <div>
          <dt className="text-muted">Doanh thu</dt>
          <dd className="text-harvest">{formatMoney(row.totalRevenue)}</dd>
        </div>
        <div>
          <dt className="text-muted">Chi phí</dt>
          <dd className="text-ink">{formatMoney(row.totalCost)}</dd>
        </div>
        <div>
          <dt className="text-muted">Lãi trên 1.000 m²</dt>
          <dd className="text-ink">
            {row.profitPer1000m2 == null
              ? 'không đủ dữ liệu'
              : formatSignedMoney(row.profitPer1000m2)}
            {row.sharedPlot && <span className="text-muted">*</span>}
          </dd>
        </div>
        <div>
          <dt className="text-muted">{year == null ? 'Số niên vụ' : 'Luỹ kế mọi niên vụ'}</dt>
          <dd className="text-ink">
            {year == null ? row.seasonCount : formatSignedMoney(row.lifetimeNetProfit)}
          </dd>
        </div>
      </dl>
    </div>
  );
}

export function ProfitByCrop({ query, year, seasonOptions, onYearChange }) {
  const [activeId, setActiveId] = useState(null);
  const selectId = useId();
  const rows = query.data?.rows ?? [];
  const values = rows.map((r) => r.netProfit);
  const domain = [Math.min(0, ...values), Math.max(0, ...values)];
  // Chưa rê chuột: chi tiết của cây biến động nhiều nhất, không phải dòng đầu (thường là 0 đ)
  const biggest = rows.reduce(
    (best, r) => (best == null || Math.abs(r.netProfit) > Math.abs(best.netProfit) ? r : best),
    null,
  );
  const active = rows.find((r) => r.id === activeId) ?? biggest;
  const anyShared = rows.some((r) => r.sharedPlot);

  return (
    <Panel
      title="Lãi/lỗ theo cây"
      aside={
        <div className="flex items-center gap-2">
          <label htmlFor={selectId} className="text-sm text-muted">
            Niên vụ
          </label>
          <select
            id={selectId}
            value={year ?? ''}
            onChange={(e) => onYearChange(e.target.value ? Number(e.target.value) : null)}
            className="min-h-11 rounded-md border border-line bg-panel2 px-2 text-sm text-ink"
          >
            <option value="">Mọi niên vụ</option>
            {seasonOptions.map((s) => (
              <option key={s.year} value={s.year}>
                {s.label}
              </option>
            ))}
          </select>
        </div>
      }
    >
      {query.isPending ? (
        <LoadingBlock />
      ) : query.error ? (
        <ErrorBlock error={query.error} onRetry={query.refetch} />
      ) : rows.length === 0 ? (
        <EmptyBlock>Chưa có lứa trồng nào để tính lãi/lỗ.</EmptyBlock>
      ) : (
        <>
          <ul className="px-2 pb-2" onMouseLeave={() => setActiveId(null)}>
            {rows.map((r) => (
              <Bar
                key={r.id}
                row={r}
                domain={domain}
                active={active?.id === r.id}
                onActive={() => setActiveId(r.id)}
              />
            ))}
          </ul>
          <Detail row={active} year={year} />
          {anyShared && (
            <p className="px-4 pb-3 text-xs text-muted">
              * Cây trồng xen: chi phí dùng chung cả lô đang ghi vào từng lứa, nên số theo diện tích
              chỉ là ước lượng.
            </p>
          )}
        </>
      )}
    </Panel>
  );
}
