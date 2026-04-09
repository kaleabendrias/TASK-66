import client from './client';
import { Warehouse, InventoryItem, InventoryMovement, StockReservation } from './types';

export async function getWarehouses(): Promise<Warehouse[]> {
  const { data } = await client.get<Warehouse[]>('/warehouses');
  return data;
}

export async function getInventoryByProduct(productId: number): Promise<InventoryItem[]> {
  const { data } = await client.get<InventoryItem[]>(`/inventory/product/${productId}`);
  return data;
}

export async function getLowStockItems(): Promise<InventoryItem[]> {
  const { data } = await client.get<InventoryItem[]>('/inventory/low-stock');
  return data;
}

export async function adjustStock(payload: {
  inventoryItemId: number;
  quantityChange: number;
  movementType: string;
  referenceDocument: string;
  notes: string;
}): Promise<InventoryMovement> {
  const { data } = await client.post<InventoryMovement>('/inventory/adjust', payload);
  return data;
}

export async function reserveStock(inventoryItemId: number, quantity: number, idempotencyKey: string): Promise<StockReservation> {
  const { data } = await client.post<StockReservation>('/reservations', { inventoryItemId, quantity, idempotencyKey });
  return data;
}

export async function confirmReservation(id: number): Promise<StockReservation> {
  const { data } = await client.post<StockReservation>(`/reservations/${id}/confirm`);
  return data;
}

export async function cancelReservation(id: number): Promise<StockReservation> {
  const { data } = await client.post<StockReservation>(`/reservations/${id}/cancel`);
  return data;
}

export async function getMyReservations(): Promise<StockReservation[]> {
  const { data } = await client.get<StockReservation[]>('/reservations/my');
  return data;
}
