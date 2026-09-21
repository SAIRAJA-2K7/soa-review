import RecruiterLayout from '@/layouts/RecruiterLayout';
import { useBackendData } from '@/data/backendData';

export default function Analytics() {
  const { data, loading, error } = useBackendData('/api/admin/dashboard', {});
  const statuses = ['applied', 'under_review', 'shortlisted', 'interview', 'selected', 'rejected'];
  return <RecruiterLayout><div className="space-y-5"><h1 className="text-2xl font-extrabold" style={{ color: 'var(--text-primary)' }}>Analytics</h1>{error ? <p className="text-sm" style={{ color: 'var(--error)' }}>{error.message}</p> : <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">{statuses.map(status => <div key={status} className="p-5 rounded-2xl border" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}><p className="text-xs capitalize" style={{ color: 'var(--text-muted)' }}>{status.replace('_', ' ')}</p><p className="text-2xl font-extrabold" style={{ color: 'var(--text-primary)' }}>{loading ? '...' : (data.applications || []).filter(application => application.status === status).length}</p></div>)}</div>}</div></RecruiterLayout>;
}
