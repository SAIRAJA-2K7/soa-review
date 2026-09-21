import { useNavigate } from 'react-router-dom';
import { ArrowRight, FileText } from 'lucide-react';
import DashboardLayout from '@/layouts/DashboardLayout';
import EmptyState from '@/components/ui/EmptyState';
import { statusBadge } from '@/components/ui/Badge';
import { useBackendData } from '@/data/backendData';
import { formatRelativeTime } from '@/utils/formatters';

export default function Applications() {
  const navigate = useNavigate();
  const { data: applications, loading, error } = useBackendData('/api/applications');
  return <DashboardLayout><div className="space-y-5"><div><h1 className="text-2xl font-extrabold mb-1" style={{ color: 'var(--text-primary)' }}>My Applications</h1><p className="text-sm" style={{ color: 'var(--text-muted)' }}>{applications.length} applications submitted</p></div>{loading ? <p className="text-sm" style={{ color: 'var(--text-muted)' }}>Loading applications...</p> : error ? <EmptyState icon={<FileText size={22} />} title="Unable to load applications" description={error.message} /> : applications.length === 0 ? <EmptyState icon={<FileText size={22} />} title="No applications yet" description="Start applying to jobs and track your progress here." action={{ label: 'Browse Jobs', onClick: () => navigate('/jobs') }} /> : <div className="space-y-4">{applications.map(application => <div key={application.id} onClick={() => navigate(`/applications/${application.id}`)} className="p-5 rounded-2xl border cursor-pointer group" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}><div className="flex items-start justify-between gap-4"><div><h3 className="text-sm font-semibold" style={{ color: 'var(--text-primary)' }}>{application.jobTitle}</h3><p className="text-xs" style={{ color: 'var(--text-muted)' }}>{application.companyName} · Applied {formatRelativeTime(application.appliedDate)}</p></div><div className="flex items-center gap-2">{statusBadge(application.status)}<ArrowRight size={14} style={{ color: 'var(--accent)' }} /></div></div></div>)}</div>}</div></DashboardLayout>;
}
