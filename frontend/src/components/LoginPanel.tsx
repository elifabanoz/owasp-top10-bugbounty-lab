import type { FormEvent } from 'react';

type Props = {
  username: string;
  password: string;
  onUsernameChange: (value: string) => void;
  onPasswordChange: (value: string) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
};

export default function LoginPanel({ username, password, onUsernameChange, onPasswordChange, onSubmit }: Props) {
  return <section className="panel login">
    <h2>Sign in</h2>
    <form onSubmit={onSubmit}>
      <label>Username<input value={username} onChange={e => onUsernameChange(e.target.value)} required /></label>
      <label>Password<input type="password" value={password} onChange={e => onPasswordChange(e.target.value)} required /></label>
      <button>Sign in</button>
    </form>
  </section>;
}
