import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { Lock } from 'lucide-react';
import Button from '@/components/ui/Button';
import Input from '@/components/ui/Input';
import { callApi } from '@/utils/api';

export default function ResetPassword() {
  const [params] = useSearchParams();
  const token = params.get('token') || '';
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const navigate = useNavigate();

  async function submit(event) {
    event.preventDefault();
    if (!token) {
      setError('This reset link is missing or invalid.');
      return;
    }
    if (password.length < 8 || password !== confirm) {
      setError('Use at least 8 characters and make both passwords match.');
      return;
    }
    setSaving(true);
    setError('');
    try {
      await callApi('/api/auth/password-reset/consume', {
        method: 'POST',
        body: JSON.stringify({ token, password }),
      });
      navigate('/login', { replace: true });
    } catch (reason) {
      setError(reason.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center px-5" style={{ background: 'var(--background)' }}>
      <form onSubmit={submit} className="w-full max-w-md space-y-4">
        <h1 className="text-2xl font-extrabold" style={{ color: 'var(--text-primary)' }}>Set new password</h1>
        <p className="text-sm" style={{ color: 'var(--text-muted)' }}>
          Choose a new password for your account.
        </p>
        <Input
          type="password"
          label="New password"
          value={password}
          onChange={event => setPassword(event.target.value)}
          icon={<Lock size={15} />}
          minLength={8}
          required
        />
        <Input
          type="password"
          label="Confirm new password"
          value={confirm}
          onChange={event => setConfirm(event.target.value)}
          icon={<Lock size={15} />}
          minLength={8}
          required
        />
        {error && <p role="alert" className="text-sm text-red-600">{error}</p>}
        <Button type="submit" className="w-full" loading={saving}>Update password</Button>
        <p className="text-sm text-center">
          <Link to="/login" style={{ color: 'var(--accent)' }}>Return to sign in</Link>
        </p>
      </form>
    </div>
  );
}
