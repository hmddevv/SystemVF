import { useEffect, useId, useRef, useState } from 'react';
import { Controller, useForm, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../../services/api';
import { digitsOnly, formatArea, formatNumber, todayIso } from '../../services/format';
import { useSession } from '../../hooks/useSession';
import { useCrops } from '../../hooks/useFarmData';
import { CropDot, EmptyBlock, ErrorBlock, LoadingBlock } from '../ui';
import { Icon } from '../Icon';
import { DigitsInput, Field, FieldError, SubmitError, inputClass } from '../form/formParts';
import { applyServerErrors, submitLabel } from '../form/formLogic';

/*
 * Bắt đầu lần đầu (plan 6.3, M6a bước 6). Lời nhắc và báo cáo chỉ có khi đã có nông trại → lô →
 * lứa trồng, nên người dùng mới được dẫn qua đúng ba bước đó, mỗi bước một form tối thiểu.
 *
 * Bước hiện tại do màn Tổng quan suy ra từ DỮ LIỆU (chưa có nông trại / chưa có lô / chưa có lứa
 * đang trồng), không lưu cờ "đã xong hướng dẫn": đổi máy, đổi nông trại vẫn đúng. Tạo xong một
 * bước thì ghi thẳng kết quả vào cache để bước sau hiện ngay, không chờ tải lại.
 *
 * Giới hạn độ dài lấy từ docs/openapi.json (FarmRequest, PlotRequest, PlantCropRequest).
 */

const STEPS = [
  {
    title: 'Tạo nông trại',
    why: 'Nông trại gom các lô đất ở cùng một nơi. Có vườn ở nhiều xã thì mỗi nơi tạo một nông trại.',
  },
  {
    title: 'Thêm lô đất',
    why: 'Mỗi lô là một khu canh tác riêng. Chi phí, sản lượng và lãi/lỗ được tính riêng cho từng lô.',
  },
  {
    title: 'Trồng cây trên lô',
    why: 'Khai loại cây, ngày trồng và số cây. Niên vụ do hệ thống tự tính theo loại cây; lời nhắc chăm sóc chạy ngay sau đó.',
  },
];

const SOIL_SUGGESTIONS = ['Đất đỏ bazan', 'Đất xám', 'Đất phù sa', 'Đất đen'];
const AREA_MAX_M2 = 100_000_000;
const TREE_MAX = 10_000_000;

// 4 chữ số thập phân: lô nhỏ (30 m² = 0,003 ha) không bị làm tròn thành "0 ha"
const hectares = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 4 });
const collator = new Intl.Collator('vi');

// Thao tác ghi: không tự thử lại, mất mạng báo ngay (plan 6.2) — nói rõ ở đây dù đã là mặc định.
const WRITE = { retry: 0, networkMode: 'always' };

function StepForm({ onSubmit, children, footer }) {
  return (
    <form onSubmit={onSubmit} noValidate className="space-y-5 px-5 pt-5 pb-5">
      {children}
      <div className="border-t border-line pt-5">{footer}</div>
    </form>
  );
}

function SubmitButton({ mutation, label }) {
  return (
    <button
      type="submit"
      disabled={mutation.isPending}
      className="min-h-12 w-full rounded-md bg-leaf px-6 font-semibold text-bg hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-50 sm:w-auto"
    >
      {submitLabel(mutation, label)}
    </button>
  );
}

// Tạo xong mà mất sóng lúc nhận phản hồi thì bản ghi có thể đã có — xem lại rồi mới gửi lại.
const RETRY_HINT = 'Nếu vừa mất sóng giữa chừng, tải lại trang xem đã tạo chưa rồi mới gửi lại.';

/* ---------- Bước 1: nông trại ---------- */

const farmSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, { error: 'Nhập tên nông trại' })
    .max(120, { error: 'Tên tối đa 120 ký tự' }),
  location: z.string().trim().max(255, { error: 'Địa chỉ tối đa 255 ký tự' }),
});

function FarmStep({ onDone }) {
  const { userId, setFarmId } = useSession();
  const queryClient = useQueryClient();
  const form = useForm({
    resolver: zodResolver(farmSchema),
    defaultValues: { name: '', location: '' },
  });
  const { register, handleSubmit, setError, formState } = form;
  const errors = formState.errors;

  const mutation = useMutation({
    ...WRITE,
    mutationFn: api.farms.create,
    onSuccess: (farm) => {
      // Nông trại mới chắc chắn chưa có lô, chưa có cây: ghi sẵn để bước 2 hiện ngay.
      queryClient.setQueryData(['farms', userId], (old = []) => [...old, farm]);
      queryClient.setQueryData(['plots', userId, farm.id], []);
      queryClient.setQueryData(['plantings', userId, farm.id, 'active'], []);
      queryClient.invalidateQueries({ queryKey: ['farms'] });
      setFarmId(farm.id);
      onDone(`Đã tạo nông trại ${farm.name}.`);
    },
    onError: (error) => applyServerErrors(error, setError, ['name', 'location']),
  });

  const onSubmit = handleSubmit((v) =>
    mutation.mutate({ name: v.name, location: v.location || null }),
  );

  return (
    <StepForm
      onSubmit={onSubmit}
      footer={
        <>
          <SubmitError error={mutation.error} retryHint={RETRY_HINT} />
          <SubmitButton mutation={mutation} label="Tạo nông trại" />
        </>
      }
    >
      <Field id="farm-name" label="Tên nông trại" error={errors.name?.message}>
        {({ id, describedBy, invalid }) => (
          <input
            id={id}
            type="text"
            maxLength={120}
            autoComplete="off"
            placeholder="Ví dụ: Vườn nhà"
            aria-invalid={invalid}
            aria-describedby={describedBy}
            className={inputClass}
            {...register('name')}
          />
        )}
      </Field>
      <Field id="farm-location" label="Địa chỉ" optional error={errors.location?.message}>
        {({ id, describedBy, invalid }) => (
          <input
            id={id}
            type="text"
            maxLength={255}
            placeholder="Ví dụ: xã Ea Tar, huyện Cư M'gar, Đắk Lắk"
            aria-invalid={invalid}
            aria-describedby={describedBy}
            className={inputClass}
            {...register('location')}
          />
        )}
      </Field>
    </StepForm>
  );
}

/* ---------- Bước 2: lô đất ---------- */

const plotSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, { error: 'Nhập tên lô' })
    .max(60, { error: 'Tên tối đa 60 ký tự' }),
  areaM2: z
    .string()
    .refine((v) => Number(digitsOnly(v)) > 0, { error: 'Nhập diện tích lớn hơn 0' })
    .refine((v) => Number(digitsOnly(v)) <= AREA_MAX_M2, {
      error: `Diện tích tối đa ${formatNumber(AREA_MAX_M2)} m²`,
    }),
  soilType: z.string().trim().max(60, { error: 'Loại đất tối đa 60 ký tự' }),
});

function PlotStep({ farm, onDone }) {
  const { userId } = useSession();
  const queryClient = useQueryClient();
  const soilListId = useId();
  const form = useForm({
    resolver: zodResolver(plotSchema),
    defaultValues: { name: '', areaM2: '', soilType: '' },
  });
  const { register, handleSubmit, setError, control, formState } = form;
  const errors = formState.errors;
  const area = Number(digitsOnly(useWatch({ control, name: 'areaM2' })));

  const mutation = useMutation({
    ...WRITE,
    mutationFn: (body) => api.plots.create(farm.id, body),
    onSuccess: (plot) => {
      queryClient.setQueryData(['plots', userId, farm.id], (old = []) => [...old, plot]);
      // Số lô và tổng diện tích của nông trại vừa đổi
      ['plots', 'farms'].forEach((key) => queryClient.invalidateQueries({ queryKey: [key] }));
      onDone(`Đã thêm ${plot.name}, ${hectares.format(plot.areaM2 / 10_000)} ha.`);
    },
    onError: (error) => applyServerErrors(error, setError, ['name', 'areaM2', 'soilType']),
  });

  const onSubmit = handleSubmit((v) =>
    mutation.mutate({
      name: v.name,
      areaM2: Number(digitsOnly(v.areaM2)),
      soilType: v.soilType || null,
    }),
  );

  return (
    <StepForm
      onSubmit={onSubmit}
      footer={
        <>
          <SubmitError error={mutation.error} retryHint={RETRY_HINT} />
          <SubmitButton mutation={mutation} label="Thêm lô" />
        </>
      }
    >
      <Field
        id="plot-name"
        label="Tên lô"
        hint={`Tên lô không trùng nhau trong ${farm.name}.`}
        error={errors.name?.message}
      >
        {({ id, describedBy, invalid }) => (
          <input
            id={id}
            type="text"
            maxLength={60}
            autoComplete="off"
            placeholder="Ví dụ: Lô A1"
            aria-invalid={invalid}
            aria-describedby={describedBy}
            className={inputClass}
            {...register('name')}
          />
        )}
      </Field>
      <Field
        id="plot-area"
        label="Diện tích"
        hint={area > 0 ? `Bằng ${hectares.format(area / 10_000)} ha` : '1 ha = 10.000 m²'}
        error={errors.areaM2?.message}
      >
        {({ id, describedBy, invalid }) => (
          <Controller
            name="areaM2"
            control={control}
            render={({ field }) => (
              <DigitsInput
                id={id}
                value={field.value}
                onValueChange={field.onChange}
                onBlur={field.onBlur}
                ref={field.ref}
                suffix="m²"
                placeholder="10.000"
                aria-invalid={invalid}
                aria-describedby={describedBy}
              />
            )}
          />
        )}
      </Field>
      <Field id="plot-soil" label="Loại đất" optional error={errors.soilType?.message}>
        {({ id, describedBy, invalid }) => (
          <>
            <input
              id={id}
              type="text"
              maxLength={60}
              list={soilListId}
              placeholder="Ví dụ: Đất đỏ bazan"
              aria-invalid={invalid}
              aria-describedby={describedBy}
              className={inputClass}
              {...register('soilType')}
            />
            <datalist id={soilListId}>
              {SOIL_SUGGESTIONS.map((s) => (
                <option key={s} value={s} />
              ))}
            </datalist>
          </>
        )}
      </Field>
    </StepForm>
  );
}

/* ---------- Bước 3: lứa trồng ---------- */

function buildPlantingSchema(today) {
  return z.object({
    plotId: z.string().min(1, { error: 'Chọn lô để trồng' }),
    cropId: z.string().min(1, { error: 'Chọn loại cây' }),
    plantingDate: z
      .string()
      .min(1, { error: 'Chọn ngày trồng' })
      .refine((d) => d <= today, { error: 'Ngày trồng không được ở tương lai' }),
    treeCount: z
      .string()
      .refine((v) => Number(digitsOnly(v)) > 0, { error: 'Nhập số cây lớn hơn 0' })
      .refine((v) => Number(digitsOnly(v)) <= TREE_MAX, {
        error: `Số cây tối đa ${formatNumber(TREE_MAX)}`,
      }),
    alreadyProducing: z.boolean(),
  });
}

// Ô chọn dạng thẻ (lô, loại cây): cùng kiểu với form ghi hoạt động.
const choiceClass =
  'group flex min-h-14 cursor-pointer items-center gap-3 rounded-md border border-line bg-panel px-3 py-2 transition-colors hover:border-muted has-checked:border-leaf has-checked:bg-panel2 has-focus-visible:outline-2 has-focus-visible:outline-offset-2 has-focus-visible:outline-leaf';

function CheckMark() {
  return (
    <span
      aria-hidden="true"
      className="grid h-6 w-6 shrink-0 place-items-center rounded-full border border-line text-bg group-has-checked:border-leaf group-has-checked:bg-leaf"
    >
      <Icon name="check" size={14} className="opacity-0 group-has-checked:opacity-100" />
    </span>
  );
}

function ChoiceGroup({ legend, error, errorId, children }) {
  return (
    <fieldset
      className="min-w-0"
      aria-invalid={error ? true : undefined}
      aria-describedby={error ? errorId : undefined}
    >
      <legend className="mb-1.5 font-medium text-ink">{legend}</legend>
      {children}
      <FieldError id={errorId} message={error} />
    </fieldset>
  );
}

function PlantingStep({ farm, plots, onDone }) {
  const { userId } = useSession();
  const queryClient = useQueryClient();
  const crops = useCrops();
  const today = todayIso();
  const form = useForm({
    resolver: zodResolver(buildPlantingSchema(today)),
    defaultValues: {
      // Một lô thì chọn sẵn — không bắt bấm một lựa chọn duy nhất
      plotId: plots.length === 1 ? String(plots[0].id) : '',
      cropId: '',
      plantingDate: '',
      treeCount: '',
      alreadyProducing: false,
    },
  });
  const { register, handleSubmit, setError, control, formState } = form;
  const errors = formState.errors;

  const mutation = useMutation({
    ...WRITE,
    mutationFn: ({ plotId, body }) => api.plantings.plant(plotId, body),
    onSuccess: (planting) => {
      queryClient.setQueryData(['plantings', userId, farm.id, 'active'], (old = []) => [
        ...old,
        planting,
      ]);
      // Lứa mới kéo theo lời nhắc chăm sóc (BR-18) và một dòng báo cáo với số 0 (BR-14)
      ['plantings', 'reminders', 'seasons', 'profit-loss'].forEach((key) =>
        queryClient.invalidateQueries({ queryKey: [key] }),
      );
      onDone(`Đã trồng ${planting.cropName} trên ${planting.plotName}.`);
    },
    onError: (error) => applyServerErrors(error, setError, ['cropId', 'plantingDate', 'treeCount']),
  });

  const onSubmit = handleSubmit((v) =>
    mutation.mutate({
      plotId: Number(v.plotId),
      body: {
        cropId: Number(v.cropId),
        plantingDate: v.plantingDate,
        treeCount: Number(digitsOnly(v.treeCount)),
        alreadyProducing: v.alreadyProducing,
      },
    }),
  );

  // Xếp theo chữ cái tiếng Việt, các giống của cùng một cây nằm cạnh nhau
  const cropList = [...(crops.data ?? [])].sort((a, b) =>
    collator.compare(a.displayName ?? a.name, b.displayName ?? b.name),
  );

  return (
    <StepForm
      onSubmit={onSubmit}
      footer={
        <>
          <SubmitError error={mutation.error} retryHint={RETRY_HINT} />
          <SubmitButton mutation={mutation} label="Trồng cây" />
        </>
      }
    >
      {plots.length > 1 && (
        <ChoiceGroup legend="Lô" error={errors.plotId?.message} errorId="plant-plot-error">
          <div className="grid gap-2 sm:grid-cols-2">
            {plots.map((p) => (
              <label key={p.id} className={choiceClass}>
                <input
                  type="radio"
                  value={String(p.id)}
                  className="sr-only"
                  {...register('plotId')}
                />
                <span className="min-w-0 flex-1">
                  <span className="block truncate font-medium text-ink">{p.name}</span>
                  <span className="num block text-sm text-muted">{formatArea(p.areaM2)}</span>
                </span>
                <CheckMark />
              </label>
            ))}
          </div>
        </ChoiceGroup>
      )}

      <ChoiceGroup legend="Loại cây" error={errors.cropId?.message} errorId="plant-crop-error">
        {crops.isLoading ? (
          <LoadingBlock className="px-0" />
        ) : crops.error ? (
          <ErrorBlock className="px-0" error={crops.error} onRetry={crops.refetch} />
        ) : cropList.length === 0 ? (
          <EmptyBlock className="px-0 py-2">
            Danh mục cây trồng đang trống. Cần thêm loại cây vào danh mục trước.
          </EmptyBlock>
        ) : (
          <div className="grid gap-2 sm:grid-cols-2">
            {cropList.map((c) => (
              <label key={c.id} className={choiceClass}>
                <input
                  type="radio"
                  value={String(c.id)}
                  className="sr-only"
                  {...register('cropId')}
                />
                <CropDot cropName={c.name} size={12} />
                <span className="min-w-0 flex-1">
                  <span className="block truncate font-medium text-ink">{c.name}</span>
                  <span className="block truncate text-sm text-muted">
                    {[c.variety, c.perennial ? 'lâu năm' : 'ngắn ngày'].filter(Boolean).join(', ')}
                  </span>
                </span>
                <CheckMark />
              </label>
            ))}
          </div>
        )}
      </ChoiceGroup>

      <div className="grid gap-5 sm:grid-cols-2">
        <Field id="plant-date" label="Ngày trồng" error={errors.plantingDate?.message}>
          {({ id, describedBy, invalid }) => (
            <input
              id={id}
              type="date"
              max={today}
              aria-invalid={invalid}
              aria-describedby={describedBy}
              className={`num ${inputClass}`}
              {...register('plantingDate')}
            />
          )}
        </Field>
        <Field id="plant-trees" label="Số cây" error={errors.treeCount?.message}>
          {({ id, describedBy, invalid }) => (
            <Controller
              name="treeCount"
              control={control}
              render={({ field }) => (
                <DigitsInput
                  id={id}
                  value={field.value}
                  onValueChange={field.onChange}
                  onBlur={field.onBlur}
                  ref={field.ref}
                  suffix="cây"
                  placeholder="1.100"
                  aria-invalid={invalid}
                  aria-describedby={describedBy}
                />
              )}
            />
          )}
        </Field>
      </div>

      <label className="flex cursor-pointer items-start gap-3 rounded-md border border-line bg-panel px-3 py-3 has-checked:border-leaf has-focus-visible:outline-2 has-focus-visible:outline-offset-2 has-focus-visible:outline-leaf">
        <input
          type="checkbox"
          className="mt-0.5 h-5 w-5 shrink-0 accent-leaf"
          aria-describedby="plant-producing-hint"
          {...register('alreadyProducing')}
        />
        <span>
          <span className="block font-medium text-ink">Vườn đã cho thu hoạch</span>
          <span id="plant-producing-hint" className="block text-sm text-muted">
            Chọn khi đưa một vườn có sẵn vào sổ, ví dụ vườn cà phê đã thu hoạch nhiều vụ. Bỏ trống
            nếu cây mới trồng, còn đang kiến thiết cơ bản.
          </span>
        </span>
      </label>
    </StepForm>
  );
}

/* ---------- Khung ba bước ---------- */

function StepList({ step, farm, plots }) {
  const done = [farm?.name, plots.length ? `${plots.length} lô` : null];
  return (
    <ol className="grid grid-cols-3 gap-2 lg:grid-cols-1 lg:gap-0">
      {STEPS.map((s, i) => {
        const n = i + 1;
        const state = n < step ? 'done' : n === step ? 'current' : 'todo';
        return (
          <li
            key={s.title}
            aria-current={state === 'current' ? 'step' : undefined}
            className="relative flex flex-col items-start gap-2 lg:flex-row lg:gap-3 lg:pb-6 lg:last:pb-0"
          >
            {n < STEPS.length && (
              <span
                aria-hidden="true"
                className={`absolute top-9 bottom-1 left-4 hidden w-px lg:block ${
                  n < step ? 'bg-leaf' : 'bg-line'
                }`}
              />
            )}
            <span
              aria-hidden="true"
              className={`num grid h-8 w-8 shrink-0 place-items-center rounded-full border-2 font-display text-sm font-semibold ${
                state === 'done'
                  ? 'border-leaf bg-leaf text-bg'
                  : state === 'current'
                    ? 'border-leaf text-leaf'
                    : 'border-line text-muted'
              }`}
            >
              {state === 'done' ? <Icon name="check" size={16} /> : n}
            </span>
            <span className="min-w-0 lg:pt-1">
              <span
                className={`block text-sm leading-tight font-medium lg:text-base ${
                  state === 'todo' ? 'text-muted' : 'text-ink'
                }`}
              >
                {s.title}
              </span>
              <span className="mt-0.5 block truncate text-xs text-muted lg:text-sm">
                {state === 'done' ? done[i] : state === 'current' ? 'Đang làm' : 'Chưa tới'}
                <span className="sr-only">{state === 'done' ? ', đã xong' : ''}</span>
              </span>
            </span>
          </li>
        );
      })}
    </ol>
  );
}

export function GettingStarted({ step, farm, plots }) {
  const [message, setMessage] = useState(null);
  const headingRef = useRef(null);
  const firstRender = useRef(true);
  const current = STEPS[step - 1];

  // Sang bước mới thì đưa focus lên tiêu đề bước đó: người dùng bàn phím và trình đọc màn hình
  // biết form vừa đổi, không bị bỏ lại trên nút đã biến mất.
  useEffect(() => {
    if (firstRender.current) {
      firstRender.current = false;
      return;
    }
    headingRef.current?.focus();
  }, [step]);

  return (
    <div className="mx-auto max-w-5xl">
      <div className="mb-6">
        <h1 className="font-display text-2xl font-semibold text-ink md:text-3xl">Bắt đầu</h1>
        <p className="mt-1 max-w-2xl text-muted">
          Ba bước để có lời nhắc chăm sóc và báo cáo lãi/lỗ cho vườn của bạn.
        </p>
      </div>

      <div className="grid gap-6 lg:grid-cols-[16rem_1fr]">
        <nav aria-label="Các bước bắt đầu">
          <StepList step={step} farm={farm} plots={plots} />
        </nav>

        <section
          aria-labelledby="getting-started-step"
          className="rounded-lg border border-line bg-panel"
        >
          {/* Vùng thông báo luôn có mặt (kể cả khi rỗng) để trình đọc màn hình đọc được lần đổi đầu tiên */}
          <p role="status" className={message ? 'border-b border-line px-5 py-3 text-leaf' : ''}>
            {message}
          </p>
          <header className="px-5 pt-5">
            <p className="text-sm text-muted">
              Bước <span className="num">{step}</span> trên <span className="num">3</span>
              {farm && step > 1 ? `, ${farm.name}` : ''}
            </p>
            <h2
              id="getting-started-step"
              ref={headingRef}
              tabIndex={-1}
              className="mt-1 font-display text-xl font-semibold text-ink outline-none"
            >
              {current.title}
            </h2>
            <p className="mt-1 text-muted">{current.why}</p>
          </header>
          {step === 1 && <FarmStep onDone={setMessage} />}
          {/* key theo nông trại: đổi nông trại ở thanh trên thì form dựng lại từ đầu, không giữ lô
              của nông trại cũ (lô chọn sẵn còn bị ẩn khi nông trại mới chỉ có một lô) */}
          {step === 2 && <PlotStep key={farm.id} farm={farm} onDone={setMessage} />}
          {step === 3 && (
            <PlantingStep key={farm.id} farm={farm} plots={plots} onDone={setMessage} />
          )}
        </section>
      </div>
    </div>
  );
}
