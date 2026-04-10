import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

vi.mock('@/state/authStore', () => ({
  useAuthStore: vi.fn((selector: any) => {
    const state = { user: { id: 2, username: 'member', role: 'MEMBER', displayName: 'Member', email: 'm@t.c', enabled: true }, isAuthenticated: true, token: 'tok', loading: false, error: null, login: vi.fn(), register: vi.fn(), logout: vi.fn(), loadFromStorage: vi.fn() };
    return typeof selector === 'function' ? selector(state) : state;
  }),
}));
vi.mock('@/api/members', () => ({
  getMyProfile: vi.fn(() => Promise.resolve({ id: 1, userId: 2, tierId: 1, tierName: 'Gold', totalSpend: 500, phoneMasked: '***1234' })),
  getTiers: vi.fn(() => Promise.resolve([
    { id: 1, name: 'Gold', rank: 2, minSpend: 200 },
    { id: 2, name: 'Platinum', rank: 3, minSpend: 1000 },
  ])),
  getPackagesByTier: vi.fn(() => Promise.resolve([{ id: 1, name: 'Gold Benefits', description: 'Exclusive perks', tierId: 1, active: true }])),
  getItemsByPackage: vi.fn(() => Promise.resolve([{ id: 1, packageId: 1, benefitType: 'DISCOUNT', benefitValue: '10%' }])),
  redeemBenefit: vi.fn(() => Promise.resolve({})),
  getSpendHistory: vi.fn(() => Promise.resolve([])),
  updatePhone: vi.fn(() => Promise.resolve({})),
}));

import MemberProfilePage from '@/pages/MemberProfilePage';

describe('MemberProfilePage', () => {
  it('renders tier card', async () => {
    render(<MemoryRouter><MemberProfilePage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/Gold Member/i).length).toBeGreaterThan(0), { timeout: 3000 });
  });

  it('shows spend amount', async () => {
    render(<MemoryRouter><MemberProfilePage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/\$500/i).length).toBeGreaterThan(0), { timeout: 3000 });
  });

  it('shows benefits section', async () => {
    render(<MemoryRouter><MemberProfilePage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/Benefits/i).length).toBeGreaterThan(0), { timeout: 3000 });
    expect(screen.getAllByText(/DISCOUNT/i).length).toBeGreaterThan(0);
  });

  it('shows spend history tab', async () => {
    render(<MemoryRouter><MemberProfilePage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/Spend History/i).length).toBeGreaterThan(0), { timeout: 3000 });
  });
});
