import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const { mockCreateIncident } = vi.hoisted(() => ({
  mockCreateIncident: vi.fn(),
}));

vi.mock('@/state/authStore', () => ({
  useAuthStore: vi.fn((selector) => {
    const state = {
      user: { id: 2, username: 'member', role: 'MEMBER', displayName: 'Member', email: 'm@t.c', enabled: true },
      isAuthenticated: true,
      token: 'tok',
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

vi.mock('@/api/incidents', () => ({
  getIncidents: vi.fn().mockResolvedValue([]),
  getMyIncidents: vi.fn().mockResolvedValue([
    {
      id: 1,
      reporterId: 2,
      assigneeId: null,
      incidentType: 'ORDER_ISSUE',
      severity: 'HIGH',
      title: 'Wrong item',
      description: 'Test',
      status: 'OPEN',
      slaAckDeadline: new Date(Date.now() + 3600000).toISOString(),
      slaResolveDeadline: '2026-04-11T12:00:00',
      escalationLevel: 0,
      createdAt: '2026-04-10T11:00:00',
      acknowledgedAt: null,
      resolvedAt: null,
      address: '123 Main St',
      crossStreet: '5th Ave',
    },
  ]),
  createIncident: mockCreateIncident,
}));

import IncidentsPage from '@/pages/IncidentsPage';

describe('IncidentsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockCreateIncident.mockResolvedValue({ id: 2 });
  });

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
    fireEvent.click(await screen.findByText('Report Incident'));
    expect(await screen.findByPlaceholderText('e.g. 123 Main St')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('e.g. 5th Ave')).toBeInTheDocument();
  });

  it('exposes a Seller ID field on the create form', async () => {
    render(<MemoryRouter><IncidentsPage /></MemoryRouter>);
    fireEvent.click(await screen.findByText('Report Incident'));
    expect(await screen.findByLabelText(/seller id/i)).toBeInTheDocument();
  });

  it('shows validation error when sellerId is empty on submit', async () => {
    render(<MemoryRouter><IncidentsPage /></MemoryRouter>);
    fireEvent.click(await screen.findByText('Report Incident'));

    await screen.findByText('Submit Incident');

    // Title and description inputs (no label association — use getAllByRole('textbox'))
    const textboxes = screen.getAllByRole('textbox');
    fireEvent.change(textboxes[0], { target: { value: 'Test incident' } });
    fireEvent.change(textboxes[1], { target: { value: 'Description text' } });
    // Leave sellerId empty — use fireEvent.submit to bypass native HTML5 required validation

    const form = document.querySelector('form')!;
    fireEvent.submit(form);

    await waitFor(() => {
      expect(screen.getByText(/Seller ID is required/i)).toBeInTheDocument();
    });
    expect(mockCreateIncident).not.toHaveBeenCalled();
  });

  it('shows validation error when sellerId is zero or negative', async () => {
    render(<MemoryRouter><IncidentsPage /></MemoryRouter>);
    fireEvent.click(await screen.findByText('Report Incident'));

    await screen.findByText('Submit Incident');
    const textboxes = screen.getAllByRole('textbox');
    fireEvent.change(textboxes[0], { target: { value: 'Test incident' } });
    fireEvent.change(textboxes[1], { target: { value: 'Desc' } });
    fireEvent.change(screen.getByLabelText(/seller id/i), { target: { value: '-1' } });

    // Use fireEvent.submit to bypass native HTML5 number min validation
    const form = document.querySelector('form')!;
    fireEvent.submit(form);

    await waitFor(() => {
      expect(screen.getByText(/positive number/i)).toBeInTheDocument();
    });
    expect(mockCreateIncident).not.toHaveBeenCalled();
  });

  it('calls createIncident with correct payload on valid form submit', async () => {
    render(<MemoryRouter><IncidentsPage /></MemoryRouter>);
    fireEvent.click(await screen.findByText('Report Incident'));

    const submitBtn = await screen.findByText('Submit Incident');

    const textboxes = screen.getAllByRole('textbox');
    fireEvent.change(textboxes[0], { target: { value: 'Test incident title' } });
    fireEvent.change(textboxes[1], { target: { value: 'Detailed description' } });
    fireEvent.change(screen.getByLabelText(/seller id/i), { target: { value: '3' } });
    fireEvent.change(screen.getByPlaceholderText('e.g. 123 Main St'), { target: { value: '456 Oak Ave' } });

    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(mockCreateIncident).toHaveBeenCalledWith(
        expect.objectContaining({
          sellerId: 3,
          title: 'Test incident title',
          address: '456 Oak Ave',
        })
      );
    });
  });

  it('shows empty state when no incidents', async () => {
    const { getMyIncidents } = await import('@/api/incidents');
    vi.mocked(getMyIncidents).mockResolvedValueOnce([]);

    render(<MemoryRouter><IncidentsPage /></MemoryRouter>);
    expect(await screen.findByText('No incidents found.')).toBeInTheDocument();
  });

  it('shows loading indicator initially', () => {
    render(<MemoryRouter><IncidentsPage /></MemoryRouter>);
    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  it('hides form when Cancel is clicked', async () => {
    render(<MemoryRouter><IncidentsPage /></MemoryRouter>);
    fireEvent.click(await screen.findByText('Report Incident'));
    expect(await screen.findByText('Submit Incident')).toBeInTheDocument();

    fireEvent.click(screen.getByText('Cancel'));
    await waitFor(() => {
      expect(screen.queryByText('Submit Incident')).not.toBeInTheDocument();
    });
  });

  it('shows SLA countdown badge for open incidents within deadline', async () => {
    render(<MemoryRouter><IncidentsPage /></MemoryRouter>);
    expect(await screen.findByText(/m to ack/i)).toBeInTheDocument();
  });
});

describe('IncidentsPage - MODERATOR view', () => {
  beforeEach(async () => {
    vi.clearAllMocks();
    // Override authStore for all renders in this describe block
    const { useAuthStore: mockStore } = await import('@/state/authStore');
    vi.mocked(mockStore).mockImplementation((selector: any) => {
      const state = {
        user: { id: 5, username: 'moderator', role: 'MODERATOR', displayName: 'Mod', email: '', enabled: true },
        isAuthenticated: true, token: 'mod-tok', loading: false, error: null,
        login: vi.fn(), register: vi.fn(), logout: vi.fn(), loadFromStorage: vi.fn(),
      };
      return typeof selector === 'function' ? selector(state) : state;
    });
  });

  afterEach(async () => {
    // Restore authStore to original MEMBER mock so outer describe tests are unaffected
    const { useAuthStore: mockStore } = await import('@/state/authStore');
    vi.mocked(mockStore).mockImplementation((selector: any) => {
      const state = {
        user: { id: 2, username: 'member', role: 'MEMBER', displayName: 'Member', email: 'm@t.c', enabled: true },
        isAuthenticated: true, token: 'tok', loading: false, error: null,
        login: vi.fn(), register: vi.fn(), logout: vi.fn(), loadFromStorage: vi.fn(),
      };
      return typeof selector === 'function' ? selector(state) : state;
    });
  });

  it('calls getIncidents (all incidents) and shows escalation badge for MODERATOR', async () => {
    const { getIncidents, getMyIncidents } = await import('@/api/incidents');
    vi.mocked(getIncidents).mockResolvedValue([
      {
        id: 10,
        reporterId: 2,
        assigneeId: 5,
        incidentType: 'POLICY_VIOLATION',
        severity: 'HIGH',
        title: 'Policy breach reported',
        description: 'Details',
        status: 'OPEN',
        slaAckDeadline: null,
        slaResolveDeadline: null,
        escalationLevel: 1,
        createdAt: '2026-04-10T11:00:00',
        acknowledgedAt: null,
        resolvedAt: null,
        address: null,
        crossStreet: null,
      },
    ]);

    render(<MemoryRouter><IncidentsPage /></MemoryRouter>);

    await waitFor(() => expect(getIncidents).toHaveBeenCalled());
    expect(getMyIncidents).not.toHaveBeenCalled();
    expect(await screen.findByText('Policy breach reported')).toBeInTheDocument();
    expect(screen.getByText('L1')).toBeInTheDocument();
  });
});
