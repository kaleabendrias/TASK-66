import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getListingBySlug, publishListing, archiveListing } from '@/api/listings';
import { getMyProfile, getPackagesByTier, getItemsByPackage } from '@/api/members';
import { useAuthStore } from '@/state/authStore';
import { Listing, MemberProfile, BenefitItem } from '@/api/types';

const ListingDetailPage: React.FC = () => {
  const { slug } = useParams<{ slug: string }>();
  const { user, isAuthenticated } = useAuthStore();
  const [listing, setListing] = useState<Listing | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionLoading, setActionLoading] = useState(false);
  const [memberProfile, setMemberProfile] = useState<MemberProfile | null>(null);
  const [discount, setDiscount] = useState<number | null>(null);

  useEffect(() => {
    if (!slug) return;
    const load = async () => {
      try {
        const data = await getListingBySlug(slug);
        setListing(data);
      } catch {
        setError('Listing not found');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [slug]);

  useEffect(() => {
    if (!isAuthenticated || !user) return;
    if (user.role !== 'MEMBER') return;

    const loadMember = async () => {
      try {
        const profile = await getMyProfile();
        setMemberProfile(profile);
        const packages = await getPackagesByTier(profile.tierId);
        for (const pkg of packages) {
          if (!pkg.active) continue;
          const items = await getItemsByPackage(pkg.id);
          const discountItem = items.find(
            (i: BenefitItem) => i.benefitType === 'DISCOUNT' || i.benefitType.toLowerCase().includes('discount')
          );
          if (discountItem) {
            const val = parseFloat(discountItem.benefitValue);
            if (!isNaN(val) && val > 0) {
              setDiscount(val);
              break;
            }
          }
        }
      } catch {
        // Member profile may not exist, that's fine
      }
    };
    loadMember();
  }, [isAuthenticated, user]);

  const handlePublish = async () => {
    if (!listing) return;
    setActionLoading(true);
    try {
      const updated = await publishListing(listing.id);
      setListing(updated);
    } catch {
      setError('Failed to publish listing');
    } finally {
      setActionLoading(false);
    }
  };

  const handleArchive = async () => {
    if (!listing) return;
    setActionLoading(true);
    try {
      const updated = await archiveListing(listing.id);
      setListing(updated);
    } catch {
      setError('Failed to archive listing');
    } finally {
      setActionLoading(false);
    }
  };

  const statusBadge = (status: string) => {
    switch (status) {
      case 'PUBLISHED': return 'badge-success';
      case 'DRAFT': return 'badge-warning';
      case 'ARCHIVED': return 'badge-neutral';
      default: return 'badge-info';
    }
  };

  if (loading) return <div className="page-loading">Loading...</div>;
  if (error || !listing) {
    return (
      <div className="page">
        <div className="alert alert-danger">{error || 'Listing not found'}</div>
        <Link to="/discover" className="btn btn-secondary">Back to Discover</Link>
      </div>
    );
  }

  const canEdit = user && (user.role === 'SELLER' || user.role === 'ADMINISTRATOR');
  const canModerate = user && (user.role === 'MODERATOR' || user.role === 'ADMINISTRATOR');
  const canReserve = user && user.role === 'MEMBER';

  return (
    <div className="page">
      <div style={{ marginBottom: '1rem' }}>
        <Link to="/discover" className="btn btn-secondary btn-sm">Back to Discover</Link>
      </div>

      <div className="card">
        <div className="card-header">
          <div className="flex gap-sm" style={{ justifyContent: 'space-between', flexWrap: 'wrap' }}>
            <span>{listing.title}</span>
            <span className={`badge ${statusBadge(listing.status)}`}>{listing.status}</span>
          </div>
        </div>
        <div className="card-body">
          {error && <div className="alert alert-danger">{error}</div>}

          <p style={{ marginBottom: '1rem' }}>{listing.summary}</p>

          <div className="detail-grid">
            <div className="detail-row">
              <span className="form-label">Tags</span>
              <div className="flex gap-sm" style={{ flexWrap: 'wrap' }}>
                {listing.tags.length > 0 ? listing.tags.map((tag) => (
                  <span key={tag} className="badge badge-neutral">{tag}</span>
                )) : <span className="text-muted">No tags</span>}
              </div>
            </div>
            <div className="detail-row">
              <span className="form-label">Views</span>
              <span>{listing.viewCount}</span>
            </div>
            <div className="detail-row">
              <span className="form-label">Search Rank</span>
              <span>{listing.searchRank}</span>
            </div>
            {listing.featured && (
              <div className="detail-row">
                <span className="form-label">Featured</span>
                <span className="badge badge-warning">Featured</span>
              </div>
            )}
            {listing.publishedAt && (
              <div className="detail-row">
                <span className="form-label">Published</span>
                <span>{new Date(listing.publishedAt).toLocaleDateString()}</span>
              </div>
            )}
          </div>

          {memberProfile && discount !== null && discount > 0 && (
            <div className="card mt-md" style={{ border: '1px solid var(--success)', background: 'var(--success-light)' }}>
              <div className="card-body">
                <strong>Your tier: {memberProfile.tierName} - {discount}% discount applies</strong>
              </div>
            </div>
          )}

          <p className="text-muted mt-md" style={{ fontSize: '0.8125rem' }}>
            Benefits are non-stackable. Only one benefit applies per transaction.
          </p>

          <div className="form-actions" style={{ justifyContent: 'flex-start' }}>
            {canReserve && listing.status === 'PUBLISHED' && (
              <Link to={`/reservations?listingId=${listing.id}`} className="btn btn-primary">
                Reserve Stock
              </Link>
            )}
            {canEdit && (
              <Link to={`/my-listings?edit=${listing.id}`} className="btn btn-secondary">
                Edit
              </Link>
            )}
            {canModerate && listing.status !== 'PUBLISHED' && (
              <button
                className="btn btn-primary"
                onClick={handlePublish}
                disabled={actionLoading}
              >
                {actionLoading ? 'Publishing...' : 'Publish'}
              </button>
            )}
            {canModerate && listing.status !== 'ARCHIVED' && (
              <button
                className="btn btn-danger"
                onClick={handleArchive}
                disabled={actionLoading}
              >
                {actionLoading ? 'Archiving...' : 'Archive'}
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default ListingDetailPage;
