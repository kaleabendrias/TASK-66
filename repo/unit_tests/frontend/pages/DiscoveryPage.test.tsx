import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const mockListings = [
  { id: 1, title: 'Raku Tea Bowl', slug: 'raku-tea-bowl', summary: 'Hand-thrown', tags: ['pottery'], featured: true, viewCount: 100, weeklyViews: 25, searchRank: 8, price: 85, sqft: 200, layout: 'studio', status: 'PUBLISHED', publishedAt: '2026-01-01', neighborhood: 'Arts District', latitude: 40.71, longitude: -74.0 },
  { id: 2, title: 'Shibori Tapestry', slug: 'shibori', summary: 'Indigo dyed', tags: ['textiles'], featured: false, viewCount: 50, weeklyViews: 10, searchRank: 6, price: 220, sqft: 350, layout: 'workshop', status: 'PUBLISHED', publishedAt: '2026-02-01', neighborhood: 'Craft Quarter', latitude: 40.75, longitude: -73.98 },
];

vi.mock('@/api/listings', () => ({
  getListings: vi.fn(() => Promise.resolve(mockListings)),
  searchListings: vi.fn(() => Promise.resolve([])),
}));
vi.mock('@/api/categories', () => ({
  getCategories: vi.fn(() => Promise.resolve([{ id: 1, name: 'Pottery', description: '' }])),
}));

import ListingDiscoveryPage from '@/pages/ListingDiscoveryPage';

describe('ListingDiscoveryPage', () => {
  beforeEach(() => { localStorage.clear(); });

  it('renders search bar and filter controls', async () => {
    render(<MemoryRouter><ListingDiscoveryPage /></MemoryRouter>);
    expect(await screen.findByPlaceholderText('Search listings...')).toBeInTheDocument();
  });

  it('displays listing cards with real prices', async () => {
    render(<MemoryRouter><ListingDiscoveryPage /></MemoryRouter>);
    await waitFor(() => {
      expect(screen.getAllByText('Raku Tea Bowl').length).toBeGreaterThan(0);
    }, { timeout: 5000 });
    expect(screen.getByText('$85.00')).toBeInTheDocument();
  });

  it('shows neighborhood in listing cards', async () => {
    render(<MemoryRouter><ListingDiscoveryPage /></MemoryRouter>);
    expect(await screen.findByText(/Arts District/)).toBeInTheDocument();
  });

  it('renders trending section using weeklyViews', async () => {
    render(<MemoryRouter><ListingDiscoveryPage /></MemoryRouter>);
    expect(await screen.findByText('Trending This Week')).toBeInTheDocument();
  });

  it('has availability date filter inputs', async () => {
    render(<MemoryRouter><ListingDiscoveryPage /></MemoryRouter>);
    expect(await screen.findByText('Available After')).toBeInTheDocument();
    expect(screen.getByText('Available Before')).toBeInTheDocument();
  });

  it('has address-based search inputs for lat/lng', async () => {
    render(<MemoryRouter><ListingDiscoveryPage /></MemoryRouter>);
    expect(await screen.findByPlaceholderText('40.71')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('-74.00')).toBeInTheDocument();
  });

  it('has radius filter input', async () => {
    render(<MemoryRouter><ListingDiscoveryPage /></MemoryRouter>);
    expect(await screen.findByPlaceholderText('5')).toBeInTheDocument();
  });

  it('stores recent searches in localStorage', async () => {
    render(<MemoryRouter><ListingDiscoveryPage /></MemoryRouter>);
    const input = await screen.findByPlaceholderText('Search listings...');
    fireEvent.change(input, { target: { value: 'pottery' } });
    fireEvent.submit(input.closest('form')!);
    const stored = JSON.parse(localStorage.getItem('recentSearches') || '[]');
    expect(stored).toContain('pottery');
  });
});
