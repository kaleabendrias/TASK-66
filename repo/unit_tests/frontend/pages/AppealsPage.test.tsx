import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

vi.mock('@/state/authStore', () => ({
  useAuthStore: vi.fn((selector: any) => {
    const state = { user: { id: 2, username: 'member', role: 'MEMBER', displayName: 'M', email: '', enabled: true }, isAuthenticated: true, token: 'tok', loading: false, error: null, login: vi.fn(), register: vi.fn(), logout: vi.fn(), loadFromStorage: vi.fn() };
    return typeof selector === 'function' ? selector(state) : state;
  }),
}));
vi.mock('@/api/appeals', () => ({
  getAppeals: vi.fn(() => Promise.resolve([])),
  getMyAppeals: vi.fn(() => Promise.resolve([
    { id: 1, userId: 2, relatedEntityType: 'PRODUCT', relatedEntityId: 1, reason: 'Test', status: 'SUBMITTED', reviewerId: null, reviewNotes: null, createdAt: '2026-01-01', reviewedAt: null, resolvedAt: null },
  ])),
  createAppeal: vi.fn(() => Promise.resolve({ id: 2 })),
  reviewAppeal: vi.fn(() => Promise.resolve({})),
}));
vi.mock('@/api/client', () => ({ default: { post: vi.fn(() => Promise.resolve({ data: {} })) } }));

import AppealsPage from '@/pages/AppealsPage';

describe('AppealsPage', () => {
  it('renders appeals heading', async () => {
    render(<MemoryRouter><AppealsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText(/appeals/i)).toBeInTheDocument(), { timeout: 3000 });
  });
  it('shows submit appeal button', async () => {
    render(<MemoryRouter><AppealsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText(/submit appeal/i)).toBeInTheDocument(), { timeout: 3000 });
  });
  it('displays appeal list', async () => {
    render(<MemoryRouter><AppealsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('SUBMITTED')).toBeInTheDocument(), { timeout: 3000 });
  });
});
