import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

vi.mock('@/state/authStore', () => ({
  useAuthStore: vi.fn((selector: any) => {
    const state = { user: { id: 3, username: 'seller', role: 'SELLER', displayName: 'S', email: '', enabled: true }, isAuthenticated: true, token: 'tok', loading: false, error: null, login: vi.fn(), register: vi.fn(), logout: vi.fn(), loadFromStorage: vi.fn() };
    return typeof selector === 'function' ? selector(state) : state;
  }),
}));
vi.mock('@/api/products', () => ({
  getProducts: vi.fn(() => Promise.resolve([
    { id: 1, name: 'Raku Bowl', description: 'Test', price: 85, stockQuantity: 12, categoryId: 1, categoryName: 'Pottery', sellerId: 3, sellerName: 'Seller', status: 'APPROVED' },
  ])),
}));
vi.mock('@/api/categories', () => ({ getCategories: vi.fn(() => Promise.resolve([{ id: 1, name: 'Pottery', description: '' }])) }));

import ProductsPage from '@/pages/ProductsPage';

describe('ProductsPage', () => {
  it('renders products heading', async () => {
    render(<MemoryRouter><ProductsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText(/products/i)).toBeInTheDocument(), { timeout: 3000 });
  });
  it('displays product cards', async () => {
    render(<MemoryRouter><ProductsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Raku Bowl')).toBeInTheDocument(), { timeout: 3000 });
  });
});
