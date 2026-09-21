import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useState } from 'react';
import { Moon, Sun, Monitor, Bell } from 'lucide-react';
import RecruiterLayout from '@/layouts/RecruiterLayout';
import Button from '@/components/ui/Button';
import Modal from '@/components/ui/Modal';
import { useTheme } from '@/context/ThemeContext';
import { useAuth } from '@/context/AuthContext';
import { useToast } from '@/context/ToastContext';
import { useNavigate } from 'react-router-dom';
import { callApi } from '@/utils/api';
export default function RecruiterSettings() {
    const { theme, setTheme } = useTheme();
    const { logout } = useAuth();
    const toast = useToast();
    const navigate = useNavigate();
    const [deleteOpen, setDeleteOpen] = useState(false);
    const [deleteConfirm, setDeleteConfirm] = useState('');
    const [deleting, setDeleting] = useState(false);
    async function handleDelete() {
        if (deleteConfirm !== 'DELETE')
            return;
        setDeleting(true);
        await callApi('/api/user/account', { method: 'DELETE' });
        logout();
        toast('info', 'Account deleted');
        navigate('/');
    }
    return (_jsxs(RecruiterLayout, { children: [_jsxs("div", { className: "max-w-2xl space-y-5", children: [_jsx("h1", { className: "text-2xl font-extrabold", style: { color: 'var(--text-primary)', fontFamily: 'Plus Jakarta Sans, sans-serif' }, children: "Settings" }), _jsxs("div", { className: "p-6 rounded-2xl border", style: { background: 'var(--surface)', borderColor: 'var(--border)' }, children: [_jsxs("h2", { className: "text-base font-semibold mb-4 flex items-center gap-2", style: { color: 'var(--text-primary)', fontFamily: 'Plus Jakarta Sans, sans-serif' }, children: [_jsx(Monitor, { size: 16 }), " Appearance"] }), _jsx("div", { className: "grid grid-cols-3 gap-3", children: [{ key: 'light', label: 'Light', icon: Sun }, { key: 'dark', label: 'Dark', icon: Moon }, { key: 'system', label: 'System', icon: Monitor }].map(opt => (_jsxs("button", { onClick: () => setTheme(opt.key), className: "flex flex-col items-center gap-2 p-4 rounded-xl border-2 transition-all", style: { borderColor: theme === opt.key ? 'var(--accent)' : 'var(--border)', background: theme === opt.key ? 'var(--accent-light)' : 'var(--surface-elevated)' }, children: [_jsx(opt.icon, { size: 20, style: { color: theme === opt.key ? 'var(--accent)' : 'var(--text-muted)' } }), _jsx("span", { className: "text-xs font-medium", style: { color: theme === opt.key ? 'var(--accent)' : 'var(--text-secondary)', fontFamily: 'Plus Jakarta Sans, sans-serif' }, children: opt.label })] }, opt.key))) })] }), _jsxs("div", { className: "p-6 rounded-2xl border", style: { background: 'var(--surface)', borderColor: 'var(--border)' }, children: [_jsxs("h2", { className: "text-base font-semibold mb-4 flex items-center gap-2", style: { color: 'var(--text-primary)', fontFamily: 'Plus Jakarta Sans, sans-serif' }, children: [_jsx(Bell, { size: 16 }), " Notifications"] }), ['New applications', 'Interview reminders', 'Candidate messages', 'Pipeline updates'].map(item => (_jsxs("div", { className: "flex items-center justify-between py-3 border-b last:border-0", style: { borderColor: 'var(--border)' }, children: [_jsx("span", { className: "text-sm", style: { color: 'var(--text-secondary)' }, children: item }), _jsxs("label", { className: "relative inline-flex items-center cursor-pointer", children: [_jsx("input", { type: "checkbox", defaultChecked: true, className: "sr-only peer" }), _jsx("div", { className: "w-9 h-5 rounded-full peer peer-checked:after:translate-x-4 after:content-[''] after:absolute after:top-0.5 after:left-0.5 after:bg-white after:rounded-full after:h-4 after:w-4 after:transition-all", style: { background: 'var(--accent)' } })] })] }, item)))] }), _jsxs("div", { className: "p-6 rounded-2xl border", style: { background: 'var(--error-light)', borderColor: 'var(--error)' }, children: [_jsx("h2", { className: "text-base font-semibold mb-2", style: { color: 'var(--error)', fontFamily: 'Plus Jakarta Sans, sans-serif' }, children: "Delete Account" }), _jsx("p", { className: "text-sm mb-4", style: { color: 'var(--text-secondary)' }, children: "This will permanently delete your account and all job postings." }), _jsx(Button, { variant: "danger", size: "sm", onClick: () => setDeleteOpen(true), children: "Delete Account" })] })] }), _jsx(Modal, { open: deleteOpen, onClose: () => setDeleteOpen(false), title: "Delete Account", size: "sm", children: _jsxs("div", { className: "space-y-4", children: [_jsxs("p", { className: "text-sm", style: { color: 'var(--text-secondary)' }, children: ["Type ", _jsx("strong", { children: "DELETE" }), " to permanently remove your account and all data."] }), _jsx("input", { value: deleteConfirm, onChange: e => setDeleteConfirm(e.target.value), placeholder: "Type DELETE to confirm", className: "w-full rounded-lg border p-3 text-sm", style: { background: 'var(--surface)', borderColor: 'var(--border-strong)', color: 'var(--text-primary)' } }), _jsxs("div", { className: "flex gap-3 justify-end", children: [_jsx(Button, { variant: "secondary", onClick: () => setDeleteOpen(false), children: "Cancel" }), _jsx(Button, { variant: "danger", loading: deleting, disabled: deleteConfirm !== 'DELETE', onClick: handleDelete, children: "Delete" })] })] }) })] }));
}
