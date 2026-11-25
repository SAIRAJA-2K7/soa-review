import { useEffect, useMemo, useState } from 'react';
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
  const [resumeName, setResumeName] = useState('');
  const [resumeSize, setResumeSize] = useState(0);
  const [resumeLoading, setResumeLoading] = useState(false);
  const toast = useToast();
  useEffect(() => { setForm({ name: profile.name || '', professionalTitle: profile.professionalTitle || '', location: profile.location || '', bio: profile.bio || '' }); }, [profile]);
  const update = event => setForm(previous => ({ ...previous, [event.target.name]: event.target.value }));
  async function save(event) { event.preventDefault(); setSaving(true); try { await callApi('/api/user/profile', { method: 'PATCH', body: JSON.stringify(form) }); toast('success', 'Profile updated', 'Your changes have been saved.'); } catch (reason) { toast('error', 'Unable to update profile', reason.message); } finally { setSaving(false); } }
  useEffect(() => {
    let active = true;
    callApi('/api/user/resume/metadata')
      .then(metadata => {
        if (!active || !metadata) return;
        setResumeName(metadata.fileName);
        setResumeSize(metadata.size);
      })
      .catch(reason => {
        if (active) toast('error', 'Unable to load resume details', reason.message);
      });
    return () => { active = false; };
  }, []);
  async function uploadResume(event) {
    const file = event.target.files?.[0];
    if (!file) return;
    if (file.size > 5 * 1024 * 1024) {
      toast('error', 'Resume too large', 'Choose a PDF smaller than 5 MB.');
      event.target.value = '';
      return;
    }
    if (file.type !== 'application/pdf') {
      toast('error', 'Unsupported resume type', 'Upload a PDF resume.');
      event.target.value = '';
      return;
    }
    setResumeLoading(true);
    try {
      const body = new FormData();
      body.append('file', file);
      const metadata = await callApi('/api/user/resume', { method: 'PUT', body });
      setResumeName(metadata.fileName);
      setResumeSize(metadata.size);
      toast('success', 'Resume uploaded', 'Your PDF is stored securely with your account.');
    } catch (reason) {
      toast('error', 'Unable to upload resume', reason.message);
    } finally {
      setResumeLoading(false);
      event.target.value = '';
    }
  }
  async function downloadResume() {
    try {
      const response = await fetch(`${(import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/$/, '')}/api/user/resume`, {
        headers: { Authorization: `Bearer ${sessionStorage.getItem('ts-token') || ''}` },
      });
      if (!response.ok) throw new Error(await response.text() || `Download failed (${response.status})`);
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = resumeName || 'resume.pdf';
      link.click();
      URL.revokeObjectURL(url);
    } catch (reason) {
      toast('error', 'Unable to download resume', reason.message);
    }
  }
  async function deleteResume() {
    try {
      await callApi('/api/user/resume', { method: 'DELETE' });
      setResumeName('');
      setResumeSize(0);
      toast('success', 'Resume removed', 'Your resume has been deleted.');
    } catch (reason) {
      toast('error', 'Unable to remove resume', reason.message);
    }
  }
  const profileCompletion = useMemo(() => {
    const fields = [form.name, form.professionalTitle, form.location, form.bio, resumeName];
    return Math.round(fields.filter(value => String(value || '').trim()).length * 100 / fields.length);
  }, [form, resumeName]);
  if (loading) return <DashboardLayout><p className="text-sm" style={{ color: 'var(--text-muted)' }}>Loading profile...</p></DashboardLayout>;
  if (error) return <DashboardLayout><EmptyState title="Unable to load profile" description={error.message} /></DashboardLayout>;
  return (
    <DashboardLayout>
      <form onSubmit={save} className="max-w-3xl space-y-5">
        <div className="flex items-end justify-between gap-4">
          <h1 className="text-2xl font-extrabold" style={{ color: 'var(--text-primary)' }}>My Profile</h1>
          <span className="text-sm font-semibold" style={{ color: 'var(--accent)' }}>{profileCompletion}% complete</span>
        </div>
        <div className="h-2 rounded-full overflow-hidden" style={{ background: 'var(--muted)' }}><div className="h-full rounded-full transition-all" style={{ width: `${profileCompletion}%`, background: 'var(--accent)' }} /></div>
        <div className="grid md:grid-cols-2 gap-4">
          <div className="p-6 rounded-2xl border space-y-4 md:col-span-1" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}>
            <Input name="name" label="Full name" value={form.name} onChange={update} required />
            <Input name="professionalTitle" label="Professional title" value={form.professionalTitle} onChange={update} />
            <Input name="location" label="Location" value={form.location} onChange={update} />
            <textarea name="bio" value={form.bio} onChange={update} rows={4} placeholder="Professional bio" className="w-full rounded-lg border p-3 text-sm" style={{ background: 'var(--surface)', borderColor: 'var(--border)', color: 'var(--text-primary)' }} />
            <Button type="submit" loading={saving}>Save profile</Button>
          </div>
          <div className="p-6 rounded-2xl border" style={{ background: 'var(--surface)', borderColor: 'var(--border)' }}>
            <h2 className="text-sm font-semibold mb-2" style={{ color: 'var(--text-primary)' }}>Resume</h2>
            <p className="text-xs mb-4" style={{ color: 'var(--text-muted)' }}>PDF only, up to 5 MB.</p>
            {resumeName && <div className="mb-4 p-3 rounded-lg border" style={{ borderColor: 'var(--border)' }}>
              <p className="text-sm break-all" style={{ color: 'var(--text-primary)' }}>{resumeName}</p>
              <p className="text-xs mt-1" style={{ color: 'var(--text-muted)' }}>{(resumeSize / 1024).toFixed(0)} KB · PDF</p>
              <div className="flex gap-2 mt-3">
                <button type="button" onClick={downloadResume} className="text-xs font-medium" style={{ color: 'var(--accent)' }}>Download</button>
                <button type="button" onClick={deleteResume} className="text-xs font-medium" style={{ color: 'var(--error)' }}>Remove</button>
              </div>
            </div>}
            <label className="inline-flex items-center px-4 py-2 rounded-lg border cursor-pointer text-sm font-medium" style={{ borderColor: 'var(--accent)', color: 'var(--accent)' }}>
              {resumeLoading ? 'Uploading…' : resumeName ? 'Replace PDF' : 'Choose PDF'}
              <input type="file" accept="application/pdf" className="sr-only" disabled={resumeLoading} onChange={uploadResume} />
            </label>
          </div>
        </div>
      </form>
    </DashboardLayout>
  );
}
