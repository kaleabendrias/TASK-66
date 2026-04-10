import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('@/api/client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}));

import client from '@/api/client';
import {
  getTiers,
  getMyProfile,
  getProfileByUser,
  updatePhone,
  adjustSpend,
  getSpendHistory,
  getPackagesByTier,
  getItemsByPackage,
  redeemBenefit,
} from '@/api/members';

describe('members API', () => {
  beforeEach(() => vi.clearAllMocks());

  it('getTiers calls GET /tiers', async () => {
    (client.get as any).mockResolvedValue({ data: [{ id: 1, name: 'Bronze' }] });
    const result = await getTiers();
    expect(client.get).toHaveBeenCalledWith('/tiers');
    expect(result).toHaveLength(1);
  });

  it('getMyProfile calls GET /members/me', async () => {
    (client.get as any).mockResolvedValue({ data: { id: 1, tierName: 'Gold' } });
    const result = await getMyProfile();
    expect(client.get).toHaveBeenCalledWith('/members/me');
    expect(result.tierName).toBe('Gold');
  });

  it('getProfileByUser calls GET /members/:id', async () => {
    (client.get as any).mockResolvedValue({ data: { id: 1 } });
    await getProfileByUser(5);
    expect(client.get).toHaveBeenCalledWith('/members/5');
  });

  it('updatePhone calls PUT /members/me/phone', async () => {
    (client.put as any).mockResolvedValue({ data: { phoneMasked: '***1234' } });
    const result = await updatePhone('555-1234');
    expect(client.put).toHaveBeenCalledWith('/members/me/phone', { phone: '555-1234' });
    expect(result.phoneMasked).toBe('***1234');
  });

  it('adjustSpend calls POST /members/me/spend', async () => {
    (client.post as any).mockResolvedValue({ data: { totalSpend: 150 } });
    await adjustSpend(50, 'bonus');
    expect(client.post).toHaveBeenCalledWith('/members/me/spend', {
      amount: 50,
      reference: 'bonus',
    });
  });

  it('getSpendHistory calls GET /members/me/spend/history', async () => {
    (client.get as any).mockResolvedValue({ data: [] });
    const result = await getSpendHistory();
    expect(client.get).toHaveBeenCalledWith('/members/me/spend/history');
    expect(result).toEqual([]);
  });

  it('getPackagesByTier calls GET /benefits/packages/tier/:id', async () => {
    (client.get as any).mockResolvedValue({ data: [{ id: 1 }] });
    await getPackagesByTier(2);
    expect(client.get).toHaveBeenCalledWith('/benefits/packages/tier/2');
  });

  it('getItemsByPackage calls GET /benefits/items/package/:id', async () => {
    (client.get as any).mockResolvedValue({ data: [{ id: 1 }] });
    await getItemsByPackage(3);
    expect(client.get).toHaveBeenCalledWith('/benefits/items/package/3');
  });

  it('redeemBenefit calls POST /benefits/redeem', async () => {
    (client.post as any).mockResolvedValue({});
    await redeemBenefit(1, 'ref-123');
    expect(client.post).toHaveBeenCalledWith('/benefits/redeem', {
      benefitItemId: 1,
      reference: 'ref-123',
    });
  });
});
