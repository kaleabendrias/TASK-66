import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('@/api/client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

import client from '@/api/client';
import { getCategories, createCategory } from '@/api/categories';

describe('categories API', () => {
  beforeEach(() => vi.clearAllMocks());

  it('getCategories calls GET /categories', async () => {
    const categories = [{ id: 1, name: 'Electronics' }, { id: 2, name: 'Books' }];
    (client.get as any).mockResolvedValue({ data: categories });

    const result = await getCategories();
    expect(client.get).toHaveBeenCalledWith('/categories');
    expect(result).toHaveLength(2);
    expect(result[0].name).toBe('Electronics');
  });

  it('createCategory calls POST /categories with name and description', async () => {
    const payload = { name: 'Clothing', description: 'Apparel and fashion' };
    const created = { id: 3, ...payload };
    (client.post as any).mockResolvedValue({ data: created });

    const result = await createCategory(payload);
    expect(client.post).toHaveBeenCalledWith('/categories', payload);
    expect(result.id).toBe(3);
    expect(result.name).toBe('Clothing');
  });
});
