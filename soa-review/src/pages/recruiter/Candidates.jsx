import { Users } from 'lucide-react';
import RecruiterLayout from '@/layouts/RecruiterLayout';
import EmptyState from '@/components/ui/EmptyState';
import { useBackendData } from '@/data/backendData';

export default function Candidates() {
  const { data: candidates, loading, error } = useBackendData('/api/admin/candidates');
  const uniqueCandidates = Array.from(new Map((candidates || []).map(candidate => [
    `${candidate.email}|${candidate.appliedJob}|${candidate.appliedDate}`,
    candidate,
  ])).values());

  return <RecruiterLayout><div className="space-y-5"><div><h1 className="text-2xl font-extrabold mb-1" style={{ color: 'var(--text-primary)' }}>Candidates</h1><p className="text-sm" style={{ color: 'var(--text-muted)' }}>{uniqueCandidates.length} candidates in pipeline</p></div>{loading ? <p className="text-sm" style={{ color: 'var(--text-muted)' }}>Loading candidates...</p> : error ? <EmptyState icon={<Users size={22} />} title="Unable to load candidates" description={error.message} /> : uniqueCandidates.length === 0 ? <EmptyState icon={<Users size={22} />} title="No candidates yet" description="Candidates will appear here as applications arrive." /> : <div className="grid md:grid-cols-2 gap-4">{uniqueCandidates.map(candidate => <div key={`${candidate.email}-${candidate.appliedJob}`} className="p-5 rounded-2xl border" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}><p className="text-sm font-semibold" style={{ color: 'var(--text-primary)' }}>{candidate.name}</p><p className="text-xs" style={{ color: 'var(--text-muted)' }}>{candidate.email}</p><p className="text-xs mt-3" style={{ color: 'var(--text-secondary)' }}>{candidate.appliedJob} · {candidate.status}</p></div>)}</div>}</div></RecruiterLayout>;
}
