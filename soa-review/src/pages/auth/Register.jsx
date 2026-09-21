import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Mail, Lock, User, EyeOff, Zap } from 'lucide-react';
import Button from '@/components/ui/Button';
import Input from '@/components/ui/Input';
import ThemeToggle from '@/components/ui/ThemeToggle';
import { useAuth } from '@/context/AuthContext';
import { useToast } from '@/context/ToastContext';
export default function Register() {
    const [name, setName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirm, setConfirm] = useState('');
    const [role, setRole] = useState('candidate');
    const [showPass, setShowPass] = useState(false);
    const [errors, setErrors] = useState({});
    const { register, isLoading } = useAuth();
    const toast = useToast();
    const navigate = useNavigate();
    async function handleSubmit(e) {
        e.preventDefault();
        const errs = {};
        if (!name.trim())
            errs.name = 'Full name is required';
        if (!email)
            errs.email = 'Email is required';
        else if (!/\S+@\S+\.\S+/.test(email))
            errs.email = 'Enter a valid email address';
        if (!password)
            errs.password = 'Password is required';
        else if (password.length < 8)
            errs.password = 'Password must be at least 8 characters';
        if (password !== confirm)
            errs.confirm = 'Passwords do not match';
        if (Object.keys(errs).length) {
            setErrors(errs);
            return;
        }
        setErrors({});
        try {
            await register(name, email, password, role);
            toast('success', 'Account created!', 'Please verify your email to continue.');
            navigate('/verify-email');
        }
        catch (err) {
            setErrors({ auth: err.message || 'Registration failed. Please try again.' });
        }
    }
    return (_jsxs("div", { className: "min-h-screen flex flex-col", style: { background: 'var(--background)' }, children: [_jsxs("div", { className: "flex items-center justify-between px-6 py-5", children: [_jsxs(Link, { to: "/", className: "flex items-center gap-2", children: [_jsx("div", { className: "w-7 h-7 rounded-lg flex items-center justify-center", style: { background: 'var(--accent)' }, children: _jsx(Zap, { size: 13, className: "text-white", fill: "white" }) }), _jsx("span", { className: "font-bold text-sm", style: { color: 'var(--text-primary)', fontFamily: 'Plus Jakarta Sans, sans-serif' }, children: "TalentSync" })] }), _jsxs("div", { className: "flex items-center gap-3", children: [_jsx(ThemeToggle, { compact: true }), _jsx("span", { className: "text-sm", style: { color: 'var(--text-muted)' }, children: "Already a member?" }), _jsx(Link, { to: "/login", className: "text-sm font-medium", style: { color: 'var(--accent)' }, children: "Sign in" })] })] }), _jsx("div", { className: "flex-1 flex items-center justify-center px-5 py-8", children: _jsxs("div", { className: "w-full max-w-md", children: [_jsxs("div", { className: "mb-8", children: [_jsx("h1", { className: "text-2xl font-extrabold mb-2", style: { color: 'var(--text-primary)', fontFamily: 'Plus Jakarta Sans, sans-serif' }, children: "Create your account" }), _jsx("p", { className: "text-sm", style: { color: 'var(--text-muted)' }, children: "Join thousands of professionals on TalentSync" })] }), _jsx("div", { className: "grid grid-cols-2 gap-3 mb-6 p-1 rounded-xl", style: { background: 'var(--surface-elevated)' }, children: ['candidate', 'recruiter'].map(r => (_jsx("button", { type: "button", onClick: () => setRole(r), className: "py-2.5 px-4 rounded-lg text-sm font-medium transition-all", style: {
                                    background: role === r ? 'var(--surface)' : 'transparent',
                                    color: role === r ? 'var(--text-primary)' : 'var(--text-muted)',
                                    boxShadow: role === r ? 'var(--elevation-1)' : 'none',
                                    fontFamily: 'Plus Jakarta Sans, sans-serif',
                                }, children: r === 'candidate' ? '👤 Job Seeker' : '🏢 Recruiter' }, r))) }), errors.auth && (_jsx("div", { className: "mb-5 px-4 py-3 rounded-xl border text-sm", style: { background: 'var(--error-light)', borderColor: 'var(--error)', color: 'var(--error)' }, children: errors.auth })), _jsxs("form", { onSubmit: handleSubmit, className: "space-y-4", noValidate: true, children: [_jsx(Input, { id: "name", label: "Full name", placeholder: "Alex Morgan", value: name, onChange: e => setName(e.target.value), error: errors.name, icon: _jsx(User, { size: 15 }), autoComplete: "name" }), _jsx(Input, { id: "email", type: "email", label: "Work email", placeholder: "you@company.com", value: email, onChange: e => setEmail(e.target.value), error: errors.email, icon: _jsx(Mail, { size: 15 }), autoComplete: "email" }), _jsx(Input, { id: "password", type: showPass ? 'text' : 'password', label: "Password", placeholder: "Min. 8 characters", value: password, onChange: e => setPassword(e.target.value), error: errors.password, icon: _jsx(Lock, { size: 15 }), suffix: _jsx("button", { type: "button", onClick: () => setShowPass(!showPass), "aria-label": "Toggle password", children: _jsx(EyeOff, { size: 15 }) }), autoComplete: "new-password" }), _jsx(Input, { id: "confirm", type: showPass ? 'text' : 'password', label: "Confirm password", placeholder: "Repeat password", value: confirm, onChange: e => setConfirm(e.target.value), error: errors.confirm, icon: _jsx(Lock, { size: 15 }), autoComplete: "new-password" }), _jsx(Button, { type: "submit", loading: isLoading, className: "w-full", size: "lg", children: "Create account" })] }), _jsxs("p", { className: "text-center text-xs mt-6 leading-relaxed", style: { color: 'var(--text-muted)' }, children: ["By creating an account, you agree to our", ' ', _jsx("a", { href: "#", className: "underline hover:no-underline", style: { color: 'var(--text-secondary)' }, children: "Terms of Service" }), " and", ' ', _jsx("a", { href: "#", className: "underline hover:no-underline", style: { color: 'var(--text-secondary)' }, children: "Privacy Policy" }), "."] })] }) })] }));
}
