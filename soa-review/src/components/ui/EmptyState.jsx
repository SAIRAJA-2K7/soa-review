import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import Button from './Button';
export default function EmptyState({ icon, title, description, action }) {
    return (_jsxs("div", { className: "flex flex-col items-center justify-center py-16 px-6 text-center", children: [_jsx("div", { className: "w-14 h-14 rounded-2xl flex items-center justify-center mb-4", style: { background: 'var(--accent-light)', color: 'var(--accent)' }, children: icon }), _jsx("h3", { className: "text-base font-semibold mb-1.5", style: { color: 'var(--text-primary)', fontFamily: 'var(--font-display)' }, children: title }), _jsx("p", { className: "text-sm max-w-xs", style: { color: 'var(--text-muted)' }, children: description }), action && (_jsx("div", { className: "mt-5", children: _jsx(Button, { onClick: action.onClick, size: "sm", children: action.label }) }))] }));
}
