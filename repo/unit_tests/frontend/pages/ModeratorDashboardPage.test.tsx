import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

vi.mock('@/state/authStore', () => ({
  useAuthStore: vi.fn((selector: any) => {
    const state = { user: { id: 3, username: 'mod', role: 'MODERATOR', displayName: 'Mod', email: 'mod@t.c', enabled: true }, isAuthenticated: true, token: 'tok', loading: false, error: null, login: vi.fn(), register: vi.fn(), logout: vi.fn(), loadFromStorage: vi.fn() };
    return typeof selector === 'function' ? selector(state) : state;
  }),
}));
vi.mock('@/api/incidents', () => ({
  getIncidents: vi.fn(() => Promise.resolve([
    { id: 1, reporterId: 2, assigneeId: null, incidentType: 'ORDER_ISSUE', severity: 'HIGH', title: 'Test Incident', description: 'Desc', status: 'OPEN', slaAckDeadline: '2099-12-31T23:59:59', slaResolveDeadline: '2099-12-31T23:59:59', escalationLevel: 0, createdAt: '2026-04-10T10:00:00', acknowledgedAt: null, resolvedAt: null, address: '', crossStreet: '' },
  ])),
}));
vi.mock('@/api/appeals', () => ({
  getAppeals: vi.fn(() => Promise.resolve([
    { id: 1, relatedEntityType: 'INCIDENT', relatedEntityId: 1, reason: 'Unfair resolution applied to my case', status: 'SUBMITTED', createdAt: '2026-04-10T10:00:00' },
  ])),
}));

import ModeratorDashboardPage from '@/pages/ModeratorDashboardPage';

describe('ModeratorDashboardPage', () => {
  it('renders heading', async () => {
    render(<MemoryRouter><ModeratorDashboardPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/Moderator Dashboard/i).length).toBeGreaterThan(0), { timeout: 3000 });
  });

  it('shows stats cards', async () => {
    render(<MemoryRouter><ModeratorDashboardPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/Open Incidents/i).length).toBeGreaterThan(0), { timeout: 3000 });
    expect(screen.getAllByText(/SLA Breached/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/Escalated/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/Pending Appeals/i).length).toBeGreaterThan(0);
  });

  it('shows incident list', async () => {
    render(<MemoryRouter><ModeratorDashboardPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/Test Incident/i).length).toBeGreaterThan(0), { timeout: 3000 });
  });

  it('shows pending appeals section', async () => {
    render(<MemoryRouter><ModeratorDashboardPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/SUBMITTED/i).length).toBeGreaterThan(0), { timeout: 3000 });
  });
});
