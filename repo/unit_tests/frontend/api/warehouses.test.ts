import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('@/api/client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

import client from '@/api/client';
import {
  getWarehouses,
  getInventoryByProduct,
  getLowStockItems,
  adjustStock,
  reserveStock,
  confirmReservation,
  cancelReservation,
  getMyReservations,
} from '@/api/warehouses';

describe('warehouses API', () => {
  beforeEach(() => vi.clearAllMocks());

  it('getWarehouses calls GET /warehouses', async () => {
    (client.get as any).mockResolvedValue({ data: [{ id: 1, name: 'WH-A' }] });
    const result = await getWarehouses();
    expect(client.get).toHaveBeenCalledWith('/warehouses');
    expect(result).toHaveLength(1);
  });

  it('getInventoryByProduct calls GET /inventory/product/:id', async () => {
    (client.get as any).mockResolvedValue({ data: [{ id: 1 }] });
    const result = await getInventoryByProduct(5);
    expect(client.get).toHaveBeenCalledWith('/inventory/product/5');
    expect(result).toHaveLength(1);
  });

  it('getLowStockItems calls GET /inventory/low-stock', async () => {
    (client.get as any).mockResolvedValue({ data: [] });
    const result = await getLowStockItems();
    expect(client.get).toHaveBeenCalledWith('/inventory/low-stock');
    expect(result).toEqual([]);
  });

  it('adjustStock calls POST /inventory/adjust', async () => {
    const payload = {
      inventoryItemId: 1,
      quantityChange: 10,
      movementType: 'ADJUSTMENT',
      referenceDocument: 'REF-1',
      notes: 'test',
    };
    (client.post as any).mockResolvedValue({ data: { id: 1 } });
    await adjustStock(payload);
    expect(client.post).toHaveBeenCalledWith('/inventory/adjust', payload);
  });

  it('reserveStock calls POST /reservations', async () => {
    (client.post as any).mockResolvedValue({ data: { id: 1, status: 'PENDING' } });
    const result = await reserveStock(1, 5, 'key-abc');
    expect(client.post).toHaveBeenCalledWith('/reservations', {
      inventoryItemId: 1,
      quantity: 5,
      idempotencyKey: 'key-abc',
    });
    expect(result.status).toBe('PENDING');
  });

  it('confirmReservation calls POST /reservations/:id/confirm', async () => {
    (client.post as any).mockResolvedValue({ data: { id: 1, status: 'CONFIRMED' } });
    const result = await confirmReservation(1);
    expect(client.post).toHaveBeenCalledWith('/reservations/1/confirm');
    expect(result.status).toBe('CONFIRMED');
  });

  it('cancelReservation calls POST /reservations/:id/cancel', async () => {
    (client.post as any).mockResolvedValue({ data: { id: 1, status: 'CANCELLED' } });
    const result = await cancelReservation(1);
    expect(client.post).toHaveBeenCalledWith('/reservations/1/cancel');
    expect(result.status).toBe('CANCELLED');
  });

  it('getMyReservations calls GET /reservations/my', async () => {
    (client.get as any).mockResolvedValue({ data: [] });
    const result = await getMyReservations();
    expect(client.get).toHaveBeenCalledWith('/reservations/my');
    expect(result).toEqual([]);
  });
});
