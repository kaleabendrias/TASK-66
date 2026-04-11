import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getListingBySlug, publishListing, archiveListing } from '@/api/listings';
import { getMyProfile, getPackagesByTier, getItemsByPackage } from '@/api/members';
import { useAuthStore } from '@/state/authStore';
import { Listing, MemberProfile, BenefitItem } from '@/api/types';

// Mirrors the checkout/pricing copy so buyers see the same non-stacking
// semantics here that PricingService will actually apply at order time.
interface ResolvedBenefits {
  discountPercent: number | null;
  freeShipping: boolean;
  priorityPerks: string[];
}

const ListingDetailPage: React.FC = () => {
  const { slug } = useParams<{ slug: string }>();
  const { user, isAuthenticated } = useAuthStore();
  const [listing, setListing] = useState<Listing | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionLoading, setActionLoading] = useState(false);
  const [memberProfile, setMemberProfile] = useState<MemberProfile | null>(null);
  const [benefits, setBenefits] = useState<ResolvedBenefits>({
    discountPercent: null,
    freeShipping: false,
    priorityPerks: [],
  });

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

        // Collect the union of every benefit this member's tier exposes
        // across the currently active packages, so the listing page can
        // show the same surface the checkout would see. We still only
        // ever apply ONE (the best) at order time — the backend's
        // PricingService enforces that and the copy below explains it.
        let bestDiscount: number | null = null;
        let freeShipping = false;
        const priorityPerks: string[] = [];

        for (const pkg of packages) {
          if (!pkg.active) continue;
          const items = await getItemsByPackage(pkg.id);
          for (const item of items as BenefitItem[]) {
            const type = (item.benefitType || '').toUpperCase();
            if (type === 'DISCOUNT') {
              const val = parseFloat(item.benefitValue);
              if (!isNaN(val) && val > 0 && (bestDiscount === null || val > bestDiscount)) {
                bestDiscount = val;
              }
            } else if (type === 'FREE_SHIPPING') {
              freeShipping = true;
            } else if (type === 'PRIORITY_SUPPORT' || type === 'EXCLUSIVE_ACCESS') {
              priorityPerks.push(type === 'PRIORITY_SUPPORT' ? 'Priority support' : 'Exclusive access');
            }
          }
        }

        setBenefits({ discountPercent: bestDiscount, freeShipping, priorityPerks });
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

          {memberProfile && (
            benefits.discountPercent !== null ||
            benefits.freeShipping ||
            benefits.priorityPerks.length > 0
          ) && (
            <div className="card mt-md" style={{ border: '1px solid var(--success)', background: 'var(--success-light)' }}>
              <div className="card-body">
                <div style={{ marginBottom: '0.5rem' }}>
                  <strong>Your tier: {memberProfile.tierName}</strong>
                </div>
                <ul style={{ margin: 0, paddingLeft: '1.1rem', fontSize: '0.9rem' }}>
                  {benefits.discountPercent !== null && (
                    <li>
                      <strong>{benefits.discountPercent}% tier discount</strong> on the line subtotal.
                    </li>
                  )}
                  {benefits.freeShipping && (
                    <li>
                      <strong>Free shipping</strong> eligible — the shipping fee is waived at checkout.
                    </li>
                  )}
                  {benefits.priorityPerks.map((perk) => (
                    <li key={perk}>{perk}</li>
                  ))}
                </ul>
                <p className="text-muted mt-sm" style={{ fontSize: '0.78rem', marginBottom: 0 }}>
                  Final totals are computed server-side by PricingService at order time.
                </p>
              </div>
            </div>
          )}

          {/* Explicit non-stacking copy. The server's PricingService picks
              ONE discount (the highest) and ONE item per exclusion group,
              so at most one DISCOUNT and one FREE_SHIPPING can ever
              combine per order. We spell it out here so buyers don't
              expect a 15% discount AND free shipping to compose beyond
              the server's actual rules. */}
          <div className="alert alert-info mt-md" style={{ fontSize: '0.8125rem' }}>
            <strong>Benefits do not stack.</strong> At most one <em>discount</em> applies per
            transaction — if several are available the highest one wins. Within any other
            exclusion group (e.g. free shipping), a single benefit is selected. The checkout
            total you see at order time is the authoritative number.
          </div>

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
