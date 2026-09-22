import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Briefcase, CheckCircle, Clock, DollarSign, MapPin, Send } from 'lucide-react';
import DashboardLayout from '@/layouts/DashboardLayout';
import Button from '@/components/ui/Button';
import EmptyState from '@/components/ui/EmptyState';
import { useToast } from '@/context/ToastContext';
import { useBackendData } from '@/data/backendData';
import { callApi } from '@/utils/api';
import { formatSalary, formatRelativeTime } from '@/utils/formatters';
import { useAuth } from '@/context/AuthContext';

export default function JobDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const toast = useToast();
  const { user } = useAuth();
  const { data: jobs, loading, error } = useBackendData('/api/jobs');
  const [applied, setApplied] = useState(false);
  const [applying, setApplying] = useState(false);
  const job = jobs.find(item => String(item.id) === id);
  useEffect(() => { if (job?.applied) setApplied(true); }, [job]);
  async function handleApply() {
    setApplying(true);
    try {
      await callApi(`/api/jobs/${id}/apply`, {
        method: 'POST',
        body: JSON.stringify({
          candidateId: user?.id,
          candidateName: user?.name,
          candidateEmail: user?.email,
        }),
      });
      setApplied(true);
      toast('success', 'Application submitted!', `Your application to ${job.company.name} has been sent.`);
    }
    catch (reason) { toast('error', 'Application failed', reason.message); }
    finally { setApplying(false); }
  }
  if (loading) return <DashboardLayout><p className="text-sm" style={{ color: 'var(--text-muted)' }}>Loading job...</p></DashboardLayout>;
  if (error || !job) return <DashboardLayout><EmptyState icon={<Briefcase size={22} />} title="Job not found" description={error?.message || 'This job is no longer available.'} action={{ label: 'Back to Jobs', onClick: () => navigate('/jobs') }} /></DashboardLayout>;
  return <DashboardLayout><div className="max-w-4xl"><button onClick={() => navigate('/jobs')} className="flex items-center gap-2 mb-6 text-sm" style={{ color: 'var(--text-muted)' }}><ArrowLeft size={16} /> Back to Jobs</button><div className="p-6 rounded-2xl border" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}><div className="flex items-start justify-between gap-4 mb-5"><div><h1 className="text-xl font-extrabold mb-1" style={{ color: 'var(--text-primary)' }}>{job.title}</h1><p className="text-sm mb-3" style={{ color: 'var(--text-secondary)' }}>{job.company?.name}</p><div className="flex flex-wrap gap-3 text-xs" style={{ color: 'var(--text-muted)' }}><span><MapPin size={12} className="inline mr-1" />{job.location}</span><span><Briefcase size={12} className="inline mr-1" />{job.type}</span><span><Clock size={12} className="inline mr-1" />{formatRelativeTime(job.posted)}</span>{job.salaryMin && <span><DollarSign size={12} className="inline mr-1" />{formatSalary(job.salaryMin, job.salaryMax)}</span>}</div></div><Button onClick={handleApply} disabled={applying || applied}>{applied ? <><CheckCircle size={15} /> Applied</> : <><Send size={15} /> {applying ? 'Applying...' : 'Apply now'}</>}</Button></div><p className="text-sm leading-relaxed mb-6" style={{ color: 'var(--text-secondary)' }}>{job.description}</p><h2 className="text-sm font-semibold mb-2" style={{ color: 'var(--text-primary)' }}>Skills</h2><div className="flex flex-wrap gap-2">{(job.skills || []).map(skill => <span key={skill} className="text-xs px-3 py-1 rounded-full" style={{ background: 'var(--accent-light)', color: 'var(--accent)' }}>{skill}</span>)}</div></div></div></DashboardLayout>;
}
