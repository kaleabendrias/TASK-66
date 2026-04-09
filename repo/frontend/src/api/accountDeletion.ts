import client from './client';
import { AccountDeletionStatus } from './types';

export async function requestDeletion(): Promise<AccountDeletionStatus> {
  const { data } = await client.post<AccountDeletionStatus>('/account-deletion/request');
  return data;
}

export async function cancelDeletion(id: number): Promise<AccountDeletionStatus> {
  const { data } = await client.post<AccountDeletionStatus>(`/account-deletion/${id}/cancel`);
  return data;
}

export async function getDeletionStatus(): Promise<AccountDeletionStatus> {
  const { data } = await client.get<AccountDeletionStatus>('/account-deletion/status');
  return data;
}
