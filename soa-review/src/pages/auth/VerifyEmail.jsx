import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Button from '@/components/ui/Button';
import Input from '@/components/ui/Input';
import { useAuth } from '@/context/AuthContext';
import { useToast } from '@/context/ToastContext';

export default function VerifyEmail() {
  const { user, verifyOtp, requestOtp, isLoading } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const [otp, setOtp] = useState('');
  async function verify(event) { event.preventDefault(); try { const account = await verifyOtp(user?.email, otp); navigate(account.role === 'recruiter' ? '/onboarding/recruiter' : account.role === 'admin' ? '/' : '/onboarding/candidate'); } catch (reason) { toast('error', 'Verification failed', reason.message); } }
  async function resend() { try { await requestOtp(user?.email); toast('success', 'OTP sent', 'Check your email for a new code.'); } catch (reason) { toast('error', 'Unable to resend OTP', reason.message); } }
  return <div className="min-h-screen flex items-center justify-center p-6" style={{ background: 'var(--background)' }}><form onSubmit={verify} className="w-full max-w-md p-8 rounded-2xl border" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}><h1 className="text-2xl font-extrabold mb-2" style={{ color: 'var(--text-primary)' }}>Verify your email</h1><p className="text-sm mb-6" style={{ color: 'var(--text-muted)' }}>Enter the one-time password sent to {user?.email || 'your email address'}.</p><Input inputMode="numeric" value={otp} onChange={event => setOtp(event.target.value)} placeholder="One-time password" required /><Button type="submit" className="w-full mt-4" loading={isLoading}>Verify email</Button><Button type="button" variant="secondary" className="w-full mt-3" onClick={resend} disabled={isLoading}>Resend code</Button></form></div>;
}
