import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

let authState: any = { user: { role: 'MEMBER' }, isAuthenticated: true };

vi.mock('@/state/authStore', () => ({
  useAuthStore: vi.fn((selector: any) => typeof selector === 'function' ? selector(authState) : authState),
}));

import ProtectedRoute from '@/components/layout/ProtectedRoute';

describe('ProtectedRoute', () => {
  it('renders children when authenticated', () => {
    authState = { user: { role: 'MEMBER' }, isAuthenticated: true };
    render(<MemoryRouter><ProtectedRoute><div>Protected Content</div></ProtectedRoute></MemoryRouter>);
    expect(screen.getByText('Protected Content')).toBeInTheDocument();
  });

  it('redirects when not authenticated', () => {
    authState = { user: null, isAuthenticated: false };
    render(<MemoryRouter><ProtectedRoute><div>Secret</div></ProtectedRoute></MemoryRouter>);
    expect(screen.queryByText('Secret')).not.toBeInTheDocument();
  });

  it('blocks access for wrong role', () => {
    authState = { user: { role: 'GUEST' }, isAuthenticated: true };
    render(<MemoryRouter><ProtectedRoute roles={['ADMINISTRATOR']}><div>Admin Only</div></ProtectedRoute></MemoryRouter>);
    expect(screen.queryByText('Admin Only')).not.toBeInTheDocument();
  });
});
