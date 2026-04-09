import client from './client';
import { Category } from './types';

export async function getCategories(): Promise<Category[]> {
  const { data } = await client.get<Category[]>('/categories');
  return data;
}

export async function createCategory(categoryData: { name: string; description: string }): Promise<Category> {
  const { data } = await client.post<Category>('/categories', categoryData);
  return data;
}
