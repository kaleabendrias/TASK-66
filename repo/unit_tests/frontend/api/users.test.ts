import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('@/api/client', () => ({
  default: {
    get: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

import client from '@/api/client';
import { getUsers, getMe, updateUser, deleteUser } from '@/api/users';

describe('users API', () => {
  beforeEach(() => vi.clearAllMocks());

  it('getUsers calls GET /users', async () => {
    const users = [{ id: 1, username: 'admin', role: 'ADMINISTRATOR' }];
    (client.get as any).mockResolvedValue({ data: users });

    const result = await getUsers();
    expect(client.get).toHaveBeenCalledWith('/users');
    expect(result).toHaveLength(1);
  });

  it('getMe calls GET /users/me', async () => {
    const me = { id: 2, username: 'member', role: 'MEMBER' };
    (client.get as any).mockResolvedValue({ data: me });

    const result = await getMe();
    expect(client.get).toHaveBeenCalledWith('/users/me');
    expect(result.username).toBe('member');
  });

  it('updateUser calls PUT /users/:id with partial data', async () => {
    const updated = { id: 3, username: 'newname', role: 'MEMBER' };
    (client.put as any).mockResolvedValue({ data: updated });

    const result = await updateUser(3, { username: 'newname' } as any);
    expect(client.put).toHaveBeenCalledWith('/users/3', { username: 'newname' });
    expect(result.username).toBe('newname');
  });

  it('deleteUser calls DELETE /users/:id', async () => {
    (client.delete as any).mockResolvedValue({ data: undefined });

    await deleteUser(5);
    expect(client.delete).toHaveBeenCalledWith('/users/5');
  });
});
