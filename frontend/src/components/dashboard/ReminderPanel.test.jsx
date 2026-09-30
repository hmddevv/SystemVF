import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClientProvider } from '@tanstack/react-query';
import { http, HttpResponse } from 'msw';
import { createQueryClient } from '../../services/queryClient';
import { api as client } from '../../services/api';
import { SessionProvider } from '../../hooks/useSession';
import { LogActivityProvider } from '../../hooks/useLogActivity';
import { ReminderPanel } from './ReminderPanel';
import { api, server } from '../../test/server';

function renderPanel(data) {
  render(
    <QueryClientProvider client={createQueryClient({ queries: { retry: false } })}>
      <SessionProvider>
        <LogActivityProvider>
          <ReminderPanel query={{ data, isPending: false, error: null }} />
        </LogActivityProvider>
      </SessionProvider>
    </QueryClientProvider>,
  );
}

describe('Lời nhắc', () => {
  it('lời nhắc là dữ liệu mẫu thì khóa nút "Ghi" — id lứa mẫu có thể trùng một lứa thật', async () => {
    server.use(http.get(api('/reminders'), () => HttpResponse.error()));
    renderPanel(await client.reminders.list(1));

    const buttons = screen.getAllByRole('button', { name: /^Ghi / });
    expect(buttons.length).toBeGreaterThan(0);
    buttons.forEach((b) => expect(b).toBeDisabled());
  });

  it('lời nhắc thật thì bấm "Ghi" được', async () => {
    const reminder = {
      ruleCode: 'CARE-02',
      title: 'Bón phân mùa mưa',
      plantingId: 1,
      plotName: 'Lô A2',
      cropName: 'Cà phê (Robusta)',
      severity: 'OVERDUE',
      daysOverdue: 3,
      dueDate: '2026-09-27',
      suggestedActivity: 'FERTILIZING',
    };
    server.use(http.get(api('/reminders'), () => HttpResponse.json([reminder])));
    renderPanel(await client.reminders.list(1));

    expect(screen.getByRole('button', { name: /^Ghi / })).toBeEnabled();
  });
});
