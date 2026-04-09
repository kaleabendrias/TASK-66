import { useProductStore } from '@/state/productStore';

export function useProducts() {
  const store = useProductStore();
  return {
    products: store.products,
    loading: store.loading,
    error: store.error,
    fetchProducts: store.fetchProducts,
    addProduct: store.addProduct,
    updateProduct: store.updateProduct,
    removeProduct: store.removeProduct,
  };
}
