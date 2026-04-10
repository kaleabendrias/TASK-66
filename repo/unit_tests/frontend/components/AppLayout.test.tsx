import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

vi.mock('@/state/authStore', () => ({
  useAuthStore: vi.fn((selector: any) => {
    const state = { user: { id: 6, username: 'admin', role: 'ADMINISTRATOR', displayName: 'Admin', email: '', enabled: true }, isAuthenticated: true, token: 'tok', loading: false, error: null, login: vi.fn(), register: vi.fn(), logout: vi.fn(), loadFromStorage: vi.fn() };
    return typeof selector === 'function' ? selector(state) : state;
  }),
}));

import AppLayout from '@/components/layout/AppLayout';

describe('AppLayout', () => {
  it('renders sidebar with nav links', () => {
    render(<MemoryRouter><AppLayout /></MemoryRouter>);
    expect(screen.getByText('Demo App')).toBeInTheDocument();
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Discover')).toBeInTheDocument();
  });
  it('shows user info in topbar', () => {
    render(<MemoryRouter><AppLayout /></MemoryRouter>);
    expect(screen.getAllByText(/Admin/).length).toBeGreaterThan(0);
    expect(screen.getByText('ADMINISTRATOR')).toBeInTheDocument();
  });
  it('shows logout button', () => {
    render(<MemoryRouter><AppLayout /></MemoryRouter>);
    expect(screen.getByText('Logout')).toBeInTheDocument();
  });
  it('shows admin-only nav items for admin user', () => {
    render(<MemoryRouter><AppLayout /></MemoryRouter>);
    expect(screen.getByText('Users')).toBeInTheDocument();
    expect(screen.getByText('Moderation')).toBeInTheDocument();
  });
});
