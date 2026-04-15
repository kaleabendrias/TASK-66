import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const { mockUseAuth, mockUseDashboard } = vi.hoisted(() => ({
  mockUseAuth: vi.fn(),
  mockUseDashboard: vi.fn(),
}));

const mockFetchStats = vi.fn();

vi.mock('@/state/authStore', () => ({
  useAuthStore: vi.fn((selector: any) => {
    const state = {
      user: { id: 2, username: 'member', role: 'MEMBER', displayName: 'Member User', email: 'm@t.c', enabled: true },
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

vi.mock('@/features/dashboard/useDashboard', () => ({
  useDashboard: mockUseDashboard,
}));

import DashboardPage from '@/pages/DashboardPage';

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseAuth.mockReturnValue({
      user: { id: 2, username: 'member', role: 'MEMBER', displayName: 'Member User', email: 'm@t.c', enabled: true },
      isAuthenticated: true,
      loading: false,
      error: null,
    });
    mockUseDashboard.mockReturnValue({
      stats: { totalProducts: 12, myOrders: 3 },
      loading: false,
      fetchStats: mockFetchStats,
    });
  });

  it('renders welcome message with display name', async () => {
    render(<MemoryRouter><DashboardPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText(/welcome back/i)).toBeInTheDocument(), { timeout: 3000 });
    expect(screen.getByText('Member User')).toBeInTheDocument();
  });

  it('shows user role badge', async () => {
    render(<MemoryRouter><DashboardPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('MEMBER')).toBeInTheDocument(), { timeout: 3000 });
  });

  it('calls fetchStats on mount', async () => {
    render(<MemoryRouter><DashboardPage /></MemoryRouter>);
    await waitFor(() => expect(mockFetchStats).toHaveBeenCalledTimes(1));
  });

  it('shows stats cards when data is loaded', async () => {
    render(<MemoryRouter><DashboardPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('12')).toBeInTheDocument(), { timeout: 3000 });
    expect(screen.getByText('Total Products')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getByText('Orders Placed')).toBeInTheDocument();
  });

  it('shows loading spinner while fetching stats', () => {
    mockUseDashboard.mockReturnValue({
      stats: {},
      loading: true,
      fetchStats: mockFetchStats,
    });
    render(<MemoryRouter><DashboardPage /></MemoryRouter>);
    expect(screen.getByText('Loading stats...')).toBeInTheDocument();
  });

  it('renders nothing when user is null', () => {
    mockUseAuth.mockReturnValue({
      user: null,
      isAuthenticated: false,
      loading: false,
      error: null,
    });
    const { container } = render(<MemoryRouter><DashboardPage /></MemoryRouter>);
    expect(container.firstChild).toBeNull();
  });

  it('shows admin-specific stats (totalOrders, totalUsers) for ADMINISTRATOR role', async () => {
    mockUseAuth.mockReturnValue({
      user: { id: 6, username: 'admin', role: 'ADMINISTRATOR', displayName: 'Admin', email: 'a@b.c', enabled: true },
      isAuthenticated: true,
      loading: false,
      error: null,
    });
    mockUseDashboard.mockReturnValue({
      stats: { totalProducts: 5, totalOrders: 20, totalUsers: 8 },
      loading: false,
      fetchStats: mockFetchStats,
    });
    render(<MemoryRouter><DashboardPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('20')).toBeInTheDocument(), { timeout: 3000 });
    expect(screen.getByText('Total Orders')).toBeInTheDocument();
    expect(screen.getByText('8')).toBeInTheDocument();
    expect(screen.getByText('Total Users')).toBeInTheDocument();
  });

  it('shows pendingProducts card only when count > 0', async () => {
    mockUseDashboard.mockReturnValue({
      stats: { totalProducts: 10, pendingProducts: 2 },
      loading: false,
      fetchStats: mockFetchStats,
    });
    render(<MemoryRouter><DashboardPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Products Pending')).toBeInTheDocument(), { timeout: 3000 });
    expect(screen.getByText('2')).toBeInTheDocument();
  });

  it('does not show pendingProducts card when count is 0', async () => {
    mockUseDashboard.mockReturnValue({
      stats: { totalProducts: 10, pendingProducts: 0 },
      loading: false,
      fetchStats: mockFetchStats,
    });
    render(<MemoryRouter><DashboardPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('10')).toBeInTheDocument(), { timeout: 3000 });
    expect(screen.queryByText('Products Pending')).not.toBeInTheDocument();
  });
});
