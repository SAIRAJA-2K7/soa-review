import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Briefcase, Plus, Search } from 'lucide-react';
import RecruiterLayout from '@/layouts/RecruiterLayout';
import Button from '@/components/ui/Button';
import Input from '@/components/ui/Input';
import EmptyState from '@/components/ui/EmptyState';
import { useBackendData } from '@/data/backendData';

export default function RecruiterJobs() {
  const { data: jobs, loading, error } = useBackendData('/api/admin/jobs');
  const [search, setSearch] = useState('');
  const navigate = useNavigate();
  const filtered = jobs.filter(job => !search || job.title.toLowerCase().includes(search.toLowerCase()) || job.location.toLowerCase().includes(search.toLowerCase()));
  return <RecruiterLayout><div className="space-y-5"><div className="flex items-center justify-between gap-4"><div><h1 className="text-2xl font-extrabold mb-1" style={{ color: 'var(--text-primary)' }}>Job Postings</h1><p className="text-sm" style={{ color: 'var(--text-muted)' }}>{jobs.length} active positions</p></div><Button size="sm" onClick={() => navigate('/recruiter/jobs/new')}><Plus size={14} /> Post Job</Button></div><Input placeholder="Search jobs..." value={search} onChange={event => setSearch(event.target.value)} icon={<Search size={15} />} />{loading ? <p className="text-sm" style={{ color: 'var(--text-muted)' }}>Loading jobs...</p> : error ? <EmptyState icon={<Briefcase size={22} />} title="Unable to load jobs" description={error.message} /> : filtered.length === 0 ? <EmptyState icon={<Briefcase size={22} />} title="No jobs found" description="Create your first job posting to start receiving applications." action={{ label: 'Post a Job', onClick: () => navigate('/recruiter/jobs/new') }} /> : <div className="rounded-2xl border overflow-hidden" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}><table className="w-full text-sm"><thead><tr style={{ borderBottom: '1px solid var(--border)' }}>{['Position', 'Location', 'Type', 'Posted'].map(label => <th key={label} className="px-4 py-3 text-left text-xs" style={{ color: 'var(--text-muted)' }}>{label}</th>)}</tr></thead><tbody>{filtered.map(job => <tr key={job.id} onClick={() => navigate(`/recruiter/jobs/${job.id}`)} className="cursor-pointer" style={{ borderBottom: '1px solid var(--border)' }}><td className="px-4 py-3" style={{ color: 'var(--text-primary)' }}>{job.title}</td><td className="px-4 py-3" style={{ color: 'var(--text-secondary)' }}>{job.location}</td><td className="px-4 py-3" style={{ color: 'var(--text-secondary)' }}>{job.type}</td><td className="px-4 py-3" style={{ color: 'var(--text-muted)' }}>{job.posted}</td></tr>)}</tbody></table></div>}</div></RecruiterLayout>;
}
