import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('@/api/client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

import client from '@/api/client';
import {
  getFulfillmentByOrder,
  createFulfillment,
  advanceFulfillment,
  cancelFulfillment,
  getFulfillmentSteps,
} from '@/api/fulfillments';

describe('fulfillments API', () => {
  beforeEach(() => vi.clearAllMocks());

  it('getFulfillmentByOrder calls GET /fulfillments/order/:orderId', async () => {
    const ful = { id: 1, orderId: 10, warehouseId: 2, status: 'PENDING' };
    (client.get as any).mockResolvedValue({ data: ful });

    const result = await getFulfillmentByOrder(10);
    expect(client.get).toHaveBeenCalledWith('/fulfillments/order/10');
    expect(result.orderId).toBe(10);
  });

  it('createFulfillment calls POST /fulfillments with orderId, warehouseId, idempotencyKey', async () => {
    const ful = { id: 2, orderId: 5, warehouseId: 1, status: 'PENDING' };
    (client.post as any).mockResolvedValue({ data: ful });

    const result = await createFulfillment(5, 1, 'key-abc');
    expect(client.post).toHaveBeenCalledWith('/fulfillments', {
      orderId: 5,
      warehouseId: 1,
      idempotencyKey: 'key-abc',
    });
    expect(result.id).toBe(2);
  });

  it('advanceFulfillment calls POST /fulfillments/:id/advance', async () => {
    const ful = { id: 3, status: 'PICKING' };
    (client.post as any).mockResolvedValue({ data: ful });

    const result = await advanceFulfillment(3, 'PICK', 'Picked items');
    expect(client.post).toHaveBeenCalledWith('/fulfillments/3/advance', {
      stepName: 'PICK',
      notes: 'Picked items',
    });
    expect(result.status).toBe('PICKING');
  });

  it('cancelFulfillment calls POST /fulfillments/:id/cancel', async () => {
    const ful = { id: 4, status: 'CANCELLED' };
    (client.post as any).mockResolvedValue({ data: ful });

    const result = await cancelFulfillment(4);
    expect(client.post).toHaveBeenCalledWith('/fulfillments/4/cancel');
    expect(result.status).toBe('CANCELLED');
  });

  it('getFulfillmentSteps calls GET /fulfillments/:id/steps', async () => {
    const steps = [
      { id: 1, fulfillmentId: 2, stepName: 'PICK', completedAt: '2026-04-01T10:00:00', status: 'COMPLETED' },
    ];
    (client.get as any).mockResolvedValue({ data: steps });

    const result = await getFulfillmentSteps(2);
    expect(client.get).toHaveBeenCalledWith('/fulfillments/2/steps');
    expect(result[0].stepName).toBe('PICK');
  });
});
