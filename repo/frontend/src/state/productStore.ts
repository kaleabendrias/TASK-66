import { create } from 'zustand';
import * as productsApi from '@/api/products';
import { Product, ProductFormData } from '@/api/types';

interface ProductState {
  products: Product[];
  loading: boolean;
  error: string | null;
  fetchProducts: () => Promise<void>;
  addProduct: (data: ProductFormData) => Promise<Product>;
  updateProduct: (id: number, data: ProductFormData) => Promise<Product>;
  removeProduct: (id: number) => Promise<void>;
}

export const useProductStore = create<ProductState>((set, get) => ({
  products: [],
  loading: false,
  error: null,

  fetchProducts: async () => {
    set({ loading: true, error: null });
    try {
      const products = await productsApi.getProducts();
      set({ products, loading: false });
    } catch (err: any) {
      set({ error: err.message || 'Failed to fetch products', loading: false });
    }
  },

  addProduct: async (data: ProductFormData) => {
    const product = await productsApi.createProduct(data);
    set({ products: [...get().products, product] });
    return product;
  },

  updateProduct: async (id: number, data: ProductFormData) => {
    const product = await productsApi.updateProduct(id, data);
    set({
      products: get().products.map((p) => (p.id === id ? product : p)),
    });
    return product;
  },

  removeProduct: async (id: number) => {
    await productsApi.deleteProduct(id);
    set({ products: get().products.filter((p) => p.id !== id) });
  },
}));
