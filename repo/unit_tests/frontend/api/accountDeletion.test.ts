import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('@/api/client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

import client from '@/api/client';
import { requestDeletion, cancelDeletion, getDeletionStatus } from '@/api/accountDeletion';

describe('accountDeletion API', () => {
  beforeEach(() => vi.clearAllMocks());

  it('requestDeletion calls POST /account-deletion/request', async () => {
    const status = { id: 1, userId: 2, status: 'PENDING', requestedAt: '2026-04-10T12:00:00' };
    (client.post as any).mockResolvedValue({ data: status });

    const result = await requestDeletion();
    expect(client.post).toHaveBeenCalledWith('/account-deletion/request');
    expect(result.status).toBe('PENDING');
  });

  it('cancelDeletion calls POST /account-deletion/:id/cancel', async () => {
    const status = { id: 1, userId: 2, status: 'CANCELLED', requestedAt: '2026-04-10T12:00:00' };
    (client.post as any).mockResolvedValue({ data: status });

    const result = await cancelDeletion(1);
    expect(client.post).toHaveBeenCalledWith('/account-deletion/1/cancel');
    expect(result.status).toBe('CANCELLED');
  });

  it('getDeletionStatus calls GET /account-deletion/status', async () => {
    const status = { id: 1, userId: 2, status: 'PENDING', requestedAt: '2026-04-10T12:00:00' };
    (client.get as any).mockResolvedValue({ data: status });

    const result = await getDeletionStatus();
    expect(client.get).toHaveBeenCalledWith('/account-deletion/status');
    expect(result.userId).toBe(2);
  });
});
