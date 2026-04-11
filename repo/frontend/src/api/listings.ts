import client from './client';
import { Listing } from './types';

export async function getListings(): Promise<Listing[]> {
  const { data } = await client.get<Listing[]>('/listings');
  return data;
}

export async function getListingBySlug(slug: string): Promise<Listing> {
  const { data } = await client.get<Listing>(`/listings/slug/${slug}`);
  return data;
}

// Canonical SortMode enum — kept in lock-step with
// ListingService.SortMode on the backend. Passing an unknown value
// returns a 400 from the server, which is the correct failure mode
// (we never want to silently fall back to a client-local rank).
export type SortMode =
  | 'RELEVANCE'
  | 'PRICE_ASC'
  | 'PRICE_DESC'
  | 'SQFT_ASC'
  | 'SQFT_DESC'
  | 'AVAILABLE_FROM_ASC'
  | 'AVAILABLE_FROM_DESC'
  | 'DISTANCE'
  | 'WEEKLY_VIEWS_DESC';

export interface ListingSearchParams {
  q?: string;
  neighborhood?: string;
  lat?: number;
  lng?: number;
  radiusMiles?: number;
  availableAfter?: string;
  availableBefore?: string;
  minPrice?: number;
  maxPrice?: number;
  minSqft?: number;
  maxSqft?: number;
  layout?: string;
  // Tags and sort are first-class server parameters. The frontend
  // MUST NOT sort or tag-filter client-side — that would cause the
  // rendered list to drift away from the canonical backend rank and
  // page boundaries (the discovery tests explicitly verify the
  // server's order is preserved).
  tags?: string[];
  sort?: SortMode;
}

export async function searchListings(params: ListingSearchParams = {}): Promise<Listing[]> {
  // Axios serializes `tags: ['a','b']` as `tags=a&tags=b`, which matches
  // the Spring @RequestParam List<String> binding in ListingController.
  const { data } = await client.get<Listing[]>('/listings/search', {
    params,
    paramsSerializer: { indexes: null },
  });
  return data;
}

export async function createListing(listingData: Partial<Listing>): Promise<Listing> {
  const { data } = await client.post<Listing>('/listings', listingData);
  return data;
}

export async function updateListing(id: number, listingData: Partial<Listing>): Promise<Listing> {
  const { data } = await client.put<Listing>(`/listings/${id}`, listingData);
  return data;
}

export async function publishListing(id: number): Promise<Listing> {
  const { data } = await client.post<Listing>(`/listings/${id}/publish`);
  return data;
}

export async function archiveListing(id: number): Promise<Listing> {
  const { data } = await client.post<Listing>(`/listings/${id}/archive`);
  return data;
}
