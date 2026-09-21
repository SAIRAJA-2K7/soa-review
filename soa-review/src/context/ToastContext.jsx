import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { createContext, useContext, useState, useCallback } from 'react';
import { CheckCircle, XCircle, AlertCircle, Info, X } from 'lucide-react';
const ToastContext = createContext(undefined);
export function ToastProvider({ children }) {
    const [toasts, setToasts] = useState([]);
    const toast = useCallback((type, title, message) => {
        const id = Date.now().toString();
        setToasts(prev => [...prev, { id, type, title, message }]);
        setTimeout(() => setToasts(prev => prev.filter(t => t.id !== id)), 4000);
    }, []);
    const dismiss = useCallback((id) => {
        setToasts(prev => prev.filter(t => t.id !== id));
    }, []);
    const icons = { success: CheckCircle, error: XCircle, warning: AlertCircle, info: Info };
    const colors = {
        success: 'border-green-500/30 bg-green-500/10 text-green-400',
        error: 'border-red-500/30 bg-red-500/10 text-red-400',
        warning: 'border-yellow-500/30 bg-yellow-500/10 text-yellow-400',
        info: 'border-blue-500/30 bg-blue-500/10 text-blue-400',
    };
    return (_jsxs(ToastContext.Provider, { value: { toasts, toast, dismiss }, children: [children, _jsx("div", { className: "fixed bottom-6 right-6 z-[100] flex flex-col gap-3 max-w-sm w-full", children: toasts.map(t => {
                    const Icon = icons[t.type];
                    return (_jsxs("div", { className: `flex items-start gap-3 p-4 rounded-xl border backdrop-blur-sm animate-slide-up shadow-lg ${colors[t.type]}`, style: { background: 'var(--surface)', borderColor: 'var(--border-strong)', color: 'var(--text-primary)' }, children: [_jsx(Icon, { size: 18, className: colors[t.type].split(' ').find(c => c.startsWith('text-')) || '', style: { flexShrink: 0, marginTop: 1 } }), _jsxs("div", { className: "flex-1 min-w-0", children: [_jsx("p", { className: "text-sm font-medium", style: { color: 'var(--text-primary)' }, children: t.title }), t.message && _jsx("p", { className: "text-xs mt-0.5", style: { color: 'var(--text-secondary)' }, children: t.message })] }), _jsx("button", { onClick: () => dismiss(t.id), className: "shrink-0 hover:opacity-70 transition-opacity", style: { color: 'var(--text-muted)' }, children: _jsx(X, { size: 14 }) })] }, t.id));
                }) })] }));
}
export function useToast() {
    const ctx = useContext(ToastContext);
    if (!ctx)
        throw new Error('useToast must be used within ToastProvider');
    return ctx.toast;
}
