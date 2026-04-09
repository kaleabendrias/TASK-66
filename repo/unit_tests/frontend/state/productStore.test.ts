import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('@/api/products', () => ({
  getProducts: vi.fn(),
  createProduct: vi.fn(),
  updateProduct: vi.fn(),
  deleteProduct: vi.fn(),
}));

import { useProductStore } from '@/state/productStore';
import * as productsApi from '@/api/products';

const mockProduct = {
  id: 1,
  name: 'Widget',
  description: 'A widget',
  price: 9.99,
  stockQuantity: 100,
  categoryId: 1,
  categoryName: 'Cat',
  sellerId: 1,
  sellerName: 'Seller',
  status: 'APPROVED' as const,
};

describe('productStore', () => {
  beforeEach(() => {
    useProductStore.setState({ products: [], loading: false, error: null });
    vi.clearAllMocks();
  });

  it('starts with empty products', () => {
    expect(useProductStore.getState().products).toEqual([]);
    expect(useProductStore.getState().loading).toBe(false);
  });

  it('fetchProducts populates products', async () => {
    (productsApi.getProducts as any).mockResolvedValue([mockProduct]);

    await useProductStore.getState().fetchProducts();

    const state = useProductStore.getState();
    expect(state.products).toHaveLength(1);
    expect(state.products[0].name).toBe('Widget');
    expect(state.loading).toBe(false);
  });

  it('fetchProducts sets error on failure', async () => {
    (productsApi.getProducts as any).mockRejectedValue(new Error('Network error'));

    await useProductStore.getState().fetchProducts();

    expect(useProductStore.getState().error).toBe('Network error');
    expect(useProductStore.getState().loading).toBe(false);
  });

  it('addProduct appends to list', async () => {
    useProductStore.setState({ products: [] });
    (productsApi.createProduct as any).mockResolvedValue(mockProduct);

    const result = await useProductStore.getState().addProduct({
      name: 'Widget',
      description: 'A widget',
      price: 9.99,
      stockQuantity: 100,
      categoryId: 1,
    });

    expect(result.id).toBe(1);
    expect(useProductStore.getState().products).toHaveLength(1);
  });

  it('updateProduct replaces in list', async () => {
    useProductStore.setState({ products: [mockProduct] });
    const updated = { ...mockProduct, name: 'Updated Widget' };
    (productsApi.updateProduct as any).mockResolvedValue(updated);

    await useProductStore.getState().updateProduct(1, {
      name: 'Updated Widget',
      description: 'A widget',
      price: 9.99,
      stockQuantity: 100,
      categoryId: 1,
    });

    expect(useProductStore.getState().products[0].name).toBe('Updated Widget');
  });

  it('removeProduct filters from list', async () => {
    useProductStore.setState({ products: [mockProduct] });
    (productsApi.deleteProduct as any).mockResolvedValue(undefined);

    await useProductStore.getState().removeProduct(1);

    expect(useProductStore.getState().products).toHaveLength(0);
  });
});
