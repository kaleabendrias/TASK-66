import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { getListings, searchListings, ListingSearchParams, SortMode } from '@/api/listings';
import { getCategories } from '@/api/categories';
import { Listing, Category } from '@/api/types';

const RECENT_SEARCHES_KEY = 'recentSearches';
const MAX_RECENT = 20;

// UI sort key -> canonical server SortMode. Kept as an explicit table
// so the dropdown can never drift out of sync with the backend enum.
const SORT_MODE_MAP: Record<string, SortMode> = {
  newest: 'AVAILABLE_FROM_DESC',
  price_asc: 'PRICE_ASC',
  price_desc: 'PRICE_DESC',
  popular: 'WEEKLY_VIEWS_DESC',
  distance: 'DISTANCE',
  relevance: 'RELEVANCE',
};

const ListingDiscoveryPage: React.FC = () => {
  const [listings, setListings] = useState<Listing[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [filtersOpen, setFiltersOpen] = useState(true);
  const [recentSearches, setRecentSearches] = useState<string[]>(() => {
    try {
      const stored = localStorage.getItem(RECENT_SEARCHES_KEY);
      return stored ? JSON.parse(stored) : [];
    } catch {
      return [];
    }
  });
  const [filters, setFilters] = useState({
    minPrice: '',
    maxPrice: '',
    category: '',
    tag: '',
    publishedOnly: true,
    neighborhood: '',
    addressLat: '',
    addressLng: '',
    radiusMiles: '',
    minSqft: '',
    maxSqft: '',
    layout: '',
    availableAfter: '',
    availableBefore: '',
  });
  const [sortBy, setSortBy] = useState('newest');

  useEffect(() => {
    const load = async () => {
      try {
        const [listingsData, categoriesData] = await Promise.all([
          getListings(),
          getCategories(),
        ]);
        setListings(listingsData);
        setCategories(categoriesData);
      } catch {
        setError('Failed to load listings');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const addRecentSearch = useCallback((query: string) => {
    setRecentSearches((prev) => {
      const filtered = prev.filter((s) => s !== query);
      const updated = [query, ...filtered].slice(0, MAX_RECENT);
      localStorage.setItem(RECENT_SEARCHES_KEY, JSON.stringify(updated));
      return updated;
    });
  }, []);

  const buildSearchParams = useCallback(
    (q: string): ListingSearchParams => {
      const params: ListingSearchParams = {};
      if (q.trim()) params.q = q.trim();
      if (filters.neighborhood) params.neighborhood = filters.neighborhood;
      if (filters.addressLat && filters.addressLng) {
        params.lat = parseFloat(filters.addressLat);
        params.lng = parseFloat(filters.addressLng);
      }
      if (filters.radiusMiles) params.radiusMiles = parseFloat(filters.radiusMiles);
      if (filters.minPrice) params.minPrice = parseFloat(filters.minPrice);
      if (filters.maxPrice) params.maxPrice = parseFloat(filters.maxPrice);
      if (filters.minSqft) params.minSqft = parseInt(filters.minSqft);
      if (filters.maxSqft) params.maxSqft = parseInt(filters.maxSqft);
      if (filters.layout) params.layout = filters.layout;
      if (filters.availableAfter) params.availableAfter = filters.availableAfter;
      if (filters.availableBefore) params.availableBefore = filters.availableBefore;

      // Tags are a server-side filter — `category` picks a canonical tag
      // and the free-text `tag` box adds one more. Both are passed to
      // the backend as `tags=...&tags=...` so the rank/order returned by
      // the server already reflects them.
      const tags: string[] = [];
      if (filters.category) tags.push(filters.category);
      if (filters.tag.trim()) tags.push(filters.tag.trim());
      if (tags.length > 0) params.tags = tags;

      // Sort is also server-authoritative — we always send a SortMode
      // so the server never has to guess, and we never re-sort locally.
      params.sort = SORT_MODE_MAP[sortBy] ?? 'RELEVANCE';

      return params;
    },
    [filters, sortBy]
  );

  const handleSearch = async (query?: string) => {
    const q = query ?? searchInput;
    setSearchQuery(q);
    if (q.trim()) addRecentSearch(q.trim());
    setLoading(true);
    try {
      const params = buildSearchParams(q);
      const data = await searchListings(params);
      setListings(data);
    } catch {
      setError('Search failed');
    } finally {
      setLoading(false);
    }
  };

  // Re-hit the server whenever sort or tags change so the user sees
  // the canonical server-ranked list, not a locally re-shuffled view.
  useEffect(() => {
    if (loading && listings.length === 0) return;
    handleSearch(searchQuery);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sortBy, filters.category, filters.tag]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    handleSearch();
  };

  const handleRecentClick = (query: string) => {
    setSearchInput(query);
    handleSearch(query);
  };

  // The backend already applies tags, sort, and every price/sqft/layout
  // filter (see ListingController.search / ListingService.searchAdvanced).
  // The only thing we still enforce locally is the "published only"
  // toggle, which is a display preference (the server always returns
  // published listings via /listings/search, but the unauthenticated
  // getListings() call at page load may return drafts in some dev
  // fixtures). Everything else — rank, tag filter — is trusted as-is.
  const filteredAndSorted = useMemo(() => {
    if (!filters.publishedOnly) return listings;
    return listings.filter((l) => l.status === 'PUBLISHED');
  }, [listings, filters.publishedOnly]);

  const trending = useMemo(() => {
    return [...listings]
      .filter((l) => l.status === 'PUBLISHED')
      .sort((a, b) => (b.weeklyViews ?? 0) - (a.weeklyViews ?? 0))
      .slice(0, 5);
  }, [listings]);

  if (loading && listings.length === 0) {
    return <div className="page-loading">Loading listings...</div>;
  }

  return (
    <div className="page">
      <div className="page-header">
        <h1>Discover Listings</h1>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}

      <form className="search-bar" onSubmit={handleSearchSubmit}>
        <input
          type="text"
          className="form-input"
          placeholder="Search listings..."
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
        />
        <button type="submit" className="btn btn-primary">Search</button>
      </form>

      <div className="discovery-layout">
        <div className="discovery-main">
          <div className="flex gap-md" style={{ marginBottom: '1rem', flexWrap: 'wrap', justifyContent: 'space-between' }}>
            <button
              className="btn btn-secondary btn-sm"
              onClick={() => setFiltersOpen(!filtersOpen)}
            >
              {filtersOpen ? 'Hide Filters' : 'Show Filters'}
            </button>
            <div className="form-group" style={{ marginBottom: 0 }}>
              <select
                className="form-input"
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value)}
                style={{ width: 'auto' }}
              >
                <option value="newest">Newest</option>
                <option value="price_asc">Price: Low to High</option>
                <option value="price_desc">Price: High to Low</option>
                <option value="popular">Most Popular</option>
                <option value="distance">Distance (nearest)</option>
              </select>
            </div>
          </div>

          {filtersOpen && (
            <div className="filter-panel">
              <div className="filter-panel-inner">
                <div className="form-group">
                  <label className="form-label">Min Price</label>
                  <input
                    type="number"
                    className="form-input"
                    value={filters.minPrice}
                    onChange={(e) => setFilters({ ...filters, minPrice: e.target.value })}
                    placeholder="0"
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Max Price</label>
                  <input
                    type="number"
                    className="form-input"
                    value={filters.maxPrice}
                    onChange={(e) => setFilters({ ...filters, maxPrice: e.target.value })}
                    placeholder="999"
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Category</label>
                  <select
                    className="form-input"
                    value={filters.category}
                    onChange={(e) => setFilters({ ...filters, category: e.target.value })}
                  >
                    <option value="">All Categories</option>
                    {categories.map((c) => (
                      <option key={c.id} value={c.name}>{c.name}</option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label className="form-label">Tag</label>
                  <input type="text" className="form-input" value={filters.tag}
                    onChange={(e) => setFilters({ ...filters, tag: e.target.value })} placeholder="Filter by tag" />
                </div>
                <div className="form-group">
                  <label className="form-label">Neighborhood</label>
                  <input type="text" className="form-input" value={filters.neighborhood}
                    onChange={(e) => setFilters({ ...filters, neighborhood: e.target.value })} placeholder="e.g. Downtown" />
                </div>
                <div className="form-group">
                  <label className="form-label">Your Address (for distance sort)</label>
                  <input type="text" className="form-input" placeholder="e.g. Arts District, 40.71 -74.00, or a neighborhood name"
                    onChange={(e) => {
                      const v = e.target.value.trim();
                      // Local offline parsing: detect "lat, lng" or "lat lng" patterns
                      const coordMatch = v.match(/^(-?\d+\.?\d*)[,\s]+(-?\d+\.?\d*)$/);
                      if (coordMatch) {
                        setFilters({ ...filters, addressLat: coordMatch[1], addressLng: coordMatch[2] });
                        return;
                      }
                      // Local neighborhood lookup table (offline, no external APIs)
                      const neighborhoods: Record<string, [string, string]> = {
                        'arts district': ['40.7128', '-74.0060'],
                        'craft quarter': ['40.7549', '-73.9840'],
                        'old town': ['40.7870', '-73.9754'],
                        'foundry row': ['40.7233', '-73.9985'],
                        'chelsea': ['40.7465', '-74.0014'],
                        'east village': ['40.7265', '-73.9815'],
                        'soho': ['40.7233', '-73.9985'],
                        'midtown': ['40.7549', '-73.9840'],
                        'downtown': ['40.7128', '-74.0060'],
                      };
                      const match = neighborhoods[v.toLowerCase()];
                      if (match) {
                        setFilters({ ...filters, addressLat: match[0], addressLng: match[1] });
                      } else {
                        setFilters({ ...filters, addressLat: '', addressLng: '' });
                      }
                    }} />
                  <span className="text-muted" style={{ fontSize: '0.75rem' }}>
                    {filters.addressLat && filters.addressLng
                      ? `Resolved: ${filters.addressLat}, ${filters.addressLng}`
                      : 'Enter coordinates (40.71, -74.00) or a neighborhood name'}
                  </span>
                </div>
                <div className="form-group">
                  <label className="form-label">Radius (miles)</label>
                  <input type="number" className="form-input" value={filters.radiusMiles}
                    onChange={(e) => setFilters({ ...filters, radiusMiles: e.target.value })} placeholder="5" />
                </div>
                <div className="form-group">
                  <label className="form-label">Min Sqft</label>
                  <input type="number" className="form-input" value={filters.minSqft}
                    onChange={(e) => setFilters({ ...filters, minSqft: e.target.value })} placeholder="0" />
                </div>
                <div className="form-group">
                  <label className="form-label">Max Sqft</label>
                  <input type="number" className="form-input" value={filters.maxSqft}
                    onChange={(e) => setFilters({ ...filters, maxSqft: e.target.value })} placeholder="5000" />
                </div>
                <div className="form-group">
                  <label className="form-label">Layout</label>
                  <select className="form-input" value={filters.layout}
                    onChange={(e) => setFilters({ ...filters, layout: e.target.value })}>
                    <option value="">Any</option>
                    <option value="studio">Studio</option>
                    <option value="workshop">Workshop</option>
                    <option value="gallery">Gallery</option>
                    <option value="storefront">Storefront</option>
                  </select>
                </div>
                <div className="form-group">
                  <label className="form-label">Available After</label>
                  <input type="date" className="form-input" value={filters.availableAfter}
                    onChange={(e) => setFilters({ ...filters, availableAfter: e.target.value })} />
                </div>
                <div className="form-group">
                  <label className="form-label">Available Before</label>
                  <input type="date" className="form-input" value={filters.availableBefore}
                    onChange={(e) => setFilters({ ...filters, availableBefore: e.target.value })} />
                </div>
                <div className="form-group">
                  <label className="flex gap-sm">
                    <input type="checkbox" checked={filters.publishedOnly}
                      onChange={(e) => setFilters({ ...filters, publishedOnly: e.target.checked })} />
                    <span className="form-label" style={{ marginBottom: 0 }}>Published only</span>
                  </label>
                </div>
                <button className="btn btn-primary btn-sm btn-block" type="button" onClick={() => handleSearch()}>Apply Filters</button>
              </div>
            </div>
          )}

          <p className="text-muted mt-sm" style={{ marginBottom: '1rem' }}>
            {filteredAndSorted.length} listing{filteredAndSorted.length !== 1 ? 's' : ''} found
            {searchQuery && <> for &quot;{searchQuery}&quot;</>}
          </p>

          <div className="listing-grid">
            {filteredAndSorted.map((listing) => (
              <Link
                key={listing.id}
                to={`/listings/${listing.slug}`}
                style={{ textDecoration: 'none', color: 'inherit' }}
              >
                <div className="card listing-card">
                  <div className="card-body">
                    <div className="flex gap-sm" style={{ marginBottom: '0.5rem', flexWrap: 'wrap' }}>
                      {listing.featured && <span className="badge badge-warning">Featured</span>}
                      <span className="badge badge-info">{listing.status}</span>
                    </div>
                    <h3>{listing.title}</h3>
                    <p className="text-muted" style={{ fontSize: '0.875rem', marginTop: '0.25rem' }}>
                      {listing.summary}
                    </p>
                    <div className="flex gap-sm mt-sm" style={{ flexWrap: 'wrap' }}>
                      {listing.tags.map((tag) => (
                        <span key={tag} className="badge badge-neutral">{tag}</span>
                      ))}
                    </div>
                    <div className="product-meta mt-sm">
                      <span className="product-price">{listing.price != null ? `$${listing.price.toFixed(2)}` : 'Price N/A'}</span>
                      <span className="text-muted">
                        {listing.sqft ? `${listing.sqft} sqft` : ''}{listing.sqft && listing.neighborhood ? ' · ' : ''}{listing.neighborhood || ''}
                      </span>
                    </div>
                  </div>
                </div>
              </Link>
            ))}
            {filteredAndSorted.length === 0 && (
              <p className="text-muted">No listings match your criteria.</p>
            )}
          </div>
        </div>

        <div className="discovery-sidebar">
          {recentSearches.length > 0 && (
            <div className="card recent-searches">
              <div className="card-header">Recent Searches</div>
              <div className="card-body">
                <div className="chip-list">
                  {recentSearches.map((q) => (
                    <button key={q} className="chip" onClick={() => handleRecentClick(q)}>
                      {q}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          )}

          {trending.length > 0 && (
            <div className="card trending-panel mt-md">
              <div className="card-header">Trending This Week</div>
              <div className="card-body">
                {trending.map((listing) => (
                  <Link
                    key={listing.id}
                    to={`/listings/${listing.slug}`}
                    className="trending-item"
                  >
                    <span>{listing.title}</span>
                    <span className="text-muted" style={{ fontSize: '0.8rem' }}>
                      {listing.weeklyViews ?? 0} this week
                    </span>
                  </Link>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ListingDiscoveryPage;
