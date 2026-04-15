import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const { mockUpdateOrderStatus, mockUseAuth } = vi.hoisted(() => ({
  mockUpdateOrderStatus: vi.fn(),
  mockUseAuth: vi.fn(),
}));

vi.mock('@/state/authStore', () => ({
  useAuthStore: vi.fn((selector: any) => {
    const state = {
      user: { id: 2, username: 'member', role: 'MEMBER', displayName: 'M', email: '', enabled: true },
      isAuthenticated: true,
      token: 'tok',
      loading: false,
      error: null,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      loadFromStorage: vi.fn(),
    };
    return typeof selector === 'function' ? selector(state) : state;
  }),
}));

vi.mock('@/features/auth/useAuth', () => ({
  useAuth: mockUseAuth,
}));

vi.mock('@/api/orders', () => ({
  getOrders: vi.fn(() => Promise.resolve([
    { id: 1, buyerId: 2, productId: 1, quantity: 1, totalPrice: 85, status: 'PLACED' },
  ])),
  getMyOrders: vi.fn(() => Promise.resolve([
    { id: 1, buyerId: 2, productId: 1, quantity: 1, totalPrice: 85.50, status: 'PLACED' },
  ])),
  updateOrderStatus: mockUpdateOrderStatus,
}));

import OrdersPage from '@/pages/OrdersPage';
import { getOrders } from '@/api/orders';

describe('OrdersPage - MEMBER', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUpdateOrderStatus.mockResolvedValue({ id: 1, status: 'CANCELLED' });
    mockUseAuth.mockReturnValue({
      user: { id: 2, username: 'member', role: 'MEMBER', displayName: 'M', email: '', enabled: true },
      isAuthenticated: true,
      loading: false,
      error: null,
      hasAnyRole: (...roles: string[]) => roles.includes('MEMBER'),
    });
  });

  it('renders orders heading', async () => {
    render(<MemoryRouter><OrdersPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText(/orders/i)).toBeInTheDocument(), { timeout: 3000 });
  });

  it('shows loading state before data arrives', () => {
    render(<MemoryRouter><OrdersPage /></MemoryRouter>);
    expect(screen.getByText('Loading orders...')).toBeInTheDocument();
  });

  it('displays order rows with price formatted', async () => {
    render(<MemoryRouter><OrdersPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('$85.50')).toBeInTheDocument(), { timeout: 3000 });
    expect(screen.getByText('PLACED')).toBeInTheDocument();
  });

  it('shows empty state when no orders', async () => {
    const { getMyOrders } = await import('@/api/orders');
    vi.mocked(getMyOrders).mockResolvedValueOnce([]);

    render(<MemoryRouter><OrdersPage /></MemoryRouter>);
    expect(await screen.findByText('No orders found.')).toBeInTheDocument();
  });

  it('does not show action buttons for MEMBER role', async () => {
    render(<MemoryRouter><OrdersPage /></MemoryRouter>);
    await waitFor(() => expect(screen.queryByText(/Mark CONFIRMED/i)).not.toBeInTheDocument());
  });
});

describe('OrdersPage - WAREHOUSE_STAFF', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUpdateOrderStatus.mockResolvedValue({ id: 10, status: 'CONFIRMED' });
    mockUseAuth.mockReturnValue({
      user: { id: 4, username: 'warehouse', role: 'WAREHOUSE_STAFF', displayName: 'W', email: '', enabled: true },
      isAuthenticated: true,
      loading: false,
      error: null,
      hasAnyRole: (...roles: string[]) => roles.includes('WAREHOUSE_STAFF'),
    });

    vi.mocked(getOrders).mockResolvedValue([
      { id: 10, buyerId: 2, productId: 3, quantity: 2, totalPrice: 50, status: 'PLACED' },
    ]);
  });

  it('shows Mark CONFIRMED action for PLACED order', async () => {
    render(<MemoryRouter><OrdersPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Mark CONFIRMED')).toBeInTheDocument(), { timeout: 3000 });
  });

  it('calls updateOrderStatus when action button is clicked', async () => {
    render(<MemoryRouter><OrdersPage /></MemoryRouter>);
    const btn = await screen.findByText('Mark CONFIRMED');
    fireEvent.click(btn);
    await waitFor(() => {
      expect(mockUpdateOrderStatus).toHaveBeenCalledWith(10, 'CONFIRMED');
    });
  });

  it('shows Cancel button for non-terminal orders', async () => {
    render(<MemoryRouter><OrdersPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Cancel')).toBeInTheDocument(), { timeout: 3000 });
  });

  it('shows error message when updateOrderStatus fails', async () => {
    mockUpdateOrderStatus.mockRejectedValueOnce(new Error('Network error'));
    render(<MemoryRouter><OrdersPage /></MemoryRouter>);
    const btn = await screen.findByText('Mark CONFIRMED');
    fireEvent.click(btn);
    await waitFor(() => expect(screen.getByText('Failed to update order status')).toBeInTheDocument());
  });
});

describe('OrdersPage - ADMINISTRATOR role', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUpdateOrderStatus.mockResolvedValue({ id: 1, status: 'CONFIRMED' });
    mockUseAuth.mockReturnValue({
      user: { id: 1, username: 'admin', role: 'ADMINISTRATOR', displayName: 'Admin', email: '', enabled: true },
      isAuthenticated: true,
      loading: false,
      error: null,
      hasAnyRole: (...roles: string[]) => roles.includes('ADMINISTRATOR'),
    });

    vi.mocked(getOrders).mockResolvedValue([
      { id: 20, buyerId: 2, productId: 1, quantity: 1, totalPrice: 120, status: 'PLACED' },
    ]);
  });

  it('admin uses getOrders (all orders, not buyer-scoped)', async () => {
    render(<MemoryRouter><OrdersPage /></MemoryRouter>);
    await waitFor(() => expect(getOrders).toHaveBeenCalled(), { timeout: 3000 });
  });

  it('admin sees Mark CONFIRMED action button', async () => {
    render(<MemoryRouter><OrdersPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Mark CONFIRMED')).toBeInTheDocument(), { timeout: 3000 });
  });

  it('admin can advance order status to CONFIRMED', async () => {
    render(<MemoryRouter><OrdersPage /></MemoryRouter>);
    const btn = await screen.findByText('Mark CONFIRMED');
    fireEvent.click(btn);
    await waitFor(() => expect(mockUpdateOrderStatus).toHaveBeenCalledWith(20, 'CONFIRMED'));
  });
});
