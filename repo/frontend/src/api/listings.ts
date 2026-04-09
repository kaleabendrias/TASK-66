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
}

export async function searchListings(params: ListingSearchParams = {}): Promise<Listing[]> {
  const { data } = await client.get<Listing[]>('/listings/search', { params });
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
