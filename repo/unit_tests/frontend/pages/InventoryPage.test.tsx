import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

vi.mock('@/state/authStore', () => ({
  useAuthStore: vi.fn((selector: any) => {
    const state = { user: { id: 5, username: 'staff', role: 'WAREHOUSE_STAFF', displayName: 'Staff', email: 's@t.c', enabled: true }, isAuthenticated: true, token: 'tok', loading: false, error: null, login: vi.fn(), register: vi.fn(), logout: vi.fn(), loadFromStorage: vi.fn() };
    return typeof selector === 'function' ? selector(state) : state;
  }),
}));

const mockRecordInbound = vi.fn();
const mockRecordOutbound = vi.fn();
const mockRecordStocktake = vi.fn();

vi.mock('@/api/warehouses', () => ({
  getWarehouses: vi.fn(() => Promise.resolve([{ id: 1, name: 'Main Warehouse', code: 'WH1' }])),
  getLowStockItems: vi.fn(() => Promise.resolve([
    { id: 1, productId: 1, warehouseId: 1, warehouseName: 'Main Warehouse', quantityOnHand: 3, quantityReserved: 0, quantityAvailable: 3, lowStockThreshold: 10, lowStock: true },
  ])),
  getInventoryByProduct: vi.fn(() => Promise.resolve([
    { id: 42, productId: 1, warehouseId: 1, warehouseName: 'Main Warehouse', quantityOnHand: 3, quantityReserved: 0, quantityAvailable: 3, lowStockThreshold: 10, lowStock: true },
  ])),
  recordInbound: (...args: any[]) => mockRecordInbound(...args),
  recordOutbound: (...args: any[]) => mockRecordOutbound(...args),
  recordStocktake: (...args: any[]) => mockRecordStocktake(...args),
  getMyReservations: vi.fn(() => Promise.resolve([])),
  confirmReservation: vi.fn(() => Promise.resolve({})),
  cancelReservation: vi.fn(() => Promise.resolve({})),
}));
vi.mock('@/api/products', () => ({
  getProducts: vi.fn(() => Promise.resolve([{ id: 1, name: 'Widget', price: 10, stockQuantity: 50, categoryId: 1, sellerId: 1, status: 'APPROVED' }])),
}));

import InventoryPage from '@/pages/InventoryPage';

describe('InventoryPage', () => {
  beforeEach(() => {
    mockRecordInbound.mockReset().mockResolvedValue({});
    mockRecordOutbound.mockReset().mockResolvedValue({});
    mockRecordStocktake.mockReset().mockResolvedValue({});
  });

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

  it('shows dedicated Inbound, Outbound, and Stocktake actions per row', async () => {
    render(<MemoryRouter><InventoryPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByRole('button', { name: /^Inbound$/i }).length).toBeGreaterThan(0), { timeout: 3000 });
    expect(screen.getAllByRole('button', { name: /^Outbound$/i }).length).toBeGreaterThan(0);
    expect(screen.getAllByRole('button', { name: /^Stocktake$/i }).length).toBeGreaterThan(0);
  });

  it('submitting an Inbound movement calls /inventory/inbound with the correct payload', async () => {
    render(<MemoryRouter><InventoryPage /></MemoryRouter>);
    const inboundBtn = await waitFor(() => screen.getAllByRole('button', { name: /^Inbound$/i })[0]);
    fireEvent.click(inboundBtn);

    const dialog = await waitFor(() => screen.getByRole('dialog'));
    const qty = within(dialog).getByLabelText(/^Quantity$/i) as HTMLInputElement;
    fireEvent.change(qty, { target: { value: '12' } });
    fireEvent.click(within(dialog).getByRole('button', { name: /record inbound/i }));

    await waitFor(() => expect(mockRecordInbound).toHaveBeenCalledWith({
      inventoryItemId: 42,
      quantity: 12,
      referenceDocument: '',
      notes: '',
    }));
    expect(mockRecordOutbound).not.toHaveBeenCalled();
    expect(mockRecordStocktake).not.toHaveBeenCalled();
  });

  it('submitting a Stocktake routes to /inventory/stocktake with counted quantity', async () => {
    render(<MemoryRouter><InventoryPage /></MemoryRouter>);
    const stocktakeBtn = await waitFor(() => screen.getAllByRole('button', { name: /^Stocktake$/i })[0]);
    fireEvent.click(stocktakeBtn);

    const dialog = await waitFor(() => screen.getByRole('dialog'));
    const counted = within(dialog).getByLabelText(/counted quantity/i) as HTMLInputElement;
    fireEvent.change(counted, { target: { value: '8' } });
    fireEvent.click(within(dialog).getByRole('button', { name: /record stocktake/i }));

    await waitFor(() => expect(mockRecordStocktake).toHaveBeenCalledWith({
      productId: 1,
      warehouseId: 1,
      countedQuantity: 8,
      referenceDocument: '',
    }));
    expect(mockRecordInbound).not.toHaveBeenCalled();
  });
});
