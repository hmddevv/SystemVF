import { Link } from 'react-router';
import { Panel } from '../components/ui';

// Màn chưa làm — nói thẳng là chưa có, thay vì dựng giao diện giả.
export function ComingSoonPage({ title, milestone = 'M6b', children }) {
  return (
    <div className="mx-auto max-w-[1400px]">
      <h1 className="mb-5 font-display text-2xl font-semibold text-ink md:text-3xl">{title}</h1>
      <Panel className="max-w-xl p-4">
        <p className="text-ink">Màn này làm ở {milestone}.</p>
        {children && <p className="mt-2 text-muted">{children}</p>}
        <Link to="/" className="mt-4 inline-flex min-h-11 items-center text-leaf underline">
          Về Tổng quan
        </Link>
      </Panel>
    </div>
  );
}

export function NotFoundPage() {
  return <ComingSoonPage title="Không có trang này" milestone="—" />;
}
