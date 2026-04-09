import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getProduct } from '@/api/products';
import { placeOrder } from '@/api/orders';
import { updateProduct } from '@/api/products';
import { useAuth } from '@/features/auth/useAuth';
import { Product, ProductFormData } from '@/api/types';
import Card from '@/components/ui/Card';
import Badge from '@/components/ui/Badge';
import Button from '@/components/ui/Button';
import Input from '@/components/ui/Input';
import Modal from '@/components/ui/Modal';

const ProductDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { user, hasAnyRole } = useAuth();
  const navigate = useNavigate();
  const [product, setProduct] = useState<Product | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [orderQty, setOrderQty] = useState(1);
  const [ordering, setOrdering] = useState(false);
  const [orderSuccess, setOrderSuccess] = useState('');
  const [showEdit, setShowEdit] = useState(false);
  const [editForm, setEditForm] = useState<ProductFormData>({
    name: '', description: '', price: 0, stockQuantity: 0, categoryId: 0,
  });
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    getProduct(parseInt(id))
      .then((p) => {
        setProduct(p);
        setEditForm({
          name: p.name,
          description: p.description,
          price: p.price,
          stockQuantity: p.stockQuantity,
          categoryId: p.categoryId,
          sellerId: p.sellerId,
          status: p.status,
        });
      })
      .catch(() => setError('Product not found'))
      .finally(() => setLoading(false));
  }, [id]);

  const handleOrder = async () => {
    if (!product || !user) return;
    setOrdering(true);
    setOrderSuccess('');
    try {
      await placeOrder({
        buyerId: user.id,
        productId: product.id,
        quantity: orderQty,
        totalPrice: product.price * orderQty,
      });
      setOrderSuccess('Order placed successfully!');
    } catch {
      setError('Failed to place order');
    } finally {
      setOrdering(false);
    }
  };

  const handleEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!product) return;
    setSaving(true);
    try {
      const updated = await updateProduct(product.id, editForm);
      setProduct(updated);
      setShowEdit(false);
    } catch {
      setError('Failed to update product');
    } finally {
      setSaving(false);
    }
  };

  const handleStatusChange = async (status: 'APPROVED' | 'REJECTED') => {
    if (!product) return;
    try {
      const updated = await updateProduct(product.id, { ...editForm, status });
      setProduct(updated);
    } catch {
      setError('Failed to update status');
    }
  };

  if (loading) return <div className="page"><p>Loading...</p></div>;
  if (error && !product) return <div className="page"><div className="alert alert-danger">{error}</div></div>;
  if (!product) return null;

  const canEdit = (user?.role === 'SELLER' && product.sellerId === user.id) || user?.role === 'ADMINISTRATOR';
  const canOrder = hasAnyRole('MEMBER');
  const canModerate = hasAnyRole('MODERATOR', 'ADMINISTRATOR');

  const statusVariant = product.status === 'APPROVED' ? 'success' : product.status === 'PENDING' ? 'warning' : 'danger';

  return (
    <div className="page">
      <Button variant="secondary" onClick={() => navigate('/products')}>Back to Products</Button>

      <Card title={product.name} className="mt-md">
        {error && <div className="alert alert-danger">{error}</div>}
        {orderSuccess && <div className="alert alert-success">{orderSuccess}</div>}

        <div className="product-detail-grid">
          <div>
            <p><strong>Description:</strong> {product.description || 'N/A'}</p>
            <p><strong>Price:</strong> ${Number(product.price).toFixed(2)}</p>
            <p><strong>Stock:</strong> {product.stockQuantity}</p>
            <p><strong>Category:</strong> {product.categoryName || 'N/A'}</p>
            <p><strong>Seller:</strong> {product.sellerName || 'N/A'}</p>
            <p><strong>Status:</strong> <Badge variant={statusVariant}>{product.status}</Badge></p>
          </div>

          <div className="product-actions">
            {canEdit && (
              <Button onClick={() => setShowEdit(true)}>Edit Product</Button>
            )}
            {canModerate && product.status === 'PENDING' && (
              <div className="flex gap-sm">
                <Button variant="primary" onClick={() => handleStatusChange('APPROVED')}>Approve</Button>
                <Button variant="danger" onClick={() => handleStatusChange('REJECTED')}>Reject</Button>
              </div>
            )}
            {canOrder && product.status === 'APPROVED' && (
              <div className="order-form">
                <Input
                  label="Quantity"
                  type="number"
                  min={1}
                  max={product.stockQuantity}
                  value={orderQty}
                  onChange={(e) => setOrderQty(parseInt(e.target.value) || 1)}
                />
                <p>Total: ${(product.price * orderQty).toFixed(2)}</p>
                <Button onClick={handleOrder} loading={ordering}>Place Order</Button>
              </div>
            )}
          </div>
        </div>
      </Card>

      <Modal open={showEdit} onClose={() => setShowEdit(false)} title="Edit Product">
        <form onSubmit={handleEdit}>
          <Input
            label="Name"
            value={editForm.name}
            onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
            required
          />
          <Input
            label="Description"
            value={editForm.description}
            onChange={(e) => setEditForm({ ...editForm, description: e.target.value })}
          />
          <Input
            label="Price"
            type="number"
            step="0.01"
            value={editForm.price}
            onChange={(e) => setEditForm({ ...editForm, price: parseFloat(e.target.value) || 0 })}
            required
          />
          <Input
            label="Stock Quantity"
            type="number"
            value={editForm.stockQuantity}
            onChange={(e) => setEditForm({ ...editForm, stockQuantity: parseInt(e.target.value) || 0 })}
            required
          />
          <div className="form-actions">
            <Button type="button" variant="secondary" onClick={() => setShowEdit(false)}>Cancel</Button>
            <Button type="submit" loading={saving}>Save</Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default ProductDetailPage;
