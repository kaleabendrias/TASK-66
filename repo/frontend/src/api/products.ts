import client from './client';
import { Product, ProductFormData } from './types';

export async function getProducts(): Promise<Product[]> {
  const { data } = await client.get<Product[]>('/products');
  return data;
}

export async function getProduct(id: number): Promise<Product> {
  const { data } = await client.get<Product>(`/products/${id}`);
  return data;
}

export async function createProduct(formData: ProductFormData): Promise<Product> {
  const { data } = await client.post<Product>('/products', formData);
  return data;
}

export async function updateProduct(id: number, formData: ProductFormData): Promise<Product> {
  const { data } = await client.put<Product>(`/products/${id}`, formData);
  return data;
}

export async function deleteProduct(id: number): Promise<void> {
  await client.delete(`/products/${id}`);
}
