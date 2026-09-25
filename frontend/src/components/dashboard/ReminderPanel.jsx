import { CropDot, EmptyBlock, ErrorBlock, LoadingBlock, Panel } from '../ui';
import { formatDayMonth } from '../../services/format';
import { ACTIVITY_TYPE } from '../../services/labels';
import { useLogActivity } from '../../hooks/useLogActivity';

/*
 * Lời nhắc chăm sóc từ GET /reminders (luật CARE-01…04). Không có nút "đã làm": lời nhắc tự biến
 * mất khi nhật ký tương ứng được ghi (BR-18). "Cảnh báo" = quá hạn; "Sắp đến hạn" = trong 7 ngày tới.
 */

/*
 * Nút "Ghi" mở form với lứa trồng và việc gợi ý điền sẵn. Ghi xong, lời nhắc tự biến mất vì
 * backend tính lại từ nhật ký — đó là cách duy nhất để "xong" một lời nhắc.
 */
function ReminderRow({ reminder }) {
  const { open } = useLogActivity();
  const late = reminder.severity === 'OVERDUE';
  const days = Math.abs(reminder.daysOverdue);
  const when = late ? `quá ${days} ngày` : days === 0 ? 'hạn hôm nay' : `còn ${days} ngày`;
  const action = reminder.suggestedActivity
    ? ACTIVITY_TYPE[reminder.suggestedActivity].toLowerCase()
    : 'hoạt động';
  return (
    <li className="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-x-3 border-t border-line px-4 py-3 first:border-t-0">
      <div className="min-w-0">
        <p className="font-medium text-ink">{reminder.title}</p>
        <p className="mt-0.5 flex items-center gap-1.5 text-sm text-muted">
          <CropDot cropName={reminder.cropName} size={8} />
          <span className="truncate">
            {reminder.plotName}, {reminder.cropName}
          </span>
        </p>
        <p className="num mt-0.5 text-sm">
          <span className={late ? 'font-semibold text-clay' : 'text-muted'}>{when}</span>
          {days !== 0 && (
            <span className="text-muted">, hạn {formatDayMonth(reminder.dueDate)}</span>
          )}
        </p>
      </div>
      <button
        type="button"
        onClick={() =>
          open({ plantingId: reminder.plantingId, type: reminder.suggestedActivity ?? null })
        }
        aria-label={`Ghi ${action} cho ${reminder.cropName}, ${reminder.plotName}`}
        className="min-h-11 rounded-md border border-line px-4 text-sm font-medium text-ink hover:border-leaf hover:text-leaf"
      >
        Ghi
      </button>
    </li>
  );
}

function ReminderGroup({ title, items, empty }) {
  return (
    <Panel
      title={title}
      aside={
        items.length > 0 && <span className="num text-sm text-muted">{items.length} việc</span>
      }
    >
      {items.length === 0 ? (
        <EmptyBlock className="pt-0">{empty}</EmptyBlock>
      ) : (
        <ul>
          {items.map((r) => (
            <ReminderRow key={`${r.ruleCode}-${r.plantingId}`} reminder={r} />
          ))}
        </ul>
      )}
    </Panel>
  );
}

export function ReminderPanel({ query }) {
  if (query.isPending) {
    return (
      <Panel title="Cảnh báo">
        <LoadingBlock />
      </Panel>
    );
  }
  if (query.error) {
    return (
      <Panel title="Cảnh báo">
        <ErrorBlock error={query.error} onRetry={query.refetch} />
      </Panel>
    );
  }
  const overdue = query.data.filter((r) => r.severity === 'OVERDUE');
  const soon = query.data.filter((r) => r.severity !== 'OVERDUE');
  return (
    <div className="flex flex-col gap-4">
      <ReminderGroup title="Cảnh báo" items={overdue} empty="Không có việc chăm sóc nào quá hạn." />
      <ReminderGroup
        title="Sắp đến hạn"
        items={soon}
        empty="Không có việc nào đến hạn trong 7 ngày tới."
      />
    </div>
  );
}
