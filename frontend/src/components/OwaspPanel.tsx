import { useState } from 'react';
import { api, write } from '../api';
import type { Profile, Report, User } from '../types';

type Props = {
  profile: Profile;
  reports: Report[];
  users: User[];
  onReportsChanged: () => Promise<void>;
  onUsersChanged: () => Promise<void>;
};

type SecurityEvent = { id: number; kind: string; actorId: number; targetId: number };
type IntegrationEvent = { id: number; reportId: number; decision: string };
type Component = { id: number; name: string; version: string; source: string };
type Manifest = { name: string; version: string; source: string; sha256: string; content: string };

export default function OwaspPanel({ profile, reports, users, onReportsChanged, onUsersChanged }: Props) {
  const [reportId, setReportId] = useState('1');
  const [result, setResult] = useState('Select an exercise to see its result.');
  const report = reports.find(item => item.id === Number(reportId));

  function selectedId() {
    const id = Number(reportId);
    if (!Number.isSafeInteger(id) || id <= 0) throw new Error('Enter a valid report ID');
    return id;
  }

  async function run(label: string, action: () => Promise<string>) {
    try {
      setResult(`${label}: running...`);
      setResult(`${label}: ${await action()}`);
    } catch (error) {
      setResult(`${label}: ${(error as Error).message}`);
    }
  }

  async function approve(candidate: Manifest) {
    await write<void>('/admin/lab/components', {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(candidate)
    }, profile);
    const approved = await api<Component[]>('/admin/lab/components');
    return `approved; ${approved.length} component(s) in the local catalog`;
  }

  async function workflow(action: 'triage' | 'close', reviewFailure = false) {
    const id = selectedId();
    const suffix = action === 'close' && reviewFailure ? '?simulateReviewFailure=true' : '';
    const updated = await write<Report>(`/admin/lab/reports/${id}/${action}${suffix}`,
      { method: 'POST' }, profile);
    await onReportsChanged();
    return `report #${id} is ${updated.status}`;
  }

  return <section className="panel owasp">
    <h2>OWASP Top 10:2025 exercises</h2>
    <p className="empty">Use these local fixtures to compare vulnerable and secure profiles. Each button shows the result below.</p>
    <div className="owaspSetup">
      <label>Report ID for workflow and webhook exercises
        <input type="number" min="1" value={reportId} onChange={event => setReportId(event.target.value)} />
      </label>
      <span>{report ? `${report.title} · ${report.status}` : 'Choose a report from the list above.'}</span>
    </div>
    <div className="owaspGrid">
      <article><h3>A01 · Broken Access Control</h3><p>Alice can request Bob's report by ID only in the vulnerable profile.</p></article>
      <article><h3>A02 · Security Misconfiguration</h3><p>An unnecessary debug route exposes user emails without login only in the vulnerable profile.</p>
        <button onClick={() => run('A02', async () => {
          const exposed = await api<User[]>('/lab/debug/users');
          return exposed.map(item => `${item.username}: ${item.id}`).join(', ');
        })}>Read debug users</button></article>
      <article><h3>A03 · Software Supply Chain Failures</h3><p>A local component manifest from an untrusted source is approved only in the vulnerable profile. No package is downloaded or run.</p>
        <button onClick={() => run('A03 untrusted', () => approve({ name: 'demo-parser', version: '0.1.0',
          source: 'https://untrusted.example.test/parser', sha256: '0'.repeat(64),
          content: 'untrusted local component text' }))}>Approve untrusted</button>
        <button onClick={() => run('A03 trusted', async () => approve(
          await api<Manifest>('/admin/lab/component-fixture')))}>Approve trusted</button></article>
      <article><h3>A04 · Cryptographic Failures</h3><p>The vulnerable profile stores MD5 password hashes; the secure profile uses BCrypt.</p></article>
      <article><h3>A05 · Injection</h3><p>Compare SQL injection in report search and stored XSS in report comments.</p></article>
      <article><h3>A06 · Insecure Design</h3><p>Closing a NEW report skips triage only in the vulnerable profile.</p>
        <button onClick={() => run('A06', () => workflow('close'))}>Close without triage</button>
        <button onClick={() => run('Triage', () => workflow('triage'))}>Triage report</button></article>
      <article><h3>A07 · Authentication Failures</h3><p>Compare login errors, attempt limits, token expiry and signing keys.</p></article>
      <article><h3>A08 · Software or Data Integrity Failures</h3><p>An unsigned external triage event is stored only in the vulnerable profile.</p>
        <button onClick={() => run('A08 unsigned', async () => {
          await api<void>('/lab/webhook/triage', { method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ reportId: selectedId(), decision: 'TRIAGED' }) });
          await onReportsChanged();
          const events = await api<IntegrationEvent[]>('/admin/lab/integration-events');
          return `accepted; report triaged, ${events.length} integration event(s) stored`;
        })}>Send unsigned event</button></article>
      <article><h3>A09 · Security Logging &amp; Alerting Failures</h3><p>Promoting Bob creates an audit event and alert only in the secure profile.</p>
        <button onClick={() => run('A09', async () => {
          const bob = users.find(item => item.username === 'bob');
          if (!bob) throw new Error('Bob is not in the user list');
          await write<void>(`/admin/users/${bob.id}/role`, { method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams({ role: 'ADMIN' }) }, profile);
          await onUsersChanged();
          const events = await api<SecurityEvent[]>('/admin/lab/security-events');
          return `Bob promoted; ${events.length} event(s), ${events.filter(item => item.kind.endsWith('ALERT')).length} alert(s)`;
        })}>Promote Bob</button>
        <button onClick={() => run('A09 reset', async () => {
          const bob = users.find(item => item.username === 'bob');
          if (!bob) throw new Error('Bob is not in the user list');
          await write<void>(`/admin/users/${bob.id}/role`, { method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams({ role: 'RESEARCHER' }) }, profile);
          await onUsersChanged();
          return 'Bob is a researcher again';
        })}>Reset Bob</button>
        <button onClick={() => run('A09 events', async () => {
          const events = await api<SecurityEvent[]>('/admin/lab/security-events');
          return events.map(item => `${item.kind} for user #${item.targetId}`).join(', ') || 'no audit events';
        })}>View audit events</button></article>
      <article><h3>A10 · Mishandling of Exceptional Conditions</h3><p>After triage, simulate a review service outage. Vulnerable mode closes the report; secure mode keeps it TRIAGED.</p>
        <button onClick={() => run('A10 outage', () => workflow('close', true))}>Close during outage</button></article>
    </div>
    <p className="owaspResult" role="status">{result}</p>
  </section>;
}
