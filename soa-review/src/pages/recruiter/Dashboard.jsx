import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Briefcase, FileText, Users, Calendar, TrendingUp, ArrowRight, CheckCircle } from 'lucide-react';
import RecruiterLayout from '@/layouts/RecruiterLayout';
import { useBackendData } from '@/data/backendData';
import {
  AreaChart, Area, BarChart, Bar, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from 'recharts';

export default function Dashboard() {
  const navigate = useNavigate();
  const { data, loading, error } = useBackendData('/api/admin/dashboard', {});
  const [period, setPeriod] = useState('30');
  const applications = data.applications || [];
  const interviews = data.scheduledInterviews || [];

  const filteredApplications = useMemo(() => {
    if (period === 'all') return applications;
    const cutoff = new Date();
    cutoff.setDate(cutoff.getDate() - Number(period));
    return applications.filter(application => {
      const date = new Date(application.appliedDate);
      return Number.isNaN(date.getTime()) || date >= cutoff;
    });
  }, [applications, period]);
  const filteredInterviews = useMemo(() => {
    if (period === 'all') return interviews;
    const cutoff = new Date();
    cutoff.setDate(cutoff.getDate() - Number(period));
    return interviews.filter(interview => {
      const date = new Date(interview.date);
      return Number.isNaN(date.getTime()) || date >= cutoff;
    });
  }, [interviews, period]);

  const stageEntries = ['under_review', 'shortlisted', 'interview', 'selected', 'rejected'].map(status => ({
    stage: status.replace('_', ' '),
    count: filteredApplications.filter(application => application.status === status).length,
  }));
  const trendMap = filteredApplications.reduce((result, application) => {
    const date = String(application.appliedDate || '').slice(0, 10) || 'Unknown';
    result[date] = (result[date] || 0) + 1;
    return result;
  }, {});
  const trendEntries = Object.entries(trendMap).map(([date, count]) => ({ date, count })).sort((a, b) => a.date.localeCompare(b.date));
  const decisionEntries = [
    { name: 'Selected', value: filteredApplications.filter(application => application.status === 'selected').length },
    { name: 'Rejected', value: filteredApplications.filter(application => application.status === 'rejected').length },
  ].filter(entry => entry.value > 0);
  const colors = ['#22c55e', '#ef4444'];
  const selectedCount = filteredApplications.filter(application => application.status === 'selected').length;
  const selectionRate = filteredApplications.length ? Math.round(selectedCount * 100 / filteredApplications.length) : 0;
  const recentActivity = [
    ...applications.map(application => {
      const timeline = Array.isArray(application.timeline) ? application.timeline : [];
      const latest = timeline[timeline.length - 1];
      return {
        id: `application-${application.id}`,
        title: `Application ${String(latest?.stage || application.status || 'updated').replace('_', ' ')}`,
        detail: `${application.candidateName} · ${application.jobTitle}`,
        date: latest?.date || application.appliedDate,
      };
    }),
    ...interviews.map(interview => ({
      id: `interview-${interview.id}`,
      title: 'Interview scheduled',
      detail: `${interview.candidate} · ${interview.job}`,
      date: interview.date,
    })),
  ].sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime()).slice(0, 6);
  const metrics = [
    ['activeJobs', 'Active jobs', data.activeJobs || 0, Briefcase, () => navigate('/recruiter/jobs')],
    ['totalApplicants', 'Applicants', filteredApplications.length, FileText, () => navigate('/recruiter/applications')],
    ['shortlisted', 'Shortlisted', filteredApplications.filter(application => application.status === 'shortlisted').length, Users, () => navigate('/recruiter/pipeline')],
    ['interviews', 'Interviews', filteredInterviews.length, Calendar, () => navigate('/recruiter/interviews')],
  ];

  return (
    <RecruiterLayout>
      <div className="space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-3">
          <div>
            <h1 className="text-2xl font-extrabold" style={{ color: 'var(--text-primary)' }}>Recruiter Dashboard</h1>
            <p className="text-sm" style={{ color: 'var(--text-muted)' }}>Live recruitment activity from your database.</p>
          </div>
          <div className="flex gap-2">
            {[['7', '7 days'], ['30', '30 days'], ['all', 'All time']].map(([value, label]) =>
              <button key={value} type="button" onClick={() => setPeriod(value)} className="px-3 py-2 rounded-lg border text-xs font-medium" style={{ background: period === value ? 'var(--accent)' : 'var(--surface)', borderColor: period === value ? 'var(--accent)' : 'var(--border)', color: period === value ? 'white' : 'var(--text-secondary)' }}>{label}</button>
            )}
          </div>
        </div>

        {error ? <p className="text-sm" style={{ color: 'var(--error)' }}>{error.message}</p> : (
          <>
            <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4">
              {metrics.map(([key, label, value, Icon, action]) => (
                <button key={key} type="button" onClick={action} className="p-5 rounded-2xl border text-left transition-transform hover:-translate-y-1" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}>
                  <Icon size={18} style={{ color: 'var(--accent)' }} />
                  <p className="text-xs mt-3" style={{ color: 'var(--text-muted)' }}>{label}</p>
                  {loading ? <span aria-label={`Loading ${label}`} className="skeleton mt-2 block h-8 w-16 rounded" /> : <p className="text-2xl font-extrabold" style={{ color: 'var(--text-primary)' }}>{value}</p>}
                </button>
              ))}
            </div>

            <div className="grid lg:grid-cols-3 gap-4">
              <div className="lg:col-span-2 p-5 rounded-2xl border" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}>
                <div className="flex items-center justify-between mb-4"><div><h2 className="text-sm font-semibold" style={{ color: 'var(--text-primary)' }}>Applications trend</h2><p className="text-xs" style={{ color: 'var(--text-muted)' }}>New applications over time</p></div><TrendingUp size={17} style={{ color: 'var(--accent)' }} /></div>
                <div className="h-64">
                  <ResponsiveContainer width="100%" height="100%">
                    <AreaChart data={trendEntries}>
                      <defs><linearGradient id="recruiterArea" x1="0" y1="0" x2="0" y2="1"><stop offset="5%" stopColor="#6366f1" stopOpacity={0.35} /><stop offset="95%" stopColor="#6366f1" stopOpacity={0} /></linearGradient></defs>
                      <CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="date" /><YAxis allowDecimals={false} /><Tooltip /><Area type="monotone" dataKey="count" stroke="#6366f1" fill="url(#recruiterArea)" strokeWidth={3} />
                    </AreaChart>
                  </ResponsiveContainer>
                </div>
              </div>

              <div className="p-5 rounded-2xl border" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}>
                <h2 className="text-sm font-semibold mb-1" style={{ color: 'var(--text-primary)' }}>Decision distribution</h2>
                <p className="text-xs" style={{ color: 'var(--text-muted)' }}>{selectionRate}% selection rate</p>
                <div className="h-52">
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart><Pie data={decisionEntries} dataKey="value" nameKey="name" cx="50%" cy="50%" innerRadius={50} outerRadius={78} paddingAngle={3}>{decisionEntries.map((entry, index) => <Cell key={entry.name} fill={colors[index % colors.length]} />)}</Pie><Tooltip /></PieChart>
                  </ResponsiveContainer>
                </div>
              </div>

              <div className="p-5 rounded-2xl border" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}>
                <h2 className="text-sm font-semibold mb-4" style={{ color: 'var(--text-primary)' }}>Pipeline stage</h2>
                <div className="h-64">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={stageEntries} layout="vertical"><CartesianGrid strokeDasharray="3 3" /><XAxis type="number" allowDecimals={false} /><YAxis type="category" dataKey="stage" width={85} /><Tooltip /><Bar dataKey="count" fill="#8b5cf6" radius={[0, 6, 6, 0]} /></BarChart>
                  </ResponsiveContainer>
                </div>
              </div>

              <div className="lg:col-span-2 p-5 rounded-2xl border" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}>
                <div className="flex items-center justify-between mb-4"><h2 className="text-sm font-semibold" style={{ color: 'var(--text-primary)' }}>Hiring actions</h2><ArrowRight size={16} style={{ color: 'var(--accent)' }} /></div>
                <div className="grid sm:grid-cols-3 gap-3">
                  <button type="button" onClick={() => navigate('/recruiter/jobs/new')} className="p-4 rounded-xl border text-left" style={{ borderColor: 'var(--border)' }}><Briefcase size={17} style={{ color: 'var(--accent)' }} /><p className="text-sm font-semibold mt-2" style={{ color: 'var(--text-primary)' }}>Post a job</p><p className="text-xs mt-1" style={{ color: 'var(--text-muted)' }}>Open a new position</p></button>
                  <button type="button" onClick={() => navigate('/recruiter/applications')} className="p-4 rounded-xl border text-left" style={{ borderColor: 'var(--border)' }}><CheckCircle size={17} style={{ color: '#22c55e' }} /><p className="text-sm font-semibold mt-2" style={{ color: 'var(--text-primary)' }}>Review applicants</p><p className="text-xs mt-1" style={{ color: 'var(--text-muted)' }}>{filteredApplications.length} applications</p></button>
                  <button type="button" onClick={() => navigate('/recruiter/interviews')} className="p-4 rounded-xl border text-left" style={{ borderColor: 'var(--border)' }}><Calendar size={17} style={{ color: '#f59e0b' }} /><p className="text-sm font-semibold mt-2" style={{ color: 'var(--text-primary)' }}>Schedule interview</p><p className="text-xs mt-1" style={{ color: 'var(--text-muted)' }}>{filteredInterviews.length} interviews scheduled</p></button>
                </div>
              </div>
            </div>
            <section className="p-5 rounded-2xl border" aria-labelledby="recruiter-activity-title" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}>
              <div className="flex items-center justify-between mb-4"><h2 id="recruiter-activity-title" className="text-sm font-semibold" style={{ color: 'var(--text-primary)' }}>Recent pipeline activity</h2><button type="button" onClick={() => navigate('/recruiter/applications')} className="text-xs font-medium" style={{ color: 'var(--accent)' }}>Open applications</button></div>
              {recentActivity.length ? <ol className="grid md:grid-cols-2 gap-3">{recentActivity.map(activity => (
                <li key={activity.id} className="flex gap-3 rounded-xl p-3" style={{ background: 'var(--surface-elevated)' }}>
                  <span aria-hidden="true" className="mt-1.5 w-2 h-2 rounded-full shrink-0" style={{ background: 'var(--accent)' }} />
                  <span className="min-w-0"><span className="block text-sm font-medium capitalize" style={{ color: 'var(--text-primary)' }}>{activity.title}</span><span className="block text-xs mt-1 truncate" style={{ color: 'var(--text-muted)' }}>{activity.detail} · {activity.date || 'Date unavailable'}</span></span>
                </li>
              ))}</ol> : <p className="text-sm" style={{ color: 'var(--text-muted)' }}>Applications and interviews will appear here as activity occurs.</p>}
            </section>
          </>
        )}
      </div>
    </RecruiterLayout>
  );
}
