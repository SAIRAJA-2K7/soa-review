import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Mail } from 'lucide-react';
import Button from '@/components/ui/Button';
import Input from '@/components/ui/Input';
import { callApi } from '@/utils/api';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [sent, setSent] = useState(false);
  const [loading, setLoading] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setError('');
    setLoading(true);
    try {
      await callApi('/api/auth/password-reset/request', {
        method: 'POST',
        body: JSON.stringify({ email }),
      });
      setSent(true);
    } catch (reason) {
      setError(reason.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center px-5" style={{ background: 'var(--background)' }}>
      <form onSubmit={submit} className="w-full max-w-md space-y-4">
        <h1 className="text-2xl font-extrabold" style={{ color: 'var(--text-primary)' }}>
          {sent ? 'Check your email' : 'Forgot your password?'}
        </h1>
        <p className="text-sm" style={{ color: 'var(--text-muted)' }}>
          {sent
            ? 'If an account exists for that address, a password reset link will be sent.'
            : 'Enter your account email and we will send a password reset link.'}
        </p>
        {!sent && (
          <>
            <Input
              type="email"
              label="Email address"
              value={email}
              onChange={event => setEmail(event.target.value)}
              icon={<Mail size={15} />}
              required
            />
            {error && <p role="alert" className="text-sm text-red-600">{error}</p>}
            <Button type="submit" className="w-full" loading={loading}>Send reset link</Button>
          </>
        )}
        <p className="text-sm text-center" style={{ color: 'var(--text-muted)' }}>
          <Link to="/login" style={{ color: 'var(--accent)' }}>Return to sign in</Link>
        </p>
      </form>
    </div>
  );
}
