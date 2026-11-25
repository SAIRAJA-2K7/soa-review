import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, MapPin, Bookmark, BookmarkCheck, ArrowRight, BellRing } from 'lucide-react';
import DashboardLayout from '@/layouts/DashboardLayout';
import Input from '@/components/ui/Input';
import Badge from '@/components/ui/Badge';
import EmptyState from '@/components/ui/EmptyState';
import { useBackendData } from '@/data/backendData';
import { callApi } from '@/utils/api';
import { formatSalary, formatRelativeTime } from '@/utils/formatters';
import { useToast } from '@/context/ToastContext';

function readStorage(key, fallback) {
  try {
    const value = localStorage.getItem(key);
    return value ? JSON.parse(value) : fallback;
  } catch {
    return fallback;
  }
}

function matchesSearch(job, filters) {
  const query = filters.search.trim().toLowerCase();
  const location = filters.location.trim().toLowerCase();
  const title = String(job.title || '').toLowerCase();
  const company = String(job.company?.name || '').toLowerCase();
  const jobLocation = String(job.location || '').toLowerCase();
  const skills = Array.isArray(job.skills) ? job.skills : [];
  return (!query || title.includes(query) || company.includes(query) || skills.some(skill => String(skill).toLowerCase().includes(query))) &&
    (!location || jobLocation.includes(location)) &&
    (!filters.type || job.type === filters.type) &&
    (!filters.salary || Number(job.salaryMin || 0) >= Number(filters.salary));
}

export default function Jobs() {
  const { data: jobs, loading, error } = useBackendData('/api/jobs');
  const savedSearch = readStorage('ts-saved-job-search', {});
  const [search, setSearch] = useState(savedSearch.search || '');
  const [locationFilter, setLocationFilter] = useState(savedSearch.location || '');
  const [typeFilter, setTypeFilter] = useState(savedSearch.type || '');
  const [salaryFilter, setSalaryFilter] = useState(savedSearch.salary || '');
  const [alertEnabled, setAlertEnabled] = useState(() => localStorage.getItem('ts-job-alert') === 'true');
  const [newMatchesCount, setNewMatchesCount] = useState(0);
  const [recentSearches, setRecentSearches] = useState(() => readStorage('ts-recent-job-searches', []));
  const [saved, setSaved] = useState(new Set());
  const navigate = useNavigate();
  const toast = useToast();
  const filters = useMemo(() => ({ search, location: locationFilter, type: typeFilter, salary: salaryFilter }), [search, locationFilter, typeFilter, salaryFilter]);
  const filtered = jobs.filter(job => matchesSearch(job, filters));
  useEffect(() => {
    if (!alertEnabled || loading) return;
    const existing = readStorage('ts-saved-job-search', null);
    const matches = jobs.filter(job => matchesSearch(job, {
      search: existing?.search || '',
      location: existing?.location || '',
      type: existing?.type || '',
      salary: existing?.salary || '',
    }));
    const currentIds = matches.map(job => String(job.id));
    if (existing?.jobIds && existing.criteria === JSON.stringify({
      search: existing.search || '',
      location: existing.location || '',
      type: existing.type || '',
      salary: existing.salary || '',
    })) {
      const previousIds = new Set(existing.jobIds);
      setNewMatchesCount(matches.filter(job => !previousIds.has(String(job.id))).length);
    }
    const criteria = { search: existing?.search || '', location: existing?.location || '', type: existing?.type || '', salary: existing?.salary || '' };
    localStorage.setItem('ts-saved-job-search', JSON.stringify({ ...criteria, criteria: JSON.stringify(criteria), jobIds: currentIds }));
  }, [jobs, loading, alertEnabled]);
  function rememberSearch() {
    const query = search.trim();
    if (!query) return;
    const next = [query, ...recentSearches.filter(item => item !== query)].slice(0, 6);
    setRecentSearches(next);
    localStorage.setItem('ts-recent-job-searches', JSON.stringify(next));
  }
  function saveSearchAlert() {
    const criteria = { search, location: locationFilter, type: typeFilter, salary: salaryFilter };
    const matchIds = jobs.filter(job => matchesSearch(job, criteria)).map(job => String(job.id));
    localStorage.setItem('ts-saved-job-search', JSON.stringify({ ...criteria, criteria: JSON.stringify(criteria), jobIds: matchIds }));
    localStorage.setItem('ts-job-alert', 'true');
    setNewMatchesCount(0);
    setAlertEnabled(true);
    toast('success', 'Search alert saved', 'We will show new matches the next time you visit this page in this browser.');
  }
  async function toggleSave(id, event) {
    event.stopPropagation();
    const nextSaved = !saved.has(id);
    try {
      await callApi(`/api/user/saved-jobs/${id}`, { method: nextSaved ? 'POST' : 'DELETE' });
      setSaved(previous => {
        const next = new Set(previous);
        nextSaved ? next.add(id) : next.delete(id);
        return next;
      });
    } catch (reason) {
      toast('error', 'Unable to update saved jobs', reason.message);
    }
  }
  return <DashboardLayout><div className="space-y-5">
    <div><h1 className="text-2xl font-extrabold mb-1" style={{ color: 'var(--text-primary)', fontFamily: 'Plus Jakarta Sans, sans-serif' }}>Find Jobs</h1><p className="text-sm" style={{ color: 'var(--text-muted)' }}>{jobs.length} positions available</p></div>
    <div className="flex flex-col sm:flex-row gap-3">
      <div className="flex-1">
        <Input id="job-search" list="recent-job-searches" placeholder="Role, company, or skill..." value={search} onChange={event => setSearch(event.target.value)} onKeyDown={event => { if (event.key === 'Enter') rememberSearch(); }} icon={<Search size={15} />} />
        <datalist id="recent-job-searches">{recentSearches.map(term => <option key={term} value={term} />)}</datalist>
      </div>
      <Input placeholder="Location or Remote" value={locationFilter} onChange={event => setLocationFilter(event.target.value)} icon={<MapPin size={15} />} className="sm:w-52" />
      <select aria-label="Minimum salary" value={salaryFilter} onChange={event => setSalaryFilter(event.target.value)} className="rounded-lg border px-3 py-2 text-xs" style={{ background: 'var(--surface)', borderColor: 'var(--border-strong)', color: 'var(--text-secondary)' }}>
        <option value="">Any salary</option><option value="50000">$50k+</option><option value="100000">$100k+</option><option value="150000">$150k+</option>
      </select>
      <button type="button" role="switch" aria-checked={alertEnabled} onClick={() => {
        if (alertEnabled) {
          localStorage.setItem('ts-job-alert', 'false');
          setAlertEnabled(false);
          toast('info', 'Job alert paused');
        } else {
          saveSearchAlert();
        }
      }} className="px-3 py-2 rounded-lg border text-xs font-medium whitespace-nowrap" style={{ background: alertEnabled ? 'var(--accent-light)' : 'var(--surface)', borderColor: alertEnabled ? 'var(--accent)' : 'var(--border-strong)', color: alertEnabled ? 'var(--accent)' : 'var(--text-secondary)' }}>
        {alertEnabled ? 'Job alert on' : 'Save search alert'}
      </button>
    </div>
    {newMatchesCount > 0 && <div role="status" className="flex items-center gap-2 rounded-xl border p-3 text-sm" style={{ background: 'var(--accent-light)', borderColor: 'var(--accent)', color: 'var(--text-primary)' }}><BellRing size={16} /> {newMatchesCount} new job{newMatchesCount === 1 ? '' : 's'} match your saved search.</div>}
    <div className="flex flex-wrap gap-2">{['Full-time', 'Remote', 'Contract'].map(type => <button key={type} onClick={() => setTypeFilter(typeFilter === type ? '' : type)} className="px-3 py-2 rounded-lg border text-xs font-medium" style={{ background: typeFilter === type ? 'var(--accent)' : 'var(--surface)', borderColor: typeFilter === type ? 'var(--accent)' : 'var(--border-strong)', color: typeFilter === type ? 'white' : 'var(--text-secondary)' }}>{type}</button>)}</div>
    {loading ? <p className="text-sm" style={{ color: 'var(--text-muted)' }}>Loading jobs...</p> : error ? <EmptyState icon={<Search size={22} />} title="Unable to load jobs" description={error.message} /> : filtered.length === 0 ? <EmptyState icon={<Search size={22} />} title="No jobs match your search" description="Try adjusting your filters." /> : <div className="grid md:grid-cols-2 gap-4">{filtered.map(job => <div key={job.id} onClick={() => navigate(`/jobs/${job.id}`)} className="p-5 rounded-2xl border cursor-pointer group" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}><div className="flex items-start justify-between gap-3 mb-3"><div><h3 className="text-sm font-semibold" style={{ color: 'var(--text-primary)' }}>{job.title}</h3><p className="text-xs" style={{ color: 'var(--text-muted)' }}>{job.company?.name}</p></div><button onClick={event => toggleSave(job.id, event)} aria-label="Save job">{saved.has(job.id) ? <BookmarkCheck size={16} style={{ color: 'var(--accent)' }} /> : <Bookmark size={16} style={{ color: 'var(--text-muted)' }} />}</button></div><div className="flex flex-wrap gap-1.5 mb-3"><Badge variant="muted">{job.location}</Badge><Badge variant="muted">{job.type}</Badge><Badge variant="muted">{job.experience}</Badge></div>{job.salaryMin && <p className="text-xs font-mono mb-3" style={{ color: 'var(--success)' }}>{formatSalary(job.salaryMin, job.salaryMax)}</p>}<div className="flex items-center justify-between"><span className="text-[10px]" style={{ color: 'var(--text-muted)' }}>{formatRelativeTime(job.posted)}</span><ArrowRight size={14} style={{ color: 'var(--accent)' }} /></div></div>)}</div>}
  </div></DashboardLayout>;
}
