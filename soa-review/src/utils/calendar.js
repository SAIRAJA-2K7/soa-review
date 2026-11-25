function escapeCalendarText(value) {
  return String(value || '')
    .replaceAll('\\', '\\\\')
    .replaceAll(';', '\\;')
    .replaceAll(',', '\\,')
    .replaceAll(/\r?\n/g, '\\n');
}

function toCalendarDate(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    throw new Error('Interview date is invalid; calendar invite was not created.');
  }
  return date.toISOString().replaceAll('-', '').replaceAll(':', '').replace(/\.\d{3}Z$/, 'Z');
}

export function downloadInterviewInvite(interview) {
  const startValue = String(interview.date || '').includes('T')
    ? interview.date
    : `${interview.date || ''}T${interview.time || '09:00:00'}`;
  const start = new Date(startValue);
  if (Number.isNaN(start.getTime())) {
    throw new Error('Interview date is invalid; calendar invite was not created.');
  }
  const end = new Date(start.getTime() + 60 * 60 * 1000);
  const title = `${interview.type || 'Recruitment'} interview: ${interview.job || 'TalentSync'}`;
  const eventId = `${interview.id || start.getTime()}@talentsync.local`;
  const lines = [
    'BEGIN:VCALENDAR',
    'VERSION:2.0',
    'PRODID:-//TalentSync//Interview Calendar//EN',
    'CALSCALE:GREGORIAN',
    'BEGIN:VEVENT',
    `UID:${escapeCalendarText(eventId)}`,
    `DTSTAMP:${toCalendarDate(new Date().toISOString())}`,
    `DTSTART:${toCalendarDate(start.toISOString())}`,
    `DTEND:${toCalendarDate(end.toISOString())}`,
    `SUMMARY:${escapeCalendarText(title)}`,
    `DESCRIPTION:${escapeCalendarText(`Format: ${interview.mode || 'Not specified'}\nInterviewer: ${interview.interviewer || 'Hiring Team'}`)}`,
    'END:VEVENT',
    'END:VCALENDAR',
  ];
  const blob = new Blob([lines.join('\r\n')], { type: 'text/calendar;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = 'talentsync-interview.ics';
  link.click();
  URL.revokeObjectURL(url);
}
