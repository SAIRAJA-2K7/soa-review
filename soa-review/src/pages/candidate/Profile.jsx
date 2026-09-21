import { useEffect, useState } from 'react';
import DashboardLayout from '@/layouts/DashboardLayout';
import Button from '@/components/ui/Button';
import Input from '@/components/ui/Input';
import EmptyState from '@/components/ui/EmptyState';
import { useBackendData } from '@/data/backendData';
import { callApi } from '@/utils/api';
import { useToast } from '@/context/ToastContext';

export default function Profile() {
  const { data: profile, loading, error } = useBackendData('/api/user/profile', {});
  const [form, setForm] = useState({ name: '', professionalTitle: '', location: '', bio: '' });
  const [saving, setSaving] = useState(false);
  const toast = useToast();
  useEffect(() => { setForm({ name: profile.name || '', professionalTitle: profile.professionalTitle || '', location: profile.location || '', bio: profile.bio || '' }); }, [profile]);
  const update = event => setForm(previous => ({ ...previous, [event.target.name]: event.target.value }));
  async function save(event) { event.preventDefault(); setSaving(true); try { await callApi('/api/user/profile', { method: 'PATCH', body: JSON.stringify(form) }); toast('success', 'Profile updated', 'Your changes have been saved.'); } catch (reason) { toast('error', 'Unable to update profile', reason.message); } finally { setSaving(false); } }
  if (loading) return <DashboardLayout><p className="text-sm" style={{ color: 'var(--text-muted)' }}>Loading profile...</p></DashboardLayout>;
  if (error) return <DashboardLayout><EmptyState title="Unable to load profile" description={error.message} /></DashboardLayout>;
  return <DashboardLayout><form onSubmit={save} className="max-w-2xl space-y-5"><h1 className="text-2xl font-extrabold" style={{ color: 'var(--text-primary)' }}>My Profile</h1><div className="p-6 rounded-2xl border space-y-4" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}><Input name="name" label="Full name" value={form.name} onChange={update} required /><Input name="professionalTitle" label="Professional title" value={form.professionalTitle} onChange={update} /><Input name="location" label="Location" value={form.location} onChange={update} /><textarea name="bio" value={form.bio} onChange={update} rows={4} placeholder="Professional bio" className="w-full rounded-lg border p-3 text-sm" style={{ background: 'var(--surface)', borderColor: 'var(--border)', color: 'var(--text-primary)' }} /><Button type="submit" loading={saving}>Save profile</Button></div></form></DashboardLayout>;
}
