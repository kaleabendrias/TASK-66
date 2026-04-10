import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

vi.mock('@/state/authStore', () => ({
  useAuthStore: vi.fn(() => ({
    user: { id: 4, username: 'seller', role: 'SELLER', displayName: 'Seller', email: 's@t.c', enabled: true },
    isAuthenticated: true, token: 'tok', loading: false, error: null,
    login: vi.fn(), register: vi.fn(), logout: vi.fn(), loadFromStorage: vi.fn(),
  })),
}));
vi.mock('@/api/listings', () => ({
  getListings: vi.fn(() => Promise.resolve([
    { id: 1, productId: 1, title: 'Cool Widget', slug: 'cool-widget', summary: 'Best widget', tags: ['new'], status: 'DRAFT', viewCount: 10, searchRank: 3, featured: false, publishedAt: null, createdAt: '2026-04-01T00:00:00' },
  ])),
  createListing: vi.fn(() => Promise.resolve({})),
}));
vi.mock('@/api/products', () => ({
  getProducts: vi.fn(() => Promise.resolve([
    { id: 1, name: 'Widget', price: 10, stockQuantity: 50, categoryId: 1, sellerId: 4, status: 'APPROVED' },
  ])),
}));

import SellerListingsPage from '@/pages/SellerListingsPage';

describe('SellerListingsPage', () => {
  it('renders heading', async () => {
    render(<MemoryRouter><SellerListingsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/My Listings/i).length).toBeGreaterThan(0), { timeout: 3000 });
  });

  it('shows create button', async () => {
    render(<MemoryRouter><SellerListingsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/Create Listing/i).length).toBeGreaterThan(0), { timeout: 3000 });
  });

  it('shows listing table', async () => {
    render(<MemoryRouter><SellerListingsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/Cool Widget/i).length).toBeGreaterThan(0), { timeout: 3000 });
    expect(screen.getAllByText(/DRAFT/i).length).toBeGreaterThan(0);
  });

  it('shows table headers', async () => {
    render(<MemoryRouter><SellerListingsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText(/Title/i).length).toBeGreaterThan(0), { timeout: 3000 });
    expect(screen.getAllByText(/Status/i).length).toBeGreaterThan(0);
  });
});
