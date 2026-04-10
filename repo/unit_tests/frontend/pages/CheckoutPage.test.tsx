import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const mockUser = { id: 1, username: 'member', role: 'MEMBER', displayName: 'Member', email: 'm@t.c', enabled: true };
vi.mock('@/state/authStore', () => ({
  useAuthStore: vi.fn((selector: any) => {
    const state = { user: mockUser, isAuthenticated: true, token: 'tok', loading: false, error: null, login: vi.fn(), register: vi.fn(), logout: vi.fn(), loadFromStorage: vi.fn() };
    return typeof selector === 'function' ? selector(state) : state;
  }),
}));
vi.mock('@/api/members', () => ({
  getMyProfile: vi.fn(() => Promise.resolve({ id: 1, userId: 1, tierId: 1, tierName: 'Bronze', totalSpend: 200, phoneMasked: null, joinedAt: '2026-01-01' })),
  getPackagesByTier: vi.fn(() => Promise.resolve([{ id: 1, tierId: 1, name: 'Bronze Basics', description: '', active: true }])),
  getItemsByPackage: vi.fn(() => Promise.resolve([
    { id: 1, packageId: 1, benefitType: 'DISCOUNT', benefitValue: '5', scope: 'ORDER', exclusionGroup: 'DISCOUNT_GROUP', categoryId: null, sellerId: null, validFrom: null, validTo: null },
    { id: 2, packageId: 1, benefitType: 'FREE_SHIPPING', benefitValue: 'true', scope: 'ORDER', exclusionGroup: null, categoryId: null, sellerId: null, validFrom: null, validTo: null },
  ])),
}));

import CheckoutPage from '@/pages/CheckoutPage';

describe('CheckoutPage', () => {
  beforeEach(() => {});

  it('renders tier name and benefits', async () => {
    render(<MemoryRouter><CheckoutPage /></MemoryRouter>);
    await waitFor(() => {
      expect(screen.getAllByText(/Bronze/).length).toBeGreaterThan(0);
    }, { timeout: 5000 });
    expect(screen.getByText(/5% Discount/)).toBeInTheDocument();
  });

  it('shows free shipping benefit', async () => {
    render(<MemoryRouter><CheckoutPage /></MemoryRouter>);
    expect(await screen.findByText('Free Shipping')).toBeInTheDocument();
  });

  it('displays non-stackability warning', async () => {
    render(<MemoryRouter><CheckoutPage /></MemoryRouter>);
    expect(await screen.findByText(/Non-Stackable Benefits/)).toBeInTheDocument();
  });

  it('shows scope badge for ORDER-scoped benefits', async () => {
    render(<MemoryRouter><CheckoutPage /></MemoryRouter>);
    expect(await screen.findByText('Order scope')).toBeInTheDocument();
  });
});
