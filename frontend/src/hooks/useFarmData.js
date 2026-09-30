import { useCallback, useEffect, useSyncExternalStore } from 'react';
import { useQueries, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, isMockData, onReconnect } from '../services/api';
import { daysBetween, todayIso } from '../services/format';
import { useSession } from './useSession';

/*
 * Hook đọc dữ liệu cho các màn hình. Mọi queryKey đều có userId: đổi người dùng là đổi cache.
 * Chỉ có truy vấn đọc ở đây — thao tác ghi nằm trong hook mutation riêng, không tự thử lại.
 */

const holdsMock = (query) => isMockData(query.state.data);

// Còn truy vấn nào trong cache đang giữ dữ liệu mẫu không — dải báo dựa vào đây, không vào cờ chung.
export function useMockActive() {
  const cache = useQueryClient().getQueryCache();
  const subscribe = useCallback((fn) => cache.subscribe(fn), [cache]);
  return useSyncExternalStore(subscribe, () => cache.getAll().some(holdsMock));
}

/*
 * Backend chạy lại: tải lại mọi truy vấn còn giữ dữ liệu mẫu, kể cả truy vấn không ở trên màn hình.
 * cancelRefetch: false — truy vấn vừa báo có kết nối lại đang về đích, hủy nó rồi gửi lại là thừa,
 * và nếu có endpoint khác lỗi liên tục thì hai bên cứ thế hủy nhau thành vòng lặp.
 */
export function useRefetchMockOnReconnect() {
  const queryClient = useQueryClient();
  useEffect(
    () =>
      onReconnect(() =>
        queryClient.invalidateQueries(
          { predicate: holdsMock, refetchType: 'all' },
          { cancelRefetch: false },
        ),
      ),
    [queryClient],
  );
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
  return { ...farms, farms: list, farm, isMock: isMockData(farms.data) };
}

// Lứa đang canh tác của một nông trại — một lần gọi GET /plantings (plan 6.1).
export function useActivePlantings(farmId) {
  const { userId } = useSession();
  return useQuery({
    queryKey: ['plantings', userId, farmId, 'active'],
    queryFn: () => api.plantings.owned({ farmId, activeOnly: true }),
    enabled: farmId != null,
  });
}

// Có lứa đang canh tác thì mới có gì để ghi — nút "Ghi" ẩn khi chưa có, màn Bắt đầu dẫn đường.
export function useCanLog() {
  const { farm } = useCurrentFarm();
  const plantings = useActivePlantings(farm?.id ?? null);
  return farm != null && (plantings.data?.length ?? 0) > 0;
}

// Danh mục cây dùng chung cho mọi chủ nông trại (dữ liệu tham chiếu, migration V2).
export function useCrops() {
  const { userId } = useSession();
  return useQuery({ queryKey: ['crops', userId], queryFn: api.crops.list });
}

/*
 * Lô đất kèm các lứa đang canh tác của từng lô: hai lần gọi cố định, ghép theo plotId ở máy.
 * Vẫn cần danh sách lô riêng — lô chưa trồng gì cũng phải hiện trên sơ đồ.
 */
export function usePlotsWithPlantings(farmId) {
  const { userId } = useSession();
  const plots = useQuery({
    queryKey: ['plots', userId, farmId],
    queryFn: () => api.plots.byFarm(farmId),
    enabled: farmId != null,
  });
  const plantings = useActivePlantings(farmId);
  // Không dùng Map.groupBy: điện thoại cũ ngoài vườn (Safari < 17.4) chưa có.
  const byPlot = new Map();
  (plantings.data ?? []).forEach((p) => byPlot.set(p.plotId, [...(byPlot.get(p.plotId) ?? []), p]));
  const data = (plots.data ?? []).map((plot) => ({
    ...plot,
    plantings: byPlot.get(plot.id) ?? [],
  }));
  return {
    data,
    isPending: plots.isPending || plantings.isPending,
    isMock: isMockData(plots.data) || isMockData(plantings.data),
    error: plots.error ?? plantings.error ?? null,
    refetch: () => {
      plots.refetch();
      plantings.refetch();
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
 * Lựa chọn cho bộ lọc báo cáo: báo cáo lọc theo SEASON.year (BR-13), nhãn lấy nguyên từ API (plan
 * 6.5). Cùng một năm bắt đầu, cà phê là "2025/2026" còn cây ngắn ngày là "2025" — khác nhau thì
 * hiện đủ các nhãn, không tự ghép.
 * Giới hạn đã biết: chỉ có niên vụ của lứa đang canh tác; năm chỉ có lứa đã kết thúc thì chưa chọn
 * được (M6a chưa kết thúc lứa được — xử lý cùng trang Báo cáo ở M6d).
 */
export function seasonOptions(seasons) {
  const labelsByYear = new Map();
  seasons.forEach((s) => {
    const labels = labelsByYear.get(s.year) ?? new Set();
    labels.add(s.label);
    labelsByYear.set(s.year, labels);
  });
  return [...labelsByYear]
    .sort(([a], [b]) => b - a)
    .map(([year, labels]) => ({ year, label: [...labels].sort().reverse().join(' · ') }));
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

  return {
    months: months.map((m) => ({ month: m, cost: byMonth[m] })),
    seasonOptions: seasonOptions(seasonsByPlanting.flatMap(({ seasons }) => seasons)),
    progress,
    truncated,
    isPending: seasonQueries.some((q) => q.isPending) || activityQueries.some((q) => q.isPending),
    error: [...seasonQueries, ...activityQueries].find((q) => q.error)?.error ?? null,
  };
}
