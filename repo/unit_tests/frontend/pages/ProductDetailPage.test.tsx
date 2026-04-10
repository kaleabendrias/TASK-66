import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useParams: () => ({ id: '1' }) };
});
vi.mock('@/state/authStore', () => ({
  useAuthStore: vi.fn((selector: any) => {
    const state = { user: { id: 2, username: 'member', role: 'MEMBER', displayName: 'Member', email: 'm@t.c', enabled: true }, isAuthenticated: true, token: 'tok', loading: false, error: null, login: vi.fn(), register: vi.fn(), logout: vi.fn(), loadFromStorage: vi.fn() };
    return typeof selector === 'function' ? selector(state) : state;
  }),
}));
vi.mock('@/features/auth/useAuth', () => ({
  useAuth: vi.fn(() => ({
    user: { id: 2, username: 'member', role: 'MEMBER', displayName: 'Member', email: 'm@t.c', enabled: true },
    isAuthenticated: true,
    hasAnyRole: (...roles: string[]) => roles.includes('MEMBER'),
  })),
}));
vi.mock('@/api/products', () => ({
  getProduct: vi.fn(() => Promise.resolve({ id: 1, name: 'Test Product', description: 'A great product', price: 25.99, stockQuantity: 100, categoryId: 1, categoryName: 'Electronics', sellerId: 3, sellerName: 'Seller1', status: 'APPROVED' })),
  getProducts: vi.fn(() => Promise.resolve([])),
  updateProduct: vi.fn(() => Promise.resolve({})),
}));
vi.mock('@/api/orders', () => ({
  placeOrder: vi.fn(() => Promise.resolve({ id: 1 })),
}));
vi.mock('@/components/ui/Card', () => ({ default: ({ children, title }: any) => <div><h2>{title}</h2>{children}</div> }));
vi.mock('@/components/ui/Badge', () => ({ default: ({ children }: any) => <span>{children}</span> }));
vi.mock('@/components/ui/Button', () => ({ default: ({ children, onClick, ...props }: any) => <button onClick={onClick} {...props}>{children}</button> }));
vi.mock('@/components/ui/Input', () => ({ default: ({ label, ...props }: any) => <div><label>{label}</label><input {...props} /></div> }));
vi.mock('@/components/ui/Modal', () => ({ default: ({ children, open }: any) => open ? <div>{children}</div> : null }));

import ProductDetailPage from '@/pages/ProductDetailPage';

describe('ProductDetailPage', () => {
  it('renders product info', async () => {
    render(<MemoryRouter><ProductDetailPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/Test Product/i).length).toBeGreaterThan(0), { timeout: 3000 });
  });

  it('shows price', async () => {
    render(<MemoryRouter><ProductDetailPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/25\.99/i).length).toBeGreaterThan(0), { timeout: 3000 });
  });

  it('shows order form for members', async () => {
    render(<MemoryRouter><ProductDetailPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/Quantity/i).length).toBeGreaterThan(0), { timeout: 3000 });
    expect(screen.getAllByText(/Place Order/i).length).toBeGreaterThan(0);
  });
});
