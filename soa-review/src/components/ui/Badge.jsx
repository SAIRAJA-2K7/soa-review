import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { cn } from '@/utils/cn';
const styles = {
    default: 'bg-[var(--surface-elevated)] text-[var(--text-secondary)] border-[var(--border)]',
    success: 'bg-[var(--success-light)] text-[var(--success)] border-[var(--success)]/20',
    warning: 'bg-[var(--warning-light)] text-[var(--warning)] border-[var(--warning)]/20',
    error: 'bg-[var(--error-light)] text-[var(--error)] border-[var(--error)]/20',
    info: 'bg-[var(--info-light)] text-[var(--info)] border-[var(--info)]/20',
    accent: 'bg-[var(--accent-light)] text-[var(--accent)] border-[var(--accent)]/20',
    muted: 'bg-[var(--muted)] text-[var(--muted-foreground)] border-transparent',
};
const dotColors = {
    default: 'bg-[var(--text-muted)]',
    success: 'bg-[var(--success)]',
    warning: 'bg-[var(--warning)]',
    error: 'bg-[var(--error)]',
    info: 'bg-[var(--info)]',
    accent: 'bg-[var(--accent)]',
    muted: 'bg-[var(--text-muted)]',
};
export default function Badge({ variant = 'default', children, className, dot }) {
    return (_jsxs("span", { className: cn('inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium border', styles[variant], className), children: [dot && _jsx("span", { className: cn('w-1.5 h-1.5 rounded-full flex-shrink-0', dotColors[variant]) }), children] }));
}
export function statusBadge(status) {
    const map = {
        applied: { label: 'Applied', variant: 'info' },
        under_review: { label: 'Under Review', variant: 'default' },
        shortlisted: { label: 'Shortlisted', variant: 'accent' },
        interview: { label: 'Interview', variant: 'warning' },
        final_review: { label: 'Final Review', variant: 'warning' },
        selected: { label: 'Selected', variant: 'success' },
        rejected: { label: 'Rejected', variant: 'error' },
        upcoming: { label: 'Upcoming', variant: 'accent' },
        completed: { label: 'Completed', variant: 'success' },
        scheduled: { label: 'Scheduled', variant: 'info' },
        cancelled: { label: 'Cancelled', variant: 'error' },
        draft: { label: 'Draft', variant: 'muted' },
        published: { label: 'Published', variant: 'success' },
        closed: { label: 'Closed', variant: 'default' },
    };
    const config = map[status] || { label: status, variant: 'default' };
    return _jsx(Badge, { variant: config.variant, dot: true, children: config.label });
}
