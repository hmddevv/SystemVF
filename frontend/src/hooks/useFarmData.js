import { useSyncExternalStore } from 'react';
import { useQueries, useQuery } from '@tanstack/react-query';
import { api, mockStatus } from '../services/api';
import { daysBetween, todayIso } from '../services/format';
import { useSession } from './useSession';

/*
 * Hook đọc dữ liệu cho các màn hình. Mọi queryKey đều có userId: đổi người dùng là đổi cache.
 * Chỉ có truy vấn đọc ở đây — thao tác ghi nằm trong hook mutation riêng, không tự thử lại.
 */

export function useMockActive() {
  return useSyncExternalStore(mockStatus.subscribe, mockStatus.get);
}

export function useFarms() {
  const { userId } = useSession();
  return useQuery({ queryKey: ['farms', userId], queryFn: api.farms.list });
}

// Nông trại đang xem: lựa chọn của người dùng, mặc định nông trại đầu tiên.
export function useCurrentFarm() {
  const { farmId } = useSession();
  const farms = useFarms();
  const list = farms.data ?? [];
  const farm = list.find((f) => f.id === farmId) ?? list[0] ?? null;
  return { ...farms, farms: list, farm };
}

/*
 * Lô đất kèm các lứa đang canh tác của từng lô.
 * Hiện phải gọi mỗi lô một lần (N+1 qua mạng) — sẽ thay bằng GET /plantings?farmId= (plan 6.1).
 */
export function usePlotsWithPlantings(farmId) {
  const { userId } = useSession();
  const plots = useQuery({
    queryKey: ['plots', userId, farmId],
    queryFn: () => api.plots.byFarm(farmId),
    enabled: farmId != null,
  });
  const plantingQueries = useQueries({
    queries: (plots.data ?? []).map((plot) => ({
      queryKey: ['plantings', userId, plot.id, 'active'],
      queryFn: () => api.plantings.byPlot(plot.id, true),
    })),
  });
  const loadingPlantings = plantingQueries.some((q) => q.isPending);
  const error = plots.error ?? plantingQueries.find((q) => q.error)?.error ?? null;
  const data = (plots.data ?? []).map((plot, i) => ({
    ...plot,
    plantings: plantingQueries[i]?.data ?? [],
  }));
  return {
    data,
    isPending: plots.isPending || loadingPlantings,
    error,
    refetch: () => {
      plots.refetch();
      plantingQueries.forEach((q) => q.refetch());
    },
  };
}

export function useReminders(farmId) {
  const { userId } = useSession();
  return useQuery({
    queryKey: ['reminders', userId, farmId],
    queryFn: () => api.reminders.list(farmId),
    enabled: farmId != null,
  });
}

export function useProfitByCrop(farmId, year) {
  const { userId } = useSession();
  return useQuery({
    queryKey: ['profit-loss', userId, farmId, 'CROP', year ?? 'all'],
    queryFn: () => api.reports.profitLoss({ groupBy: 'CROP', farmId, year: year ?? undefined }),
    enabled: farmId != null,
  });
}

// 12 tháng dương lịch gần nhất, kết thúc ở tháng hiện tại: ['2025-10', …, '2026-09'].
export function lastTwelveMonths(today = todayIso()) {
  const [y, m] = today.split('-').map(Number);
  return Array.from({ length: 12 }, (_, i) => {
    const d = new Date(Date.UTC(y, m - 1 - (11 - i), 1));
    return `${d.getUTCFullYear()}-${String(d.getUTCMonth() + 1).padStart(2, '0')}`;
  });
}

/*
 * Giám sát mùa vụ: chi phí theo tháng (12 tháng gần nhất) và tiến độ niên vụ hiện tại của từng lứa.
 * Niên vụ lấy nguyên từ API (startDate/endDate) — frontend không tự tính niên vụ (plan 6.5).
 * Niên vụ chỉ tồn tại khi đã có ghi chép; lứa chưa ghi gì trong niên vụ hiện tại thì không có tiến độ.
 */
export function useSeasonMonitor(plantings) {
  const { userId } = useSession();
  const today = todayIso();
  const months = lastTwelveMonths(today);
  const windowStart = `${months[0]}-01`;

  const seasonQueries = useQueries({
    queries: plantings.map((p) => ({
      queryKey: ['seasons', userId, p.id],
      queryFn: () => api.seasons.byPlanting(p.id),
    })),
  });
  const seasonsByPlanting = plantings.map((p, i) => ({
    planting: p,
    seasons: seasonQueries[i]?.data ?? [],
  }));

  // Chỉ tải hoạt động của niên vụ chồng lên cửa sổ 12 tháng.
  const relevant = seasonsByPlanting.flatMap(({ seasons }) =>
    seasons.filter((s) => s.endDate == null || s.endDate >= windowStart),
  );
  const activityQueries = useQueries({
    queries: relevant.map((s) => ({
      queryKey: ['activities', userId, s.id, 0, 200],
      queryFn: () => api.activities.bySeason(s.id),
    })),
  });

  const byMonth = Object.fromEntries(months.map((m) => [m, 0]));
  let truncated = false;
  activityQueries.forEach((q) => {
    if (!q.data) return;
    if (q.data.totalPages > 1) truncated = true;
    q.data.content.forEach((a) => {
      const key = a.activityDate.slice(0, 7);
      if (key in byMonth) byMonth[key] += Number(a.cost ?? 0);
    });
  });

  const progress = seasonsByPlanting.map(({ planting, seasons }) => {
    const current = seasons.find(
      (s) => s.startDate <= today && (s.endDate == null || s.endDate >= today),
    );
    if (!current) return { planting, season: null, ratio: null };
    const ratio =
      current.endDate == null
        ? null
        : Math.min(
            1,
            Math.max(
              0,
              daysBetween(current.startDate, today) /
                daysBetween(current.startDate, current.endDate),
            ),
          );
    return { planting, season: current, ratio };
  });

  // Mọi niên vụ đã có ghi chép — dùng cho bộ lọc báo cáo.
  const years = [
    ...new Set(seasonsByPlanting.flatMap(({ seasons }) => seasons.map((s) => s.year))),
  ].sort((a, b) => b - a);

  return {
    months: months.map((m) => ({ month: m, cost: byMonth[m] })),
    years,
    progress,
    truncated,
    isPending: seasonQueries.some((q) => q.isPending) || activityQueries.some((q) => q.isPending),
    error: [...seasonQueries, ...activityQueries].find((q) => q.error)?.error ?? null,
  };
}
