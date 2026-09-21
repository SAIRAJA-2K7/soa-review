import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import RecruiterLayout from '@/layouts/RecruiterLayout';
import Button from '@/components/ui/Button';
import Input from '@/components/ui/Input';
import { callApi } from '@/utils/api';
import { useToast } from '@/context/ToastContext';

const emptyForm = { title: '', companyName: '', location: '', type: 'FULL_TIME', experience: '', salaryMin: '', salaryMax: '', description: '', skills: '' };
export default function JobCreate() {
  const { id } = useParams();
  const editing = Boolean(id);
  const navigate = useNavigate();
  const toast = useToast();
  const [form, setForm] = useState(emptyForm);
  const [loading, setLoading] = useState(editing);
  const [saving, setSaving] = useState(false);
  useEffect(() => { if (!editing) return; callApi(`/api/jobs/${id}`).then(job => setForm({ title: job.title || '', companyName: job.company?.name || '', location: job.location || '', type: job.type === 'Full-time' ? 'FULL_TIME' : job.type?.toUpperCase() || 'FULL_TIME', experience: job.experience || '', salaryMin: job.salaryMin || '', salaryMax: job.salaryMax || '', description: job.description || '', skills: (job.skills || []).join(', ') })).catch(reason => toast('error', 'Unable to load job', reason.message)).finally(() => setLoading(false)); }, [editing, id]);
  const update = event => setForm(previous => ({ ...previous, [event.target.name]: event.target.value }));
  async function submit(event) { event.preventDefault(); setSaving(true); const body = { ...form, salaryMin: form.salaryMin ? Number(form.salaryMin) : null, salaryMax: form.salaryMax ? Number(form.salaryMax) : null, skills: form.skills.split(',').map(value => value.trim()).filter(Boolean) }; try { await callApi(editing ? `/api/jobs/${id}` : '/api/jobs', { method: editing ? 'PUT' : 'POST', body: JSON.stringify(body) }); toast('success', editing ? 'Job updated' : 'Job published', 'Candidates will see the persisted changes.'); navigate('/recruiter/jobs'); } catch (reason) { toast('error', editing ? 'Unable to update job' : 'Unable to publish job', reason.message); } finally { setSaving(false); } }
  if (loading) return <RecruiterLayout><p className="text-sm" style={{ color: 'var(--text-muted)' }}>Loading job...</p></RecruiterLayout>;
  return <RecruiterLayout><form onSubmit={submit} className="max-w-2xl space-y-4"><h1 className="text-2xl font-extrabold" style={{ color: 'var(--text-primary)' }}>{editing ? 'Edit Job' : 'Post a Job'}</h1><Input name="title" value={form.title} onChange={update} placeholder="Job title" required /><Input name="companyName" value={form.companyName} onChange={update} placeholder="Company" required /><Input name="location" value={form.location} onChange={update} placeholder="Location" required /><Input name="experience" value={form.experience} onChange={update} placeholder="Experience" required /><Input name="salaryMin" value={form.salaryMin} onChange={update} placeholder="Minimum salary" type="number" /><Input name="salaryMax" value={form.salaryMax} onChange={update} placeholder="Maximum salary" type="number" /><Input name="skills" value={form.skills} onChange={update} placeholder="Skills, separated by commas" /><textarea name="description" value={form.description} onChange={update} placeholder="Description" required className="w-full rounded-lg border p-3 text-sm min-h-32" style={{ background: 'var(--surface)', borderColor: 'var(--border)', color: 'var(--text-primary)' }} /><Button type="submit" loading={saving}>{editing ? 'Save changes' : 'Publish job'}</Button></form></RecruiterLayout>;
}
