import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';

/*
 * Dữ liệu API giả cho test — hình dạng theo docs/openapi.json. Mỗi test tự thay handler khi cần
 * một tình huống khác (lỗi, mất mạng).
 */
export const FARM = {
  id: 1,
  name: "Nông trại Cư M'gar",
  location: null,
  plotCount: 1,
  totalAreaM2: 15000,
};

export const PLANTINGS = [
  {
    id: 3,
    plotId: 1,
    plotName: 'Lô A2',
    cropId: 4,
    cropName: 'Sầu riêng (Ri6)',
    perennial: true,
    plantingDate: '2023-05-20',
    treeCount: 80,
    status: 'PRODUCING',
    ageMonths: 40,
    endDate: null,
    endReason: null,
    endNote: null,
  },
  {
    id: 1,
    plotId: 1,
    plotName: 'Lô A2',
    cropId: 1,
    cropName: 'Cà phê (Robusta)',
    perennial: true,
    plantingDate: '2016-06-15',
    treeCount: 1100,
    status: 'PRODUCING',
    ageMonths: 123,
    endDate: null,
    endReason: null,
    endNote: null,
  },
];

export const api = (path) => `*/api/v1${path}`;

export const handlers = [
  http.get(api('/farms'), () => HttpResponse.json([FARM])),
  http.get(api('/plantings'), () => HttpResponse.json(PLANTINGS)),
];

export const server = setupServer(...handlers);

// ProblemDetail như GlobalExceptionHandler trả về
export function problem(status, body) {
  return HttpResponse.json(
    { type: 'urn:farm:problem:business-rule', status, ...body },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  );
}
