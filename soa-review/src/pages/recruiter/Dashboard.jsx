import { Briefcase, FileText, Users, Calendar } from 'lucide-react';
import RecruiterLayout from '@/layouts/RecruiterLayout';
import { useBackendData } from '@/data/backendData';

export default function Dashboard() {
  const { data, loading, error } = useBackendData('/api/admin/dashboard', {});
  const metrics = [['activeJobs', 'Active jobs', Briefcase], ['totalApplicants', 'Applicants', FileText], ['shortlisted', 'Shortlisted', Users], ['interviews', 'Interviews', Calendar]];
  return <RecruiterLayout><div className="space-y-6"><div><h1 className="text-2xl font-extrabold" style={{ color: 'var(--text-primary)' }}>Recruiter Dashboard</h1><p className="text-sm" style={{ color: 'var(--text-muted)' }}>Live pipeline activity from your database.</p></div>{error ? <p className="text-sm" style={{ color: 'var(--error)' }}>{error.message}</p> : <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4">{metrics.map(([key, label, Icon]) => <div key={key} className="p-5 rounded-2xl border" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}><Icon size={18} style={{ color: 'var(--accent)' }} /><p className="text-xs mt-3" style={{ color: 'var(--text-muted)' }}>{label}</p><p className="text-2xl font-extrabold" style={{ color: 'var(--text-primary)' }}>{loading ? '...' : data[key] || 0}</p></div>)}</div>}</div></RecruiterLayout>;
}
