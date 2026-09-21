import { Briefcase, FileText, Calendar } from 'lucide-react';
import DashboardLayout from '@/layouts/DashboardLayout';
import { useBackendData } from '@/data/backendData';

export default function Dashboard() {
  const { data: jobs, loading: jobsLoading } = useBackendData('/api/jobs');
  const { data: applications, loading: applicationsLoading } = useBackendData('/api/applications');
  const metrics = [{ label: 'Available jobs', value: jobs.length, icon: Briefcase }, { label: 'Applications', value: applications.length, icon: FileText }, { label: 'Interviews', value: applications.filter(item => item.status === 'interview').length, icon: Calendar }];
  return <DashboardLayout><div className="space-y-6"><div><h1 className="text-2xl font-extrabold" style={{ color: 'var(--text-primary)' }}>Candidate Dashboard</h1><p className="text-sm" style={{ color: 'var(--text-muted)' }}>Your recruitment activity at a glance.</p></div><div className="grid sm:grid-cols-3 gap-4">{metrics.map(metric => <div key={metric.label} className="p-5 rounded-2xl border" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}><metric.icon size={18} style={{ color: 'var(--accent)' }} /><p className="text-xs mt-3" style={{ color: 'var(--text-muted)' }}>{metric.label}</p><p className="text-2xl font-extrabold" style={{ color: 'var(--text-primary)' }}>{jobsLoading || applicationsLoading ? '...' : metric.value}</p></div>)}</div></div></DashboardLayout>;
}
