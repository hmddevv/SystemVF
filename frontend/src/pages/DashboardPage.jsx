import { useState } from 'react';
import { PlotAreaMap } from '../components/dashboard/PlotAreaMap';
import { ReminderPanel } from '../components/dashboard/ReminderPanel';
import { SeasonMonitor } from '../components/dashboard/SeasonMonitor';
import { ProfitByCrop } from '../components/dashboard/ProfitByCrop';
import { ErrorBlock, LoadingBlock, Panel } from '../components/ui';
import {
  useCurrentFarm,
  usePlotsWithPlantings,
  useProfitByCrop,
  useReminders,
  useSeasonMonitor,
} from '../hooks/useFarmData';
import { formatLongToday } from '../services/format';

/*
 * Tổng quan: hàng trên là sơ đồ lô (2/3) + cảnh báo và việc sắp đến hạn (1/3);
 * hàng dưới là giám sát mùa vụ + lãi/lỗ theo cây (plan mục 3).
 */

// Người dùng mới chưa có nông trại: dẫn qua ba bước theo đúng thứ tự phải làm (plan 6.3).
function FirstRun() {
  return (
    <Panel title="Bắt đầu" className="max-w-xl">
      <p className="px-4 text-muted">
        Lời nhắc và báo cáo chỉ có khi đã có lô đất và cây trồng. Làm lần lượt ba bước:
      </p>
      <ol className="list-decimal space-y-1 py-3 pr-4 pl-9 text-ink">
        <li>Tạo nông trại</li>
        <li>Thêm lô đất vào nông trại</li>
        <li>Trồng cây trên lô: chọn loại cây, ngày trồng, số cây</li>
      </ol>
      <p className="px-4 pb-4 text-sm text-muted">
        Các form tạo mới sẽ có ở bước tiếp theo của M6a.
      </p>
    </Panel>
  );
}

export function DashboardPage() {
  const { farm, isPending: farmsPending, error: farmsError, refetch } = useCurrentFarm();
  const farmId = farm?.id ?? null;
  const [year, setYear] = useState(null);

  const plots = usePlotsWithPlantings(farmId);
  const reminders = useReminders(farmId);
  const profit = useProfitByCrop(farmId, year);
  const plantings = plots.data.flatMap((p) => p.plantings);
  const monitor = useSeasonMonitor(plantings);

  if (farmsPending) return <LoadingBlock />;
  if (farmsError) return <ErrorBlock error={farmsError} onRetry={refetch} />;
  if (!farm) return <FirstRun />;

  return (
    <div className="mx-auto max-w-[1400px]">
      <div className="mb-5 flex flex-wrap items-baseline justify-between gap-2">
        <h1 className="font-display text-2xl font-semibold text-ink md:text-3xl">Tổng quan</h1>
        <p className="text-muted first-letter:uppercase">{formatLongToday()}</p>
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <PlotAreaMap
            plots={plots.data}
            isPending={plots.isPending}
            error={plots.error}
            onRetry={plots.refetch}
          />
        </div>
        <ReminderPanel query={reminders} />
      </div>

      <div className="mt-4 grid gap-4 xl:grid-cols-2">
        <SeasonMonitor monitor={monitor} hasPlantings={plantings.length > 0} />
        <ProfitByCrop query={profit} year={year} years={monitor.years} onYearChange={setYear} />
      </div>
    </div>
  );
}
