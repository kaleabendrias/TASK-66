import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('@/api/client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

import client from '@/api/client';
import {
  getListings,
  getListingBySlug,
  searchListings,
  createListing,
  updateListing,
  publishListing,
  archiveListing,
} from '@/api/listings';

describe('listings API', () => {
  beforeEach(() => vi.clearAllMocks());

  it('getListings calls GET /listings', async () => {
    (client.get as any).mockResolvedValue({ data: [{ id: 1 }] });
    const result = await getListings();
    expect(client.get).toHaveBeenCalledWith('/listings');
    expect(result).toEqual([{ id: 1 }]);
  });

  it('getListingBySlug calls GET /listings/slug/:slug', async () => {
    (client.get as any).mockResolvedValue({ data: { id: 1, slug: 'test' } });
    const result = await getListingBySlug('test');
    expect(client.get).toHaveBeenCalledWith('/listings/slug/test');
    expect(result.slug).toBe('test');
  });

  it('searchListings calls GET with query', async () => {
    (client.get as any).mockResolvedValue({ data: [] });
    await searchListings('widget');
    expect(client.get).toHaveBeenCalledWith('/listings/search?q=widget');
  });

  it('createListing calls POST /listings', async () => {
    const payload = { title: 'New' };
    (client.post as any).mockResolvedValue({ data: { id: 1, ...payload } });
    const result = await createListing(payload);
    expect(client.post).toHaveBeenCalledWith('/listings', payload);
    expect(result.title).toBe('New');
  });

  it('updateListing calls PUT /listings/:id', async () => {
    (client.put as any).mockResolvedValue({ data: { id: 1, title: 'Updated' } });
    const result = await updateListing(1, { title: 'Updated' });
    expect(client.put).toHaveBeenCalledWith('/listings/1', { title: 'Updated' });
    expect(result.title).toBe('Updated');
  });

  it('publishListing calls POST /listings/:id/publish', async () => {
    (client.post as any).mockResolvedValue({ data: { id: 1, status: 'PUBLISHED' } });
    const result = await publishListing(1);
    expect(client.post).toHaveBeenCalledWith('/listings/1/publish');
    expect(result.status).toBe('PUBLISHED');
  });

  it('archiveListing calls POST /listings/:id/archive', async () => {
    (client.post as any).mockResolvedValue({ data: { id: 1, status: 'ARCHIVED' } });
    const result = await archiveListing(1);
    expect(client.post).toHaveBeenCalledWith('/listings/1/archive');
    expect(result.status).toBe('ARCHIVED');
  });
});
