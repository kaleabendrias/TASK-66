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
  recordInbound,
  recordOutbound,
  recordStocktake,
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

  it('recordInbound calls POST /inventory/inbound with the inbound payload', async () => {
    const payload = {
      inventoryItemId: 1,
      quantity: 10,
      referenceDocument: 'PO-1',
      notes: 'received',
    };
    (client.post as any).mockResolvedValue({ data: { id: 1, movementType: 'INBOUND' } });
    await recordInbound(payload);
    expect(client.post).toHaveBeenCalledWith('/inventory/inbound', payload);
  });

  it('recordOutbound calls POST /inventory/outbound with the outbound payload', async () => {
    const payload = {
      inventoryItemId: 2,
      quantity: 5,
      referenceDocument: 'SO-7',
      notes: 'shipped',
    };
    (client.post as any).mockResolvedValue({ data: { id: 2, movementType: 'OUTBOUND' } });
    await recordOutbound(payload);
    expect(client.post).toHaveBeenCalledWith('/inventory/outbound', payload);
  });

  it('recordStocktake calls POST /inventory/stocktake with productId/warehouseId/countedQuantity', async () => {
    const payload = {
      productId: 7,
      warehouseId: 1,
      countedQuantity: 42,
      referenceDocument: 'COUNT-Q2',
    };
    (client.post as any).mockResolvedValue({ data: { id: 3, movementType: 'STOCKTAKE' } });
    await recordStocktake(payload);
    expect(client.post).toHaveBeenCalledWith('/inventory/stocktake', payload);
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
