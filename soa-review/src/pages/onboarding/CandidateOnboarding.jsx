import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { User, MapPin, Briefcase, Upload, Check, Zap } from 'lucide-react';
import Button from '@/components/ui/Button';
import Input from '@/components/ui/Input';
import { useToast } from '@/context/ToastContext';
import { callApi } from '@/utils/api';
const STEPS = [
    { id: 1, label: 'Profile', icon: User },
    { id: 2, label: 'Experience', icon: Briefcase },
    { id: 3, label: 'Preferences', icon: MapPin },
    { id: 4, label: 'Resume', icon: Upload },
];
const SKILLS_LIST = ['React', 'TypeScript', 'Node.js', 'Python', 'Java', 'AWS', 'Kubernetes', 'Machine Learning', 'SQL', 'Product Management', 'Data Analysis', 'UX Design'];
export default function CandidateOnboarding() {
    const [step, setStep] = useState(1);
    const [skills, setSkills] = useState([]);
    const [location, setLocation] = useState('');
    const [jobType, setJobType] = useState('');
    const [title, setTitle] = useState('');
    const [experience, setExperience] = useState('');
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();
    const toast = useToast();
    function toggleSkill(s) {
        setSkills(prev => prev.includes(s) ? prev.filter(x => x !== s) : [...prev, s]);
    }
    async function finish() {
        setLoading(true);
        await callApi('/api/user/profile', { method: 'PATCH', body: JSON.stringify({ professionalTitle: title, location, bio: `Skills: ${skills.join(', ')}; Experience: ${experience}; Preferred type: ${jobType}`, onboarded: 'true' }) });
        setLoading(false);
        toast('success', 'Profile complete!', 'Welcome to TalentSync.');
        navigate('/dashboard');
    }
    return (_jsxs("div", { className: "min-h-screen flex flex-col", style: { background: 'var(--background)' }, children: [_jsxs("div", { className: "flex items-center justify-between px-6 py-4 border-b", style: { background: 'var(--surface)', borderColor: 'var(--border)' }, children: [_jsxs("div", { className: "flex items-center gap-2", children: [_jsx("div", { className: "w-7 h-7 rounded-lg flex items-center justify-center", style: { background: 'var(--accent)' }, children: _jsx(Zap, { size: 13, className: "text-white", fill: "white" }) }), _jsx("span", { className: "font-bold text-sm", style: { color: 'var(--text-primary)', fontFamily: 'Plus Jakarta Sans, sans-serif' }, children: "TalentSync" })] }), _jsxs("div", { className: "text-xs", style: { color: 'var(--text-muted)' }, children: ["Step ", step, " of ", STEPS.length] })] }), _jsx("div", { className: "flex-1 flex flex-col items-center justify-center px-5 py-8", children: _jsxs("div", { className: "w-full max-w-lg", children: [_jsx("div", { className: "flex items-center mb-10", children: STEPS.map((s, i) => (_jsxs("div", { className: "flex items-center flex-1", children: [_jsx("div", { className: `flex items-center justify-center w-9 h-9 rounded-full text-sm font-bold transition-all flex-shrink-0`, style: {
                                            background: s.id < step ? 'var(--success)' : s.id === step ? 'var(--accent)' : 'var(--surface-elevated)',
                                            color: s.id <= step ? 'white' : 'var(--text-muted)',
                                        }, children: s.id < step ? _jsx(Check, { size: 14 }) : _jsx(s.icon, { size: 14 }) }), _jsx("div", { className: "mx-2 text-xs hidden sm:block", style: { color: s.id === step ? 'var(--text-primary)' : 'var(--text-muted)', fontFamily: 'Plus Jakarta Sans, sans-serif', fontWeight: s.id === step ? 600 : 400 }, children: s.label }), i < STEPS.length - 1 && (_jsx("div", { className: "flex-1 h-px mx-2", style: { background: s.id < step ? 'var(--success)' : 'var(--border)' } }))] }, s.id))) }), _jsxs("div", { className: "p-6 rounded-2xl border", style: { background: 'var(--surface)', borderColor: 'var(--border)' }, children: [step === 1 && (_jsxs("div", { className: "space-y-4", children: [_jsxs("div", { className: "mb-6", children: [_jsx("h2", { className: "text-xl font-bold mb-1", style: { color: 'var(--text-primary)', fontFamily: 'Plus Jakarta Sans, sans-serif' }, children: "Tell us about yourself" }), _jsx("p", { className: "text-sm", style: { color: 'var(--text-muted)' }, children: "This helps us personalize your job recommendations." })] }), _jsx(Input, { id: "title", label: "Current or desired job title", placeholder: "e.g. Senior Software Engineer", value: title, onChange: e => setTitle(e.target.value) }), _jsxs("div", { children: [_jsx("label", { className: "text-sm font-medium block mb-1.5", style: { color: 'var(--text-secondary)', fontFamily: 'Plus Jakarta Sans, sans-serif' }, children: "Skills" }), _jsx("div", { className: "flex flex-wrap gap-2", children: SKILLS_LIST.map(s => (_jsx("button", { onClick: () => toggleSkill(s), type: "button", className: "px-3 py-1.5 rounded-full text-xs font-medium border transition-all", style: {
                                                            background: skills.includes(s) ? 'var(--accent)' : 'var(--surface-elevated)',
                                                            borderColor: skills.includes(s) ? 'var(--accent)' : 'var(--border)',
                                                            color: skills.includes(s) ? 'white' : 'var(--text-secondary)',
                                                        }, children: s }, s))) })] })] })), step === 2 && (_jsxs("div", { className: "space-y-4", children: [_jsxs("div", { className: "mb-6", children: [_jsx("h2", { className: "text-xl font-bold mb-1", style: { color: 'var(--text-primary)', fontFamily: 'Plus Jakarta Sans, sans-serif' }, children: "Work experience" }), _jsx("p", { className: "text-sm", style: { color: 'var(--text-muted)' }, children: "Share your professional background." })] }), _jsxs("div", { children: [_jsx("label", { className: "text-sm font-medium block mb-1.5", style: { color: 'var(--text-secondary)', fontFamily: 'Plus Jakarta Sans, sans-serif' }, children: "Years of experience" }), _jsx("div", { className: "grid grid-cols-3 gap-2", children: ['0-1 years', '2-4 years', '5-7 years', '8-10 years', '10+ years', 'Student'].map(opt => (_jsx("button", { type: "button", onClick: () => setExperience(opt), className: "py-2 px-3 rounded-lg border text-sm transition-all text-center", style: {
                                                            background: experience === opt ? 'var(--accent-light)' : 'var(--surface-elevated)',
                                                            borderColor: experience === opt ? 'var(--accent)' : 'var(--border)',
                                                            color: experience === opt ? 'var(--accent)' : 'var(--text-secondary)',
                                                            fontFamily: 'Plus Jakarta Sans, sans-serif',
                                                        }, children: opt }, opt))) })] })] })), step === 3 && (_jsxs("div", { className: "space-y-4", children: [_jsxs("div", { className: "mb-6", children: [_jsx("h2", { className: "text-xl font-bold mb-1", style: { color: 'var(--text-primary)', fontFamily: 'Plus Jakarta Sans, sans-serif' }, children: "Your preferences" }), _jsx("p", { className: "text-sm", style: { color: 'var(--text-muted)' }, children: "What are you looking for?" })] }), _jsx(Input, { id: "location", label: "Preferred location", placeholder: "e.g. San Francisco, CA or Remote", value: location, onChange: e => setLocation(e.target.value), icon: _jsx(MapPin, { size: 15 }) }), _jsxs("div", { children: [_jsx("label", { className: "text-sm font-medium block mb-1.5", style: { color: 'var(--text-secondary)', fontFamily: 'Plus Jakarta Sans, sans-serif' }, children: "Employment type" }), _jsx("div", { className: "grid grid-cols-2 gap-2", children: ['Full-time', 'Part-time', 'Contract', 'Remote'].map(opt => (_jsx("button", { type: "button", onClick: () => setJobType(opt), className: "py-2.5 rounded-lg border text-sm transition-all", style: {
                                                            background: jobType === opt ? 'var(--accent-light)' : 'var(--surface-elevated)',
                                                            borderColor: jobType === opt ? 'var(--accent)' : 'var(--border)',
                                                            color: jobType === opt ? 'var(--accent)' : 'var(--text-secondary)',
                                                            fontFamily: 'Plus Jakarta Sans, sans-serif',
                                                        }, children: opt }, opt))) })] })] })), step === 4 && (_jsxs("div", { className: "space-y-4", children: [_jsxs("div", { className: "mb-6", children: [_jsx("h2", { className: "text-xl font-bold mb-1", style: { color: 'var(--text-primary)', fontFamily: 'Plus Jakarta Sans, sans-serif' }, children: "Upload your resume" }), _jsx("p", { className: "text-sm", style: { color: 'var(--text-muted)' }, children: "PDF format, max 5MB. You can update this later." })] }), _jsxs("div", { className: "border-2 border-dashed rounded-xl p-10 text-center cursor-pointer transition-colors hover:border-[var(--accent)]", style: { borderColor: 'var(--border-strong)', background: 'var(--surface-elevated)' }, children: [_jsx(Upload, { size: 32, className: "mx-auto mb-3", style: { color: 'var(--text-muted)' } }), _jsx("p", { className: "text-sm font-medium mb-1", style: { color: 'var(--text-primary)', fontFamily: 'Plus Jakarta Sans, sans-serif' }, children: "Drop your resume here" }), _jsx("p", { className: "text-xs", style: { color: 'var(--text-muted)' }, children: "or click to browse files" })] }), _jsx("p", { className: "text-center text-sm", style: { color: 'var(--text-muted)' }, children: "You can skip this and add your resume later in your profile." })] }))] }), _jsxs("div", { className: "flex items-center justify-between mt-6", children: [_jsx(Button, { variant: "ghost", onClick: () => setStep(s => Math.max(1, s - 1)), disabled: step === 1, children: "Back" }), _jsx("div", { className: "flex gap-2", children: step < STEPS.length ? (_jsx(Button, { onClick: () => setStep(s => s + 1), children: "Continue" })) : (_jsx(Button, { onClick: finish, loading: loading, children: "Complete Setup" })) })] })] }) })] }));
}
