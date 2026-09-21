import { useState } from 'react';
import { Calendar } from 'lucide-react';
import RecruiterLayout from '@/layouts/RecruiterLayout';
import Button from '@/components/ui/Button';
import Input from '@/components/ui/Input';
import EmptyState from '@/components/ui/EmptyState';
import { useBackendData } from '@/data/backendData';
import { callApi } from '@/utils/api';
import { useToast } from '@/context/ToastContext';

export default function RecruiterInterviews() {
  const { data: interviews, loading, error, setData } = useBackendData('/api/admin/interviews');
  const { data: candidates } = useBackendData('/api/admin/candidates');
  const { data: jobs } = useBackendData('/api/admin/jobs');
  const [form, setForm] = useState({ candidateId: '', jobId: '', scheduledAt: '', type: 'Technical', mode: 'Video Call' });
  const [saving, setSaving] = useState(false);
  const toast = useToast();
  const update = event => setForm(previous => ({ ...previous, [event.target.name]: event.target.value }));
  async function schedule(event) {
    event.preventDefault();
    setSaving(true);
    try {
      const created = await callApi('/api/admin/interviews', { method: 'POST', body: JSON.stringify(form) });
      setData(previous => [...previous, created]);
      setForm(previous => ({ ...previous, candidateId: '', jobId: '', scheduledAt: '' }));
      toast('success', 'Interview scheduled', 'The candidate can now see the interview.');
    } catch (reason) { toast('error', 'Unable to schedule interview', reason.message); }
    finally { setSaving(false); }
  }
  return <RecruiterLayout><div className="space-y-5"><div><h1 className="text-2xl font-extrabold mb-1" style={{ color: 'var(--text-primary)' }}>Interviews</h1><p className="text-sm" style={{ color: 'var(--text-muted)' }}>{interviews.length} interviews scheduled</p></div><form onSubmit={schedule} className="grid sm:grid-cols-2 lg:grid-cols-5 gap-2 p-4 rounded-xl border" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}><select name="candidateId" value={form.candidateId} onChange={update} required className="rounded-lg border p-2 text-xs" style={{ background: 'var(--surface)', color: 'var(--text-primary)', borderColor: 'var(--border)' }}><option value="">Candidate</option>{candidates.map(candidate => <option key={candidate.id} value={candidate.id}>{candidate.name}</option>)}</select><select name="jobId" value={form.jobId} onChange={update} required className="rounded-lg border p-2 text-xs" style={{ background: 'var(--surface)', color: 'var(--text-primary)', borderColor: 'var(--border)' }}><option value="">Job</option>{jobs.map(job => <option key={job.id} value={job.id}>{job.title}</option>)}</select><Input name="scheduledAt" type="datetime-local" value={form.scheduledAt} onChange={update} required /><select name="mode" value={form.mode} onChange={update} className="rounded-lg border p-2 text-xs" style={{ background: 'var(--surface)', color: 'var(--text-primary)', borderColor: 'var(--border)' }}><option>Video Call</option><option>In-Person</option></select><Button type="submit" loading={saving}><Calendar size={14} /> Schedule</Button></form>{loading ? <p className="text-sm" style={{ color: 'var(--text-muted)' }}>Loading interviews...</p> : error ? <EmptyState icon={<Calendar size={22} />} title="Unable to load interviews" description={error.message} /> : interviews.length === 0 ? <EmptyState icon={<Calendar size={22} />} title="No interviews scheduled" description="Schedule interviews with shortlisted candidates." /> : <div className="grid sm:grid-cols-2 gap-4">{interviews.map(interview => <div key={interview.id} className="p-5 rounded-2xl border" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}><p className="text-sm font-semibold" style={{ color: 'var(--text-primary)' }}>{interview.candidate}</p><p className="text-xs" style={{ color: 'var(--text-muted)' }}>{interview.job}</p><p className="text-xs mt-3" style={{ color: 'var(--text-secondary)' }}>{interview.date} at {interview.time} · {interview.mode}</p></div>)}</div>}</div></RecruiterLayout>;
}
