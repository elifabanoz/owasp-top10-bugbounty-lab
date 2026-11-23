import React, { useEffect, useState } from 'react';
import { api, write } from './api';
import AdminPanel from './components/AdminPanel';
import LoginPanel from './components/LoginPanel';
import OwaspPanel from './components/OwaspPanel';
import ReportDetail from './components/ReportDetail';
import ReportList from './components/ReportList';
import type { Comment, Profile, Program, Report, User } from './types';

export default function App() {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [user, setUser] = useState<User | null>(null);
  const [username, setUsername] = useState('alice');
  const [password, setPassword] = useState('');
  const [query, setQuery] = useState('');
  const [reports, setReports] = useState<Report[]>([]);
  const [selected, setSelected] = useState<Report | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [comment, setComment] = useState('');
  const [users, setUsers] = useState<User[]>([]);
  const [programs, setPrograms] = useState<Program[]>([]);
  const [newTitle, setNewTitle] = useState('');
  const [newDescription, setNewDescription] = useState('');
  const [newSeverity, setNewSeverity] = useState('MEDIUM');
  const [error, setError] = useState('');

  useEffect(() => {
    api<{ profile: Profile }>('/meta').then(data => setProfile(data.profile)).catch(e => setError(e.message));
    api<User>('/auth/me').then(setUser).catch(() => {});
  }, []);

  useEffect(() => {
    if (!user) return;
    api<Report[]>(`/reports/search?q=${encodeURIComponent(query)}`).then(setReports).catch(e => setError(e.message));
    api<Program[]>('/programs').then(setPrograms).catch(e => setError(e.message));
  }, [user, query]);

  useEffect(() => {
    if (!user || user.role !== 'ADMIN') return;
    api<User[]>('/admin/users').then(setUsers).catch(e => setError(e.message));
  }, [user]);

  async function login(event: React.FormEvent) {
    event.preventDefault();
    setError('');
    try {
      const signedIn = await api<User>('/auth/login', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      });
      setUser(signedIn);
      setPassword('');
    } catch (e) { setError((e as Error).message); }
  }

  async function select(id: number) {
    setError('');
    try {
      const detail = await api<Report>(`/reports/${id}`);
      const related = await api<Comment[]>(`/reports/${id}/comments`);
      setSelected(detail);
      setComments(related);
    } catch (e) { setError((e as Error).message); }
  }

  async function addComment(event: React.FormEvent) {
    event.preventDefault();
    if (!selected || !profile) return;
    try {
      await write<void>(`/reports/${selected.id}/comments`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ body: comment })
      }, profile);
      setComment('');
      setComments(await api<Comment[]>(`/reports/${selected.id}/comments`));
    } catch (e) { setError((e as Error).message); }
  }

  async function changeRole(id: number, role: string) {
    if (!profile) return;
    try {
      await write<void>(`/admin/users/${id}/role`, {
        method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({ role })
      }, profile);
      setUsers(await api<User[]>('/admin/users'));
    } catch (e) { setError((e as Error).message); }
  }

  async function refreshUsers() {
    setUsers(await api<User[]>('/admin/users'));
  }

  async function refreshReports() {
    setReports(await api<Report[]>(`/reports/search?q=${encodeURIComponent(query)}`));
    if (selected) setSelected(await api<Report>(`/reports/${selected.id}`));
  }

  async function createReport(event: React.FormEvent) {
    event.preventDefault();
    if (!profile || programs.length === 0) return;
    try {
      await write<void>('/reports', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ programId: programs[0].id, title: newTitle,
          description: newDescription, severity: newSeverity })
      }, profile);
      setNewTitle('');
      setNewDescription('');
      setReports(await api<Report[]>(`/reports/search?q=${encodeURIComponent(query)}`));
    } catch (e) { setError((e as Error).message); }
  }

  async function logout() {
    if (!profile) return;
    try {
      await write<void>('/auth/logout', { method: 'POST' }, profile);
      setUser(null);
      setSelected(null);
      setReports([]);
    } catch (e) { setError((e as Error).message); }
  }

  return <div className="shell">
    <header>
      <div><p className="eyebrow">LOCAL SECURITY LAB</p><h1>BugBounty Admin</h1></div>
      {user && <div className="identity">{user.username} <span>{user.role}</span> <button className="textButton" onClick={logout}>Sign out</button></div>}
    </header>
    {profile === 'vulnerable' && <div className="warning">VULNERABLE PROFILE · Local training only</div>}
    {profile === 'secure' && <div className="mode">SECURE PROFILE</div>}
    {error && <div className="error" role="alert">{error}</div>}
    {!user ? <LoginPanel username={username} password={password} onUsernameChange={setUsername}
      onPasswordChange={setPassword} onSubmit={login} /> : <main>
      <ReportList reports={reports} programs={programs} researcher={user.role === 'RESEARCHER'}
        query={query} newTitle={newTitle} newDescription={newDescription} newSeverity={newSeverity}
        onQueryChange={setQuery} onSelect={select} onTitleChange={setNewTitle}
        onDescriptionChange={setNewDescription} onSeverityChange={setNewSeverity} onCreate={createReport} />
      <ReportDetail selected={selected} comments={comments} comment={comment} profile={profile}
        onCommentChange={setComment} onAddComment={addComment} />
      {user.role === 'ADMIN' && <AdminPanel users={users} onRoleChange={changeRole} />}
      {user.role === 'ADMIN' && profile && <OwaspPanel profile={profile} reports={reports} users={users}
        onReportsChanged={refreshReports} onUsersChanged={refreshUsers} />}
    </main>}
  </div>;
}
