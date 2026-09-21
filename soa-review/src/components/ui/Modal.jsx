import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { X } from 'lucide-react';
import { useEffect } from 'react';
export default function Modal({ open, onClose, title, children, size = 'md' }) {
    useEffect(() => {
        if (open) {
            document.body.style.overflow = 'hidden';
        }
        else {
            document.body.style.overflow = '';
        }
        return () => { document.body.style.overflow = ''; };
    }, [open]);
    useEffect(() => {
        const handle = (e) => { if (e.key === 'Escape')
            onClose(); };
        document.addEventListener('keydown', handle);
        return () => document.removeEventListener('keydown', handle);
    }, [onClose]);
    if (!open)
        return null;
    const widths = { sm: 'max-w-sm', md: 'max-w-lg', lg: 'max-w-2xl', xl: 'max-w-4xl' };
    return (_jsxs("div", { className: "fixed inset-0 z-50 flex items-center justify-center p-4", role: "dialog", "aria-modal": "true", children: [_jsx("div", { className: "absolute inset-0 bg-black/50 backdrop-blur-sm", onClick: onClose }), _jsxs("div", { className: `relative w-full ${widths[size]} rounded-2xl border animate-slide-up`, style: { background: 'var(--surface)', borderColor: 'var(--border-strong)', boxShadow: 'var(--elevation-3)' }, children: [title && (_jsxs("div", { className: "flex items-center justify-between px-6 py-4 border-b", style: { borderColor: 'var(--border)' }, children: [_jsx("h2", { className: "text-base font-semibold font-display", style: { color: 'var(--text-primary)', fontFamily: 'var(--font-display)' }, children: title }), _jsx("button", { onClick: onClose, className: "p-1.5 rounded-lg hover:bg-[var(--surface-elevated)] transition-colors", style: { color: 'var(--text-muted)' }, "aria-label": "Close", children: _jsx(X, { size: 16 }) })] })), _jsx("div", { className: "p-6", children: children })] })] }));
}
