import { useState } from 'react';
import { CropDot, EmptyBlock, ErrorBlock, LoadingBlock, Panel } from '../ui';
import { formatMoney, formatMoneyShort } from '../../services/format';

/*
 * Giám sát mùa vụ: (1) chi phí theo tháng 12 tháng gần nhất — một chuỗi, nên không cần chú giải,
 * tiêu đề gọi tên nó; (2) tiến độ niên vụ hiện tại của từng lứa đang canh tác.
 * Biểu đồ theo skill dataviz: thanh mảnh, đầu bo 4px neo vào đường gốc, lưới mờ, tooltip khi rê
 * chuột/focus, có bảng xem thay thế.
 */

const VB_W = 600;
const VB_H = 200;
// Lề trái đủ cho nhãn trục dài nhất ("500 nghìn"); 52 từng cắt mất chữ số đầu.
const M = { top: 12, right: 8, bottom: 30, left: 70 };

function niceMax(v) {
  if (v <= 0) return 1_000_000;
  const exp = 10 ** Math.floor(Math.log10(v));
  const f = v / exp;
  const step = f <= 1 ? 1 : f <= 2 ? 2 : f <= 5 ? 5 : 10;
  return step * exp;
}

// Thanh có đầu trên bo 4px, đáy vuông neo vào đường gốc.
function barPath(x, y, w, h) {
  const r = Math.min(4, w / 2, h);
  return `M${x},${y + h}V${y + r}Q${x},${y} ${x + r},${y}H${x + w - r}Q${x + w},${y} ${x + w},${y + r}V${y + h}Z`;
}

function monthLabel(ym) {
  const [y, m] = ym.split('-');
  return { short: `T${Number(m)}`, year: y, full: `tháng ${Number(m)}/${y}` };
}

function CostChart({ months }) {
  const [active, setActive] = useState(null);
  const max = niceMax(Math.max(...months.map((m) => m.cost)));
  const iw = VB_W - M.left - M.right;
  const ih = VB_H - M.top - M.bottom;
  const band = iw / months.length;
  const bw = Math.min(22, band * 0.56);
  const ticks = [0, max / 2, max];
  const y = (v) => M.top + ih - (v / max) * ih;
  const act = active != null ? months[active] : null;

  return (
    <div className="relative">
      <svg
        viewBox={`0 0 ${VB_W} ${VB_H}`}
        className="block h-auto w-full"
        role="group"
        aria-label="Biểu đồ cột chi phí theo tháng, 12 tháng gần nhất"
        onMouseLeave={() => setActive(null)}
      >
        {ticks.map((t) => (
          <g key={t}>
            <line
              x1={M.left}
              x2={VB_W - M.right}
              y1={y(t)}
              y2={y(t)}
              stroke="var(--color-line)"
              strokeWidth="1"
            />
            <text
              x={M.left - 8}
              y={y(t) + 4}
              textAnchor="end"
              fontSize="11"
              fill="var(--color-muted)"
            >
              {formatMoneyShort(t)}
            </text>
          </g>
        ))}
        {months.map((m, i) => {
          const cx = M.left + band * i + band / 2;
          const h = Math.max(m.cost > 0 ? 2 : 0, (m.cost / max) * ih);
          const lb = monthLabel(m.month);
          const showYear = i === 0 || lb.short === 'T1';
          return (
            <g key={m.month}>
              {h > 0 && (
                <path
                  d={barPath(cx - bw / 2, M.top + ih - h, bw, h)}
                  fill={active === i ? 'var(--color-ink)' : 'var(--color-muted)'}
                />
              )}
              <text
                x={cx}
                y={VB_H - 14}
                textAnchor="middle"
                fontSize="11"
                fill="var(--color-muted)"
              >
                {lb.short}
              </text>
              {showYear && (
                <text
                  x={cx}
                  y={VB_H - 2}
                  textAnchor="middle"
                  fontSize="10"
                  fill="var(--color-muted)"
                >
                  {lb.year}
                </text>
              )}
              {/* Vùng bắt chuột/focus rộng hơn thanh — dễ trúng trên điện thoại */}
              <rect
                x={M.left + band * i}
                y={M.top}
                width={band}
                height={ih}
                fill="transparent"
                tabIndex={0}
                role="img"
                aria-label={`${lb.full}: ${formatMoney(m.cost)}`}
                onMouseEnter={() => setActive(i)}
                onFocus={() => setActive(i)}
                onBlur={() => setActive(null)}
                style={{ outline: 'none' }}
              />
            </g>
          );
        })}
        <line
          x1={M.left}
          x2={VB_W - M.right}
          y1={M.top + ih}
          y2={M.top + ih}
          stroke="var(--color-muted)"
          strokeWidth="1"
        />
      </svg>
      {act && (
        <div
          role="status"
          className="pointer-events-none absolute top-1 rounded-md border border-line bg-bg px-3 py-2 text-sm shadow-none"
          style={{
            left: `${((M.left + band * active + band / 2) / VB_W) * 100}%`,
            transform: `translateX(${active > 8 ? '-100%' : active < 3 ? '0' : '-50%'})`,
          }}
        >
          <span className="block text-xs text-muted">Chi phí {monthLabel(act.month).full}</span>
          <span className="num font-display font-semibold text-ink">{formatMoney(act.cost)}</span>
        </div>
      )}
    </div>
  );
}

function ProgressRow({ item }) {
  const { planting, season, ratio } = item;
  return (
    <li className="border-t border-line px-4 py-3 first:border-t-0">
      <div className="flex flex-wrap items-baseline justify-between gap-x-3">
        <p className="flex min-w-0 items-center gap-1.5 text-ink">
          <CropDot cropName={planting.cropName} />
          <span className="truncate">
            {planting.cropName}, {planting.plotName}
          </span>
        </p>
        <p className="num text-sm text-muted">
          {season ? `niên vụ ${season.label}` : 'chưa ghi chép niên vụ này'}
        </p>
      </div>
      {season && (
        <>
          {ratio != null && (
            <div
              className="mt-2 h-1.5 overflow-hidden rounded-full bg-panel2"
              role="progressbar"
              aria-valuemin={0}
              aria-valuemax={100}
              aria-valuenow={Math.round(ratio * 100)}
              aria-label={`Tiến độ niên vụ ${season.label} của ${planting.cropName}`}
            >
              <div
                className="h-full rounded-full"
                style={{ width: `${ratio * 100}%`, background: 'var(--color-muted)' }}
              />
            </div>
          )}
          <p className="num mt-1.5 text-sm text-muted">
            {ratio != null ? `đã qua ${Math.round(ratio * 100)}%, ` : 'đang diễn ra, '}
            đã chi <span className="text-ink">{formatMoney(season.totalCost)}</span>
            {season.totalRevenue > 0 && (
              <>
                , đã thu <span className="text-harvest">{formatMoney(season.totalRevenue)}</span>
              </>
            )}
          </p>
        </>
      )}
    </li>
  );
}

export function SeasonMonitor({ monitor, hasPlantings }) {
  const total = monitor.months.reduce((s, m) => s + m.cost, 0);
  return (
    <Panel
      title="Giám sát mùa vụ"
      aside={
        hasPlantings &&
        !monitor.isPending &&
        total > 0 && (
          <p className="num text-sm text-muted">
            12 tháng qua đã chi <span className="text-ink">{formatMoney(total)}</span>
          </p>
        )
      }
    >
      {!hasPlantings ? (
        <EmptyBlock>Chưa có lứa trồng nào đang canh tác.</EmptyBlock>
      ) : monitor.isPending ? (
        <LoadingBlock />
      ) : monitor.error ? (
        <ErrorBlock error={monitor.error} />
      ) : (
        <>
          <div className="px-4">
            <h3 className="text-sm text-muted">Chi phí theo tháng</h3>
            {total === 0 ? (
              // Không vẽ đường phẳng ở 0: nói thẳng là chưa có gì, và cách để có
              <p className="py-6 text-muted">
                12 tháng qua chưa ghi khoản chi nào. Ghi hoạt động kèm chi phí thì biểu đồ sẽ hiện ở
                đây.
              </p>
            ) : (
              <>
                <CostChart months={monitor.months} />
                {monitor.truncated && (
                  <p className="text-xs text-harvest">
                    Có niên vụ trên 200 hoạt động — biểu đồ chưa tính hết.
                  </p>
                )}
                <details className="mt-1 text-sm">
                  <summary className="min-h-11 cursor-pointer py-2 text-muted hover:text-ink">
                    Xem dạng bảng
                  </summary>
                  <table className="num mb-3 w-full text-left">
                    <thead className="text-muted">
                      <tr>
                        <th className="py-1 font-normal">Tháng</th>
                        <th className="py-1 text-right font-normal">Chi phí</th>
                      </tr>
                    </thead>
                    <tbody>
                      {monitor.months.map((m) => (
                        <tr key={m.month} className="border-t border-line">
                          <td className="py-1">{monthLabel(m.month).full}</td>
                          <td className="py-1 text-right">{formatMoney(m.cost)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </details>
              </>
            )}
          </div>
          <h3 className="border-t border-line px-4 pt-3 text-sm text-muted">
            Tiến độ niên vụ hiện tại
          </h3>
          <ul>
            {monitor.progress.map((item) => (
              <ProgressRow key={item.planting.id} item={item} />
            ))}
          </ul>
        </>
      )}
    </Panel>
  );
}
