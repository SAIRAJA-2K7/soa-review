import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { LayoutDashboard, Search, FileText, Calendar, Bookmark, User, Settings, LogOut, Menu, X, Zap, Bell } from 'lucide-react';
import { useAuth } from '@/context/AuthContext';
import ThemeToggle from '@/components/ui/ThemeToggle';
const NAV_ITEMS = [
    { icon: LayoutDashboard, label: 'Dashboard', href: '/dashboard' },
    { icon: Search, label: 'Find Jobs', href: '/jobs' },
    { icon: FileText, label: 'Applications', href: '/applications' },
    { icon: Calendar, label: 'Interviews', href: '/interviews' },
    { icon: Bookmark, label: 'Saved Jobs', href: '/saved' },
    { icon: User, label: 'Profile', href: '/profile' },
    { icon: Settings, label: 'Settings', href: '/settings' },
];
export default function DashboardLayout({ children }) {
    const [sidebarOpen, setSidebarOpen] = useState(false);
    const { user, logout } = useAuth();
    const location = useLocation();
    const navigate = useNavigate();
    function handleLogout() {
        logout();
        navigate('/');
    }
    const SidebarContent = () => (_jsxs("div", { className: "flex flex-col h-full", children: [_jsx("div", { className: "px-4 py-5 border-b", style: { borderColor: 'var(--sidebar-border)' }, children: _jsxs(Link, { to: "/", className: "flex items-center gap-2.5", children: [_jsx("div", { className: "w-7 h-7 rounded-lg flex items-center justify-center", style: { background: 'var(--accent)' }, children: _jsx(Zap, { size: 14, className: "text-white", fill: "white" }) }), _jsx("span", { className: "font-bold text-sm", style: { color: 'var(--text-primary)', fontFamily: 'Plus Jakarta Sans, sans-serif' }, children: "TalentSync" })] }) }), _jsx("nav", { className: "flex-1 px-3 py-4 space-y-0.5 overflow-y-auto", children: NAV_ITEMS.map(item => {
                    const active = location.pathname === item.href || (item.href !== '/dashboard' && location.pathname.startsWith(item.href));
                    return (_jsxs(Link, { to: item.href, onClick: () => setSidebarOpen(false), className: `flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all group ${active ? 'text-white' : 'hover:bg-[var(--surface-elevated)]'}`, style: {
                            background: active ? 'var(--accent)' : 'transparent',
                            color: active ? 'white' : 'var(--text-secondary)',
                        }, children: [_jsx(item.icon, { size: 16 }), item.label] }, item.href));
                }) }), _jsxs("div", { className: "px-3 py-4 border-t", style: { borderColor: 'var(--sidebar-border)' }, children: [_jsxs("div", { className: "flex items-center gap-3 px-3 py-2.5 mb-2 rounded-xl", style: { background: 'var(--surface-elevated)' }, children: [_jsx("div", { className: "w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold text-white flex-shrink-0", style: { background: 'var(--accent)' }, children: user?.name?.[0] || 'U' }), _jsxs("div", { className: "flex-1 min-w-0", children: [_jsx("div", { className: "text-xs font-semibold truncate", style: { color: 'var(--text-primary)', fontFamily: 'Plus Jakarta Sans, sans-serif' }, children: user?.name }), _jsx("div", { className: "text-[10px] truncate capitalize", style: { color: 'var(--text-muted)' }, children: user?.role })] })] }), _jsxs("button", { onClick: handleLogout, className: "w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm transition-colors hover:bg-[var(--surface-elevated)]", style: { color: 'var(--text-muted)' }, children: [_jsx(LogOut, { size: 16 }), "Sign out"] })] })] }));
    return (_jsxs("div", { className: "min-h-screen flex", style: { background: 'var(--background)' }, children: [_jsx("aside", { className: "hidden lg:flex w-56 flex-col fixed inset-y-0 left-0 z-30 border-r", style: { background: 'var(--sidebar-bg)', borderColor: 'var(--sidebar-border)' }, children: _jsx(SidebarContent, {}) }), _jsxs("div", { className: `lg:hidden fixed inset-0 z-50 transition-opacity duration-300 ${sidebarOpen ? 'opacity-100 pointer-events-auto' : 'opacity-0 pointer-events-none'}`, children: [_jsx("div", { className: "absolute inset-0 bg-black/50", onClick: () => setSidebarOpen(false) }), _jsxs("aside", { className: `absolute inset-y-0 left-0 w-64 border-r transition-transform duration-300 ${sidebarOpen ? 'translate-x-0' : '-translate-x-full'}`, style: { background: 'var(--sidebar-bg)', borderColor: 'var(--sidebar-border)' }, children: [_jsx("button", { className: "absolute top-4 right-4", onClick: () => setSidebarOpen(false), style: { color: 'var(--text-muted)' }, "aria-label": "Close", children: _jsx(X, { size: 18 }) }), _jsx(SidebarContent, {})] })] }), _jsxs("div", { className: "flex-1 lg:ml-56 min-w-0", children: [_jsxs("header", { className: "sticky top-0 z-20 h-14 flex items-center justify-between px-5 border-b", style: { background: 'var(--surface)', borderColor: 'var(--border)' }, children: [_jsx("button", { className: "lg:hidden p-2 rounded-lg hover:bg-[var(--surface-elevated)]", onClick: () => setSidebarOpen(true), style: { color: 'var(--text-secondary)' }, "aria-label": "Open menu", children: _jsx(Menu, { size: 18 }) }), _jsx("div", { className: "hidden lg:block text-sm font-medium capitalize", style: { color: 'var(--text-secondary)', fontFamily: 'Plus Jakarta Sans, sans-serif' }, children: NAV_ITEMS.find(n => location.pathname.startsWith(n.href))?.label || 'Dashboard' }), _jsxs("div", { className: "flex items-center gap-2 ml-auto", children: [_jsx(ThemeToggle, { compact: true }), _jsxs("button", { className: "p-2 rounded-lg hover:bg-[var(--surface-elevated)] relative", style: { color: 'var(--text-secondary)' }, "aria-label": "Notifications", children: [_jsx(Bell, { size: 16 }), _jsx("span", { className: "absolute top-1.5 right-1.5 w-1.5 h-1.5 rounded-full", style: { background: 'var(--accent)' } })] })] })] }), _jsx("main", { className: "p-5 lg:p-8 max-w-[1200px] mx-auto", children: children })] })] }));
}
