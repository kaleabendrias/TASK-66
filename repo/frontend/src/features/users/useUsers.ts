import { useState, useCallback } from 'react';
import * as usersApi from '@/api/users';
import { User } from '@/api/types';

export function useUsers() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await usersApi.getUsers();
      setUsers(data);
    } catch (err: any) {
      setError(err.message || 'Failed to fetch users');
    } finally {
      setLoading(false);
    }
  }, []);

  const updateUser = useCallback(async (id: number, userData: Partial<User>) => {
    const updated = await usersApi.updateUser(id, userData);
    setUsers((prev) => prev.map((u) => (u.id === id ? updated : u)));
    return updated;
  }, []);

  const removeUser = useCallback(async (id: number) => {
    await usersApi.deleteUser(id);
    setUsers((prev) => prev.filter((u) => u.id !== id));
  }, []);

  return { users, loading, error, fetchUsers, updateUser, removeUser };
}
