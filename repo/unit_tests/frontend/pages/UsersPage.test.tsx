import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

vi.mock('@/state/authStore', () => ({
  useAuthStore: vi.fn((selector: any) => {
    const state = { user: { id: 6, username: 'admin', role: 'ADMINISTRATOR', displayName: 'Admin', email: '', enabled: true }, isAuthenticated: true, token: 'tok', loading: false, error: null, login: vi.fn(), register: vi.fn(), logout: vi.fn(), loadFromStorage: vi.fn() };
    return typeof selector === 'function' ? selector(state) : state;
  }),
}));
vi.mock('@/api/users', () => ({
  getUsers: vi.fn(() => Promise.resolve([
    { id: 1, username: 'guest', email: 'g@t.c', displayName: 'Guest', role: 'GUEST', enabled: true },
    { id: 2, username: 'member', email: 'm@t.c', displayName: 'Member', role: 'MEMBER', enabled: true },
  ])),
  updateUser: vi.fn(() => Promise.resolve({})),
  deleteUser: vi.fn(() => Promise.resolve()),
}));

import UsersPage from '@/pages/UsersPage';

describe('UsersPage', () => {
  it('renders users heading', async () => {
    render(<MemoryRouter><UsersPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText(/users/i)).toBeInTheDocument(), { timeout: 3000 });
  });
  it('displays user list', async () => {
    render(<MemoryRouter><UsersPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('guest')).toBeInTheDocument(), { timeout: 3000 });
  });
});
