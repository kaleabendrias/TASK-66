import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

vi.mock('@/state/authStore', () => ({
  useAuthStore: vi.fn((selector: any) => {
    const state = { user: null, isAuthenticated: false, token: null, loading: false, error: null, login: vi.fn(), register: vi.fn(), logout: vi.fn(), loadFromStorage: vi.fn() };
    return typeof selector === 'function' ? selector(state) : state;
  }),
}));
vi.mock('@/features/auth/useAuth', () => ({
  useAuth: () => ({ user: null, isAuthenticated: false, login: vi.fn(), register: vi.fn(), logout: vi.fn(), loading: false, error: null }),
}));

import RegisterPage from '@/pages/RegisterPage';

describe('RegisterPage', () => {
  it('renders registration form with labels', () => {
    render(<MemoryRouter><RegisterPage /></MemoryRouter>);
    expect(screen.getByText('Username')).toBeInTheDocument();
    expect(screen.getByText('Email')).toBeInTheDocument();
    expect(screen.getByText('Password')).toBeInTheDocument();
  });
  it('has a submit button', () => {
    render(<MemoryRouter><RegisterPage /></MemoryRouter>);
    expect(screen.getByRole('button', { name: /register/i })).toBeInTheDocument();
  });
  it('has login link', () => {
    render(<MemoryRouter><RegisterPage /></MemoryRouter>);
    expect(screen.getByText(/login/i)).toBeInTheDocument();
  });
});
