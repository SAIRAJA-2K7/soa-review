import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Briefcase, Plus, Search } from 'lucide-react';
import RecruiterLayout from '@/layouts/RecruiterLayout';
import Button from '@/components/ui/Button';
import Input from '@/components/ui/Input';
import EmptyState from '@/components/ui/EmptyState';
import { useBackendData } from '@/data/backendData';
import { callApi } from '@/utils/api';
import { useToast } from '@/context/ToastContext';

export default function RecruiterJobs() {
  const { data: jobs, loading, error, setData } = useBackendData('/api/admin/jobs');
  const [search, setSearch] = useState('');
  const navigate = useNavigate();
  const toast = useToast();
  const filtered = jobs.filter(job => !search || job.title.toLowerCase().includes(search.toLowerCase()) || job.location.toLowerCase().includes(search.toLowerCase()));
  async function setPublished(job, published, event) {
    event.stopPropagation();
    try {
      const updated = await callApi(`/api/jobs/${job.id}/published`, {
        method: 'PATCH',
        body: JSON.stringify({ published }),
      });
      setData(items => items.map(item => item.id === job.id ? updated : item));
      toast('success', published ? 'Job reopened' : 'Job closed', published ? 'Candidates can apply again.' : 'Candidates can no longer apply.');
    } catch (reason) {
      toast('error', 'Unable to update job', reason.message);
    }
  }
  async function deleteJob(job, event) {
    event.stopPropagation();
    if (!window.confirm(`Delete "${job.title}"? Existing applications will remain in the system.`)) return;
    try {
      await callApi(`/api/jobs/${job.id}`, { method: 'DELETE' });
      setData(items => items.filter(item => item.id !== job.id));
      toast('success', 'Job deleted', 'The job posting has been removed.');
    } catch (reason) {
      toast('error', 'Unable to delete job', reason.message);
    }
  }
  return (
    <RecruiterLayout>
      <div className="space-y-5">
        <div className="flex items-center justify-between gap-4">
          <div><h1 className="text-2xl font-extrabold mb-1" style={{ color: 'var(--text-primary)' }}>Job Postings</h1><p className="text-sm" style={{ color: 'var(--text-muted)' }}>{jobs.length} positions</p></div>
          <Button size="sm" onClick={() => navigate('/recruiter/jobs/new')}><Plus size={14} /> Post Job</Button>
        </div>
        <Input placeholder="Search jobs..." value={search} onChange={event => setSearch(event.target.value)} icon={<Search size={15} />} />
        {loading ? <p className="text-sm" style={{ color: 'var(--text-muted)' }}>Loading jobs...</p> : error ? <EmptyState icon={<Briefcase size={22} />} title="Unable to load jobs" description={error.message} /> : filtered.length === 0 ? <EmptyState icon={<Briefcase size={22} />} title="No jobs found" description="Create your first job posting to start receiving applications." action={{ label: 'Post a Job', onClick: () => navigate('/recruiter/jobs/new') }} /> :
          <div className="rounded-2xl border overflow-hidden" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}>
            <table className="w-full text-sm">
              <thead><tr style={{ borderBottom: '1px solid var(--border)' }}>{['Position', 'Location', 'Type', 'Status', 'Action'].map(label => <th key={label} className="px-4 py-3 text-left text-xs" style={{ color: 'var(--text-muted)' }}>{label}</th>)}</tr></thead>
              <tbody>{filtered.map(job => (
                <tr key={job.id} onClick={() => navigate(`/recruiter/jobs/${job.id}`)} className="cursor-pointer" style={{ borderBottom: '1px solid var(--border)' }}>
                  <td className="px-4 py-3" style={{ color: 'var(--text-primary)' }}>{job.title}</td>
                  <td className="px-4 py-3" style={{ color: 'var(--text-secondary)' }}>{job.location}</td>
                  <td className="px-4 py-3" style={{ color: 'var(--text-secondary)' }}>{job.type}</td>
                  <td className="px-4 py-3" style={{ color: job.published === false ? 'var(--error)' : 'var(--success)' }}>{job.published === false ? 'Closed' : 'Published'}</td>
                  <td className="px-4 py-3">
                    <button type="button" onClick={event => setPublished(job, job.published === false, event)} className="text-xs rounded-lg border px-3 py-1" style={{ borderColor: 'var(--border-strong)', color: 'var(--text-primary)' }}>
                      {job.published === false ? 'Reopen' : 'Close'}
                    </button>
                    <button type="button" onClick={event => deleteJob(job, event)} className="ml-2 text-xs rounded-lg border px-3 py-1" style={{ borderColor: 'var(--error)', color: 'var(--error)' }}>Delete</button>
                  </td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        }
      </div>
    </RecruiterLayout>
  );
}
