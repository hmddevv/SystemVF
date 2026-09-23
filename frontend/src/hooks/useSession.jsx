import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { getUserId, setUserId } from '../services/api';

/*
 * Phiên làm việc: người dùng (dev, qua X-User-Id) và nông trại đang xem.
 * Đổi người dùng thì xoá toàn bộ cache — dữ liệu của người trước không được lọt sang người sau (BR-11).
 */
const SessionContext = createContext(null);

export function SessionProvider({ children }) {
  const queryClient = useQueryClient();
  const [userId, setUserState] = useState(getUserId);
  const [farmId, setFarmId] = useState(null);

  const changeUser = useCallback(
    (id) => {
      setUserId(id);
      setUserState(id);
      setFarmId(null);
      queryClient.removeQueries();
    },
    [queryClient],
  );

  const value = useMemo(
    () => ({ userId, changeUser, farmId, setFarmId }),
    [userId, changeUser, farmId],
  );
  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useSession() {
  const ctx = useContext(SessionContext);
  if (!ctx) throw new Error('useSession phải nằm trong <SessionProvider>');
  return ctx;
}
