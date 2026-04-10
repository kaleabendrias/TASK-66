import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useParams: () => ({ slug: 'test' }) };
});
vi.mock('@/state/authStore', () => ({
  useAuthStore: vi.fn(() => ({
    user: { id: 2, username: 'member', role: 'MEMBER', displayName: 'Member', email: 'm@t.c', enabled: true },
    isAuthenticated: true, token: 'tok', loading: false, error: null,
    login: vi.fn(), register: vi.fn(), logout: vi.fn(), loadFromStorage: vi.fn(),
  })),
}));
vi.mock('@/api/listings', () => ({
  getListingBySlug: vi.fn(() => Promise.resolve({
    id: 1, productId: 1, title: 'Test Listing', slug: 'test', summary: 'A great listing',
    tags: ['sale'], status: 'PUBLISHED', viewCount: 42, searchRank: 5, featured: false,
    publishedAt: '2026-04-01T00:00:00', createdAt: '2026-03-01T00:00:00',
  })),
  publishListing: vi.fn(() => Promise.resolve({})),
  archiveListing: vi.fn(() => Promise.resolve({})),
}));
vi.mock('@/api/members', () => ({
  getMyProfile: vi.fn(() => Promise.resolve({ id: 1, userId: 2, tierId: 1, tierName: 'Gold', totalSpend: 500, phoneMasked: '***1234' })),
  getPackagesByTier: vi.fn(() => Promise.resolve([])),
  getItemsByPackage: vi.fn(() => Promise.resolve([])),
}));

import ListingDetailPage from '@/pages/ListingDetailPage';

describe('ListingDetailPage', () => {
  it('renders listing title', async () => {
    render(<MemoryRouter><ListingDetailPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/Test Listing/i).length).toBeGreaterThan(0), { timeout: 3000 });
  });

  it('shows reserve button for members', async () => {
    render(<MemoryRouter><ListingDetailPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/Reserve Stock/i).length).toBeGreaterThan(0), { timeout: 3000 });
  });

  it('shows listing details', async () => {
    render(<MemoryRouter><ListingDetailPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/A great listing/i).length).toBeGreaterThan(0), { timeout: 3000 });
    expect(screen.getAllByText(/sale/i).length).toBeGreaterThan(0);
  });
});
