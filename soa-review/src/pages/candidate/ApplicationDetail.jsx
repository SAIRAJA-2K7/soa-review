import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, CheckCircle, Clock, FileText } from 'lucide-react';
import DashboardLayout from '@/layouts/DashboardLayout';
import EmptyState from '@/components/ui/EmptyState';
import { statusBadge } from '@/components/ui/Badge';
import { useBackendData } from '@/data/backendData';
import Button from '@/components/ui/Button';
import { callApi } from '@/utils/api';
import { useToast } from '@/context/ToastContext';
import { useState } from 'react';

export default function ApplicationDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const toast = useToast();
  const [withdrawing, setWithdrawing] = useState(false);
  const { data: applications, loading, error } = useBackendData('/api/applications');
  const application = applications.find(item => String(item.id) === id);
  async function withdraw() {
    if (!window.confirm('Withdraw this application? This cannot be undone.')) return;
    setWithdrawing(true);
    try {
      await callApi(`/api/applications/${id}`, { method: 'DELETE' });
      toast('success', 'Application withdrawn', 'The recruiter will no longer see this application.');
      navigate('/applications');
    } catch (reason) {
      toast('error', 'Unable to withdraw application', reason.message);
    } finally {
      setWithdrawing(false);
    }
  }
  if (loading) return <DashboardLayout><p className="text-sm" style={{ color: 'var(--text-muted)' }}>Loading application...</p></DashboardLayout>;
  if (error || !application) return <DashboardLayout><EmptyState icon={<FileText size={22} />} title="Application not found" description={error?.message || 'This application is unavailable.'} action={{ label: 'Back to Applications', onClick: () => navigate('/applications') }} /></DashboardLayout>;
  return (
    <DashboardLayout>
      <div className="max-w-3xl space-y-5">
        <button onClick={() => navigate('/applications')} className="flex items-center gap-2 text-sm" style={{ color: 'var(--text-muted)' }}><ArrowLeft size={16} /> Back to Applications</button>
        <div className="p-6 rounded-2xl border" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}>
          <div className="flex justify-between gap-4 mb-5"><div><h1 className="text-xl font-extrabold" style={{ color: 'var(--text-primary)' }}>{application.jobTitle}</h1><p className="text-sm" style={{ color: 'var(--text-muted)' }}>{application.companyName}</p></div>{statusBadge(application.status)}</div>
          <div className="space-y-4">{(application.timeline || []).map((stage, index) => <div key={`${stage.stage}-${index}`} className="flex gap-3"><div>{stage.completed ? <CheckCircle size={16} style={{ color: 'var(--success)' }} /> : <Clock size={16} style={{ color: 'var(--text-muted)' }} />}</div><div><p className="text-sm font-semibold" style={{ color: 'var(--text-primary)' }}>{stage.stage}</p><p className="text-xs" style={{ color: 'var(--text-muted)' }}>{stage.date} {stage.note && `· ${stage.note}`}</p></div></div>)}</div>
          {!['selected', 'rejected'].includes(application.status) && (
            <div className="mt-6 pt-5 border-t" style={{ borderColor: 'var(--border)' }}>
              <Button variant="danger" size="sm" loading={withdrawing} onClick={withdraw}>Withdraw application</Button>
            </div>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}
