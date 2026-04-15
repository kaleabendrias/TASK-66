import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';

// vi.hoisted ensures these are initialized before vi.mock factories run
const { mockUseAuth, mockNavigate } = vi.hoisted(() => ({
  mockUseAuth: vi.fn(),
  mockNavigate: vi.fn(),
}));

const mockLogin = vi.fn();

vi.mock('@/features/auth/useAuth', () => ({
  useAuth: mockUseAuth,
}));

vi.mock('react-router-dom', () => ({
  Link: ({ children, to }: any) => <a href={to}>{children}</a>,
  useNavigate: () => mockNavigate,
}));

import LoginPage from '@/pages/LoginPage';

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockLogin.mockResolvedValue(undefined);
    mockUseAuth.mockReturnValue({
      login: mockLogin,
      loading: false,
      error: null,
    });
  });

  it('renders login form', () => {
    render(<LoginPage />);
    expect(screen.getByLabelText('Username')).toBeInTheDocument();
    expect(screen.getByLabelText('Password')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Login' })).toBeInTheDocument();
  });

  it('calls login with entered credentials on submit', async () => {
    render(<LoginPage />);
    fireEvent.change(screen.getByLabelText('Username'), { target: { value: 'admin' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password123' } });
    fireEvent.click(screen.getByRole('button', { name: 'Login' }));

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith('admin', 'password123');
    });
  });

  it('navigates to dashboard on successful login', async () => {
    render(<LoginPage />);
    fireEvent.change(screen.getByLabelText('Username'), { target: { value: 'admin' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'pass' } });
    fireEvent.click(screen.getByRole('button', { name: 'Login' }));

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/dashboard');
    });
  });

  it('shows register link', () => {
    render(<LoginPage />);
    expect(screen.getByText('Register')).toBeInTheDocument();
  });

  it('displays error message when auth store reports an error', () => {
    mockUseAuth.mockReturnValue({
      login: mockLogin,
      loading: false,
      error: 'Bad credentials',
    });
    render(<LoginPage />);
    expect(screen.getByText('Bad credentials')).toBeInTheDocument();
  });

  it('does not navigate when login throws', async () => {
    mockLogin.mockRejectedValue(new Error('Unauthorized'));
    render(<LoginPage />);
    fireEvent.change(screen.getByLabelText('Username'), { target: { value: 'bad' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'wrong' } });
    fireEvent.click(screen.getByRole('button', { name: 'Login' }));

    await waitFor(() => expect(mockLogin).toHaveBeenCalled());
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  it('button shows Loading... and is disabled when loading', () => {
    mockUseAuth.mockReturnValue({
      login: mockLogin,
      loading: true,
      error: null,
    });
    render(<LoginPage />);
    expect(screen.getByRole('button', { name: 'Loading...' })).toBeDisabled();
  });

  it('shows demo credentials section', () => {
    render(<LoginPage />);
    expect(screen.getByText(/Demo credentials/i)).toBeInTheDocument();
    expect(screen.getByText('admin')).toBeInTheDocument();
  });
});
