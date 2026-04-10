import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

vi.mock('@/state/authStore', () => ({
  useAuthStore: vi.fn((selector) => {
    const state = { user: { id: 2, username: 'member', role: 'MEMBER', displayName: 'Member', email: 'm@t.c', enabled: true }, isAuthenticated: true, token: 'tok', loading: false, error: null, login: vi.fn(), register: vi.fn(), logout: vi.fn(), loadFromStorage: vi.fn() };
    return selector(state);
  }),
}));
vi.mock('@/api/incidents', () => ({
  getIncidents: vi.fn().mockResolvedValue([]),
  getMyIncidents: vi.fn().mockResolvedValue([
    { id: 1, reporterId: 2, assigneeId: null, incidentType: 'ORDER_ISSUE', severity: 'HIGH', title: 'Wrong item', description: 'Test', status: 'OPEN', slaAckDeadline: '2026-04-10T12:00:00', slaResolveDeadline: '2026-04-11T12:00:00', escalationLevel: 0, createdAt: '2026-04-10T11:00:00', acknowledgedAt: null, resolvedAt: null, address: '123 Main St', crossStreet: '5th Ave' },
  ]),
  createIncident: vi.fn().mockResolvedValue({ id: 2 }),
}));

import IncidentsPage from '@/pages/IncidentsPage';

describe('IncidentsPage', () => {
  beforeEach(() => vi.clearAllMocks());

  it('renders incident list', async () => {
    render(<MemoryRouter><IncidentsPage /></MemoryRouter>);
    expect(await screen.findByText('Wrong item')).toBeInTheDocument();
  });

  it('shows severity badge', async () => {
    render(<MemoryRouter><IncidentsPage /></MemoryRouter>);
    expect(await screen.findByText('HIGH')).toBeInTheDocument();
  });

  it('shows report incident button', async () => {
    render(<MemoryRouter><IncidentsPage /></MemoryRouter>);
    expect(await screen.findByText('Report Incident')).toBeInTheDocument();
  });

  it('has address and cross-street fields in create form', async () => {
    render(<MemoryRouter><IncidentsPage /></MemoryRouter>);
    const btn = await screen.findByText('Report Incident');
    btn.click();
    expect(await screen.findByPlaceholderText('e.g. 123 Main St')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('e.g. 5th Ave')).toBeInTheDocument();
  });
});
