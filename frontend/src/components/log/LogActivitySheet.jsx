import { useEffect, useId, useRef, useState } from 'react';
import { Controller, useForm, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api, isMockData } from '../../services/api';
import { ACTIVITY_TYPE } from '../../services/labels';
import {
  digitsOnly,
  formatMoney,
  formatNumber,
  formatWeekdayDate,
  groupDigits,
  shiftDays,
  todayIso,
} from '../../services/format';
import { useSession } from '../../hooks/useSession';
import { useActivePlantings, useCurrentFarm } from '../../hooks/useFarmData';
import { CropDot, EmptyBlock, ErrorBlock, LoadingBlock } from '../ui';
import { Icon } from '../Icon';
import { FieldError, SubmitError } from '../form/formParts';
import { applyServerErrors, submitLabel } from '../form/formLogic';

/*
 * Bảng "Ghi hoạt động" (plan 6.2, 6.6). Dùng ngoài vườn, một tay, sóng yếu:
 * - Chọn LỨA TRỒNG chứ không chọn lô — một lô trồng xen có nhiều lứa, nhật ký gắn vào từng lứa.
 * - Không hỏi niên vụ: backend tự xếp theo ngày làm (BR-05a) và trả nhãn niên vụ để báo lại.
 * - Gửi lỗi thì giữ nguyên nội dung, người dùng tự bấm "Gửi lại"; không bao giờ tự thử lại.
 * - Bản nháp lưu ở máy, lỡ tắt app không mất.
 * <dialog> gốc của trình duyệt lo bẫy focus, phím Esc, làm trơ phần nền và trả focus khi đóng.
 */

const TYPES = Object.keys(ACTIVITY_TYPE);
const NOTE_MAX = 255; // Activity.NOTE_MAX ở backend
const COST_DIGITS_MAX = 13; // @Digits(integer = 13) ở LogActivityRequest

function buildSchema(today) {
  return z
    .object({
      plantingId: z.string().min(1, { error: 'Chọn cây cần ghi' }),
      type: z.enum(TYPES, { error: 'Chọn loại việc' }),
      activityDate: z
        .string()
        .min(1, { error: 'Chọn ngày làm' })
        .refine((d) => d <= today, { error: 'Ngày làm không được ở tương lai' }),
      cost: z.string().refine((v) => digitsOnly(v).length <= COST_DIGITS_MAX, {
        error: `Chi phí tối đa ${COST_DIGITS_MAX} chữ số`,
      }),
      note: z.string().max(NOTE_MAX, { error: `Ghi chú tối đa ${NOTE_MAX} ký tự` }),
    })
    .superRefine((v, ctx) => {
      // BR-12: "Việc khác" thì phải ghi rõ là việc gì
      if (v.type === 'OTHER' && !v.note.trim()) {
        ctx.addIssue({
          code: 'custom',
          path: ['note'],
          message: 'Việc khác thì ghi rõ là việc gì',
        });
      }
    });
}

const draftKey = (userId) => `farm.logDraft.${userId}`;

function loadDraft(userId) {
  try {
    const raw = localStorage.getItem(draftKey(userId));
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

function saveDraft(userId, values) {
  try {
    localStorage.setItem(draftKey(userId), JSON.stringify(values));
  } catch {
    // Bộ nhớ bị chặn: form vẫn chạy, chỉ không giữ được nháp.
  }
}

function clearDraft(userId) {
  try {
    localStorage.removeItem(draftKey(userId));
  } catch {
    // như trên
  }
}

function ageLabel(months) {
  if (months == null) return null;
  if (months < 12) return `${months} tháng tuổi`;
  return `${Math.floor(months / 12)} năm tuổi`;
}

// Nhóm lứa trồng theo lô, giữ nguyên thứ tự API trả (đã xếp theo tên lô).
function groupByPlot(plantings) {
  const groups = [];
  plantings.forEach((p) => {
    const last = groups.at(-1);
    if (last?.plotId === p.plotId) last.items.push(p);
    else groups.push({ plotId: p.plotId, plotName: p.plotName, items: [p] });
  });
  return groups;
}

/*
 * Một khối của form. Nhóm radio dùng <fieldset>/<legend> để trình đọc màn hình đọc tên nhóm;
 * legend thả float để trình duyệt không đặt nó đè lên đường viền của fieldset.
 */
function Section({ title, hint, children, as: Tag = 'fieldset', ...rest }) {
  const heading = (
    <>
      <span className="font-display text-base font-semibold text-ink">{title}</span>
      {hint && <span className="text-sm text-muted">{hint}</span>}
    </>
  );
  const titleClass = 'mb-3 flex w-full items-baseline justify-between gap-3';
  return (
    <div className="px-5 py-5 not-first:border-t not-first:border-line">
      {Tag === 'fieldset' ? (
        <fieldset className="min-w-0" {...rest}>
          <legend className={`float-left ${titleClass}`}>{heading}</legend>
          <div className="clear-both">{children}</div>
        </fieldset>
      ) : (
        <div {...rest}>
          <p className={titleClass}>{heading}</p>
          {children}
        </div>
      )}
    </div>
  );
}

function PlantingPicker({ plantings, register, error }) {
  const groups = groupByPlot(plantings);
  return (
    <div className="space-y-4">
      {groups.map((g) => (
        <div key={g.plotId}>
          <p className="mb-1.5 text-sm text-muted">{g.plotName}</p>
          <div className="grid gap-2">
            {g.items.map((p) => (
              <label
                key={p.id}
                className="group flex min-h-14 cursor-pointer items-center gap-3 rounded-md border border-line bg-panel px-3 py-2 transition-colors hover:border-muted has-checked:border-leaf has-checked:bg-panel2 has-focus-visible:outline-2 has-focus-visible:outline-offset-2 has-focus-visible:outline-leaf"
              >
                <input
                  type="radio"
                  value={String(p.id)}
                  className="sr-only"
                  aria-describedby={error ? 'log-planting-error' : undefined}
                  {...register('plantingId')}
                />
                <CropDot cropName={p.cropName} size={12} />
                <span className="min-w-0 flex-1">
                  <span className="block truncate font-medium text-ink">{p.cropName}</span>
                  <span className="num block text-sm text-muted">
                    {formatNumber(p.treeCount)} cây
                    {ageLabel(p.ageMonths) ? `, ${ageLabel(p.ageMonths)}` : ''}
                  </span>
                </span>
                <span
                  aria-hidden="true"
                  className="grid h-6 w-6 shrink-0 place-items-center rounded-full border border-line text-bg group-has-checked:border-leaf group-has-checked:bg-leaf"
                >
                  <Icon
                    name="check"
                    size={14}
                    className="opacity-0 group-has-checked:opacity-100"
                  />
                </span>
              </label>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}

function TypePicker({ register }) {
  return (
    <div className="grid grid-cols-3 gap-2">
      {TYPES.map((t) => (
        <label
          key={t}
          className="group flex min-h-20 cursor-pointer flex-col items-center justify-center gap-1.5 rounded-md border border-line bg-panel px-1 py-2 text-center text-sm text-muted transition-colors hover:border-muted hover:text-ink has-checked:border-leaf has-checked:bg-panel2 has-checked:text-ink has-focus-visible:outline-2 has-focus-visible:outline-offset-2 has-focus-visible:outline-leaf"
        >
          <input type="radio" value={t} className="sr-only" {...register('type')} />
          <Icon name={t} size={26} className="group-has-checked:text-leaf" />
          <span className="leading-tight">{ACTIVITY_TYPE[t]}</span>
        </label>
      ))}
    </div>
  );
}

function DateChip({ active, onClick, children }) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      className={`min-h-11 rounded-md border px-4 text-sm transition-colors ${
        active
          ? 'border-leaf bg-panel2 text-ink'
          : 'border-line text-muted hover:border-muted hover:text-ink'
      }`}
    >
      {children}
    </button>
  );
}

// Màn báo đã ghi: nói rõ niên vụ hệ thống đã xếp vào — nhãn lấy nguyên từ response.
function Success({ result, planting, onAgain, onDone }) {
  const doneRef = useRef(null);
  useEffect(() => doneRef.current?.focus(), []);
  return (
    <div className="flex flex-1 flex-col justify-center px-6 py-10">
      <span className="grid h-14 w-14 place-items-center rounded-full border-2 border-leaf text-leaf">
        <Icon name="check" size={28} />
      </span>
      <h3 className="mt-5 font-display text-2xl font-semibold text-ink" role="status">
        Đã ghi {ACTIVITY_TYPE[result.type].toLowerCase()}
      </h3>
      <p className="mt-2 text-muted">
        {planting ? `${planting.cropName}, ${planting.plotName}. ` : ''}
        Ngày {formatWeekdayDate(result.activityDate).toLowerCase()}
        {Number(result.cost) > 0 ? `, chi ${formatMoney(result.cost)}` : ', không tốn chi phí'}.
      </p>
      <p className="mt-5 rounded-md border border-line bg-panel2 px-4 py-3 text-ink">
        Đã xếp vào niên vụ <span className="num font-semibold">{result.seasonLabel}</span>
        <span className="mt-0.5 block text-sm text-muted">
          Hệ thống tự chọn niên vụ theo ngày làm và loại cây.
        </span>
      </p>
      <div className="mt-8 flex flex-wrap gap-3">
        <button
          ref={doneRef}
          type="button"
          onClick={onDone}
          className="min-h-12 rounded-md bg-leaf px-6 font-semibold text-bg hover:brightness-110"
        >
          Xong
        </button>
        <button
          type="button"
          onClick={onAgain}
          className="min-h-12 rounded-md border border-line px-5 text-ink hover:border-muted"
        >
          Ghi việc khác cho cây này
        </button>
      </div>
    </div>
  );
}

export function LogActivitySheet({ prefill, onClose }) {
  const dialogRef = useRef(null);
  const titleRef = useRef(null);
  const titleId = useId();
  const today = todayIso();
  const { userId } = useSession();
  const { farm } = useCurrentFarm();
  const plantings = useActivePlantings(farm?.id ?? null);
  // Danh sách lứa là dữ liệu mẫu thì id cũng là id mẫu: gửi đi có thể ghi nhầm vào một lứa thật.
  const plantingsMock = isMockData(plantings.data);
  const queryClient = useQueryClient();
  const [result, setResult] = useState(null);

  const defaults = { plantingId: '', type: '', activityDate: today, cost: '', note: '' };
  /*
   * Mở từ lời nhắc: điền sẵn lứa trồng và việc gợi ý, không đụng tới bản nháp.
   * Mở thường: tiếp tục bản nháp, nhưng ngày làm luôn về hôm nay — nháp từ hôm trước mà giữ
   * nguyên ngày thì dễ ghi nhầm ngày, thậm chí nhầm niên vụ, mà người dùng không để ý.
   */
  const initial = prefill
    ? { ...defaults, plantingId: String(prefill.plantingId), type: prefill.type ?? '' }
    : { ...defaults, ...loadDraft(userId), activityDate: today };

  const form = useForm({
    resolver: zodResolver(buildSchema(today)),
    defaultValues: initial,
    mode: 'onSubmit',
    reValidateMode: 'onChange',
  });
  const { register, handleSubmit, control, setValue, setError, reset, formState } = form;
  const values = useWatch({ control });

  const mutation = useMutation({
    mutationFn: ({ plantingId, body }) => api.activities.log(plantingId, body),
    retry: 0,
    networkMode: 'always', // mất mạng thì báo "Chưa gửi được" ngay, không treo chờ rồi tự gửi
    onSuccess: (data) => {
      // Làm mới những gì phụ thuộc nhật ký: lời nhắc tự biến mất (BR-18), niên vụ có thể vừa
      // được tạo (BR-05a), hoạt động và báo cáo.
      ['reminders', 'seasons', 'activities', 'profit-loss'].forEach((key) =>
        queryClient.invalidateQueries({ queryKey: [key] }),
      );
      clearDraft(userId);
      setResult(data);
    },
    onError: (error) => applyServerErrors(error, setError, Object.keys(defaults)),
  });

  useEffect(() => {
    const dialog = dialogRef.current;
    if (dialog && !dialog.open) dialog.showModal();
    // Focus vào tiêu đề: trình đọc màn hình đọc tên bảng, không có vòng focus lạc trên nút Đóng
    titleRef.current?.focus();
  }, []);

  // Lưu nháp mỗi lần sửa — chỉ khi mở thường và chưa ghi xong
  useEffect(() => {
    if (!prefill && !result && formState.isDirty) saveDraft(userId, values);
  }, [values, result, formState.isDirty, userId, prefill]);

  // Lứa trồng trong nháp không còn trong danh sách (đổi nông trại, lứa đã kết thúc): bỏ chọn,
  // không để form gửi vào một lứa trồng không hiện trên màn hình.
  const loadedIds = plantings.data ? plantings.data.map((p) => p.id).join(',') : null;
  useEffect(() => {
    if (loadedIds == null || !values.plantingId) return;
    if (!loadedIds.split(',').includes(values.plantingId)) setValue('plantingId', '');
  }, [loadedIds, values.plantingId, setValue]);

  const onSubmit = handleSubmit((v) => {
    const digits = digitsOnly(v.cost);
    mutation.mutate({
      plantingId: Number(v.plantingId),
      body: {
        type: v.type,
        activityDate: v.activityDate,
        cost: digits ? Number(digits) : null, // trống = tự làm, không tốn tiền (BR-08)
        note: v.note.trim() || null,
      },
    });
  });

  const again = () => {
    // Thường ghi liền vài việc cho cùng một cây trong cùng một ngày
    reset({ ...defaults, plantingId: values.plantingId, activityDate: values.activityDate });
    mutation.reset();
    setResult(null);
  };

  const list = plantings.data ?? [];
  const selected = list.find((p) => String(p.id) === values.plantingId);
  const noteRequired = values.type === 'OTHER';
  const errors = formState.errors;
  const pending = mutation.isPending;

  return (
    <dialog
      ref={dialogRef}
      aria-labelledby={titleId}
      onClose={onClose}
      onCancel={(e) => {
        if (pending) e.preventDefault(); // đang gửi thì không cho đóng ngang
      }}
      className="sheet"
    >
      <div className="flex h-full flex-col">
        <header className="flex items-start gap-3 border-b border-line px-5 pt-5 pb-4">
          <div className="min-w-0 flex-1">
            <h2
              id={titleId}
              ref={titleRef}
              tabIndex={-1}
              className="font-display text-xl font-semibold text-ink outline-none"
            >
              Ghi hoạt động
            </h2>
            <p className="mt-0.5 text-sm text-muted">
              Ghi cho từng cây; niên vụ do hệ thống tự xếp theo ngày làm.
            </p>
          </div>
          <button
            type="button"
            onClick={() => dialogRef.current?.close()}
            disabled={pending}
            className="-mt-1 -mr-2 grid h-11 w-11 shrink-0 place-items-center rounded-md text-muted hover:text-ink disabled:opacity-40"
          >
            <Icon name="close" />
            <span className="sr-only">Đóng</span>
          </button>
        </header>

        {plantingsMock && !result && (
          <p className="border-b border-harvest/40 px-5 py-2 text-sm text-harvest">
            Danh sách cây đang là dữ liệu mẫu vì không kết nối được máy chủ — chưa ghi được. Kết nối
            lại rồi mở lại bảng này.
          </p>
        )}

        {result ? (
          <Success
            result={result}
            planting={selected}
            onAgain={again}
            onDone={() => dialogRef.current?.close()}
          />
        ) : (
          <form onSubmit={onSubmit} noValidate className="flex min-h-0 flex-1 flex-col">
            <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain">
              <Section
                title="Cây nào"
                hint={farm ? farm.name : null}
                aria-invalid={errors.plantingId ? true : undefined}
              >
                {!farm ? (
                  <EmptyBlock className="px-0 py-2">
                    Chưa có nông trại nào. Tạo nông trại, thêm lô đất và trồng cây trước rồi mới ghi
                    được hoạt động.
                  </EmptyBlock>
                ) : plantings.isLoading ? (
                  <LoadingBlock className="px-0" />
                ) : plantings.error ? (
                  <ErrorBlock
                    className="px-0"
                    error={plantings.error}
                    onRetry={plantings.refetch}
                  />
                ) : list.length === 0 ? (
                  <EmptyBlock className="px-0 py-2">
                    Nông trại này chưa có cây nào đang canh tác. Trồng cây trên lô trước rồi mới ghi
                    được hoạt động.
                  </EmptyBlock>
                ) : (
                  <PlantingPicker plantings={list} register={register} error={errors.plantingId} />
                )}
                <FieldError id="log-planting-error" message={errors.plantingId?.message} />
              </Section>

              <Section title="Việc gì">
                <TypePicker register={register} />
                <FieldError message={errors.type?.message} />
              </Section>

              <Section
                title="Ngày làm"
                as="div"
                hint={formatWeekdayDate(values.activityDate || today)}
              >
                <div className="flex flex-wrap items-center gap-2">
                  <DateChip
                    active={values.activityDate === today}
                    onClick={() =>
                      setValue('activityDate', today, { shouldDirty: true, shouldValidate: true })
                    }
                  >
                    Hôm nay
                  </DateChip>
                  <DateChip
                    active={values.activityDate === shiftDays(today, -1)}
                    onClick={() =>
                      setValue('activityDate', shiftDays(today, -1), {
                        shouldDirty: true,
                        shouldValidate: true,
                      })
                    }
                  >
                    Hôm qua
                  </DateChip>
                  <label className="sr-only" htmlFor="log-date">
                    Chọn ngày khác
                  </label>
                  <input
                    id="log-date"
                    type="date"
                    max={today}
                    className="num min-h-11 flex-1 rounded-md border border-line bg-panel px-3 text-ink"
                    aria-invalid={errors.activityDate ? true : undefined}
                    aria-describedby="log-date-error"
                    {...register('activityDate')}
                  />
                </div>
                <FieldError id="log-date-error" message={errors.activityDate?.message} />
              </Section>

              <Section title="Chi phí" as="div" hint="Để trống nếu tự làm">
                <label className="sr-only" htmlFor="log-cost">
                  Chi phí, đồng
                </label>
                <div className="relative">
                  <Controller
                    name="cost"
                    control={control}
                    render={({ field }) => (
                      <input
                        id="log-cost"
                        type="text"
                        inputMode="numeric"
                        autoComplete="off"
                        placeholder="0"
                        value={field.value}
                        onChange={(e) => field.onChange(groupDigits(e.target.value))}
                        onBlur={field.onBlur}
                        ref={field.ref}
                        aria-invalid={errors.cost ? true : undefined}
                        aria-describedby="log-cost-error"
                        className="num min-h-12 w-full rounded-md border border-line bg-panel py-2 pr-10 pl-3 font-display text-xl text-ink placeholder:text-muted/60"
                      />
                    )}
                  />
                  <span
                    aria-hidden="true"
                    className="pointer-events-none absolute top-1/2 right-3 -translate-y-1/2 text-muted"
                  >
                    đ
                  </span>
                </div>
                <FieldError id="log-cost-error" message={errors.cost?.message} />
              </Section>

              <Section
                title={noteRequired ? 'Ghi chú (bắt buộc)' : 'Ghi chú'}
                as="div"
                hint={
                  <span className="num">
                    {(values.note ?? '').length}/{NOTE_MAX}
                  </span>
                }
              >
                <label className="sr-only" htmlFor="log-note">
                  Ghi chú
                </label>
                <textarea
                  id="log-note"
                  rows={3}
                  maxLength={NOTE_MAX}
                  placeholder={
                    noteRequired ? 'Ghi rõ là việc gì' : 'Ví dụ: bón NPK 16-16-8, 20 bao'
                  }
                  aria-invalid={errors.note ? true : undefined}
                  aria-describedby="log-note-error"
                  className="w-full resize-y rounded-md border border-line bg-panel px-3 py-2 text-ink placeholder:text-muted/60"
                  {...register('note')}
                />
                <FieldError id="log-note-error" message={errors.note?.message} />
              </Section>
            </div>

            <footer className="border-t border-line bg-panel px-5 pt-4 pb-[max(1rem,env(safe-area-inset-bottom))]">
              <SubmitError
                error={mutation.error}
                retryHint="Nếu vừa mất sóng giữa chừng, xem lại nhật ký của cây này trước khi gửi lại để tránh ghi trùng."
              />
              <p className="mb-3 min-h-5 truncate text-sm text-muted" aria-live="polite">
                {selected && values.type
                  ? `${ACTIVITY_TYPE[values.type]} cho ${selected.cropName}, ${selected.plotName}`
                  : 'Chọn cây và loại việc'}
              </p>
              <div className="flex gap-3">
                <button
                  type="submit"
                  disabled={pending || !farm || list.length === 0 || plantingsMock}
                  className="min-h-12 flex-1 rounded-md bg-leaf px-5 font-semibold text-bg hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {submitLabel(mutation, 'Ghi hoạt động')}
                </button>
                <button
                  type="button"
                  onClick={() => dialogRef.current?.close()}
                  disabled={pending}
                  className="min-h-12 rounded-md border border-line px-5 text-ink hover:border-muted disabled:opacity-40"
                >
                  Để sau
                </button>
              </div>
            </footer>
          </form>
        )}
      </div>
    </dialog>
  );
}
