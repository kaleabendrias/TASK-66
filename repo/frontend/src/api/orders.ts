import client from './client';
import { Order, OrderFormData, OrderStatus } from './types';

export async function getOrders(): Promise<Order[]> {
  const { data } = await client.get<Order[]>('/orders');
  return data;
}

export async function getMyOrders(buyerId: number): Promise<Order[]> {
  const { data } = await client.get<Order[]>(`/orders/buyer/${buyerId}`);
  return data;
}

export async function placeOrder(orderData: OrderFormData): Promise<Order> {
  const { data } = await client.post<Order>('/orders', orderData);
  return data;
}

export async function updateOrderStatus(id: number, status: OrderStatus): Promise<Order> {
  const { data } = await client.patch<Order>(`/orders/${id}/status?status=${status}`);
  return data;
}
