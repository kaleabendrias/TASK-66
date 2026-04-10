import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

vi.mock('@/state/authStore', () => ({
  useAuthStore: vi.fn((selector: any) => {
    const state = { user: { id: 2, username: 'member', role: 'MEMBER', displayName: 'M', email: '', enabled: true }, isAuthenticated: true, token: 'tok', loading: false, error: null, login: vi.fn(), register: vi.fn(), logout: vi.fn(), loadFromStorage: vi.fn() };
    return typeof selector === 'function' ? selector(state) : state;
  }),
}));
vi.mock('@/api/orders', () => ({
  getOrders: vi.fn(() => Promise.resolve([
    { id: 1, buyerId: 2, productId: 1, quantity: 1, totalPrice: 85, status: 'PLACED' },
  ])),
  getMyOrders: vi.fn(() => Promise.resolve([
    { id: 1, buyerId: 2, productId: 1, quantity: 1, totalPrice: 85, status: 'PLACED' },
  ])),
  updateOrderStatus: vi.fn(() => Promise.resolve({})),
}));

import OrdersPage from '@/pages/OrdersPage';

describe('OrdersPage', () => {
  it('renders orders heading', async () => {
    render(<MemoryRouter><OrdersPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText(/orders/i)).toBeInTheDocument(), { timeout: 3000 });
  });
});
