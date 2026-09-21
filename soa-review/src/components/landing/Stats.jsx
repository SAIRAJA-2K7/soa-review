import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { TrendingUp, Clock, Star, Building2 } from 'lucide-react';
const STATS = [
    { icon: Building2, value: '3,200+', label: 'Enterprise Clients', sub: 'Across 40 countries' },
    { icon: Clock, value: '34%', label: 'Faster Hiring', sub: 'Average time-to-hire reduction' },
    { icon: TrendingUp, value: '89%', label: 'Offer Acceptance', sub: 'Industry avg. 67%' },
    { icon: Star, value: '4.9/5', label: 'Client Satisfaction', sub: 'Based on 2,400+ reviews' },
];
const TESTIMONIALS = [
    {
        quote: "TalentSync transformed how we hire. We cut our time-to-hire by 40% in the first quarter.",
        author: "Rachel Chen", role: "VP Talent Acquisition", company: "Veritas Capital",
    },
    {
        quote: "The pipeline visibility alone is worth the investment. We finally know where every candidate stands.",
        author: "Marcus Webb", role: "Head of Recruiting", company: "Nexus AI",
    },
    {
        quote: "Best-in-class candidate experience. Our offer acceptance rate jumped to 94% within 6 months.",
        author: "Jennifer Park", role: "Chief People Officer", company: "Orbital Systems",
    },
];
export default function Stats() {
    return (_jsx("section", { id: "how-it-works", className: "py-24 lg:py-32", style: { background: 'var(--surface)' }, children: _jsxs("div", { className: "max-w-7xl mx-auto px-5 lg:px-8", children: [_jsx("div", { className: "grid grid-cols-2 lg:grid-cols-4 gap-5 mb-20", children: STATS.map(s => (_jsxs("div", { className: "p-6 rounded-2xl border", style: { background: 'var(--background)', borderColor: 'var(--border)' }, children: [_jsx("div", { className: "w-9 h-9 rounded-xl flex items-center justify-center mb-4", style: { background: 'var(--accent-light)', color: 'var(--accent)' }, children: _jsx(s.icon, { size: 18 }) }), _jsx("div", { className: "text-2xl lg:text-3xl font-extrabold mb-1", style: { color: 'var(--text-primary)', fontFamily: 'Plus Jakarta Sans, sans-serif' }, children: s.value }), _jsx("div", { className: "text-sm font-medium mb-0.5", style: { color: 'var(--text-primary)' }, children: s.label }), _jsx("div", { className: "text-xs", style: { color: 'var(--text-muted)' }, children: s.sub })] }, s.label))) }), _jsx("div", { className: "grid md:grid-cols-3 gap-5", children: TESTIMONIALS.map(t => (_jsxs("div", { className: "p-6 rounded-2xl border", style: { background: 'var(--background)', borderColor: 'var(--border)' }, children: [_jsx("div", { className: "text-2xl mb-4", style: { color: 'var(--accent)' }, children: "\"" }), _jsx("p", { className: "text-sm leading-relaxed mb-5", style: { color: 'var(--text-secondary)' }, children: t.quote }), _jsxs("div", { children: [_jsx("div", { className: "text-sm font-semibold", style: { color: 'var(--text-primary)', fontFamily: 'Plus Jakarta Sans, sans-serif' }, children: t.author }), _jsxs("div", { className: "text-xs mt-0.5", style: { color: 'var(--text-muted)' }, children: [t.role, " \u00B7 ", t.company] })] })] }, t.author))) })] }) }));
}
