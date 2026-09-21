import { Calendar, Video } from 'lucide-react';
import DashboardLayout from '@/layouts/DashboardLayout';
import EmptyState from '@/components/ui/EmptyState';
import { useBackendData } from '@/data/backendData';

export default function Interviews() {
  const { data: interviews, loading, error } = useBackendData('/api/user/interviews');
  return <DashboardLayout><div className="space-y-5"><div><h1 className="text-2xl font-extrabold mb-1" style={{ color: 'var(--text-primary)' }}>Interviews</h1><p className="text-sm" style={{ color: 'var(--text-muted)' }}>{interviews.length} interviews scheduled</p></div>{loading ? <p className="text-sm" style={{ color: 'var(--text-muted)' }}>Loading interviews...</p> : error ? <EmptyState icon={<Calendar size={22} />} title="Unable to load interviews" description={error.message} /> : interviews.length === 0 ? <EmptyState icon={<Calendar size={22} />} title="No interviews scheduled" description="Your scheduled interviews will appear here." /> : <div className="grid sm:grid-cols-2 gap-4">{interviews.map(interview => <div key={interview.id} className="p-5 rounded-2xl border" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}><p className="text-sm font-semibold" style={{ color: 'var(--text-primary)' }}>{interview.type} Interview</p><p className="text-xs mb-3" style={{ color: 'var(--text-muted)' }}>{interview.job}</p><div className="text-xs space-y-1" style={{ color: 'var(--text-secondary)' }}><p><Calendar size={13} className="inline mr-2" />{interview.date} at {interview.time}</p><p><Video size={13} className="inline mr-2" />{interview.mode} with {interview.interviewer}</p></div></div>)}</div>}</div></DashboardLayout>;
}
