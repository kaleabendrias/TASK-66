import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('@/api/client', () => ({
  default: {
    get: vi.fn(),
  },
}));

import client from '@/api/client';
import { getAuditLog } from '@/api/audit';

describe('audit API', () => {
  beforeEach(() => vi.clearAllMocks());

  it('getAuditLog calls GET /audit/:entityType/:entityId', async () => {
    const entries = [
      { id: 1, entityType: 'USER', entityId: 1, action: 'CREATED', performedAt: '2026-01-01T00:00:00' },
    ];
    (client.get as any).mockResolvedValue({ data: entries });

    const result = await getAuditLog('USER', 1);
    expect(client.get).toHaveBeenCalledWith('/audit/USER/1');
    expect(result).toHaveLength(1);
    expect(result[0].action).toBe('CREATED');
  });

  it('getAuditLog works for different entity types', async () => {
    (client.get as any).mockResolvedValue({ data: [] });

    await getAuditLog('ORDER', 42);
    expect(client.get).toHaveBeenCalledWith('/audit/ORDER/42');
  });
});
