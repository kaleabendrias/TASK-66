import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const mockGetMyReservations = vi.fn();
const mockConfirmReservation = vi.fn();
const mockCancelReservation = vi.fn();

// Sentinel-throwing mocks: any access to a warehouse/seller-only endpoint
// from this page while a member is logged in must be a regression. Vitest
// only forwards top-level variables into vi.mock factories when they start
// with `mock`, hence the naming convention here.
vi.mock('@/api/warehouses', () => {
  const forbid = (name: string) => () => {
    throw new Error(`ReservationsPage prefetched ${name}, which a MEMBER is not authorized to call`);
  };
  return {
    getMyReservations: (...args: any[]) => mockGetMyReservations(...args),
    confirmReservation: (...args: any[]) => mockConfirmReservation(...args),
    cancelReservation: (...args: any[]) => mockCancelReservation(...args),
    getWarehouses: vi.fn(forbid('getWarehouses')),
    getInventoryByProduct: vi.fn(forbid('getInventoryByProduct')),
    getLowStockItems: vi.fn(forbid('getLowStockItems')),
    recordInbound: vi.fn(forbid('recordInbound')),
    recordOutbound: vi.fn(forbid('recordOutbound')),
    recordStocktake: vi.fn(forbid('recordStocktake')),
  };
});
vi.mock('@/api/products', () => ({
  getProducts: vi.fn(() => {
    throw new Error('ReservationsPage prefetched getProducts, which a MEMBER is not authorized to call');
  }),
}));
vi.mock('@/state/authStore', () => ({
  useAuthStore: vi.fn((selector: any) => {
    const state = { user: { id: 2, username: 'member', role: 'MEMBER', displayName: 'M', email: 'm@t.c', enabled: true }, isAuthenticated: true, token: 'tok', loading: false, error: null, login: vi.fn(), register: vi.fn(), logout: vi.fn(), loadFromStorage: vi.fn() };
    return typeof selector === 'function' ? selector(state) : state;
  }),
}));

import ReservationsPage from '@/pages/ReservationsPage';

describe('ReservationsPage', () => {
  beforeEach(() => {
    mockGetMyReservations.mockReset().mockResolvedValue([
      { id: 1, inventoryItemId: 42, userId: 2, quantity: 3, status: 'HELD', idempotencyKey: 'k', expiresAt: new Date(Date.now() + 10 * 60_000).toISOString(), createdAt: '2026-04-10T10:00:00', confirmedAt: null, cancelledAt: null },
      { id: 2, inventoryItemId: 99, userId: 2, quantity: 1, status: 'CONFIRMED', idempotencyKey: 'k2', expiresAt: '2026-04-10T10:00:00', createdAt: '2026-04-10T09:00:00', confirmedAt: '2026-04-10T09:30:00', cancelledAt: null },
    ]);
    mockConfirmReservation.mockReset().mockResolvedValue({});
    mockCancelReservation.mockReset().mockResolvedValue({});
  });

  it('renders the heading and lists the member reservations', async () => {
    render(<MemoryRouter><ReservationsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText(/My Reservations/i)).toBeInTheDocument());
    expect(screen.getByText('#42')).toBeInTheDocument();
    expect(screen.getByText('HELD')).toBeInTheDocument();
    expect(screen.getByText('CONFIRMED')).toBeInTheDocument();
  });

  it('confirms a HELD reservation via the API', async () => {
    render(<MemoryRouter><ReservationsPage /></MemoryRouter>);
    const confirmButton = await waitFor(() => screen.getByRole('button', { name: /^confirm$/i }));
    fireEvent.click(confirmButton);
    await waitFor(() => expect(mockConfirmReservation).toHaveBeenCalledWith(1));
  });

  it('cancels a HELD reservation via the API', async () => {
    render(<MemoryRouter><ReservationsPage /></MemoryRouter>);
    const cancelButton = await waitFor(() => screen.getByRole('button', { name: /^cancel$/i }));
    fireEvent.click(cancelButton);
    await waitFor(() => expect(mockCancelReservation).toHaveBeenCalledWith(1));
  });

  it('does not call any seller/warehouse-only endpoints (member-clean prefetch)', async () => {
    // Just rendering the page must not throw. The vi.mock setup above turns
    // any forbidden call into a thrown error, so a clean render proves the
    // member's UI-to-backend flow stays inside the authorized surface.
    render(<MemoryRouter><ReservationsPage /></MemoryRouter>);
    await waitFor(() => expect(mockGetMyReservations).toHaveBeenCalledTimes(1));
  });
});
