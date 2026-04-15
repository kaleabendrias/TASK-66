import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('@/api/client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

import client from '@/api/client';
import { getProducts, getProduct, createProduct, updateProduct, deleteProduct } from '@/api/products';

describe('products API', () => {
  beforeEach(() => vi.clearAllMocks());

  it('getProducts calls GET /products', async () => {
    const products = [{ id: 1, name: 'Widget', price: 9.99 }];
    (client.get as any).mockResolvedValue({ data: products });

    const result = await getProducts();
    expect(client.get).toHaveBeenCalledWith('/products');
    expect(result).toEqual(products);
  });

  it('getProduct calls GET /products/:id', async () => {
    const product = { id: 3, name: 'Gadget', price: 29.99 };
    (client.get as any).mockResolvedValue({ data: product });

    const result = await getProduct(3);
    expect(client.get).toHaveBeenCalledWith('/products/3');
    expect(result.name).toBe('Gadget');
  });

  it('createProduct calls POST /products with form data', async () => {
    const payload = { name: 'New Product', price: 15.0, categoryId: 1 };
    const created = { id: 5, ...payload };
    (client.post as any).mockResolvedValue({ data: created });

    const result = await createProduct(payload as any);
    expect(client.post).toHaveBeenCalledWith('/products', payload);
    expect(result.id).toBe(5);
  });

  it('updateProduct calls PUT /products/:id', async () => {
    const payload = { name: 'Updated', price: 20.0, categoryId: 1 };
    const updated = { id: 2, ...payload };
    (client.put as any).mockResolvedValue({ data: updated });

    const result = await updateProduct(2, payload as any);
    expect(client.put).toHaveBeenCalledWith('/products/2', payload);
    expect(result.name).toBe('Updated');
  });

  it('deleteProduct calls DELETE /products/:id', async () => {
    (client.delete as any).mockResolvedValue({ data: undefined });

    await deleteProduct(7);
    expect(client.delete).toHaveBeenCalledWith('/products/7');
  });
});
