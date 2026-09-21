import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, MapPin, Bookmark, BookmarkCheck, ArrowRight, X } from 'lucide-react';
import DashboardLayout from '@/layouts/DashboardLayout';
import Input from '@/components/ui/Input';
import Badge from '@/components/ui/Badge';
import EmptyState from '@/components/ui/EmptyState';
import { useBackendData } from '@/data/backendData';
import { callApi } from '@/utils/api';
import { formatSalary, formatRelativeTime } from '@/utils/formatters';

export default function Jobs() {
  const { data: jobs, loading, error } = useBackendData('/api/jobs');
  const [search, setSearch] = useState('');
  const [locationFilter, setLocationFilter] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [saved, setSaved] = useState(new Set());
  const navigate = useNavigate();
  const filtered = jobs.filter(job => {
    const query = search.toLowerCase();
    return (!query || job.title.toLowerCase().includes(query) || job.company?.name?.toLowerCase().includes(query) || job.skills?.some(skill => skill.toLowerCase().includes(query))) &&
      (!locationFilter || job.location.toLowerCase().includes(locationFilter.toLowerCase())) && (!typeFilter || job.type === typeFilter);
  });
  const toggleSave = async (id, event) => { event.stopPropagation(); const nextSaved = !saved.has(id); try { await callApi(`/api/user/saved-jobs/${id}`, { method: nextSaved ? 'POST' : 'DELETE' }); setSaved(previous => { const next = new Set(previous); nextSaved ? next.add(id) : next.delete(id); return next; }); } catch { /* keep the current visual state when persistence fails */ } };
  return <DashboardLayout><div className="space-y-5">
    <div><h1 className="text-2xl font-extrabold mb-1" style={{ color: 'var(--text-primary)', fontFamily: 'Plus Jakarta Sans, sans-serif' }}>Find Jobs</h1><p className="text-sm" style={{ color: 'var(--text-muted)' }}>{jobs.length} positions available</p></div>
    <div className="flex flex-col sm:flex-row gap-3"><div className="flex-1"><Input placeholder="Role, company, or skill..." value={search} onChange={event => setSearch(event.target.value)} icon={<Search size={15} />} /></div><Input placeholder="Location or Remote" value={locationFilter} onChange={event => setLocationFilter(event.target.value)} icon={<MapPin size={15} />} className="sm:w-52" /><div className="flex gap-2">{['Full-time', 'Remote', 'Contract'].map(type => <button key={type} onClick={() => setTypeFilter(typeFilter === type ? '' : type)} className="px-3 py-2 rounded-lg border text-xs font-medium" style={{ background: typeFilter === type ? 'var(--accent)' : 'var(--surface)', borderColor: typeFilter === type ? 'var(--accent)' : 'var(--border-strong)', color: typeFilter === type ? 'white' : 'var(--text-secondary)' }}>{type}</button>)}</div></div>
    {loading ? <p className="text-sm" style={{ color: 'var(--text-muted)' }}>Loading jobs...</p> : error ? <EmptyState icon={<Search size={22} />} title="Unable to load jobs" description={error.message} /> : filtered.length === 0 ? <EmptyState icon={<Search size={22} />} title="No jobs match your search" description="Try adjusting your filters." /> : <div className="grid md:grid-cols-2 gap-4">{filtered.map(job => <div key={job.id} onClick={() => navigate(`/jobs/${job.id}`)} className="p-5 rounded-2xl border cursor-pointer group" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}><div className="flex items-start justify-between gap-3 mb-3"><div><h3 className="text-sm font-semibold" style={{ color: 'var(--text-primary)' }}>{job.title}</h3><p className="text-xs" style={{ color: 'var(--text-muted)' }}>{job.company?.name}</p></div><button onClick={event => toggleSave(job.id, event)} aria-label="Save job">{saved.has(job.id) ? <BookmarkCheck size={16} style={{ color: 'var(--accent)' }} /> : <Bookmark size={16} style={{ color: 'var(--text-muted)' }} />}</button></div><div className="flex flex-wrap gap-1.5 mb-3"><Badge variant="muted">{job.location}</Badge><Badge variant="muted">{job.type}</Badge><Badge variant="muted">{job.experience}</Badge></div>{job.salaryMin && <p className="text-xs font-mono mb-3" style={{ color: 'var(--success)' }}>{formatSalary(job.salaryMin, job.salaryMax)}</p>}<div className="flex items-center justify-between"><span className="text-[10px]" style={{ color: 'var(--text-muted)' }}>{formatRelativeTime(job.posted)}</span><ArrowRight size={14} style={{ color: 'var(--accent)' }} /></div></div>)}</div>}
  </div></DashboardLayout>;
}
