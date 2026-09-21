import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Mail, Lock, ShieldCheck } from 'lucide-react';
import Button from '@/components/ui/Button';
import Input from '@/components/ui/Input';
import { useAuth } from '@/context/AuthContext';
import { useToast } from '@/context/ToastContext';

export default function Login() {
  const { login, requestOtp, verifyOtp, isLoading } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [otp, setOtp] = useState('');
  const [otpMode, setOtpMode] = useState(false);
  async function submit(event) { event.preventDefault(); try { if (otpMode) { await verifyOtp(email, otp); } else { await login(email, password); } navigate('/'); } catch (reason) { toast('error', 'Authentication failed', reason.message); } }
  async function sendOtp() { try { await requestOtp(email); setOtpMode(true); toast('success', 'OTP requested', 'Enter the code sent to your email.'); } catch (reason) { toast('error', 'Unable to request OTP', reason.message); } }
  return <div className="min-h-screen flex items-center justify-center p-6" style={{ background: 'var(--background)' }}><form onSubmit={submit} className="w-full max-w-md p-8 rounded-2xl border" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}><h1 className="text-2xl font-extrabold mb-2" style={{ color: 'var(--text-primary)' }}>Welcome back</h1><p className="text-sm mb-6" style={{ color: 'var(--text-muted)' }}>Sign in to TalentSync</p><div className="space-y-4"><Input type="email" placeholder="Email address" value={email} onChange={event => setEmail(event.target.value)} icon={<Mail size={15} />} required />{otpMode ? <Input inputMode="numeric" placeholder="One-time password" value={otp} onChange={event => setOtp(event.target.value)} icon={<ShieldCheck size={15} />} required /> : <Input type="password" placeholder="Password" value={password} onChange={event => setPassword(event.target.value)} icon={<Lock size={15} />} required />}<Button type="submit" className="w-full" loading={isLoading}>{otpMode ? 'Verify OTP' : 'Sign in'}</Button><Button type="button" variant="secondary" className="w-full" onClick={sendOtp} disabled={isLoading || !email}>Use email OTP</Button></div><p className="text-sm mt-6 text-center" style={{ color: 'var(--text-muted)' }}>New to TalentSync? <Link to="/register" style={{ color: 'var(--accent)' }}>Create an account</Link></p></form></div>;
}
