import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Briefcase, FileText, Calendar, Bookmark, ArrowRight, TrendingUp } from 'lucide-react';
import DashboardLayout from '@/layouts/DashboardLayout';
import { useBackendData } from '@/data/backendData';
import {
  PieChart, Pie, Cell, BarChart, Bar, AreaChart, Area,
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from 'recharts';

export default function Dashboard() {
  const navigate = useNavigate();
  const { data: jobs, loading: jobsLoading } = useBackendData('/api/jobs');
  const { data: applications, loading: applicationsLoading } = useBackendData('/api/applications');
  const { data: interviews } = useBackendData('/api/user/interviews');
  const { data: profile } = useBackendData('/api/user/profile', {});
  const [period, setPeriod] = useState('30');

  const filteredApplications = useMemo(() => {
    if (period === 'all') return applications;
    const cutoff = new Date();
    cutoff.setDate(cutoff.getDate() - Number(period));
    return applications.filter(application => {
      const date = new Date(application.appliedDate);
      return Number.isNaN(date.getTime()) || date >= cutoff;
    });
  }, [applications, period]);

  const statuses = ['under_review', 'shortlisted', 'interview', 'selected', 'rejected'];
  const statusEntries = statuses.map(status => ({
    name: status.replace('_', ' '),
    value: filteredApplications.filter(application => application.status === status).length,
  })).filter(entry => entry.value > 0);
  const stageEntries = statuses.map(status => ({
    stage: status.replace('_', ' '),
    count: filteredApplications.filter(application => application.status === status).length,
  }));
  const trendMap = filteredApplications.reduce((result, application) => {
    const date = String(application.appliedDate || '').slice(0, 10) || 'Unknown';
    result[date] = (result[date] || 0) + 1;
    return result;
  }, {});
  const trendEntries = Object.entries(trendMap).map(([date, count]) => ({ date, count })).sort((a, b) => a.date.localeCompare(b.date));
  const shortlisted = filteredApplications.filter(application => application.status === 'shortlisted').length;
  const selected = filteredApplications.filter(application => application.status === 'selected').length;
  const successRate = filteredApplications.length ? Math.round(selected * 100 / filteredApplications.length) : 0;
  const profileTerms = String(profile.professionalTitle || '').toLowerCase().split(/\W+/).filter(term => term.length > 2);
  const recommendedJobs = [...jobs].map(job => {
    const text = `${job.title || ''} ${job.company?.name || ''} ${(job.skills || []).join(' ')}`.toLowerCase();
    return { job, score: profileTerms.filter(term => text.includes(term)).length };
  }).sort((left, right) => right.score - left.score)
    .filter(item => item.score > 0)
    .slice(0, 3)
    .map(item => item.job);
  const recentActivity = [
    ...filteredApplications.map(application => ({
      id: `application-${application.id}`,
      label: `Application ${String(application.status || 'submitted').replace('_', ' ')}`,
      detail: application.jobTitle,
      date: application.updatedAt || application.appliedDate,
    })),
    ...interviews.map(interview => ({
      id: `interview-${interview.id}`,
      label: 'Interview scheduled',
      detail: interview.job,
      date: interview.date,
    })),
  ].sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime()).slice(0, 5);
  const colors = ['#6366f1', '#8b5cf6', '#f59e0b', '#22c55e', '#ef4444'];
  const metrics = [
    { label: 'Available jobs', value: jobs.length, icon: Briefcase, action: () => navigate('/jobs') },
    { label: 'Applications', value: filteredApplications.length, icon: FileText, action: () => navigate('/applications') },
    { label: 'Interviews', value: interviews.length, icon: Calendar, action: () => navigate('/interviews') },
    { label: 'Shortlisted', value: shortlisted, icon: TrendingUp, action: () => navigate('/applications') },
  ];
  const loading = jobsLoading || applicationsLoading;

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-3">
          <div>
            <h1 className="text-2xl font-extrabold" style={{ color: 'var(--text-primary)' }}>Candidate Dashboard</h1>
            <p className="text-sm" style={{ color: 'var(--text-muted)' }}>Track applications, interviews, and opportunities.</p>
          </div>
          <div className="flex gap-2">
            {[['7', '7 days'], ['30', '30 days'], ['all', 'All time']].map(([value, label]) =>
              <button key={value} type="button" onClick={() => setPeriod(value)} className="px-3 py-2 rounded-lg border text-xs font-medium" style={{ background: period === value ? 'var(--accent)' : 'var(--surface)', borderColor: period === value ? 'var(--accent)' : 'var(--border)', color: period === value ? 'white' : 'var(--text-secondary)' }}>{label}</button>
            )}
          </div>
        </div>

        <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {metrics.map(metric => (
            <button key={metric.label} type="button" onClick={metric.action} className="p-5 rounded-2xl border text-left transition-transform hover:-translate-y-1" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}>
              <metric.icon size={18} style={{ color: 'var(--accent)' }} />
              <p className="text-xs mt-3" style={{ color: 'var(--text-muted)' }}>{metric.label}</p>
              {loading ? <span aria-label={`Loading ${metric.label}`} className="skeleton mt-2 block h-8 w-16 rounded" /> : <p className="text-2xl font-extrabold" style={{ color: 'var(--text-primary)' }}>{metric.value}</p>}
            </button>
          ))}
        </div>

        <div className="grid lg:grid-cols-3 gap-4">
          <div className="lg:col-span-2 p-5 rounded-2xl border" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}>
            <div className="flex items-center justify-between mb-4">
              <div><h2 className="text-sm font-semibold" style={{ color: 'var(--text-primary)' }}>Application activity</h2><p className="text-xs" style={{ color: 'var(--text-muted)' }}>Applications submitted over time</p></div>
              <TrendingUp size={17} style={{ color: 'var(--accent)' }} />
            </div>
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={trendEntries}>
                  <defs><linearGradient id="candidateArea" x1="0" y1="0" x2="0" y2="1"><stop offset="5%" stopColor="#6366f1" stopOpacity={0.35} /><stop offset="95%" stopColor="#6366f1" stopOpacity={0} /></linearGradient></defs>
                  <CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="date" /><YAxis allowDecimals={false} /><Tooltip /><Area type="monotone" dataKey="count" stroke="#6366f1" fill="url(#candidateArea)" strokeWidth={3} />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </div>

          <div className="p-5 rounded-2xl border" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}>
            <h2 className="text-sm font-semibold mb-1" style={{ color: 'var(--text-primary)' }}>Application status</h2>
            <p className="text-xs" style={{ color: 'var(--text-muted)' }}>{successRate}% selection rate</p>
            <div className="h-52">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart><Pie data={statusEntries} dataKey="value" nameKey="name" cx="50%" cy="50%" innerRadius={50} outerRadius={78} paddingAngle={3} label>{statusEntries.map((entry, index) => <Cell key={entry.name} fill={colors[index % colors.length]} />)}</Pie><Tooltip /></PieChart>
              </ResponsiveContainer>
            </div>
          </div>

          <div className="p-5 rounded-2xl border" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}>
            <h2 className="text-sm font-semibold mb-4" style={{ color: 'var(--text-primary)' }}>Recruitment pipeline</h2>
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={stageEntries} layout="vertical"><CartesianGrid strokeDasharray="3 3" /><XAxis type="number" allowDecimals={false} /><YAxis type="category" dataKey="stage" width={85} /><Tooltip /><Bar dataKey="count" fill="#6366f1" radius={[0, 6, 6, 0]} /></BarChart>
              </ResponsiveContainer>
            </div>
          </div>

          <div className="lg:col-span-2 p-5 rounded-2xl border" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}>
            <div className="flex items-center justify-between mb-4"><h2 className="text-sm font-semibold" style={{ color: 'var(--text-primary)' }}>Recommended next actions</h2><ArrowRight size={16} style={{ color: 'var(--accent)' }} /></div>
            <div className="grid sm:grid-cols-3 gap-3">
              <button type="button" onClick={() => navigate('/jobs')} className="p-4 rounded-xl border text-left" style={{ borderColor: 'var(--border)' }}><Briefcase size={17} style={{ color: 'var(--accent)' }} /><p className="text-sm font-semibold mt-2" style={{ color: 'var(--text-primary)' }}>Discover jobs</p><p className="text-xs mt-1" style={{ color: 'var(--text-muted)' }}>{jobs.length} openings available</p></button>
              <button type="button" onClick={() => navigate('/applications')} className="p-4 rounded-xl border text-left" style={{ borderColor: 'var(--border)' }}><FileText size={17} style={{ color: '#8b5cf6' }} /><p className="text-sm font-semibold mt-2" style={{ color: 'var(--text-primary)' }}>Track applications</p><p className="text-xs mt-1" style={{ color: 'var(--text-muted)' }}>Review your latest status</p></button>
              <button type="button" onClick={() => navigate('/saved')} className="p-4 rounded-xl border text-left" style={{ borderColor: 'var(--border)' }}><Bookmark size={17} style={{ color: '#22c55e' }} /><p className="text-sm font-semibold mt-2" style={{ color: 'var(--text-primary)' }}>Saved jobs</p><p className="text-xs mt-1" style={{ color: 'var(--text-muted)' }}>Continue where you left off</p></button>
            </div>
          </div>
        </div>
        <div className="grid lg:grid-cols-2 gap-4">
          <section className="p-5 rounded-2xl border" aria-labelledby="candidate-recommendations-title" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}>
            <div className="flex items-center justify-between mb-4">
              <div><h2 id="candidate-recommendations-title" className="text-sm font-semibold" style={{ color: 'var(--text-primary)' }}>Suggested for your profile</h2><p className="text-xs mt-1" style={{ color: 'var(--text-muted)' }}>Matched using your professional title</p></div>
              <button type="button" onClick={() => navigate('/profile')} className="text-xs font-medium" style={{ color: 'var(--accent)' }}>Improve profile</button>
            </div>
            {recommendedJobs.length ? <div className="space-y-2">{recommendedJobs.map(job => (
              <button type="button" key={job.id} onClick={() => navigate(`/jobs/${job.id}`)} className="w-full flex items-center justify-between gap-3 p-3 rounded-xl border text-left" style={{ borderColor: 'var(--border)' }}>
                <span><span className="block text-sm font-medium" style={{ color: 'var(--text-primary)' }}>{job.title}</span><span className="block text-xs mt-1" style={{ color: 'var(--text-muted)' }}>{job.company?.name} · {job.location}</span></span>
                <ArrowRight size={15} style={{ color: 'var(--accent)' }} />
              </button>
            ))}</div> : <p className="text-sm" style={{ color: 'var(--text-muted)' }}>Add a professional title to your profile to see job suggestions.</p>}
          </section>
          <section className="p-5 rounded-2xl border" aria-labelledby="candidate-activity-title" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}>
            <div className="flex items-center justify-between mb-4"><h2 id="candidate-activity-title" className="text-sm font-semibold" style={{ color: 'var(--text-primary)' }}>Recent activity</h2><button type="button" onClick={() => navigate('/applications')} className="text-xs font-medium" style={{ color: 'var(--accent)' }}>View applications</button></div>
            {recentActivity.length ? <ol className="space-y-3">{recentActivity.map(activity => (
              <li key={activity.id} className="flex gap-3">
                <span aria-hidden="true" className="mt-1.5 w-2 h-2 rounded-full shrink-0" style={{ background: 'var(--accent)' }} />
                <span className="min-w-0"><span className="block text-sm font-medium capitalize" style={{ color: 'var(--text-primary)' }}>{activity.label}</span><span className="block text-xs mt-1 truncate" style={{ color: 'var(--text-muted)' }}>{activity.detail} · {activity.date || 'Date unavailable'}</span></span>
              </li>
            ))}</ol> : <p className="text-sm" style={{ color: 'var(--text-muted)' }}>Your application and interview activity will show here.</p>}
          </section>
        </div>
      </div>
    </DashboardLayout>
  );
}
