import { Bookmark } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import DashboardLayout from '@/layouts/DashboardLayout';
import EmptyState from '@/components/ui/EmptyState';
import { useBackendData } from '@/data/backendData';

export default function SavedJobs() {
  const navigate = useNavigate();
  const { data: savedJobs, loading, error } = useBackendData('/api/user/saved-jobs');
  return <DashboardLayout><div className="space-y-5"><h1 className="text-2xl font-extrabold" style={{ color: 'var(--text-primary)' }}>Saved Jobs</h1>{loading ? <p className="text-sm" style={{ color: 'var(--text-muted)' }}>Loading saved jobs...</p> : error ? <EmptyState icon={<Bookmark size={22} />} title="Unable to load saved jobs" description={error.message} /> : savedJobs.length === 0 ? <EmptyState icon={<Bookmark size={22} />} title="No saved jobs" description="Bookmark interesting positions to review them later." action={{ label: 'Browse Jobs', onClick: () => navigate('/jobs') }} /> : <div className="grid md:grid-cols-2 gap-4">{savedJobs.map(job => <button key={job.id} onClick={() => navigate(`/jobs/${job.id}`)} className="p-5 rounded-2xl border text-left" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}><p className="text-sm font-semibold" style={{ color: 'var(--text-primary)' }}>{job.title}</p><p className="text-xs" style={{ color: 'var(--text-muted)' }}>{job.company?.name} · {job.location}</p></button>)}</div>}</div></DashboardLayout>;
}
