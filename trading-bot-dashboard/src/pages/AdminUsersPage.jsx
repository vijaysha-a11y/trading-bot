import { useEffect, useState } from 'react';
import { getAdminUsers } from '../services/api.js';

export default function AdminUsersPage() {
  const [users, setUsers] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    getAdminUsers()
      .then((result) => { if (active) setUsers(result); })
      .catch((requestError) => { if (active) setError(requestError.message); });
    return () => { active = false; };
  }, []);

  return (
    <section className="panel page-panel admin-users-panel">
      <div className="panel-header">
        <div><span className="eyebrow">WORKSPACE ACCESS</span><h2>Team members</h2></div>
        <span className="user-count">{users.length} {users.length === 1 ? 'account' : 'accounts'}</span>
      </div>
      <p className="muted">Review registered accounts and their assigned access roles.</p>
      {error ? <div className="alert-error admin-error" role="alert">{error}</div> : (
        <div className="table-wrap">
          <table className="users-table">
            <thead><tr><th>Member</th><th>Email</th><th>Role</th><th>Portfolio</th></tr></thead>
            <tbody>{users.map((user) => (
              <tr key={user.id}>
                <td><span className="member-cell"><span className="member-avatar">{user.name.slice(0, 1).toUpperCase()}</span><strong>{user.name}</strong></span></td>
                <td>{user.email}</td>
                <td><span className={`role-pill role-${user.role.toLowerCase()}`}>{user.role}</span></td>
                <td>#{user.portfolioId}</td>
              </tr>
            ))}</tbody>
          </table>
        </div>
      )}
    </section>
  );
}
