import client from './client';
import { User } from './types';

export async function getUsers(): Promise<User[]> {
  const { data } = await client.get<User[]>('/users');
  return data;
}

export async function getMe(): Promise<User> {
  const { data } = await client.get<User>('/users/me');
  return data;
}

export async function updateUser(id: number, userData: Partial<User>): Promise<User> {
  const { data } = await client.put<User>(`/users/${id}`, userData);
  return data;
}

export async function deleteUser(id: number): Promise<void> {
  await client.delete(`/users/${id}`);
}
