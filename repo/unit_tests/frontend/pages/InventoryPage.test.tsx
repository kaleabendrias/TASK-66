import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

vi.mock('@/state/authStore', () => ({
  useAuthStore: vi.fn((selector: any) => {
    const state = { user: { id: 5, username: 'staff', role: 'WAREHOUSE_STAFF', displayName: 'Staff', email: 's@t.c', enabled: true }, isAuthenticated: true, token: 'tok', loading: false, error: null, login: vi.fn(), register: vi.fn(), logout: vi.fn(), loadFromStorage: vi.fn() };
    return typeof selector === 'function' ? selector(state) : state;
  }),
}));
vi.mock('@/api/warehouses', () => ({
  getWarehouses: vi.fn(() => Promise.resolve([{ id: 1, name: 'Main Warehouse', code: 'WH1' }])),
  getLowStockItems: vi.fn(() => Promise.resolve([
    { id: 1, productId: 1, warehouseId: 1, warehouseName: 'Main Warehouse', quantityOnHand: 3, quantityReserved: 0, quantityAvailable: 3, lowStockThreshold: 10, lowStock: true },
  ])),
  getInventoryByProduct: vi.fn(() => Promise.resolve([])),
  adjustStock: vi.fn(() => Promise.resolve({})),
  getMyReservations: vi.fn(() => Promise.resolve([])),
  confirmReservation: vi.fn(() => Promise.resolve({})),
  cancelReservation: vi.fn(() => Promise.resolve({})),
}));
vi.mock('@/api/products', () => ({
  getProducts: vi.fn(() => Promise.resolve([{ id: 1, name: 'Widget', price: 10, stockQuantity: 50, categoryId: 1, sellerId: 1, status: 'APPROVED' }])),
}));

import InventoryPage from '@/pages/InventoryPage';

describe('InventoryPage', () => {
  it('renders heading', async () => {
    render(<MemoryRouter><InventoryPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/Inventory Management/i).length).toBeGreaterThan(0), { timeout: 3000 });
  });

  it('shows warehouse selector', async () => {
    render(<MemoryRouter><InventoryPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/All Warehouses/i).length).toBeGreaterThan(0), { timeout: 3000 });
  });

  it('shows low stock alerts section', async () => {
    render(<MemoryRouter><InventoryPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/Low Stock Alerts/i).length).toBeGreaterThan(0), { timeout: 3000 });
  });

  it('shows inventory tab', async () => {
    render(<MemoryRouter><InventoryPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/Inventory/i).length).toBeGreaterThan(0), { timeout: 3000 });
  });
});
