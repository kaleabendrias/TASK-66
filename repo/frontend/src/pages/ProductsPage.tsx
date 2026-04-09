import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useProducts } from '@/features/products/useProducts';
import { useAuth } from '@/features/auth/useAuth';
import { getCategories } from '@/api/categories';
import { Category, ProductFormData } from '@/api/types';
import Button from '@/components/ui/Button';
import Badge from '@/components/ui/Badge';
import Card from '@/components/ui/Card';
import Modal from '@/components/ui/Modal';
import Input from '@/components/ui/Input';

const statusVariant = (status: string) => {
  switch (status) {
    case 'APPROVED': return 'success' as const;
    case 'PENDING': return 'warning' as const;
    case 'REJECTED': return 'danger' as const;
    default: return 'neutral' as const;
  }
};

const ProductsPage: React.FC = () => {
  const { products, loading, error, fetchProducts, addProduct } = useProducts();
  const { user, hasAnyRole } = useAuth();
  const navigate = useNavigate();
  const [showAdd, setShowAdd] = useState(false);
  const [categories, setCategories] = useState<Category[]>([]);
  const [form, setForm] = useState<ProductFormData>({
    name: '', description: '', price: 0, stockQuantity: 0, categoryId: 0,
  });
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    fetchProducts();
    getCategories().then(setCategories).catch(() => {});
  }, [fetchProducts]);

  const handleAdd = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await addProduct({ ...form, sellerId: user?.id });
      setShowAdd(false);
      setForm({ name: '', description: '', price: 0, stockQuantity: 0, categoryId: 0 });
    } catch {
      // error handling
    } finally {
      setSubmitting(false);
    }
  };

  const canAdd = hasAnyRole('SELLER', 'ADMINISTRATOR');

  return (
    <div className="page">
      <div className="page-header">
        <h1>Products</h1>
        {canAdd && (
          <Button onClick={() => setShowAdd(true)}>Add Product</Button>
        )}
      </div>

      {error && <div className="alert alert-danger">{error}</div>}
      {loading && <p>Loading products...</p>}

      <div className="products-grid">
        {products.map((p) => (
          <Card key={p.id} className="product-card clickable" title={p.name}>
            <div onClick={() => navigate(`/products/${p.id}`)}>
              <p className="text-muted">{p.description || 'No description'}</p>
              <div className="product-meta">
                <span className="product-price">${Number(p.price).toFixed(2)}</span>
                <Badge variant={statusVariant(p.status)}>{p.status}</Badge>
              </div>
              <div className="product-meta">
                <span>Category: {p.categoryName || 'N/A'}</span>
                <span>Stock: {p.stockQuantity}</span>
              </div>
            </div>
          </Card>
        ))}
        {!loading && products.length === 0 && <p>No products found.</p>}
      </div>

      <Modal open={showAdd} onClose={() => setShowAdd(false)} title="Add Product">
        <form onSubmit={handleAdd}>
          <Input
            label="Name"
            name="name"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            required
          />
          <Input
            label="Description"
            name="description"
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
          />
          <Input
            label="Price"
            name="price"
            type="number"
            step="0.01"
            value={form.price}
            onChange={(e) => setForm({ ...form, price: parseFloat(e.target.value) || 0 })}
            required
          />
          <Input
            label="Stock Quantity"
            name="stockQuantity"
            type="number"
            value={form.stockQuantity}
            onChange={(e) => setForm({ ...form, stockQuantity: parseInt(e.target.value) || 0 })}
            required
          />
          <div className="form-group">
            <label className="form-label">Category</label>
            <select
              className="form-input"
              value={form.categoryId}
              onChange={(e) => setForm({ ...form, categoryId: parseInt(e.target.value) || 0 })}
              required
            >
              <option value={0}>Select category</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>
          </div>
          <div className="form-actions">
            <Button type="button" variant="secondary" onClick={() => setShowAdd(false)}>Cancel</Button>
            <Button type="submit" loading={submitting}>Create</Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default ProductsPage;
