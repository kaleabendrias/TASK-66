import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('@/api/client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

import client from '@/api/client';
import {
  getAppeals,
  getMyAppeals,
  getAppeal,
  createAppeal,
  reviewAppeal,
} from '@/api/appeals';

describe('appeals API', () => {
  beforeEach(() => vi.clearAllMocks());

  it('getAppeals calls GET /appeals', async () => {
    (client.get as any).mockResolvedValue({ data: [] });
    const result = await getAppeals();
    expect(client.get).toHaveBeenCalledWith('/appeals');
    expect(result).toEqual([]);
  });

  it('getMyAppeals calls GET /appeals/my', async () => {
    (client.get as any).mockResolvedValue({ data: [{ id: 1 }] });
    const result = await getMyAppeals();
    expect(client.get).toHaveBeenCalledWith('/appeals/my');
    expect(result).toHaveLength(1);
  });

  it('getAppeal calls GET /appeals/:id', async () => {
    (client.get as any).mockResolvedValue({ data: { id: 7 } });
    const result = await getAppeal(7);
    expect(client.get).toHaveBeenCalledWith('/appeals/7');
    expect(result.id).toBe(7);
  });

  it('createAppeal calls POST /appeals', async () => {
    const payload = {
      relatedEntityType: 'ORDER',
      relatedEntityId: 1,
      reason: 'Wrong item',
    };
    (client.post as any).mockResolvedValue({ data: { id: 1, ...payload, status: 'SUBMITTED' } });
    const result = await createAppeal(payload);
    expect(client.post).toHaveBeenCalledWith('/appeals', payload);
    expect(result.status).toBe('SUBMITTED');
  });

  it('reviewAppeal calls POST /appeals/:id/review', async () => {
    (client.post as any).mockResolvedValue({
      data: { id: 1, status: 'APPROVED', reviewNotes: 'OK' },
    });
    const result = await reviewAppeal(1, 'APPROVED', 'OK');
    expect(client.post).toHaveBeenCalledWith('/appeals/1/review', {
      status: 'APPROVED',
      reviewNotes: 'OK',
    });
    expect(result.status).toBe('APPROVED');
  });
});
