import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('@/api/client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
  },
}));

import client from '@/api/client';
import { getOrders, getMyOrders, placeOrder, updateOrderStatus } from '@/api/orders';

describe('orders API', () => {
  beforeEach(() => vi.clearAllMocks());

  it('getOrders calls GET /orders and returns list', async () => {
    const orders = [{ id: 1, buyerId: 2, productId: 1, quantity: 1, totalPrice: 50, status: 'PLACED' }];
    (client.get as any).mockResolvedValue({ data: orders });

    const result = await getOrders();
    expect(client.get).toHaveBeenCalledWith('/orders');
    expect(result).toEqual(orders);
  });

  it('getMyOrders calls GET /orders/buyer/:id', async () => {
    const orders = [{ id: 2, buyerId: 3, productId: 2, quantity: 2, totalPrice: 80, status: 'PLACED' }];
    (client.get as any).mockResolvedValue({ data: orders });

    const result = await getMyOrders(3);
    expect(client.get).toHaveBeenCalledWith('/orders/buyer/3');
    expect(result).toHaveLength(1);
    expect(result[0].buyerId).toBe(3);
  });

  it('placeOrder calls POST /orders with payload', async () => {
    const payload = { productId: 1, quantity: 1, inventoryItemId: 1 };
    const created = { id: 10, ...payload, totalPrice: 49.99, status: 'PLACED' };
    (client.post as any).mockResolvedValue({ data: created });

    const result = await placeOrder(payload as any);
    expect(client.post).toHaveBeenCalledWith('/orders', payload);
    expect(result.id).toBe(10);
    expect(result.totalPrice).toBe(49.99);
  });

  it('updateOrderStatus calls PATCH /orders/:id/status with status query param', async () => {
    const updated = { id: 5, status: 'CONFIRMED' };
    (client.patch as any).mockResolvedValue({ data: updated });

    const result = await updateOrderStatus(5, 'CONFIRMED' as any);
    expect(client.patch).toHaveBeenCalledWith('/orders/5/status?status=CONFIRMED');
    expect(result.status).toBe('CONFIRMED');
  });
});
