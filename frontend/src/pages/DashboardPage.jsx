import { Suspense, lazy, useState } from 'react';
import { PlotAreaMap } from '../components/dashboard/PlotAreaMap';
import { ReminderPanel } from '../components/dashboard/ReminderPanel';
import { SeasonMonitor } from '../components/dashboard/SeasonMonitor';
import { ProfitByCrop } from '../components/dashboard/ProfitByCrop';
import { ErrorBlock, LoadingBlock } from '../components/ui';
import {
  useCurrentFarm,
  usePlotsWithPlantings,
  useProfitByCrop,
  useReminders,
  useSeasonMonitor,
} from '../hooks/useFarmData';
import { formatLongToday } from '../services/format';

// Chỉ người dùng mới cần màn Bắt đầu — nạp lười để form và zod không nằm trong bundle đầu tiên.
const GettingStarted = lazy(() =>
  import('../components/onboarding/GettingStarted').then((m) => ({ default: m.GettingStarted })),
);

/*
 * Tổng quan: hàng trên là sơ đồ lô (2/3) + cảnh báo và việc sắp đến hạn (1/3);
 * hàng dưới là giám sát mùa vụ + lãi/lỗ theo cây (plan mục 3).
 */

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

  /*
   * Bắt đầu lần đầu (plan 6.3): bước suy ra từ dữ liệu, không từ cờ lưu riêng. Lô đang tải thì
   * chờ — hiện Tổng quan rồi mới nhảy sang màn Bắt đầu sẽ giật. Lỗi tải lô thì để Tổng quan hiện
   * lỗi như thường, không đoán là "chưa có lô".
   */
  if (farm && plots.isPending) return <LoadingBlock />;
  const step = !farm
    ? 1
    : plots.error
      ? null
      : plots.data.length === 0
        ? 2
        : plantings.length === 0
          ? 3
          : null;
  if (step) {
    return (
      <Suspense fallback={<LoadingBlock />}>
        <GettingStarted step={step} farm={farm} plots={plots.data} />
      </Suspense>
    );
  }

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
