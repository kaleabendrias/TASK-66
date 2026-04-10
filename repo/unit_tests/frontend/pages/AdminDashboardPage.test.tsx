import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

vi.mock('@/state/authStore', () => ({
  useAuthStore: vi.fn((selector: any) => {
    const state = { user: { id: 1, username: 'admin', role: 'ADMINISTRATOR', displayName: 'Admin', email: 'a@t.c', enabled: true }, isAuthenticated: true, token: 'tok', loading: false, error: null, login: vi.fn(), register: vi.fn(), logout: vi.fn(), loadFromStorage: vi.fn() };
    return typeof selector === 'function' ? selector(state) : state;
  }),
}));
vi.mock('@/api/users', () => ({
  getUsers: vi.fn(() => Promise.resolve([
    { id: 1, username: 'admin', role: 'ADMINISTRATOR', displayName: 'Admin', email: 'a@t.c', enabled: true },
    { id: 2, username: 'member', role: 'MEMBER', displayName: 'M', email: 'm@t.c', enabled: true },
  ])),
}));
vi.mock('@/api/risk', () => ({
  getHighRiskUsers: vi.fn(() => Promise.resolve([])),
  computeRiskScore: vi.fn(() => Promise.resolve({})),
  getRiskEvents: vi.fn(() => Promise.resolve([])),
}));
vi.mock('@/api/audit', () => ({
  getAuditLog: vi.fn(() => Promise.resolve([])),
}));
vi.mock('@/api/accountDeletion', () => ({
  getDeletionStatus: vi.fn(() => Promise.resolve(null)),
}));

import AdminDashboardPage from '@/pages/AdminDashboardPage';

describe('AdminDashboardPage', () => {
  it('renders tabs', async () => {
    render(<MemoryRouter><AdminDashboardPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/Overview/i).length).toBeGreaterThan(0), { timeout: 3000 });
    expect(screen.getAllByText(/Risk Analytics/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/Audit Log/i).length).toBeGreaterThan(0);
  });

  it('shows user count', async () => {
    render(<MemoryRouter><AdminDashboardPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText('2').length).toBeGreaterThan(0), { timeout: 3000 });
    expect(screen.getAllByText(/Total Users/i).length).toBeGreaterThan(0);
  });

  it('shows role breakdown', async () => {
    render(<MemoryRouter><AdminDashboardPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/Users by Role/i).length).toBeGreaterThan(0), { timeout: 3000 });
    expect(screen.getAllByText(/MEMBER/i).length).toBeGreaterThan(0);
  });

  it('switches to Risk tab', async () => {
    render(<MemoryRouter><AdminDashboardPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/Risk Analytics/i).length).toBeGreaterThan(0), { timeout: 3000 });
    screen.getAllByText(/Risk Analytics/i)[0].click();
    await waitFor(() => expect(screen.getAllByText(/High Risk/i).length).toBeGreaterThan(0), { timeout: 3000 });
  });

  it('switches to Audit tab', async () => {
    render(<MemoryRouter><AdminDashboardPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/Audit Log/i).length).toBeGreaterThan(0), { timeout: 3000 });
    screen.getAllByText(/Audit Log/i)[0].click();
    await waitFor(() => expect(screen.getAllByText(/Search Audit Log/i).length).toBeGreaterThan(0), { timeout: 3000 });
  });
});
