import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Users, BarChart3, Calendar, Search, Shield, Zap, BrainCircuit, Globe } from 'lucide-react';
const FEATURES = [
    {
        icon: BrainCircuit,
        title: 'Intelligent Pipeline Orchestration',
        description: 'Automated workflow management moves candidates through stages with smart triggers, notifications, and decision support.',
        color: '#6366f1',
    },
    {
        icon: Search,
        title: 'Advanced Candidate Discovery',
        description: 'Semantic search across skills, experience, and location. Find the right candidates before they even apply.',
        color: '#8b5cf6',
    },
    {
        icon: Calendar,
        title: 'Seamless Interview Coordination',
        description: 'Integrated scheduling eliminates the back-and-forth. Automated reminders reduce no-shows by 60%.',
        color: '#a78bfa',
    },
    {
        icon: BarChart3,
        title: 'Recruitment Intelligence',
        description: 'Real-time analytics on pipeline health, conversion rates, time-to-hire, and sourcing effectiveness.',
        color: '#f59e0b',
    },
    {
        icon: Users,
        title: 'Collaborative Hiring',
        description: 'Structured scorecards, interviewer calibration, and consensus hiring tools keep teams aligned.',
        color: '#10b981',
    },
    {
        icon: Globe,
        title: 'Multi-Channel Publishing',
        description: 'Publish to 40+ job boards simultaneously with intelligent distribution and performance tracking.',
        color: '#0284c7',
    },
    {
        icon: Shield,
        title: 'Enterprise Security',
        description: 'SOC 2 Type II certified. GDPR and CCPA compliant. Role-based access with full audit trails.',
        color: '#6366f1',
    },
    {
        icon: Zap,
        title: 'Workflow Automation',
        description: 'Custom automation rules for every stage. Reduce manual work by 70% without losing the human touch.',
        color: '#8b5cf6',
    },
];
export default function Features() {
    return (_jsx("section", { id: "platform", className: "py-24 lg:py-32", style: { background: 'var(--background)' }, children: _jsxs("div", { className: "max-w-7xl mx-auto px-5 lg:px-8", children: [_jsxs("div", { className: "max-w-xl mb-16", children: [_jsx("div", { className: "text-xs font-mono font-medium tracking-widest mb-4 uppercase", style: { color: 'var(--accent)' }, children: "Platform Capabilities" }), _jsx("h2", { className: "text-3xl lg:text-4xl font-extrabold leading-tight mb-4", style: { color: 'var(--text-primary)', fontFamily: 'Plus Jakarta Sans, sans-serif' }, children: "Built for enterprise hiring at scale" }), _jsx("p", { className: "text-base leading-relaxed", style: { color: 'var(--text-secondary)' }, children: "Every feature designed to reduce time-to-hire, improve candidate experience, and give your team the intelligence to make better decisions." })] }), _jsx("div", { className: "grid sm:grid-cols-2 lg:grid-cols-4 gap-5", children: FEATURES.map((f, i) => (_jsxs("div", { className: "group p-5 rounded-2xl border transition-all duration-300 hover:shadow-lg cursor-default", style: {
                            background: 'var(--surface)',
                            borderColor: 'var(--border)',
                            boxShadow: 'var(--card-shadow)',
                            animationDelay: `${i * 50}ms`,
                        }, onMouseEnter: e => {
                            e.currentTarget.style.borderColor = `${f.color}40`;
                            e.currentTarget.style.boxShadow = `var(--card-shadow-hover), 0 0 0 1px ${f.color}20`;
                        }, onMouseLeave: e => {
                            e.currentTarget.style.borderColor = 'var(--border)';
                            e.currentTarget.style.boxShadow = 'var(--card-shadow)';
                        }, children: [_jsx("div", { className: "w-10 h-10 rounded-xl flex items-center justify-center mb-4", style: { background: `${f.color}18`, color: f.color }, children: _jsx(f.icon, { size: 20 }) }), _jsx("h3", { className: "text-sm font-semibold mb-2", style: { color: 'var(--text-primary)', fontFamily: 'Plus Jakarta Sans, sans-serif' }, children: f.title }), _jsx("p", { className: "text-xs leading-relaxed", style: { color: 'var(--text-secondary)' }, children: f.description })] }, f.title))) })] }) }));
}
