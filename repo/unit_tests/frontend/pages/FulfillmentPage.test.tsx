import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

vi.mock('@/state/authStore', () => ({
  useAuthStore: vi.fn((selector: any) => {
    const state = { user: { id: 5, username: 'staff', role: 'WAREHOUSE_STAFF', displayName: 'Staff', email: 's@t.c', enabled: true }, isAuthenticated: true, token: 'tok', loading: false, error: null, login: vi.fn(), register: vi.fn(), logout: vi.fn(), loadFromStorage: vi.fn() };
    return typeof selector === 'function' ? selector(state) : state;
  }),
}));
vi.mock('@/api/orders', () => ({
  getOrders: vi.fn(() => Promise.resolve([
    { id: 1, buyerId: 2, productId: 1, quantity: 3, totalPrice: 30, status: 'PLACED' },
  ])),
}));
vi.mock('@/api/warehouses', () => ({
  getWarehouses: vi.fn(() => Promise.resolve([{ id: 1, name: 'Main WH', code: 'WH1' }])),
}));
vi.mock('@/api/fulfillments', () => ({
  getFulfillmentByOrder: vi.fn(() => Promise.resolve(null)),
  createFulfillment: vi.fn(() => Promise.resolve({})),
  advanceFulfillment: vi.fn(() => Promise.resolve({})),
  cancelFulfillment: vi.fn(() => Promise.resolve({})),
  getFulfillmentSteps: vi.fn(() => Promise.resolve([])),
}));

import FulfillmentPage from '@/pages/FulfillmentPage';

describe('FulfillmentPage', () => {
  it('renders heading', async () => {
    render(<MemoryRouter><FulfillmentPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/Fulfillment Management/i).length).toBeGreaterThan(0), { timeout: 3000 });
  });

  it('shows order list', async () => {
    render(<MemoryRouter><FulfillmentPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/Orders/i).length).toBeGreaterThan(0), { timeout: 3000 });
    expect(screen.getByText('#1')).toBeInTheDocument();
  });

  it('shows fulfillment detail panel', async () => {
    render(<MemoryRouter><FulfillmentPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/Fulfillment Detail/i).length).toBeGreaterThan(0), { timeout: 3000 });
  });
});
