import RecruiterLayout from '@/layouts/RecruiterLayout';
import { useBackendData } from '@/data/backendData';

const stages = ['applied', 'under_review', 'shortlisted', 'interview', 'final_review', 'selected'];
export default function Pipeline() {
  const { data: applications, loading, error } = useBackendData('/api/admin/applications');
  return <RecruiterLayout><div className="space-y-5"><div><h1 className="text-2xl font-extrabold" style={{ color: 'var(--text-primary)' }}>Candidate Pipeline</h1><p className="text-sm" style={{ color: 'var(--text-muted)' }}>Application stages backed by the database.</p></div>{loading ? <p className="text-sm" style={{ color: 'var(--text-muted)' }}>Loading pipeline...</p> : error ? <p className="text-sm" style={{ color: 'var(--error)' }}>{error.message}</p> : <div className="grid md:grid-cols-3 lg:grid-cols-6 gap-3">{stages.map(stage => <div key={stage} className="p-3 rounded-xl border min-h-40" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}><h2 className="text-xs font-semibold capitalize mb-3" style={{ color: 'var(--text-primary)' }}>{stage.replace('_', ' ')}</h2>{applications.filter(application => application.status === stage).map(application => <div key={application.id} className="p-2 rounded-lg mb-2" style={{ background: 'var(--surface-elevated)' }}><p className="text-xs font-medium" style={{ color: 'var(--text-primary)' }}>{application.candidateName}</p><p className="text-[10px]" style={{ color: 'var(--text-muted)' }}>{application.jobTitle}</p></div>)}</div>)}</div>}</div></RecruiterLayout>;
}
