import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('@/api/client', () => ({
  default: {
    post: vi.fn(),
  },
}));

import client from '@/api/client';
import { login, register } from '@/api/auth';

describe('auth API', () => {
  beforeEach(() => vi.clearAllMocks());

  it('login calls POST /auth/login with credentials', async () => {
    const response = { token: 'jwt-token', username: 'alice', role: 'MEMBER' };
    (client.post as any).mockResolvedValue({ data: response });

    const result = await login('alice', 'secret');
    expect(client.post).toHaveBeenCalledWith('/auth/login', { username: 'alice', password: 'secret' });
    expect(result.token).toBe('jwt-token');
    expect(result.username).toBe('alice');
    expect(result.role).toBe('MEMBER');
  });

  it('register calls POST /auth/register with full payload', async () => {
    const response = { token: 'new-token', username: 'bob', role: 'MEMBER' };
    (client.post as any).mockResolvedValue({ data: response });

    const result = await register('bob', 'bob@example.com', 'pass123', 'Bob Smith');
    expect(client.post).toHaveBeenCalledWith('/auth/register', {
      username: 'bob',
      email: 'bob@example.com',
      password: 'pass123',
      displayName: 'Bob Smith',
    });
    expect(result.token).toBe('new-token');
  });
});
