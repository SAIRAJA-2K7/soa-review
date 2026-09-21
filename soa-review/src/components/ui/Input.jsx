import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { cn } from '@/utils/cn';
import { forwardRef } from 'react';
const Input = forwardRef(({ label, error, icon, suffix, className, id, ...props }, ref) => {
    return (_jsxs("div", { className: "flex flex-col gap-1.5", children: [label && (_jsx("label", { htmlFor: id, className: "text-sm font-medium", style: { color: 'var(--text-secondary)', fontFamily: 'var(--font-display)' }, children: label })), _jsxs("div", { className: "relative flex items-center", children: [icon && (_jsx("span", { className: "absolute left-3 pointer-events-none", style: { color: 'var(--text-muted)' }, children: icon })), _jsx("input", { ref: ref, id: id, className: cn('w-full rounded-lg border text-sm transition-all duration-150 bg-[var(--surface)] text-[var(--text-primary)] placeholder-[var(--text-muted)]', 'border-[var(--border-strong)] focus:border-[var(--accent)] focus:outline-none focus:ring-2 focus:ring-[var(--accent)]/20', error && 'border-[var(--error)] focus:ring-[var(--error)]/20', icon ? 'pl-10' : 'pl-3.5', suffix ? 'pr-10' : 'pr-3.5', 'py-2.5', className), ...props }), suffix && (_jsx("span", { className: "absolute right-3", style: { color: 'var(--text-muted)' }, children: suffix }))] }), error && _jsx("p", { className: "text-xs", style: { color: 'var(--error)' }, children: error })] }));
});
Input.displayName = 'Input';
export default Input;
