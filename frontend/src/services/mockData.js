/*
 * Dữ liệu mẫu — CHỈ dùng khi không kết nối được backend (xem api.js), giao diện luôn báo rõ.
 * Hình dạng giống hệt response thật trong docs/openapi.json. Tổng tiền của niên vụ và báo cáo được
 * cộng từ chính các dòng hoạt động/thu hoạch bên dưới, nên các màn hình luôn khớp nhau.
 * Bối cảnh: một nông hộ ở Cư M'gar, Đắk Lắk; "hôm nay" là cuối tháng 9 — giữa mùa mưa.
 */

const FARMS = [
  { id: 1, name: 'Vườn nhà', location: "Cư M'gar, Đắk Lắk", plotCount: 4, totalAreaM2: 36000 },
];

const PLOTS = [
  { id: 10, farmId: 1, name: 'Lô A1', areaM2: 10000, soilType: 'Đất đỏ bazan' },
  { id: 11, farmId: 1, name: 'Lô A2', areaM2: 12000, soilType: 'Đất đỏ bazan' },
  { id: 12, farmId: 1, name: 'Lô B1', areaM2: 8000, soilType: 'Đất đỏ bazan' },
  { id: 13, farmId: 1, name: 'Lô C1', areaM2: 6000, soilType: 'Đất xám' },
].map((p) => ({ ...p, areaHectares: p.areaM2 / 10000 }));

// Danh mục cây: đúng dữ liệu tham chiếu nạp ở migration V2 (có ở mọi môi trường, không phải demo).
const CROPS = [
  ['Cà phê', 'Robusta', true, 2],
  ['Cà phê', 'Arabica', true, 2],
  ['Hồ tiêu', 'Vĩnh Linh', true, 5],
  ['Sầu riêng', 'Ri6', true, 10],
  ['Điều', 'PN1', true, 6],
  ['Cao su', 'RRIM 600', true, 3],
  ['Ngô', 'LVN10', false, null],
].map(([name, variety, perennial, seasonStartMonth], i) => ({
  id: i + 1,
  name,
  variety,
  displayName: `${name} (${variety})`,
  perennial,
  seasonStartMonth,
}));

const PLANTINGS = [
  {
    id: 101,
    plotId: 10,
    cropId: 1,
    cropName: 'Cà phê (Robusta)',
    plantingDate: '2016-06-10',
    treeCount: 1100,
    status: 'PRODUCING',
  },
  {
    id: 102,
    plotId: 11,
    cropId: 1,
    cropName: 'Cà phê (Robusta)',
    plantingDate: '2021-05-20',
    treeCount: 1300,
    status: 'PRODUCING',
  },
  {
    id: 103,
    plotId: 11,
    cropId: 3,
    cropName: 'Hồ tiêu (Vĩnh Linh)',
    plantingDate: '2021-06-15',
    treeCount: 400,
    status: 'PRODUCING',
  },
  {
    id: 104,
    plotId: 12,
    cropId: 4,
    cropName: 'Sầu riêng (Ri6)',
    plantingDate: '2024-07-01',
    treeCount: 160,
    status: 'GROWING',
  },
  {
    id: 105,
    plotId: 13,
    cropId: 5,
    cropName: 'Điều (PN1)',
    plantingDate: '2015-05-10',
    treeCount: 500,
    status: 'PRODUCING',
  },
].map((p) => ({
  ...p,
  plotName: PLOTS.find((x) => x.id === p.plotId).name,
  perennial: true,
  ageMonths: monthsBetween(p.plantingDate, '2026-09-23'),
  endDate: null,
  endReason: null,
  endNote: null,
}));

// Hoạt động: [ngày, loại, chi phí, ghi chú]. Thu hoạch: [ngày, kg, doanh thu].
const SEASONS = [
  season(
    1001,
    101,
    2025,
    '2025-02-01',
    '2026-01-31',
    [
      ['2025-02-20', 'WATERING', 1700000, 'Tưới hoa'],
      ['2025-03-18', 'WATERING', 1700000, 'Đợt 2'],
      ['2025-06-05', 'FERTILIZING', 10400000, 'NPK đợt 1'],
      ['2025-08-02', 'FERTILIZING', 10600000, 'NPK đợt 2'],
      ['2025-10-08', 'WEEDING', 2200000, 'Làm cỏ trước thu hoạch'],
      ['2025-11-05', 'OTHER', 3900000, 'Thuê bạt, sân phơi'],
    ],
    [
      ['2025-11-22', 1300, 127400000],
      ['2025-12-10', 1600, 158400000],
    ],
  ),
  season(
    1002,
    101,
    2026,
    '2026-02-01',
    '2027-01-31',
    [
      ['2026-02-14', 'PRUNING', 1400000, 'Tỉa cành sau thu hoạch'],
      ['2026-02-26', 'WATERING', 1800000, 'Tưới hoa'],
      ['2026-03-20', 'WATERING', 1800000, 'Đợt 2'],
      ['2026-05-28', 'FERTILIZING', 10900000, 'NPK đợt 1'],
      ['2026-07-06', 'SPRAYING', 2600000, 'Trừ rệp sáp'],
      ['2026-08-01', 'FERTILIZING', 11200000, 'NPK đợt 2'],
    ],
    [],
  ),
  season(
    1003,
    102,
    2025,
    '2025-02-01',
    '2026-01-31',
    [
      ['2025-02-22', 'WATERING', 2000000, 'Tưới hoa'],
      ['2025-03-15', 'WATERING', 1800000, 'Đợt 2'],
      ['2025-06-02', 'FERTILIZING', 11200000, 'NPK đợt 1'],
      ['2025-07-10', 'SPRAYING', 2900000, 'Trừ rệp sáp'],
      ['2025-08-04', 'FERTILIZING', 11600000, 'NPK đợt 2'],
      ['2025-11-02', 'OTHER', 4500000, 'Thuê bạt, sân phơi'],
    ],
    [
      ['2025-11-20', 1200, 118800000],
      ['2025-12-08', 1400, 141400000],
      ['2026-01-05', 500, 52000000],
    ],
  ),
  season(
    1004,
    102,
    2026,
    '2026-02-01',
    '2027-01-31',
    [
      ['2026-02-12', 'PRUNING', 1500000, 'Tỉa cành sau thu hoạch'],
      ['2026-02-25', 'WATERING', 2100000, 'Tưới hoa'],
      ['2026-03-18', 'WATERING', 1900000, 'Đợt 2'],
      ['2026-04-10', 'WATERING', 1900000, 'Đợt 3'],
      ['2026-05-25', 'FERTILIZING', 11800000, 'NPK 16-16-8, 480 kg'],
      ['2026-06-18', 'WEEDING', 2600000, 'Thuê 4 công'],
      ['2026-07-02', 'SPRAYING', 3150000, 'Trừ rệp sáp'],
      ['2026-07-30', 'FERTILIZING', 12400000, 'NPK 16-16-8, 500 kg'],
    ],
    [],
  ),
  season(
    1005,
    103,
    2025,
    '2025-05-01',
    '2026-04-30',
    [
      ['2025-05-20', 'FERTILIZING', 4800000, 'Phân chuồng'],
      ['2025-07-15', 'SPRAYING', 2400000, 'Phòng chết nhanh'],
      ['2025-09-10', 'FERTILIZING', 5200000, 'NPK'],
      ['2025-12-12', 'PRUNING', 1600000, 'Cắt dây lươn'],
      ['2026-03-05', 'WATERING', 1500000, 'Tưới sau thu'],
    ],
    [
      ['2026-02-18', 420, 60900000],
      ['2026-03-02', 280, 40600000],
    ],
  ),
  season(
    1006,
    103,
    2026,
    '2026-05-01',
    '2027-04-30',
    [
      ['2026-05-22', 'FERTILIZING', 5000000, 'Phân chuồng'],
      ['2026-07-12', 'SPRAYING', 2600000, 'Phòng chết nhanh'],
      ['2026-08-01', 'FERTILIZING', 5400000, 'NPK'],
    ],
    [],
  ),
  season(
    1007,
    104,
    2024,
    '2024-07-01',
    '2025-09-30',
    [
      ['2024-07-01', 'OTHER', 24000000, 'Cây giống 160 cây, đào hố'],
      ['2024-10-15', 'FERTILIZING', 3200000, 'Phân hữu cơ'],
      ['2025-03-10', 'WATERING', 1600000, 'Tưới mùa khô'],
      ['2025-06-20', 'FERTILIZING', 3400000, 'NPK'],
    ],
    [],
  ),
  season(
    1008,
    104,
    2025,
    '2025-10-01',
    '2026-09-30',
    [
      ['2025-10-20', 'FERTILIZING', 3600000, 'Phân hữu cơ'],
      ['2026-01-15', 'WATERING', 1700000, 'Tưới mùa khô'],
      ['2026-03-10', 'WATERING', 1700000, 'Tưới mùa khô'],
      ['2026-05-12', 'SPRAYING', 1900000, 'Phòng nấm'],
      ['2026-08-12', 'FERTILIZING', 3800000, 'NPK'],
    ],
    [],
  ),
  season(
    1009,
    105,
    2025,
    '2025-06-01',
    '2026-05-31',
    [
      ['2025-06-15', 'FERTILIZING', 3600000, 'NPK'],
      ['2025-09-05', 'PRUNING', 2400000, 'Tỉa tán'],
      ['2025-12-10', 'SPRAYING', 2800000, 'Trừ bọ xít muỗi'],
      ['2026-01-20', 'WEEDING', 1500000, 'Dọn thực bì'],
    ],
    [
      ['2026-03-15', 450, 18900000],
      ['2026-04-10', 300, 12600000],
    ],
  ),
  season(
    1010,
    105,
    2026,
    '2026-06-01',
    '2027-05-31',
    [
      ['2026-06-18', 'FERTILIZING', 3800000, 'NPK'],
      ['2026-09-02', 'PRUNING', 2500000, 'Tỉa tán'],
    ],
    [],
  ),
];

const REMINDERS = [
  {
    ruleCode: 'CARE-02',
    plantingId: 102,
    cropName: 'Cà phê (Robusta)',
    plotName: 'Lô A2',
    suggestedActivity: 'FERTILIZING',
    title: 'Bón phân mùa mưa',
    detail: 'Lần bón gần nhất 30/07, chu kỳ khuyến nghị 45 ngày.',
    dueDate: '2026-09-13',
    severity: 'OVERDUE',
    daysOverdue: 10,
  },
  {
    ruleCode: 'CARE-02',
    plantingId: 103,
    cropName: 'Hồ tiêu (Vĩnh Linh)',
    plotName: 'Lô A2',
    suggestedActivity: 'FERTILIZING',
    title: 'Bón phân mùa mưa',
    detail: 'Lần bón gần nhất 01/08, chu kỳ khuyến nghị 45 ngày.',
    dueDate: '2026-09-15',
    severity: 'OVERDUE',
    daysOverdue: 8,
  },
  {
    ruleCode: 'CARE-02',
    plantingId: 101,
    cropName: 'Cà phê (Robusta)',
    plotName: 'Lô A1',
    suggestedActivity: 'FERTILIZING',
    title: 'Bón phân mùa mưa',
    detail: 'Lần bón gần nhất 01/08, chu kỳ khuyến nghị 45 ngày.',
    dueDate: '2026-09-15',
    severity: 'OVERDUE',
    daysOverdue: 8,
  },
  {
    ruleCode: 'CARE-02',
    plantingId: 104,
    cropName: 'Sầu riêng (Ri6)',
    plotName: 'Lô B1',
    suggestedActivity: 'FERTILIZING',
    title: 'Bón phân mùa mưa',
    detail: 'Lần bón gần nhất 12/08, chu kỳ khuyến nghị 45 ngày.',
    dueDate: '2026-09-26',
    severity: 'DUE_SOON',
    daysOverdue: -3,
  },
  {
    ruleCode: 'CARE-04',
    plantingId: 104,
    cropName: 'Sầu riêng (Ri6)',
    plotName: 'Lô B1',
    suggestedActivity: 'OTHER',
    title: 'Thăm vườn kiến thiết cơ bản',
    detail: 'Vườn dưới 3 năm tuổi, chưa cho thu hoạch. Lần thăm gần nhất 29/08.',
    dueDate: '2026-09-28',
    severity: 'DUE_SOON',
    daysOverdue: -5,
  },
];

function monthsBetween(from, to) {
  const [fy, fm] = from.split('-').map(Number);
  const [ty, tm] = to.split('-').map(Number);
  return (ty - fy) * 12 + (tm - fm);
}

function season(id, plantingId, year, startDate, endDate, acts, harvests) {
  const planting = PLANTINGS.find((p) => p.id === plantingId);
  const activities = acts.map(([activityDate, type, cost, note], i) => ({
    id: id * 100 + i,
    seasonId: id,
    seasonLabel: `${year}/${year + 1}`,
    type,
    activityDate,
    cost,
    note,
  }));
  const totalCost = activities.reduce((s, a) => s + a.cost, 0);
  const totalRevenue = harvests.reduce((s, h) => s + h[2], 0);
  const totalQuantityKg = harvests.reduce((s, h) => s + h[1], 0);
  return {
    id,
    plantingId,
    cropName: planting.cropName,
    year,
    label: `${year}/${year + 1}`,
    startDate,
    endDate,
    activityCount: activities.length,
    harvestCount: harvests.length,
    totalCost,
    totalQuantityKg,
    totalRevenue,
    netProfit: totalRevenue - totalCost,
    _activities: activities,
  };
}

const clone = (x) => JSON.parse(JSON.stringify(x));
const stripPrivate = ({ _activities, ...rest }) => rest; // eslint-disable-line no-unused-vars

export const farms = () => clone(FARMS);

export const crops = () => clone(CROPS);

export const plotsByFarm = (farmId) => clone(PLOTS.filter((p) => p.farmId === Number(farmId)));

export const plantingsByPlot = (plotId) =>
  clone(PLANTINGS.filter((p) => p.plotId === Number(plotId)));

// Giống GET /plantings: lọc theo nông trại, bỏ lứa đã kết thúc, xếp theo tên lô rồi lứa mới trồng trước.
export const plantingsOwned = ({ farmId, activeOnly = true } = {}) => {
  const plotIds = new Set(
    PLOTS.filter((p) => farmId == null || p.farmId === Number(farmId)).map((p) => p.id),
  );
  return clone(
    PLANTINGS.filter((p) => plotIds.has(p.plotId))
      .filter((p) => !activeOnly || p.status !== 'TERMINATED')
      .sort(
        (a, b) =>
          a.plotName.localeCompare(b.plotName, 'vi') ||
          b.plantingDate.localeCompare(a.plantingDate) ||
          b.id - a.id,
      ),
  );
};

export const seasonsByPlanting = (plantingId) =>
  clone(SEASONS.filter((s) => s.plantingId === Number(plantingId)).map(stripPrivate));

export const activitiesBySeason = (seasonId) => {
  const content = clone(SEASONS.find((s) => s.id === Number(seasonId))?._activities ?? []);
  return { content, page: 0, size: 200, totalElements: content.length, totalPages: 1 };
};

export const reminders = () => clone(REMINDERS);

// Báo cáo lãi/lỗ theo cây — cộng từ SEASONS để khớp mọi màn hình khác.
export const profitLoss = ({ groupBy = 'CROP', year } = {}) => {
  const rows = new Map();
  for (const s of SEASONS) {
    const planting = PLANTINGS.find((p) => p.id === s.plantingId);
    const key = planting.cropId;
    if (!rows.has(key)) {
      rows.set(key, {
        id: key,
        label: planting.cropName,
        seasonCount: 0,
        activityCount: 0,
        harvestCount: 0,
        totalCost: 0,
        totalRevenue: 0,
        totalQuantityKg: 0,
        netProfit: 0,
        lifetimeNetProfit: 0,
        treeCount: 0,
        areaM2: 0,
        _plantings: new Set(),
        _plots: new Set(),
      });
    }
    const row = rows.get(key);
    row.lifetimeNetProfit += s.netProfit;
    if (!row._plantings.has(planting.id)) {
      row._plantings.add(planting.id);
      row.treeCount += planting.treeCount;
    }
    if (!row._plots.has(planting.plotId)) {
      row._plots.add(planting.plotId);
      row.areaM2 += PLOTS.find((p) => p.id === planting.plotId).areaM2;
    }
    if (year != null && s.year !== Number(year)) continue;
    row.seasonCount += 1;
    row.activityCount += s.activityCount;
    row.harvestCount += s.harvestCount;
    row.totalCost += s.totalCost;
    row.totalRevenue += s.totalRevenue;
    row.totalQuantityKg += s.totalQuantityKg;
    row.netProfit += s.netProfit;
  }
  const sharedPlots = new Set(
    PLOTS.filter((p) => PLANTINGS.filter((x) => x.plotId === p.id).length > 1).map((p) => p.id),
  );
  const out = [...rows.values()].map(({ _plantings, _plots, ...r }) => ({
    ...r,
    profitPer1000m2: r.areaM2 ? Math.round((r.netProfit / r.areaM2) * 1000) : null,
    profitPerTree: r.treeCount ? Math.round(r.netProfit / r.treeCount) : null,
    yieldKgPerTree: r.treeCount ? +(r.totalQuantityKg / r.treeCount).toFixed(2) : null,
    sharedPlot: [..._plots].some((id) => sharedPlots.has(id)),
    paybackYear: null,
    _plantingCount: _plantings.size,
  }));
  out.sort((a, b) => b.netProfit - a.netProfit);
  const rowsOut = out.map(({ _plantingCount, ...r }) => r); // eslint-disable-line no-unused-vars
  return { groupBy, year: year ?? null, rows: rowsOut, total: null };
};
