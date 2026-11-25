import RecruiterLayout from '@/layouts/RecruiterLayout';
import { useBackendData } from '@/data/backendData';
import { useState } from 'react';
import { Download } from 'lucide-react';
import {
  BarChart, Bar, PieChart, Pie, Cell, LineChart, Line,
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from 'recharts';

export default function Analytics() {
  const { data, loading, error } = useBackendData('/api/admin/dashboard', {});
  const [range, setRange] = useState('all');
  const [customStart, setCustomStart] = useState('');
  const [customEnd, setCustomEnd] = useState('');
  const dateIsInRange = value => {
    const date = String(value || '').slice(0, 10);
    if (range === 'all') return true;
    if (range === '30') {
      const cutoff = new Date();
      cutoff.setDate(cutoff.getDate() - 30);
      const parsed = new Date(date);
      return Number.isNaN(parsed.getTime()) || parsed >= cutoff;
    }
    if (range === 'custom') {
      return (!customStart || date >= customStart) && (!customEnd || date <= customEnd);
    }
    return true;
  };
  const filteredApplications = (data.applications || []).filter(application => dateIsInRange(application.appliedDate));
  const filteredInterviews = (data.scheduledInterviews || []).filter(interview => dateIsInRange(interview.date));
  const countStatus = status => filteredApplications.filter(application => application.status === status).length;
  const stageNames = ['under_review', 'shortlisted', 'interview', 'selected', 'rejected'];
  const stageEntries = stageNames.map(stage => ({ stage: stage.replace('_', ' '), count: countStatus(stage) }));
  const funnelEntries = ['under_review', 'shortlisted', 'interview', 'selected'].map(stage => ({
    stage: stage.replace('_', ' '),
    count: countStatus(stage),
  }));
  const groupCounts = (records, key, fallback = 'Unknown') => records.reduce((counts, record) => {
    const value = String(record[key] || fallback);
    counts[value] = (counts[value] || 0) + 1;
    return counts;
  }, {});
  const jobEntries = Object.entries(groupCounts(filteredApplications, 'jobTitle')).map(([name, count]) => ({ name, count })).slice(0, 8);
  const locationEntries = Object.entries(groupCounts(filteredApplications, 'candidateLocation')).map(([name, count]) => ({ name, count })).slice(0, 8);
  const applicationEntries = Object.entries(groupCounts(filteredApplications, 'appliedDate')).map(([date, count]) => ({ date, count })).sort((a, b) => a.date.localeCompare(b.date));
  const interviewEntries = Object.entries(groupCounts(filteredInterviews, 'date')).map(([date, count]) => ({ date: date.slice(0, 10), count })).sort((a, b) => a.date.localeCompare(b.date));
  const decisionEntries = [
    { name: 'selected', value: countStatus('selected') },
    { name: 'rejected', value: countStatus('rejected') },
  ];
  const selectedApplications = filteredApplications.filter(application => application.status === 'selected');
  const hiringDurations = selectedApplications.map(application => {
    const applied = new Date(application.appliedDate);
    const decided = new Date(application.decisionDate || application.appliedDate);
    if (Number.isNaN(applied.getTime()) || Number.isNaN(decided.getTime())) return null;
    return Math.max(0, Math.floor((decided - applied) / 86_400_000));
  }).filter(Number.isFinite);
  const averageHiringDays = hiringDurations.length
    ? Math.round(hiringDurations.reduce((total, days) => total + days, 0) / hiringDurations.length * 10) / 10
    : 0;
  const selectionRate = filteredApplications.length ? Math.round(countStatus('selected') * 1000 / filteredApplications.length) / 10 : 0;
  const interviewConversionRate = filteredApplications.length
    ? Math.round((countStatus('interview') + countStatus('selected')) * 1000 / filteredApplications.length) / 10
    : 0;
  function exportCsv() {
    const applications = filteredApplications;
    const rows = [['Candidate', 'Job', 'Status', 'Applied Date'], ...applications.map(application =>
      [application.candidateName, application.jobTitle, application.status, application.appliedDate])];
    const csv = rows.map(row => row.map(value => `"${String(value ?? '').replaceAll('"', '""')}"`).join(',')).join('\n');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = 'talentsync-analytics.csv';
    link.click();
    URL.revokeObjectURL(url);
  }
  const colors = ['var(--accent)', '#22c55e', '#f59e0b', '#ef4444', '#8b5cf6'];
  const metricCards = [
    ['totalJobs', 'Total jobs', data.totalJobs], ['activeJobs', 'Active jobs', data.activeJobs],
    ['totalApplicants', 'Applications', filteredApplications.length], ['shortlisted', 'Shortlisted', countStatus('shortlisted')],
    ['interviews', 'Interviews', filteredInterviews.length], ['selected', 'Selected', countStatus('selected')],
    ['rejected', 'Rejected', countStatus('rejected')], ['selectionRate', 'Selection rate', selectionRate],
    ['interviewConversionRate', 'Interview conversion', interviewConversionRate],
    ['averageHiringDurationDays', 'Avg. hiring days', averageHiringDays],
  ];
  const chartCard = (title, children) => (
    <div className="p-5 rounded-2xl border" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}>
      <h2 className="text-sm font-semibold mb-4" style={{ color: 'var(--text-primary)' }}>{title}</h2>
      {children}
    </div>
  );
  return (
    <RecruiterLayout>
      <div className="space-y-5">
        <div className="flex flex-col lg:flex-row lg:items-end justify-between gap-3">
          <div>
            <h1 className="text-2xl font-extrabold" style={{ color: 'var(--text-primary)' }}>Analytics</h1>
            <p className="text-sm" style={{ color: 'var(--text-muted)' }}>Recruitment metrics calculated from persisted applications, jobs, and interviews.</p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <select aria-label="Analytics range" value={range} onChange={event => setRange(event.target.value)} className="rounded-lg border px-3 py-2 text-xs" style={{ background: 'var(--surface)', borderColor: 'var(--border-strong)', color: 'var(--text-secondary)' }}>
              <option value="all">All time</option><option value="30">Last 30 days</option><option value="custom">Custom range</option>
            </select>
            {range === 'custom' && <>
              <input aria-label="Start date" type="date" value={customStart} onChange={event => setCustomStart(event.target.value)} className="rounded-lg border px-2 py-2 text-xs" style={{ background: 'var(--surface)', borderColor: 'var(--border-strong)', color: 'var(--text-secondary)' }} />
              <input aria-label="End date" type="date" value={customEnd} onChange={event => setCustomEnd(event.target.value)} className="rounded-lg border px-2 py-2 text-xs" style={{ background: 'var(--surface)', borderColor: 'var(--border-strong)', color: 'var(--text-secondary)' }} />
            </>}
            <button type="button" onClick={exportCsv} className="flex items-center gap-2 px-3 py-2 rounded-lg border text-xs font-medium" style={{ borderColor: 'var(--border-strong)', color: 'var(--text-primary)' }}><Download size={14} /> Export CSV</button>
          </div>
        </div>
        {error ? (
          <p className="text-sm" style={{ color: 'var(--error)' }}>{error.message}</p>
        ) : (
          <>
            <div className="grid sm:grid-cols-2 lg:grid-cols-5 gap-3">
              {metricCards.map(([key, label, value]) => (
                <div key={key} className="p-4 rounded-xl border" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}>
                  <p className="text-xs" style={{ color: 'var(--text-muted)' }}>{label}</p>
                  <p className="text-xl font-extrabold mt-1" style={{ color: 'var(--text-primary)' }}>
                    {loading ? '...' : `${value ?? 0}${key.includes('Rate') ? '%' : ''}`}
                  </p>
                </div>
              ))}
            </div>
            <div className="grid lg:grid-cols-2 gap-4">
              {chartCard('Applications by recruitment stage',
                <div className="h-64">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={stageEntries}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="stage" /><YAxis allowDecimals={false} /><Tooltip /><Bar dataKey="count" fill="#6366f1" radius={[6, 6, 0, 0]} /></BarChart>
                  </ResponsiveContainer>
                </div>)}
              {chartCard('Recruitment conversion funnel',
                <div className="h-64">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={funnelEntries} layout="vertical"><CartesianGrid strokeDasharray="3 3" /><XAxis type="number" allowDecimals={false} /><YAxis type="category" dataKey="stage" width={85} /><Tooltip /><Bar dataKey="count" fill="#22c55e" radius={[0, 6, 6, 0]} /></BarChart>
                  </ResponsiveContainer>
                </div>)}
              {chartCard('Applications by job position',
                <div className="h-64">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={jobEntries}><XAxis dataKey="name" hide /><YAxis allowDecimals={false} /><Tooltip /><Bar dataKey="count" fill="#8b5cf6" /></BarChart>
                  </ResponsiveContainer>
                </div>)}
              {chartCard('Applications over time',
                <div className="h-64">
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={applicationEntries}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="date" /><YAxis allowDecimals={false} /><Tooltip /><Line type="monotone" dataKey="count" stroke="#6366f1" strokeWidth={3} /></LineChart>
                  </ResponsiveContainer>
                </div>)}
              {chartCard('Interview trends',
                <div className="h-64">
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={interviewEntries}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="date" /><YAxis allowDecimals={false} /><Tooltip /><Line type="monotone" dataKey="count" stroke="#f59e0b" strokeWidth={3} /></LineChart>
                  </ResponsiveContainer>
                </div>)}
              {chartCard('Selection and rejection',
                <div className="h-64">
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart><Pie data={decisionEntries} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={80} label>{decisionEntries.map((entry, index) => <Cell key={entry.name} fill={colors[index % colors.length]} />)}</Pie><Tooltip /></PieChart>
                  </ResponsiveContainer>
                </div>)}
              {chartCard('Applications by location',
                <div className="h-64">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={locationEntries}><XAxis dataKey="name" hide /><YAxis allowDecimals={false} /><Tooltip /><Bar dataKey="count" fill="#ef4444" /></BarChart>
                  </ResponsiveContainer>
                </div>)}
            </div>
          </>
        )}
      </div>
    </RecruiterLayout>
  );
}
