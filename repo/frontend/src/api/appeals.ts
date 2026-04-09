import client from './client';
import { Appeal } from './types';

export async function getAppeals(): Promise<Appeal[]> {
  const { data } = await client.get<Appeal[]>('/appeals');
  return data;
}

export async function getMyAppeals(): Promise<Appeal[]> {
  const { data } = await client.get<Appeal[]>('/appeals/my');
  return data;
}

export async function getAppeal(id: number): Promise<Appeal> {
  const { data } = await client.get<Appeal>(`/appeals/${id}`);
  return data;
}

export async function createAppeal(payload: {
  relatedEntityType: string;
  relatedEntityId: number;
  reason: string;
}): Promise<Appeal> {
  const { data } = await client.post<Appeal>('/appeals', payload);
  return data;
}

export async function reviewAppeal(id: number, status: string, reviewNotes: string): Promise<Appeal> {
  const { data } = await client.post<Appeal>(`/appeals/${id}/review`, { status, reviewNotes });
  return data;
}
