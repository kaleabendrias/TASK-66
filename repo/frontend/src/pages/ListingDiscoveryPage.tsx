import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { getListings, searchListings, ListingSearchParams } from '@/api/listings';
import { getCategories } from '@/api/categories';
import { Listing, Category } from '@/api/types';

const RECENT_SEARCHES_KEY = 'recentSearches';
const MAX_RECENT = 20;

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

  const handleSearch = async (query?: string) => {
    const q = query ?? searchInput;
    setSearchQuery(q);
    if (q.trim()) addRecentSearch(q.trim());
    setLoading(true);
    try {
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
      const hasParams = Object.keys(params).length > 0;
      const data = hasParams ? await searchListings(params) : await getListings();
      setListings(data);
    } catch {
      setError('Search failed');
    } finally {
      setLoading(false);
    }
  };

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    handleSearch();
  };

  const handleRecentClick = (query: string) => {
    setSearchInput(query);
    handleSearch(query);
  };

  const filteredAndSorted = useMemo(() => {
    let result = [...listings];

    if (filters.publishedOnly) {
      result = result.filter((l) => l.status === 'PUBLISHED');
    }
    if (filters.minPrice) {
      const min = parseFloat(filters.minPrice);
      if (!isNaN(min)) result = result.filter((l) => l.price != null && l.price >= min);
    }
    if (filters.maxPrice) {
      const max = parseFloat(filters.maxPrice);
      if (!isNaN(max)) result = result.filter((l) => l.price != null && l.price <= max);
    }
    if (filters.category) {
      // Category filter uses the listing tags or productId as proxy
      result = result.filter((l) =>
        l.tags.some((t) => t.toLowerCase() === filters.category.toLowerCase())
      );
    }
    if (filters.tag) {
      const tagFilter = filters.tag.toLowerCase();
      result = result.filter((l) =>
        l.tags.some((t) => t.toLowerCase().includes(tagFilter))
      );
    }

    switch (sortBy) {
      case 'price_asc':
        result.sort((a, b) => (a.price ?? 0) - (b.price ?? 0));
        break;
      case 'price_desc':
        result.sort((a, b) => (b.price ?? 0) - (a.price ?? 0));
        break;
      case 'distance':
        // Distance sort handled by server when lat/lng provided; no client re-sort needed
        break;
      case 'popular':
        result.sort((a, b) => b.viewCount - a.viewCount);
        break;
      case 'newest':
      default:
        result.sort((a, b) => {
          const da = a.publishedAt ? new Date(a.publishedAt).getTime() : 0;
          const db = b.publishedAt ? new Date(b.publishedAt).getTime() : 0;
          return db - da;
        });
        break;
    }

    return result;
  }, [listings, filters, sortBy]);

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
                  <select className="form-input" value={`${filters.addressLat},${filters.addressLng}`}
                    onChange={(e) => {
                      const [lat, lng] = e.target.value.split(',');
                      setFilters({ ...filters, addressLat: lat || '', addressLng: lng || '' });
                    }}>
                    <option value=",">Select a location...</option>
                    <option value="40.7128,-74.0060">Arts District (40.71, -74.01)</option>
                    <option value="40.7549,-73.9840">Craft Quarter (40.75, -73.98)</option>
                    <option value="40.7870,-73.9754">Old Town (40.79, -73.98)</option>
                    <option value="40.7233,-73.9985">Foundry Row (40.72, -74.00)</option>
                    <option value="40.7465,-74.0014">Chelsea (40.75, -74.00)</option>
                    <option value="40.7265,-73.9815">East Village (40.73, -73.98)</option>
                  </select>
                  <input type="text" className="form-input mt-sm" value={filters.addressLat && filters.addressLng ? `${filters.addressLat}, ${filters.addressLng}` : ''}
                    placeholder="Or enter: lat, lng (e.g. 40.71, -74.00)" readOnly style={{ fontSize: '0.8rem', color: 'var(--neutral-500)' }} />
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
                      {listing.viewCount} views
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
