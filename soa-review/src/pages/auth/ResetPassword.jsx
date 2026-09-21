import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { Lock } from 'lucide-react';
import Button from '@/components/ui/Button';
import Input from '@/components/ui/Input';
import { callApi } from '@/utils/api';

export default function ResetPassword() {
  const [params] = useSearchParams();
  const email = params.get('email') || '';
  const [otp, setOtp] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const navigate = useNavigate();
  async function submit(event) { event.preventDefault(); if (password.length < 8 || password !== confirm) { setError('Use at least 8 characters and make both passwords match.'); return; } setSaving(true); try { await callApi('/api/auth/reset-password', { method: 'POST', body: JSON.stringify({ email, otp, password }) }); navigate('/login'); } catch (reason) { setError(reason.message); } finally { setSaving(false); } }
  return <div className="min-h-screen flex items-center justify-center px-5" style={{ background: 'var(--background)' }}><form onSubmit={submit} className="w-full max-w-md space-y-4"><h1 className="text-2xl font-extrabold" style={{ color: 'var(--text-primary)' }}>Set new password</h1><p className="text-sm" style={{ color: 'var(--text-muted)' }}>Enter the code sent to {email || 'your email'}.</p><Input value={otp} onChange={event => setOtp(event.target.value)} placeholder="One-time password" required /><Input type="password" label="New password" value={password} onChange={event => setPassword(event.target.value)} error={error} icon={<Lock size={15} />} required /><Input type="password" label="Confirm password" value={confirm} onChange={event => setConfirm(event.target.value)} required /><Button type="submit" loading={saving} className="w-full">Update password</Button><Link to="/login" className="block text-center text-sm" style={{ color: 'var(--accent)' }}>Back to sign in</Link></form></div>;
}
