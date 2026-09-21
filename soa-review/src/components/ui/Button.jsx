import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { cn } from '@/utils/cn';
import { Loader2 } from 'lucide-react';
export default function Button({ variant = 'primary', size = 'md', loading, className, children, disabled, ...props }) {
    const base = 'inline-flex items-center justify-center gap-2 font-medium rounded-lg transition-all duration-150 focus-visible:outline-none focus-visible:ring-2 disabled:opacity-50 disabled:cursor-not-allowed select-none';
    const variants = {
        primary: 'bg-[var(--accent)] text-white hover:bg-[var(--accent-hover)] active:scale-[0.98] shadow-sm',
        secondary: 'bg-[var(--surface-elevated)] text-[var(--text-primary)] border border-[var(--border-strong)] hover:bg-[var(--surface-hover)] active:scale-[0.98]',
        ghost: 'text-[var(--text-secondary)] hover:bg-[var(--surface-elevated)] hover:text-[var(--text-primary)] active:scale-[0.98]',
        danger: 'bg-[var(--error)] text-white hover:opacity-90 active:scale-[0.98] shadow-sm',
        outline: 'border border-[var(--accent)] text-[var(--accent)] hover:bg-[var(--accent-light)] active:scale-[0.98]',
    };
    const sizes = {
        sm: 'px-3 py-1.5 text-xs gap-1.5',
        md: 'px-4 py-2 text-sm',
        lg: 'px-6 py-3 text-base',
    };
    return (_jsxs("button", { className: cn(base, variants[variant], sizes[size], className), disabled: disabled || loading, style: { fontFamily: 'var(--font-display)' }, ...props, children: [loading && _jsx(Loader2, { size: 14, className: "animate-spin" }), children] }));
}
