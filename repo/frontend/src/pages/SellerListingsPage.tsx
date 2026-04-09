import React, { useState, useEffect } from 'react';
import { getListings, createListing } from '@/api/listings';
import { getProducts } from '@/api/products';
import { Listing, Product } from '@/api/types';
import { useAuthStore } from '@/state/authStore';

const slugify = (text: string): string =>
  text.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '');

const statusBadge = (status: string) => {
  switch (status) {
    case 'PUBLISHED': return 'badge-success';
    case 'DRAFT': return 'badge-warning';
    case 'ARCHIVED': return 'badge-neutral';
    default: return 'badge-info';
  }
};

const SellerListingsPage: React.FC = () => {
  const { user } = useAuthStore();
  const [listings, setListings] = useState<Listing[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [formLoading, setFormLoading] = useState(false);
  const [form, setForm] = useState({
    productId: '',
    title: '',
    slug: '',
    summary: '',
    tags: '',
  });

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const [listingsData, productsData] = await Promise.all([
        getListings(),
        getProducts(),
      ]);
      setListings(listingsData);
      // Filter products to the seller's own if SELLER role
      const filtered = user?.role === 'SELLER'
        ? productsData.filter((p) => p.sellerId === user.id)
        : productsData;
      setProducts(filtered);
    } catch {
      setError('Failed to load data');
    } finally {
      setLoading(false);
    }
  };

  const handleTitleChange = (title: string) => {
    setForm({ ...form, title, slug: slugify(title) });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.productId || !form.title) {
      setError('Product and title are required');
      return;
    }
    setFormLoading(true);
    setError('');
    try {
      const tags = form.tags
        .split(',')
        .map((t) => t.trim())
        .filter(Boolean);
      await createListing({
        productId: parseInt(form.productId),
        title: form.title,
        slug: form.slug || slugify(form.title),
        summary: form.summary,
        tags,
      });
      setForm({ productId: '', title: '', slug: '', summary: '', tags: '' });
      setShowForm(false);
      await loadData();
    } catch {
      setError('Failed to create listing');
    } finally {
      setFormLoading(false);
    }
  };

  if (loading) return <div className="page-loading">Loading...</div>;

  return (
    <div className="page">
      <div className="page-header">
        <h1>My Listings</h1>
        <button className="btn btn-primary" onClick={() => setShowForm(!showForm)}>
          {showForm ? 'Cancel' : 'Create Listing'}
        </button>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}

      {showForm && (
        <div className="card" style={{ marginBottom: '1.5rem' }}>
          <div className="card-header">New Listing</div>
          <div className="card-body">
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label className="form-label">Product</label>
                <select
                  className="form-input"
                  value={form.productId}
                  onChange={(e) => setForm({ ...form, productId: e.target.value })}
                  required
                >
                  <option value="">Select a product</option>
                  {products.map((p) => (
                    <option key={p.id} value={p.id}>{p.name}</option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Title</label>
                <input
                  type="text"
                  className="form-input"
                  value={form.title}
                  onChange={(e) => handleTitleChange(e.target.value)}
                  required
                />
              </div>
              <div className="form-group">
                <label className="form-label">Slug</label>
                <input
                  type="text"
                  className="form-input"
                  value={form.slug}
                  onChange={(e) => setForm({ ...form, slug: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label className="form-label">Summary</label>
                <input
                  type="text"
                  className="form-input"
                  value={form.summary}
                  onChange={(e) => setForm({ ...form, summary: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label className="form-label">Tags (comma separated)</label>
                <input
                  type="text"
                  className="form-input"
                  value={form.tags}
                  onChange={(e) => setForm({ ...form, tags: e.target.value })}
                  placeholder="electronics, sale, new"
                />
              </div>
              <div className="form-actions">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setShowForm(false)}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={formLoading}
                >
                  {formLoading ? 'Creating...' : 'Create'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <div className="card">
        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>Title</th>
                <th>Slug</th>
                <th>Status</th>
                <th>Views</th>
                <th>Featured</th>
                <th>Tags</th>
              </tr>
            </thead>
            <tbody>
              {listings.length === 0 && (
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center' }} className="text-muted">
                    No listings yet. Create your first listing above.
                  </td>
                </tr>
              )}
              {listings.map((listing) => (
                <tr key={listing.id}>
                  <td>
                    <a href={`/listings/${listing.slug}`}>{listing.title}</a>
                  </td>
                  <td className="text-muted">{listing.slug}</td>
                  <td>
                    <span className={`badge ${statusBadge(listing.status)}`}>
                      {listing.status}
                    </span>
                  </td>
                  <td>{listing.viewCount}</td>
                  <td>{listing.featured ? 'Yes' : 'No'}</td>
                  <td>
                    <div className="flex gap-sm" style={{ flexWrap: 'wrap' }}>
                      {listing.tags.map((tag) => (
                        <span key={tag} className="badge badge-neutral">{tag}</span>
                      ))}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default SellerListingsPage;
