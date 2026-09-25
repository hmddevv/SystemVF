import {
  Suspense,
  createContext,
  lazy,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';

/*
 * Tải lười: zod + react-hook-form chỉ cần khi mở form, Tổng quan không phải gánh lúc tải trang.
 * Nhưng tải trước ngay khi trang rảnh — ngoài vườn sóng yếu, lần đầu bấm "Ghi" không được phép
 * phụ thuộc vào một lần tải mạng nữa.
 */
const loadSheet = () => import('../components/log/LogActivitySheet');
const LogActivitySheet = lazy(() => loadSheet().then((m) => ({ default: m.LogActivitySheet })));

/*
 * Form "Ghi hoạt động" mở được từ mọi nơi: nút trên thanh trên, nút "Ghi" cạnh từng lời nhắc.
 * Một bảng duy nhất cho cả ứng dụng — mở từ lời nhắc thì điền sẵn lứa trồng và loại việc gợi ý
 * (`suggestedActivity` do backend trả, frontend không tự suy từ mã luật CARE-0x).
 */
const LogActivityContext = createContext(null);

export function LogActivityProvider({ children }) {
  // `session` tăng mỗi lần mở: bảng dựng lại giá trị ban đầu, không giữ trạng thái lần trước.
  const [state, setState] = useState({ open: false, prefill: null, session: 0 });

  // Trả focus về nút đã mở bảng: bảng bị gỡ khỏi DOM khi đóng nên trình duyệt không tự làm được.
  const trigger = useRef(null);
  const open = useCallback((prefill = null) => {
    trigger.current = document.activeElement;
    setState((s) => ({ open: true, prefill, session: s.session + 1 }));
  }, []);
  const close = useCallback(() => {
    setState((s) => ({ ...s, open: false }));
    const el = trigger.current;
    // Nút "Ghi" của lời nhắc có thể đã biến mất (ghi xong thì lời nhắc tự hết) — khi đó bỏ qua.
    if (el?.isConnected) requestAnimationFrame(() => el.focus());
  }, []);
  const value = useMemo(() => ({ open }), [open]);

  useEffect(() => {
    const idle = window.requestIdleCallback ?? ((fn) => setTimeout(fn, 1500));
    idle(() => loadSheet().catch(() => {}));
  }, []);

  return (
    <LogActivityContext.Provider value={value}>
      {children}
      {state.open && (
        <Suspense fallback={null}>
          <LogActivitySheet key={state.session} prefill={state.prefill} onClose={close} />
        </Suspense>
      )}
    </LogActivityContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useLogActivity() {
  const ctx = useContext(LogActivityContext);
  if (!ctx) throw new Error('useLogActivity phải nằm trong <LogActivityProvider>');
  return ctx;
}
