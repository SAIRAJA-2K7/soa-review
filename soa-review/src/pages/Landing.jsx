import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import Navbar from '@/components/landing/Navbar';
import Hero from '@/components/landing/Hero';
import Features from '@/components/landing/Features';
import Stats from '@/components/landing/Stats';
import Footer from '@/components/landing/Footer';
import { useNavigate } from 'react-router-dom';
import Button from '@/components/ui/Button';
import { ArrowRight } from 'lucide-react';
export default function Landing() {
    const navigate = useNavigate();
    return (_jsxs("div", { style: { background: 'var(--background)' }, children: [_jsx(Navbar, {}), _jsx(Hero, {}), _jsx(Features, {}), _jsx(Stats, {}), _jsx("section", { className: "py-24 lg:py-32", style: { background: 'var(--background)' }, children: _jsxs("div", { className: "max-w-3xl mx-auto px-5 text-center", children: [_jsx("div", { className: "text-xs font-mono font-medium tracking-widest mb-4 uppercase", style: { color: 'var(--accent)' }, children: "Get Started Today" }), _jsxs("h2", { className: "text-3xl lg:text-4xl font-extrabold leading-tight mb-5", style: { color: 'var(--text-primary)', fontFamily: 'Plus Jakarta Sans, sans-serif' }, children: ["Ready to transform your", _jsx("br", {}), "talent acquisition?"] }), _jsx("p", { className: "text-base mb-10", style: { color: 'var(--text-secondary)' }, children: "Join 3,200+ companies using TalentSync to build exceptional teams faster." }), _jsxs("div", { className: "flex flex-wrap justify-center gap-4", children: [_jsxs(Button, { size: "lg", onClick: () => navigate('/register'), className: "group gap-2", children: ["Start for free", _jsx(ArrowRight, { size: 16, className: "group-hover:translate-x-1 transition-transform" })] }), _jsx(Button, { size: "lg", variant: "secondary", onClick: () => navigate('/login'), children: "Sign in" })] }), _jsx("p", { className: "mt-6 text-sm", style: { color: 'var(--text-muted)' }, children: "No credit card required \u00B7 14-day free trial" })] }) }), _jsx(Footer, {})] }));
}
