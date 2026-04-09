import client from './client';
import { Fulfillment, FulfillmentStep } from './types';

export async function getFulfillmentByOrder(orderId: number): Promise<Fulfillment> {
  const { data } = await client.get<Fulfillment>(`/fulfillments/order/${orderId}`);
  return data;
}

export async function createFulfillment(orderId: number, warehouseId: number, idempotencyKey: string): Promise<Fulfillment> {
  const { data } = await client.post<Fulfillment>('/fulfillments', { orderId, warehouseId, idempotencyKey });
  return data;
}

export async function advanceFulfillment(id: number, stepName: string, notes: string): Promise<Fulfillment> {
  const { data } = await client.post<Fulfillment>(`/fulfillments/${id}/advance`, { stepName, notes });
  return data;
}

export async function cancelFulfillment(id: number): Promise<Fulfillment> {
  const { data } = await client.post<Fulfillment>(`/fulfillments/${id}/cancel`);
  return data;
}

export async function getFulfillmentSteps(id: number): Promise<FulfillmentStep[]> {
  const { data } = await client.get<FulfillmentStep[]>(`/fulfillments/${id}/steps`);
  return data;
}
