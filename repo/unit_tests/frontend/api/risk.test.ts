import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('@/api/client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

import client from '@/api/client';
import { getRiskScore, computeRiskScore, getHighRiskUsers, getRiskEvents } from '@/api/risk';

describe('risk API', () => {
  beforeEach(() => vi.clearAllMocks());

  it('getRiskScore calls GET /risk/score/:userId', async () => {
    const score = { userId: 4, score: 0.3, riskLevel: 'LOW' };
    (client.get as any).mockResolvedValue({ data: score });

    const result = await getRiskScore(4);
    expect(client.get).toHaveBeenCalledWith('/risk/score/4');
    expect(result.userId).toBe(4);
  });

  it('computeRiskScore calls POST /risk/compute/:userId', async () => {
    const score = { userId: 4, score: 0.5, riskLevel: 'MEDIUM' };
    (client.post as any).mockResolvedValue({ data: score });

    const result = await computeRiskScore(4);
    expect(client.post).toHaveBeenCalledWith('/risk/compute/4');
    expect(result.riskLevel).toBe('MEDIUM');
  });

  it('getHighRiskUsers calls GET /risk/high-risk', async () => {
    const users = [{ userId: 5, score: 0.9, riskLevel: 'HIGH' }];
    (client.get as any).mockResolvedValue({ data: users });

    const result = await getHighRiskUsers();
    expect(client.get).toHaveBeenCalledWith('/risk/high-risk', { params: { threshold: undefined } });
    expect(result).toHaveLength(1);
  });

  it('getHighRiskUsers passes threshold param when provided', async () => {
    (client.get as any).mockResolvedValue({ data: [] });

    await getHighRiskUsers(0.8);
    expect(client.get).toHaveBeenCalledWith('/risk/high-risk', { params: { threshold: 0.8 } });
  });

  it('getRiskEvents calls GET /risk/events/:userId', async () => {
    const events = [{ id: 1, userId: 3, eventType: 'ORDER_CANCELLED' }];
    (client.get as any).mockResolvedValue({ data: events });

    const result = await getRiskEvents(3);
    expect(client.get).toHaveBeenCalledWith('/risk/events/3');
    expect(result[0].eventType).toBe('ORDER_CANCELLED');
  });
});
