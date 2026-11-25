import { useState } from 'react';
import { Calendar } from 'lucide-react';
import RecruiterLayout from '@/layouts/RecruiterLayout';
import Button from '@/components/ui/Button';
import Input from '@/components/ui/Input';
import EmptyState from '@/components/ui/EmptyState';
import { useBackendData } from '@/data/backendData';
import { callApi } from '@/utils/api';
import { useToast } from '@/context/ToastContext';
import { downloadInterviewInvite } from '@/utils/calendar';

export default function RecruiterInterviews() {
  const { data: interviews, loading, error, setData } = useBackendData('/api/admin/interviews');
  const { data: applications } = useBackendData('/api/admin/applications');
  const [form, setForm] = useState({ candidateId: '', jobId: '', scheduledAt: '', type: 'Technical', mode: 'Video Call' });
  const [saving, setSaving] = useState(false);
  const toast = useToast();
  const update = event => setForm(previous => ({ ...previous, [event.target.name]: event.target.value }));
  const shortlistedApplications = applications.filter(application => application.status === 'shortlisted');
  const candidateOptions = [...new Map(shortlistedApplications.map(application => [
    String(application.candidateId),
    { id: application.candidateId, name: application.candidateName },
  ])).values()];
  const candidateApplications = shortlistedApplications.filter(application =>
    String(application.candidateId) === form.candidateId);
  const jobOptions = [...new Map(candidateApplications.map(application => [
    String(application.jobId),
    { id: application.jobId, title: application.jobTitle },
  ])).values()];
  function updateCandidate(event) {
    const candidateId = event.target.value;
    const firstEligibleJob = shortlistedApplications.find(application =>
      String(application.candidateId) === candidateId);
    setForm(previous => ({
      ...previous,
      candidateId,
      jobId: firstEligibleJob ? String(firstEligibleJob.jobId) : '',
    }));
  }
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
  async function reschedule(interview) {
    const scheduledAt = window.prompt('New date and time (YYYY-MM-DDTHH:mm)', `${interview.date}T${interview.time}`);
    if (!scheduledAt) return;
    try {
      const updated = await callApi(`/api/admin/interviews/${interview.id}`, {
        method: 'PATCH',
        body: JSON.stringify({ scheduledAt }),
      });
      setData(previous => previous.map(item => item.id === interview.id ? updated : item));
      toast('success', 'Interview rescheduled', 'The candidate can see the updated time.');
    } catch (reason) { toast('error', 'Unable to reschedule interview', reason.message); }
  }
  async function cancel(interview) {
    if (!window.confirm(`Cancel the interview with ${interview.candidate}?`)) return;
    try {
      await callApi(`/api/admin/interviews/${interview.id}`, { method: 'DELETE' });
      setData(previous => previous.filter(item => item.id !== interview.id));
      toast('success', 'Interview cancelled', 'The interview has been removed.');
    } catch (reason) { toast('error', 'Unable to cancel interview', reason.message); }
  }
  function addToCalendar(interview) {
    try {
      downloadInterviewInvite(interview);
    } catch (reason) {
      toast('error', 'Unable to create calendar invite', reason.message);
    }
  }
  return (
    <RecruiterLayout>
      <div className="space-y-5">
        <div><h1 className="text-2xl font-extrabold mb-1" style={{ color: 'var(--text-primary)' }}>Interviews</h1><p className="text-sm" style={{ color: 'var(--text-muted)' }}>{interviews.length} interviews scheduled</p></div>
        <form onSubmit={schedule} className="grid sm:grid-cols-2 lg:grid-cols-5 gap-2 p-4 rounded-xl border" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}>
          <select name="candidateId" value={form.candidateId} onChange={updateCandidate} required className="rounded-lg border p-2 text-xs" style={{ background: 'var(--surface)', color: 'var(--text-primary)', borderColor: 'var(--border)' }}><option value="">Shortlisted candidate</option>{candidateOptions.map(candidate => <option key={candidate.id} value={candidate.id}>{candidate.name}</option>)}</select>
          <select name="jobId" value={form.jobId} onChange={update} required className="rounded-lg border p-2 text-xs" style={{ background: 'var(--surface)', color: 'var(--text-primary)', borderColor: 'var(--border)' }}><option value="">Job</option>{jobOptions.map(job => <option key={job.id} value={job.id}>{job.title}</option>)}</select>
          <Input name="scheduledAt" type="datetime-local" value={form.scheduledAt} onChange={update} required />
          <select name="mode" value={form.mode} onChange={update} className="rounded-lg border p-2 text-xs" style={{ background: 'var(--surface)', color: 'var(--text-primary)', borderColor: 'var(--border)' }}><option>Video Call</option><option>In-Person</option></select>
          <Button type="submit" loading={saving}><Calendar size={14} /> Schedule</Button>
        </form>
        {loading ? <p className="text-sm" style={{ color: 'var(--text-muted)' }}>Loading interviews...</p> : error ? <EmptyState icon={<Calendar size={22} />} title="Unable to load interviews" description={error.message} /> : interviews.length === 0 ? <EmptyState icon={<Calendar size={22} />} title="No interviews scheduled" description="Schedule interviews with shortlisted candidates." /> :
          <div className="grid sm:grid-cols-2 gap-4">{interviews.map(interview => (
            <div key={interview.id} className="p-5 rounded-2xl border" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}>
              <p className="text-sm font-semibold" style={{ color: 'var(--text-primary)' }}>{interview.candidate}</p>
              <p className="text-xs" style={{ color: 'var(--text-muted)' }}>{interview.job}</p>
              <p className="text-xs mt-3" style={{ color: 'var(--text-secondary)' }}>{interview.date} at {interview.time} · {interview.mode}</p>
              <div className="flex gap-2 mt-4">
                <button type="button" onClick={() => addToCalendar(interview)} className="text-xs rounded-lg border px-3 py-1" style={{ borderColor: 'var(--border-strong)', color: 'var(--accent)' }}>Calendar</button>
                <button type="button" onClick={() => reschedule(interview)} className="text-xs rounded-lg border px-3 py-1" style={{ borderColor: 'var(--border-strong)', color: 'var(--text-primary)' }}>Reschedule</button>
                <button type="button" onClick={() => cancel(interview)} className="text-xs rounded-lg border px-3 py-1" style={{ borderColor: 'var(--error)', color: 'var(--error)' }}>Cancel</button>
              </div>
            </div>
          ))}</div>}
      </div>
    </RecruiterLayout>
  );
}
