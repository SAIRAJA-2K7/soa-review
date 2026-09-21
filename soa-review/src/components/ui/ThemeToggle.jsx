import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Sun, Moon, Monitor } from 'lucide-react';
import { useTheme } from '@/context/ThemeContext';
import { useState } from 'react';
export default function ThemeToggle({ compact = false }) {
    const { theme, setTheme } = useTheme();
    const [open, setOpen] = useState(false);
    if (compact) {
        return (_jsx("button", { onClick: () => setTheme(theme === 'dark' ? 'light' : theme === 'light' ? 'system' : 'dark'), className: "p-2 rounded-lg transition-all hover:bg-[var(--surface-elevated)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--accent)]", "aria-label": `Current theme: ${theme}. Click to change.`, title: `Theme: ${theme}`, children: theme === 'dark' ? _jsx(Moon, { size: 16, style: { color: 'var(--accent)' } }) :
                theme === 'light' ? _jsx(Sun, { size: 16, style: { color: 'var(--warning)' } }) :
                    _jsx(Monitor, { size: 16, style: { color: 'var(--text-secondary)' } }) }));
    }
    return (_jsxs("div", { className: "relative", children: [_jsx("button", { onClick: () => setOpen(!open), onBlur: () => setTimeout(() => setOpen(false), 150), className: "flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm transition-all hover:bg-[var(--surface-elevated)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--accent)]", style: { color: 'var(--text-secondary)' }, "aria-label": "Toggle theme", children: theme === 'dark' ? _jsx(Moon, { size: 15, style: { color: 'var(--accent)' } }) :
                    theme === 'light' ? _jsx(Sun, { size: 15, style: { color: 'var(--warning)' } }) :
                        _jsx(Monitor, { size: 15 }) }), open && (_jsx("div", { className: "absolute right-0 top-full mt-1 w-36 rounded-xl border shadow-xl z-50 overflow-hidden", style: { background: 'var(--surface)', borderColor: 'var(--border-strong)', boxShadow: 'var(--elevation-3)' }, children: ['light', 'dark', 'system'].map(t => (_jsxs("button", { onClick: () => { setTheme(t); setOpen(false); }, className: `w-full flex items-center gap-2.5 px-3 py-2.5 text-sm transition-colors hover:bg-[var(--surface-elevated)] ${theme === t ? 'text-[var(--accent)]' : ''}`, style: { color: theme === t ? 'var(--accent)' : 'var(--text-secondary)' }, children: [t === 'light' && _jsx(Sun, { size: 14 }), t === 'dark' && _jsx(Moon, { size: 14 }), t === 'system' && _jsx(Monitor, { size: 14 }), _jsx("span", { className: "capitalize font-medium", children: t }), theme === t && _jsx("span", { className: "ml-auto text-xs", children: "\u2713" })] }, t))) }))] }));
}
