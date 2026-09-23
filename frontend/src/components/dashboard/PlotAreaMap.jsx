import { Link } from 'react-router';
import { cropColor, cropKind } from '../cropTone';
import { CropDot, EmptyBlock, ErrorBlock, LoadingBlock, Panel } from '../ui';
import { formatArea } from '../../services/format';

/*
 * Sơ đồ lô theo diện tích — thay cho bản đồ vị trí, vì Plot không có toạ độ (plan mục 0).
 * Mỗi lô là một khối có diện tích tỉ lệ với areaM2 thật (treemap "squarified"). Lô trồng xen tô
 * sọc đều các màu cây: sọc bằng nhau vì backend không biết mỗi cây chiếm bao nhiêu phần diện tích.
 */

const W = 100;
const H = 62; // tỉ lệ khung 100:62

// Tỉ lệ cạnh xấu nhất của một hàng khối — squarified treemap (Bruls, Huizing, van Wijk).
function worst(areas, side) {
  const sum = areas.reduce((a, b) => a + b, 0);
  const max = Math.max(...areas);
  const min = Math.min(...areas);
  return Math.max((side * side * max) / (sum * sum), (sum * sum) / (side * side * min));
}

function squarify(items) {
  const total = items.reduce((s, it) => s + it.value, 0) || 1;
  let rest = items
    .map((it) => ({ ...it, area: (it.value / total) * W * H }))
    .sort((a, b) => b.area - a.area);
  let x = 0;
  let y = 0;
  let w = W;
  let h = H;
  const out = [];
  while (rest.length) {
    const side = Math.min(w, h);
    const row = [rest[0]];
    let i = 1;
    while (
      i < rest.length &&
      worst(
        [...row, rest[i]].map((r) => r.area),
        side,
      ) <=
        worst(
          row.map((r) => r.area),
          side,
        )
    ) {
      row.push(rest[i]);
      i += 1;
    }
    rest = rest.slice(row.length);
    const rowArea = row.reduce((s, r) => s + r.area, 0);
    if (w >= h) {
      const rw = rowArea / h;
      let cy = y;
      row.forEach((r) => {
        const rh = r.area / rw;
        out.push({ ...r, x, y: cy, w: rw, h: rh });
        cy += rh;
      });
      x += rw;
      w -= rw;
    } else {
      const rh = rowArea / w;
      let cx = x;
      row.forEach((r) => {
        const rw = r.area / rh;
        out.push({ ...r, x: cx, y, w: rw, h: rh });
        cx += rw;
      });
      y += rh;
      h -= rh;
    }
  }
  return out;
}

function fillFor(plantings) {
  if (plantings.length === 0) return 'var(--color-panel2)';
  if (plantings.length === 1) return cropColor(plantings[0].cropName);
  const stripe = 12;
  const stops = plantings
    .map((p, i) => `${cropColor(p.cropName)} ${i * stripe}px ${(i + 1) * stripe}px`)
    .join(', ');
  return `repeating-linear-gradient(135deg, ${stops})`;
}

function describe(plot) {
  const crops = plot.plantings.map((p) => p.cropName);
  const what = crops.length ? crops.join(' và ') : 'chưa trồng cây nào';
  return `${plot.name}, ${formatArea(plot.areaM2)}, ${what}`;
}

function PlotBlock({ plot }) {
  const small = plot.w < 22 || plot.h < 18;
  const n = plot.plantings.length;
  return (
    <li
      className="absolute p-[2px]"
      style={{
        left: `${(plot.x / W) * 100}%`,
        top: `${(plot.y / H) * 100}%`,
        width: `${(plot.w / W) * 100}%`,
        height: `${(plot.h / H) * 100}%`,
      }}
    >
      <Link
        to={`/lo-dat/${plot.id}`}
        aria-label={describe(plot)}
        className={`group flex h-full w-full items-start overflow-hidden rounded-md ${small ? 'p-1' : 'p-2'} ${
          n === 0 ? 'border border-dashed border-line' : ''
        }`}
        style={{ background: fillFor(plot.plantings) }}
      >
        <span
          className={`max-w-full rounded bg-bg/85 py-1 leading-tight group-hover:bg-bg ${small ? 'px-1' : 'px-2'}`}
        >
          <span className="block truncate font-display font-semibold text-ink">{plot.name}</span>
          {!small && (
            <>
              <span className="num block text-xs text-muted">
                {formatArea(plot.areaM2)}
                {n > 1 ? `, ${n} loại xen` : ''}
              </span>
              <span className="block truncate text-xs text-ink">
                {n ? plot.plantings.map((p) => cropKind(p.cropName)).join(', ') : 'chưa trồng'}
              </span>
            </>
          )}
        </span>
      </Link>
    </li>
  );
}

export function PlotAreaMap({ plots, isPending, error, onRetry }) {
  const blocks = squarify(plots.map((p) => ({ ...p, value: p.areaM2 })));
  const crops = [
    ...new Map(
      plots.flatMap((p) => p.plantings).map((p) => [cropKind(p.cropName), p.cropName]),
    ).entries(),
  ];
  const hasIntercrop = plots.some((p) => p.plantings.length > 1);
  const totalArea = plots.reduce((s, p) => s + p.areaM2, 0);

  return (
    <Panel
      title="Lô đất"
      aside={
        plots.length > 0 && (
          <p className="num text-sm text-muted">
            {plots.length} lô, {formatArea(totalArea)}
          </p>
        )
      }
      className="flex flex-col"
    >
      {isPending ? (
        <LoadingBlock />
      ) : error ? (
        <ErrorBlock error={error} onRetry={onRetry} />
      ) : plots.length === 0 ? (
        <EmptyBlock>
          Nông trại chưa có lô đất nào. Thêm lô đầu tiên ở mục{' '}
          <Link to="/lo-dat" className="text-leaf underline">
            Lô đất
          </Link>
          .
        </EmptyBlock>
      ) : (
        <div className="px-4 pb-4">
          <ul className="relative w-full" style={{ aspectRatio: `${W} / ${H}` }}>
            {blocks.map((b) => (
              <PlotBlock key={b.id} plot={b} />
            ))}
          </ul>
          <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-muted">
            {crops.map(([kind, name]) => (
              <span key={kind} className="inline-flex items-center gap-1.5">
                <CropDot cropName={name} />
                {kind}
              </span>
            ))}
            {hasIntercrop && (
              <span className="inline-flex items-center gap-1.5">
                <span
                  aria-hidden="true"
                  className="inline-block h-2.5 w-4 rounded-sm"
                  style={{
                    background:
                      'repeating-linear-gradient(135deg, var(--color-muted) 0 3px, var(--color-panel2) 3px 6px)',
                  }}
                />
                trồng xen
              </span>
            )}
          </div>
          <p className="mt-2 text-xs text-muted">
            Sơ đồ theo diện tích, không phải bản đồ vị trí: khối càng lớn, lô càng rộng.
          </p>
        </div>
      )}
    </Panel>
  );
}
