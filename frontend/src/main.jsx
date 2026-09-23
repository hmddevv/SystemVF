import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { createBrowserRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import '@fontsource-variable/bitter';
import '@fontsource/ibm-plex-sans/400.css';
import '@fontsource/ibm-plex-sans/500.css';
import '@fontsource/ibm-plex-sans/600.css';
import './index.css';
import { AppShell } from './components/layout/AppShell';
import { SessionProvider } from './hooks/useSession';
import { DashboardPage } from './pages/DashboardPage';
import { ComingSoonPage, NotFoundPage } from './pages/ComingSoonPage';

/*
 * TanStack Query: truy vấn đọc thử lại 1 lần; thao tác ghi KHÔNG BAO GIỜ tự thử lại — backend chưa
 * có idempotency key, thử lại một POST có thể sinh dòng nhật ký trùng (plan 6.2).
 */
const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: 1, refetchOnWindowFocus: false, staleTime: 30_000 },
    mutations: { retry: 0 },
  },
});

const router = createBrowserRouter([
  {
    path: '/',
    Component: AppShell,
    children: [
      { index: true, Component: DashboardPage },
      {
        path: 'lo-dat',
        element: (
          <ComingSoonPage title="Lô đất">
            Danh sách lô và chi tiết từng lô: lứa đang trồng, lịch sử, vòng đời.
          </ComingSoonPage>
        ),
      },
      {
        path: 'lo-dat/:plotId',
        element: (
          <ComingSoonPage title="Chi tiết lô đất">
            Lứa trồng hiện có, lịch sử canh tác và lý do kết thúc.
          </ComingSoonPage>
        ),
      },
      { path: 'cay-trong', element: <ComingSoonPage title="Cây trồng" /> },
      { path: 'nhat-ky', element: <ComingSoonPage title="Nhật ký" /> },
      {
        path: 'bao-cao',
        element: (
          <ComingSoonPage title="Báo cáo">Lãi/lỗ theo cây, theo lô và theo niên vụ.</ComingSoonPage>
        ),
      },
      { path: '*', Component: NotFoundPage },
    ],
  },
]);

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <SessionProvider>
        <RouterProvider router={router} />
      </SessionProvider>
    </QueryClientProvider>
  </StrictMode>,
);
