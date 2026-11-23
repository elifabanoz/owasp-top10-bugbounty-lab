import type { User } from '../types';

type Props = {
  users: User[];
  onRoleChange: (id: number, role: string) => void;
};

export default function AdminPanel({ users, onRoleChange }: Props) {
  return <section className="panel admin">
    <h2>Users</h2>
    {users.map(item => <div className="userRow" key={item.id}>
      <span>ID {item.id} · {item.username}</span>
      <select aria-label={`Role for ${item.username}`} value={item.role} onChange={e => onRoleChange(item.id, e.target.value)}>
        <option>RESEARCHER</option><option>TRIAGER</option><option>ADMIN</option>
      </select>
    </div>)}
  </section>;
}
