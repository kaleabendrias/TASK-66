import React, { useEffect, useState } from 'react';
import { useAuthStore } from '@/state/authStore';
import { getMyProfile, getPackagesByTier, getItemsByPackage } from '@/api/members';
import { MemberProfile, BenefitPackage, BenefitItem } from '@/api/types';

const CheckoutPage: React.FC = () => {
  const user = useAuthStore((s) => s.user);
  const [profile, setProfile] = useState<MemberProfile | null>(null);
  const [benefits, setBenefits] = useState<BenefitItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      try {
        const p = await getMyProfile();
        setProfile(p);
        const pkgs = await getPackagesByTier(p.tierId);
        const allItems: BenefitItem[] = [];
        for (const pkg of pkgs) {
          const items = await getItemsByPackage(pkg.id);
          allItems.push(...items);
        }
        setBenefits(allItems);
      } catch {}
      setLoading(false);
    };
    if (user) load();
    else setLoading(false);
  }, [user]);

  if (loading) return <div className="page-loading">Loading checkout...</div>;

  const discountBenefits = benefits.filter((b) => b.benefitType === 'DISCOUNT');
  const shippingBenefits = benefits.filter((b) => b.benefitType === 'FREE_SHIPPING');
  const bestDiscount = discountBenefits.length > 0
    ? discountBenefits.reduce((a, b) => parseInt(a.benefitValue) > parseInt(b.benefitValue) ? a : b)
    : null;

  return (
    <div className="page">
      <h1>Checkout</h1>

      {profile && (
        <div className="card" style={{ marginBottom: '1.5rem' }}>
          <div className="card-header">Your Tier Benefits — {profile.tierName}</div>
          <div className="card-body">
            {bestDiscount && (
              <div className="benefit-card">
                <div className="benefit-info">
                  <h4>{bestDiscount.benefitValue}% Discount</h4>
                  <p>Applied automatically at checkout</p>
                  {bestDiscount.scope === 'ORDER' && <span className="badge badge-info">Order scope</span>}
                  {bestDiscount.categoryId && <span className="badge badge-neutral ml-sm">Category restricted</span>}
                  {bestDiscount.validFrom && bestDiscount.validTo && (
                    <span className="text-muted ml-sm" style={{ fontSize: '0.8rem' }}>
                      Valid: {new Date(bestDiscount.validFrom).toLocaleDateString()} — {new Date(bestDiscount.validTo).toLocaleDateString()}
                    </span>
                  )}
                </div>
              </div>
            )}
            {shippingBenefits.length > 0 && (
              <div className="benefit-card">
                <div className="benefit-info">
                  <h4>Free Shipping</h4>
                  <p>Included with your {profile.tierName} membership</p>
                </div>
              </div>
            )}
            {benefits.length === 0 && (
              <p className="text-muted">No benefits available for your current tier.</p>
            )}

            <div className="alert alert-danger" style={{ marginTop: '1rem', background: 'var(--warning-light)', color: '#92400e', borderColor: '#fde68a' }}>
              <strong>Non-Stackable Benefits:</strong> Only one benefit per exclusion group applies per transaction.
              {discountBenefits.length > 1 && (
                <span> You have {discountBenefits.length} discount benefits — only the highest ({bestDiscount?.benefitValue}%) will be applied.</span>
              )}
              {' '}Benefits in the same exclusion group (e.g., multiple discounts) cannot be combined.
              Each benefit has a scope (ORDER or ACCOUNT) and may be restricted to specific categories, sellers, or date windows.
            </div>
          </div>
        </div>
      )}

      {!user && (
        <div className="alert alert-danger">
          <strong>Sign in</strong> to view your tier benefits and complete checkout.
        </div>
      )}

      <div className="card">
        <div className="card-header">Order Summary</div>
        <div className="card-body">
          <p className="text-muted">Select items from the marketplace to begin checkout.</p>
        </div>
      </div>
    </div>
  );
};

export default CheckoutPage;
