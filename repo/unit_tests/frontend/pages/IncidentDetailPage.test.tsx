import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
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

const mockIncident: any = {
  id: 1, reporterId: 2, assigneeId: 3, sellerId: null, incidentType: 'ORDER_ISSUE', severity: 'HIGH',
  title: 'Missing Package', description: 'Package never arrived', status: 'IN_PROGRESS',
  slaAckDeadline: '2099-12-31T23:59:59', slaResolveDeadline: '2099-12-31T23:59:59',
  escalationLevel: 0, createdAt: '2026-04-10T10:00:00', acknowledgedAt: '2026-04-10T10:15:00',
  resolvedAt: null, closureCode: null, address: null, crossStreet: null, status_: 'IN_PROGRESS',
};

const mockResolveIncident = vi.fn();
const mockUpdateIncidentStatus = vi.fn();
const mockGetIncident = vi.fn();

vi.mock('@/api/incidents', () => ({
  getIncident: (...args: any[]) => mockGetIncident(...args),
  getIncidentComments: vi.fn(() => Promise.resolve([
    { id: 1, incidentId: 1, authorId: 3, content: 'Looking into this', createdAt: '2026-04-10T11:00:00' },
  ])),
  addIncidentComment: vi.fn(() => Promise.resolve({})),
  acknowledgeIncident: vi.fn(() => Promise.resolve({})),
  updateIncidentStatus: (...args: any[]) => mockUpdateIncidentStatus(...args),
  resolveIncident: (...args: any[]) => mockResolveIncident(...args),
}));

import IncidentDetailPage from '@/pages/IncidentDetailPage';

describe('IncidentDetailPage', () => {
  beforeEach(() => {
    mockResolveIncident.mockReset();
    mockUpdateIncidentStatus.mockReset();
    mockGetIncident.mockReset();
    mockGetIncident.mockResolvedValue({ ...mockIncident });
  });

  it('renders incident title', async () => {
    render(<MemoryRouter><IncidentDetailPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/Missing Package/i).length).toBeGreaterThan(0), { timeout: 3000 });
  });

  it('shows timeline', async () => {
    render(<MemoryRouter><IncidentDetailPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/Timeline/i).length).toBeGreaterThan(0), { timeout: 3000 });
    expect(screen.getAllByText(/Created/i).length).toBeGreaterThan(0);
  });

  it('shows resolve action button for moderator on IN_PROGRESS incident', async () => {
    render(<MemoryRouter><IncidentDetailPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByRole('button', { name: /resolve incident/i })).toBeTruthy(), { timeout: 3000 });
  });

  it('shows comment form', async () => {
    render(<MemoryRouter><IncidentDetailPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/Post Comment/i).length).toBeGreaterThan(0), { timeout: 3000 });
    expect(screen.getAllByText(/Looking into this/i).length).toBeGreaterThan(0);
  });

  it('opens the closure code dialog when moderator clicks Resolve', async () => {
    render(<MemoryRouter><IncidentDetailPage /></MemoryRouter>);
    const resolveButton = await waitFor(() => screen.getByRole('button', { name: /resolve incident/i }));
    fireEvent.click(resolveButton);
    await waitFor(() => expect(screen.getByRole('dialog', { name: /resolve incident/i })).toBeTruthy());
    expect(screen.getByLabelText(/closure code/i)).toBeTruthy();
  });

  it('calls resolveIncident with the selected closure code on confirm', async () => {
    mockResolveIncident.mockResolvedValueOnce({ ...mockIncident, status: 'RESOLVED', closureCode: 'REFUNDED' });
    render(<MemoryRouter><IncidentDetailPage /></MemoryRouter>);
    fireEvent.click(await waitFor(() => screen.getByRole('button', { name: /resolve incident/i })));

    const select = await waitFor(() => screen.getByLabelText(/closure code/i)) as HTMLSelectElement;
    fireEvent.change(select, { target: { value: 'REFUNDED' } });
    fireEvent.click(screen.getByRole('button', { name: /confirm resolve/i }));

    await waitFor(() => expect(mockResolveIncident).toHaveBeenCalledWith(1, 'REFUNDED'));
    // Plain status update path must not be used for resolution — it would
    // drop the closureCode and hit a 400 from the backend.
    expect(mockUpdateIncidentStatus).not.toHaveBeenCalledWith(1, 'RESOLVED');
  });

  it('surfaces an error message if resolveIncident fails', async () => {
    mockResolveIncident.mockRejectedValueOnce(new Error('Closure code is required to resolve an incident'));
    render(<MemoryRouter><IncidentDetailPage /></MemoryRouter>);
    fireEvent.click(await waitFor(() => screen.getByRole('button', { name: /resolve incident/i })));
    fireEvent.click(await waitFor(() => screen.getByRole('button', { name: /confirm resolve/i })));
    await waitFor(() => expect(screen.getByRole('alert').textContent).toMatch(/closure code/i));
  });
});
