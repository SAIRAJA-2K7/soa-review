import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Mail } from 'lucide-react';
import Button from '@/components/ui/Button';
import Input from '@/components/ui/Input';
import { callApi } from '@/utils/api';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  async function submit(event) { event.preventDefault(); if (!email) { setError('Email is required'); return; } setError(''); setLoading(true); try { await callApi('/api/auth/request-otp', { method: 'POST', body: JSON.stringify({ email }) }); navigate(`/reset-password?email=${encodeURIComponent(email)}`); } catch (reason) { setError(reason.message); } finally { setLoading(false); } }
  return <div className="min-h-screen flex items-center justify-center px-5" style={{ background: 'var(--background)' }}><form onSubmit={submit} className="w-full max-w-md space-y-4"><h1 className="text-2xl font-extrabold" style={{ color: 'var(--text-primary)' }}>Forgot your password?</h1><p className="text-sm" style={{ color: 'var(--text-muted)' }}>Enter your email and we will send a one-time reset code.</p><Input type="email" label="Email address" value={email} onChange={event => setEmail(event.target.value)} error={error} icon={<Mail size={15} />} required /><Button type="submit" loading={loading} className="w-full">Send reset code</Button><p className="text-center text-sm" style={{ color: 'var(--text-muted)' }}><Link to="/login" style={{ color: 'var(--accent)' }}>Back to sign in</Link></p></form></div>;
}
