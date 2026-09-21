import { useEffect, useState } from 'react';
import RecruiterLayout from '@/layouts/RecruiterLayout';
import Button from '@/components/ui/Button';
import Input from '@/components/ui/Input';
import EmptyState from '@/components/ui/EmptyState';
import { useBackendData } from '@/data/backendData';
import { callApi } from '@/utils/api';
import { useToast } from '@/context/ToastContext';

export default function Company() {
  const { data: company, loading, error } = useBackendData('/api/admin/company', {});
  const [form, setForm] = useState({ name: '', website: '', size: '', description: '' });
  const [saving, setSaving] = useState(false);
  const toast = useToast();
  useEffect(() => setForm({ name: company.name || '', website: company.website || '', size: company.size || '', description: company.description || '' }), [company]);
  const update = event => setForm(previous => ({ ...previous, [event.target.name]: event.target.value }));
  async function save(event) { event.preventDefault(); setSaving(true); try { await callApi('/api/admin/company', { method: 'PATCH', body: JSON.stringify(form) }); toast('success', 'Company profile updated', 'Your changes have been saved.'); } catch (reason) { toast('error', 'Unable to update company', reason.message); } finally { setSaving(false); } }
  if (loading) return <RecruiterLayout><p className="text-sm" style={{ color: 'var(--text-muted)' }}>Loading company...</p></RecruiterLayout>;
  if (error) return <RecruiterLayout><EmptyState title="Unable to load company" description={error.message} /></RecruiterLayout>;
  return <RecruiterLayout><form onSubmit={save} className="max-w-2xl space-y-5"><h1 className="text-2xl font-extrabold" style={{ color: 'var(--text-primary)' }}>Company Profile</h1><div className="p-6 rounded-2xl border space-y-4" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}><Input name="name" label="Company name" value={form.name} onChange={update} required /><Input name="website" label="Website" value={form.website} onChange={update} /><Input name="size" label="Company size" value={form.size} onChange={update} /><textarea name="description" value={form.description} onChange={update} rows={4} placeholder="Company description" className="w-full rounded-lg border p-3 text-sm" style={{ background: 'var(--surface)', borderColor: 'var(--border)', color: 'var(--text-primary)' }} /><Button type="submit" loading={saving}>Save company</Button></div></form></RecruiterLayout>;
}
