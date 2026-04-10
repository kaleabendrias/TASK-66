import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useParams: () => ({ id: '1' }) };
});
vi.mock('@/state/authStore', () => ({
  useAuthStore: vi.fn((selector: any) => {
    const state = { user: { id: 3, username: 'mod', role: 'MODERATOR', displayName: 'Mod', email: 'mod@t.c', enabled: true }, isAuthenticated: true, token: 'tok', loading: false, error: null, login: vi.fn(), register: vi.fn(), logout: vi.fn(), loadFromStorage: vi.fn() };
    return typeof selector === 'function' ? selector(state) : state;
  }),
}));
vi.mock('@/api/incidents', () => ({
  getIncident: vi.fn(() => Promise.resolve({
    id: 1, reporterId: 2, assigneeId: null, incidentType: 'ORDER_ISSUE', severity: 'HIGH',
    title: 'Missing Package', description: 'Package never arrived', status: 'OPEN',
    slaAckDeadline: '2099-12-31T23:59:59', slaResolveDeadline: '2099-12-31T23:59:59',
    escalationLevel: 0, createdAt: '2026-04-10T10:00:00', acknowledgedAt: null, resolvedAt: null,
  })),
  getIncidentComments: vi.fn(() => Promise.resolve([
    { id: 1, incidentId: 1, authorId: 3, content: 'Looking into this', createdAt: '2026-04-10T11:00:00' },
  ])),
  addIncidentComment: vi.fn(() => Promise.resolve({})),
  acknowledgeIncident: vi.fn(() => Promise.resolve({})),
  updateIncidentStatus: vi.fn(() => Promise.resolve({})),
}));

import IncidentDetailPage from '@/pages/IncidentDetailPage';

describe('IncidentDetailPage', () => {
  it('renders incident title', async () => {
    render(<MemoryRouter><IncidentDetailPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/Missing Package/i).length).toBeGreaterThan(0), { timeout: 3000 });
  });

  it('shows timeline', async () => {
    render(<MemoryRouter><IncidentDetailPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/Timeline/i).length).toBeGreaterThan(0), { timeout: 3000 });
    expect(screen.getAllByText(/Created/i).length).toBeGreaterThan(0);
  });

  it('shows action buttons for moderator', async () => {
    render(<MemoryRouter><IncidentDetailPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/Acknowledge/i).length).toBeGreaterThan(0), { timeout: 3000 });
  });

  it('shows comment form', async () => {
    render(<MemoryRouter><IncidentDetailPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/Post Comment/i).length).toBeGreaterThan(0), { timeout: 3000 });
    expect(screen.getAllByText(/Looking into this/i).length).toBeGreaterThan(0);
  });
});
