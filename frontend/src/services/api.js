import axios from 'axios';
import * as mock from './mockData';

/*
 * Lớp service — nơi DUY NHẤT gọi HTTP và biết tên trường của API.
 * Hợp đồng API: docs/openapi.json. Không có kiểu sinh tự động (phương án 2), nên backend đổi tên
 * trường thì phải rà file này bằng tay.
 */

const USER_KEY = 'farm.devUserId';
export const DEV_USERS = [
  { id: 1, name: 'Chủ nông trại mẫu' },
  { id: 2, name: 'Chủ nông trại thứ hai' },
];

// Người dùng hiện tại — tạm thời qua header X-User-Id cho tới khi có JWT (Phase 5).
export function getUserId() {
  try {
    return Number(localStorage.getItem(USER_KEY)) || 1;
  } catch {
    return 1;
  }
}

export function setUserId(id) {
  try {
    localStorage.setItem(USER_KEY, String(id));
  } catch {
    // Trình duyệt chặn bộ nhớ (chế độ riêng tư): vẫn chạy, chỉ không nhớ lựa chọn.
  }
}

// Gọi qua proxy của Vite (/api → localhost:8080): cùng origin, backend không cần CORS.
const http = axios.create({ baseURL: '/api/v1', timeout: 15000 });

http.interceptors.request.use((config) => {
  config.headers['X-User-Id'] = String(getUserId());
  return config;
});

/**
 * Lỗi đã chuẩn hoá từ ProblemDetail (RFC 9457).
 * `detail` luôn có và là tiếng Việt → hiển thị mặc định. `rule` chỉ có ở lỗi nghiệp vụ,
 * `errors[{field, message}]` chỉ có ở lỗi validation.
 */
export class ApiError extends Error {
  constructor({ status, detail, rule = null, errors = [], type = null, unreachable = false }) {
    super(detail);
    this.status = status;
    this.detail = detail;
    this.rule = rule;
    this.errors = errors;
    this.type = type;
    this.unreachable = unreachable;
  }
}

function toApiError(err) {
  const res = err.response;
  const problem = res?.data && typeof res.data === 'object' ? res.data : null;
  // Không có phản hồi, hoặc proxy của Vite trả 5xx không kèm ProblemDetail = backend chưa chạy.
  const unreachable = !res || ([500, 502, 503, 504].includes(res.status) && !problem?.type);
  if (unreachable) {
    return new ApiError({
      status: res?.status ?? 0,
      detail: 'Không kết nối được máy chủ. Kiểm tra mạng hoặc backend rồi thử lại.',
      unreachable: true,
    });
  }
  return new ApiError({
    status: res.status,
    detail: problem?.detail ?? `Máy chủ trả lỗi ${res.status}.`,
    rule: problem?.rule ?? null,
    errors: problem?.errors ?? [],
    type: problem?.type ?? null,
  });
}

/*
 * Trạng thái "đang dùng dữ liệu mẫu": bật khi một truy vấn đọc phải lấy dữ liệu mẫu vì không kết
 * nối được backend. Giao diện luôn hiện dải báo khi cờ này bật — không bao giờ giả vờ là dữ liệu thật.
 */
const mockListeners = new Set();
let mockActive = false;

function setMockActive(value) {
  if (mockActive === value) return;
  mockActive = value;
  mockListeners.forEach((fn) => fn());
}

export const mockStatus = {
  get: () => mockActive,
  subscribe(fn) {
    mockListeners.add(fn);
    return () => mockListeners.delete(fn);
  },
};

// Truy vấn đọc: mất kết nối thì lấy dữ liệu mẫu (nếu có) và bật cờ.
async function read(url, params, fallback) {
  try {
    const res = await http.get(url, { params });
    setMockActive(false);
    return res.data;
  } catch (err) {
    const apiError = toApiError(err);
    if (apiError.unreachable && fallback) {
      setMockActive(true);
      return fallback();
    }
    throw apiError;
  }
}

// Thao tác ghi: KHÔNG dữ liệu mẫu, KHÔNG tự thử lại — backend chưa có idempotency key, thử lại một
// POST mà lần trước thực ra đã tới máy chủ sẽ sinh dòng nhật ký trùng.
async function write(method, url, body) {
  try {
    const res = await http.request({ method, url, data: body });
    return res.data;
  } catch (err) {
    throw toApiError(err);
  }
}

export const api = {
  farms: {
    list: () => read('/farms', null, mock.farms),
    create: (body) => write('post', '/farms', body),
  },
  plots: {
    byFarm: (farmId) => read(`/farms/${farmId}/plots`, null, () => mock.plotsByFarm(farmId)),
    create: (farmId, body) => write('post', `/farms/${farmId}/plots`, body),
  },
  crops: {
    list: () => read('/crops', null, mock.crops),
  },
  plantings: {
    // Mọi lứa của một nông trại trong một lần gọi, xếp theo tên lô (plan mục 6.1).
    owned: ({ farmId, activeOnly = true } = {}) =>
      read('/plantings', { farmId, activeOnly }, () => mock.plantingsOwned({ farmId, activeOnly })),
    byPlot: (plotId, activeOnly = true) =>
      read(`/plots/${plotId}/plantings`, { activeOnly }, () => mock.plantingsByPlot(plotId)),
    plant: (plotId, body) => write('post', `/plots/${plotId}/plantings`, body),
  },
  seasons: {
    byPlanting: (plantingId) =>
      read(`/plantings/${plantingId}/seasons`, null, () => mock.seasonsByPlanting(plantingId)),
  },
  activities: {
    // size=200 là tối đa API cho phép; một niên vụ của một lứa hiếm khi vượt (plan mục 6.4).
    bySeason: (seasonId, page = 0, size = 200) =>
      read(`/seasons/${seasonId}/activities`, { page, size }, () =>
        mock.activitiesBySeason(seasonId),
      ),
    log: (plantingId, body) => write('post', `/plantings/${plantingId}/activities`, body),
  },
  reminders: {
    list: (farmId) => read('/reminders', { farmId }, () => mock.reminders(farmId)),
  },
  reports: {
    profitLoss: ({ groupBy = 'CROP', year, farmId } = {}) =>
      read('/reports/profit-loss', { groupBy, year, farmId }, () =>
        mock.profitLoss({ groupBy, year }),
      ),
  },
};
