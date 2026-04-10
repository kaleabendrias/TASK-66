import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

vi.mock('@/state/authStore', () => ({
  useAuthStore: vi.fn((selector: any) => {
    const state = { user: { id: 2, username: 'member', role: 'MEMBER', displayName: 'Member', email: 'm@t.c', enabled: true }, isAuthenticated: true, token: 'tok', loading: false, error: null, login: vi.fn(), register: vi.fn(), logout: vi.fn(), loadFromStorage: vi.fn() };
    return typeof selector === 'function' ? selector(state) : state;
  }),
}));
vi.mock('@/api/products', () => ({ getProducts: vi.fn(() => Promise.resolve([])) }));
vi.mock('@/api/orders', () => ({ getOrders: vi.fn(() => Promise.resolve([])), getMyOrders: vi.fn(() => Promise.resolve([])) }));
vi.mock('@/api/users', () => ({ getUsers: vi.fn(() => Promise.resolve([])) }));
vi.mock('@/api/incidents', () => ({ getIncidents: vi.fn(() => Promise.resolve([])) }));

import DashboardPage from '@/pages/DashboardPage';

describe('DashboardPage', () => {
  it('renders welcome message', async () => {
    render(<MemoryRouter><DashboardPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText(/welcome/i)).toBeInTheDocument(), { timeout: 3000 });
  });
  it('shows user role', async () => {
    render(<MemoryRouter><DashboardPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText(/MEMBER/)).toBeInTheDocument(), { timeout: 3000 });
  });
});
