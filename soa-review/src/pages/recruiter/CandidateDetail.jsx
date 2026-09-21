import { useParams } from 'react-router-dom';
import RecruiterLayout from '@/layouts/RecruiterLayout';
import EmptyState from '@/components/ui/EmptyState';
import { useBackendData } from '@/data/backendData';

export default function CandidateDetail() {
  const { id } = useParams();
  const { data: applications, loading, error } = useBackendData('/api/admin/applications');
  const candidate = applications.find(application => String(application.candidateId) === id);
  return <RecruiterLayout>{loading ? <p className="text-sm" style={{ color: 'var(--text-muted)' }}>Loading candidate...</p> : error ? <EmptyState title="Unable to load candidate" description={error.message} /> : !candidate ? <EmptyState title="Candidate not found" description="This candidate has no application in your pipeline." /> : <div className="space-y-5"><h1 className="text-2xl font-extrabold" style={{ color: 'var(--text-primary)' }}>{candidate.candidateName}</h1><div className="p-5 rounded-2xl border" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}><p className="text-sm" style={{ color: 'var(--text-primary)' }}>{candidate.candidateEmail}</p><p className="text-sm mt-2" style={{ color: 'var(--text-secondary)' }}>Applied for {candidate.jobTitle}</p><p className="text-sm mt-2" style={{ color: 'var(--accent)' }}>Status: {candidate.status}</p></div></div>}</RecruiterLayout>;
}
