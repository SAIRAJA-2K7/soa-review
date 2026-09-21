import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Menu, X, Zap } from 'lucide-react';
import Button from '@/components/ui/Button';
import ThemeToggle from '@/components/ui/ThemeToggle';
import { useAuth } from '@/context/AuthContext';
const NAV_LINKS = [
    { label: 'Jobs', href: '/jobs' },
    { label: 'Companies', href: '#companies' },
    { label: 'How it works', href: '#how-it-works' },
    { label: 'Platform', href: '#platform' },
    { label: 'About', href: '#about' },
];
export default function Navbar() {
    const [mobileOpen, setMobileOpen] = useState(false);
    const [scrolled, setScrolled] = useState(false);
    const { user } = useAuth();
    const navigate = useNavigate();
    useEffect(() => {
        const handler = () => setScrolled(window.scrollY > 20);
        window.addEventListener('scroll', handler);
        return () => window.removeEventListener('scroll', handler);
    }, []);
    useEffect(() => {
        if (mobileOpen)
            document.body.style.overflow = 'hidden';
        else
            document.body.style.overflow = '';
        return () => { document.body.style.overflow = ''; };
    }, [mobileOpen]);
    return (_jsxs(_Fragment, { children: [_jsx("header", { className: `fixed top-0 left-0 right-0 z-40 transition-all duration-300 ${scrolled ? 'py-3' : 'py-5'}`, style: {
                    background: scrolled ? 'var(--surface)' : 'transparent',
                    borderBottom: scrolled ? '1px solid var(--border)' : 'none',
                    boxShadow: scrolled ? 'var(--elevation-1)' : 'none',
                    backdropFilter: scrolled ? 'blur(12px)' : 'none',
                }, children: _jsxs("nav", { className: "max-w-7xl mx-auto px-5 lg:px-8 flex items-center justify-between", children: [_jsxs(Link, { to: "/", className: "flex items-center gap-2.5 group", children: [_jsx("div", { className: "w-8 h-8 rounded-lg flex items-center justify-center", style: { background: 'var(--accent)' }, children: _jsx(Zap, { size: 16, className: "text-white", fill: "white" }) }), _jsx("span", { className: "font-display font-bold text-base tracking-tight", style: { color: 'var(--text-primary)', fontFamily: 'Plus Jakarta Sans, sans-serif' }, children: "TalentSync" })] }), _jsx("ul", { className: "hidden md:flex items-center gap-0.5", children: NAV_LINKS.map(link => (_jsx("li", { children: _jsx(Link, { to: link.href, className: "px-3.5 py-2 rounded-lg text-sm font-medium transition-colors hover:bg-[var(--surface-elevated)]", style: { color: 'var(--text-secondary)' }, children: link.label }) }, link.label))) }), _jsxs("div", { className: "hidden md:flex items-center gap-2", children: [_jsx(ThemeToggle, {}), user ? (_jsx(Button, { size: "sm", onClick: () => navigate(user.role === 'recruiter' ? '/recruiter' : '/dashboard'), children: "Go to Dashboard" })) : (_jsxs(_Fragment, { children: [_jsx(Link, { to: "/login", className: "px-4 py-2 text-sm font-medium rounded-lg transition-colors hover:bg-[var(--surface-elevated)]", style: { color: 'var(--text-secondary)' }, children: "Sign in" }), _jsx(Button, { size: "sm", onClick: () => navigate('/register'), children: "Get started" })] }))] }), _jsxs("div", { className: "flex md:hidden items-center gap-2", children: [_jsx(ThemeToggle, { compact: true }), _jsx("button", { onClick: () => setMobileOpen(true), className: "p-2 rounded-lg transition-colors hover:bg-[var(--surface-elevated)]", style: { color: 'var(--text-secondary)' }, "aria-label": "Open menu", children: _jsx(Menu, { size: 20 }) })] })] }) }), _jsxs("div", { className: `fixed inset-0 z-50 md:hidden transition-opacity duration-300 ${mobileOpen ? 'opacity-100 pointer-events-auto' : 'opacity-0 pointer-events-none'}`, children: [_jsx("div", { className: "absolute inset-0 bg-black/60", onClick: () => setMobileOpen(false) }), _jsxs("div", { className: `absolute inset-y-0 right-0 w-full max-w-xs flex flex-col transition-transform duration-300 ${mobileOpen ? 'translate-x-0' : 'translate-x-full'}`, style: { background: 'var(--surface)', borderLeft: '1px solid var(--border)' }, children: [_jsxs("div", { className: "flex items-center justify-between px-5 py-4 border-b", style: { borderColor: 'var(--border)' }, children: [_jsxs("div", { className: "flex items-center gap-2", children: [_jsx("div", { className: "w-7 h-7 rounded-lg flex items-center justify-center", style: { background: 'var(--accent)' }, children: _jsx(Zap, { size: 14, className: "text-white", fill: "white" }) }), _jsx("span", { className: "font-semibold text-sm", style: { color: 'var(--text-primary)', fontFamily: 'Plus Jakarta Sans, sans-serif' }, children: "TalentSync" })] }), _jsx("button", { onClick: () => setMobileOpen(false), className: "p-2 rounded-lg hover:bg-[var(--surface-elevated)]", style: { color: 'var(--text-muted)' }, "aria-label": "Close menu", children: _jsx(X, { size: 18 }) })] }), _jsx("nav", { className: "flex-1 overflow-y-auto px-4 py-6 flex flex-col gap-1", children: NAV_LINKS.map(link => (_jsx(Link, { to: link.href, onClick: () => setMobileOpen(false), className: "px-4 py-3 rounded-xl text-sm font-medium transition-colors hover:bg-[var(--surface-elevated)]", style: { color: 'var(--text-secondary)' }, children: link.label }, link.label))) }), _jsx("div", { className: "px-4 pb-6 flex flex-col gap-2.5 border-t pt-4", style: { borderColor: 'var(--border)' }, children: user ? (_jsx(Button, { onClick: () => { navigate(user.role === 'recruiter' ? '/recruiter' : '/dashboard'); setMobileOpen(false); }, children: "Go to Dashboard" })) : (_jsxs(_Fragment, { children: [_jsx(Button, { variant: "secondary", onClick: () => { navigate('/login'); setMobileOpen(false); }, children: "Sign in" }), _jsx(Button, { onClick: () => { navigate('/register'); setMobileOpen(false); }, children: "Get started" })] })) })] })] })] }));
}
