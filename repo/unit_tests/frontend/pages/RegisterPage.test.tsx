import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const { mockUseAuth, mockNavigate } = vi.hoisted(() => ({
  mockUseAuth: vi.fn(),
  mockNavigate: vi.fn(),
}));

const mockRegister = vi.fn();

vi.mock('@/state/authStore', () => ({
  useAuthStore: vi.fn((selector: any) => {
    const state = {
      user: null,
      isAuthenticated: false,
      token: null,
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

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

import RegisterPage from '@/pages/RegisterPage';

describe('RegisterPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockRegister.mockResolvedValue(undefined);
    mockUseAuth.mockReturnValue({
      user: null,
      isAuthenticated: false,
      login: vi.fn(),
      register: mockRegister,
      logout: vi.fn(),
      loading: false,
      error: null,
    });
  });

  it('renders registration form with labels', () => {
    render(<MemoryRouter><RegisterPage /></MemoryRouter>);
    expect(screen.getByText('Username')).toBeInTheDocument();
    expect(screen.getByText('Email')).toBeInTheDocument();
    expect(screen.getByText('Password')).toBeInTheDocument();
    expect(screen.getByText('Display Name')).toBeInTheDocument();
  });

  it('has a submit button', () => {
    render(<MemoryRouter><RegisterPage /></MemoryRouter>);
    expect(screen.getByRole('button', { name: /register/i })).toBeInTheDocument();
  });

  it('has login link', () => {
    render(<MemoryRouter><RegisterPage /></MemoryRouter>);
    expect(screen.getByText(/login/i)).toBeInTheDocument();
  });

  it('submits all fields and navigates to dashboard on success', async () => {
    render(<MemoryRouter><RegisterPage /></MemoryRouter>);

    fireEvent.change(screen.getByLabelText('Username'), { target: { value: 'newuser' } });
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'new@test.com' } });
    fireEvent.change(screen.getByLabelText('Display Name'), { target: { value: 'New User' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password123' } });
    fireEvent.click(screen.getByRole('button', { name: /register/i }));

    await waitFor(() => {
      expect(mockRegister).toHaveBeenCalledWith('newuser', 'new@test.com', 'password123', 'New User');
    });
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/dashboard');
    });
  });

  it('does not navigate when register throws', async () => {
    mockRegister.mockRejectedValue(new Error('Username taken'));
    render(<MemoryRouter><RegisterPage /></MemoryRouter>);

    fireEvent.change(screen.getByLabelText('Username'), { target: { value: 'dup' } });
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'dup@test.com' } });
    fireEvent.change(screen.getByLabelText('Display Name'), { target: { value: 'Dup' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'pass' } });
    fireEvent.click(screen.getByRole('button', { name: /register/i }));

    await waitFor(() => expect(mockRegister).toHaveBeenCalled());
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  it('displays error when auth returns an error', () => {
    mockUseAuth.mockReturnValue({
      user: null,
      isAuthenticated: false,
      login: vi.fn(),
      register: mockRegister,
      logout: vi.fn(),
      loading: false,
      error: 'Username already taken',
    });
    render(<MemoryRouter><RegisterPage /></MemoryRouter>);
    expect(screen.getByText('Username already taken')).toBeInTheDocument();
  });

  it('button is disabled and shows Loading... while loading', () => {
    mockUseAuth.mockReturnValue({
      user: null,
      isAuthenticated: false,
      login: vi.fn(),
      register: mockRegister,
      logout: vi.fn(),
      loading: true,
      error: null,
    });
    render(<MemoryRouter><RegisterPage /></MemoryRouter>);
    expect(screen.getByRole('button', { name: 'Loading...' })).toBeDisabled();
  });
});
