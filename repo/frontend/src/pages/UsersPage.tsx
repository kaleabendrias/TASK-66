import React, { useEffect, useState } from 'react';
import { useUsers } from '@/features/users/useUsers';
import { Role } from '@/api/types';
import Table from '@/components/ui/Table';
import Badge from '@/components/ui/Badge';
import Button from '@/components/ui/Button';
import Modal from '@/components/ui/Modal';

const allRoles: Role[] = ['GUEST', 'MEMBER', 'SELLER', 'WAREHOUSE_STAFF', 'MODERATOR', 'ADMINISTRATOR'];

const UsersPage: React.FC = () => {
  const { users, loading, error, fetchUsers, updateUser, removeUser } = useUsers();
  const [editingId, setEditingId] = useState<number | null>(null);
  const [selectedRole, setSelectedRole] = useState<Role>('MEMBER');
  const [confirmDelete, setConfirmDelete] = useState<number | null>(null);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const handleRoleChange = async (id: number) => {
    try {
      const userToUpdate = users.find((u) => u.id === id);
      if (!userToUpdate) return;
      await updateUser(id, { ...userToUpdate, role: selectedRole });
      setEditingId(null);
    } catch {
      // error handling
    }
  };

  const handleToggleEnabled = async (id: number) => {
    const u = users.find((u) => u.id === id);
    if (!u) return;
    await updateUser(id, { ...u, enabled: !u.enabled });
  };

  const handleDelete = async (id: number) => {
    try {
      await removeUser(id);
      setConfirmDelete(null);
    } catch {
      // error handling
    }
  };

  return (
    <div className="page">
      <h1>User Management</h1>
      {error && <div className="alert alert-danger">{error}</div>}
      {loading ? (
        <p>Loading users...</p>
      ) : (
        <Table headers={['ID', 'Username', 'Email', 'Display Name', 'Role', 'Enabled', 'Actions']}>
          {users.map((u) => (
            <tr key={u.id}>
              <td>{u.id}</td>
              <td>{u.username}</td>
              <td>{u.email}</td>
              <td>{u.displayName}</td>
              <td>
                {editingId === u.id ? (
                  <div className="flex gap-sm">
                    <select
                      className="form-input"
                      value={selectedRole}
                      onChange={(e) => setSelectedRole(e.target.value as Role)}
                    >
                      {allRoles.map((r) => (
                        <option key={r} value={r}>{r}</option>
                      ))}
                    </select>
                    <Button size="sm" onClick={() => handleRoleChange(u.id)}>Save</Button>
                    <Button size="sm" variant="secondary" onClick={() => setEditingId(null)}>Cancel</Button>
                  </div>
                ) : (
                  <Badge variant="info">{u.role}</Badge>
                )}
              </td>
              <td>
                <Badge variant={u.enabled ? 'success' : 'danger'}>
                  {u.enabled ? 'Yes' : 'No'}
                </Badge>
              </td>
              <td>
                <div className="flex gap-sm">
                  {editingId !== u.id && (
                    <Button
                      size="sm"
                      variant="secondary"
                      onClick={() => { setEditingId(u.id); setSelectedRole(u.role); }}
                    >
                      Change Role
                    </Button>
                  )}
                  <Button size="sm" variant="secondary" onClick={() => handleToggleEnabled(u.id)}>
                    {u.enabled ? 'Disable' : 'Enable'}
                  </Button>
                  <Button size="sm" variant="danger" onClick={() => setConfirmDelete(u.id)}>
                    Delete
                  </Button>
                </div>
              </td>
            </tr>
          ))}
        </Table>
      )}

      <Modal
        open={confirmDelete !== null}
        onClose={() => setConfirmDelete(null)}
        title="Confirm Delete"
      >
        <p>Are you sure you want to delete this user?</p>
        <div className="form-actions">
          <Button variant="secondary" onClick={() => setConfirmDelete(null)}>Cancel</Button>
          <Button variant="danger" onClick={() => confirmDelete && handleDelete(confirmDelete)}>Delete</Button>
        </div>
      </Modal>
    </div>
  );
};

export default UsersPage;
