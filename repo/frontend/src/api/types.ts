export type Role =
  | 'GUEST'
  | 'MEMBER'
  | 'SELLER'
  | 'WAREHOUSE_STAFF'
  | 'MODERATOR'
  | 'ADMINISTRATOR';

export type OrderStatus =
  | 'PLACED'
  | 'CONFIRMED'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'CANCELLED';

export type ProductStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface User {
  id: number;
  username: string;
  email: string;
  displayName: string;
  role: Role;
  enabled: boolean;
}

export interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  stockQuantity: number;
  categoryId: number;
  categoryName: string;
  sellerId: number;
  sellerName: string;
  status: ProductStatus;
}

export interface Category {
  id: number;
  name: string;
  description: string;
}

export interface Order {
  id: number;
  buyerId: number;
  productId: number;
  quantity: number;
  totalPrice: number;
  status: OrderStatus;
}

export interface AuthResponse {
  token: string;
  username: string;
  role: Role;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  displayName: string;
}

export interface ProductFormData {
  name: string;
  description: string;
  price: number;
  stockQuantity: number;
  categoryId: number;
  sellerId?: number;
  status?: ProductStatus;
}

export interface OrderFormData {
  buyerId: number;
  productId: number;
  quantity: number;
  totalPrice: number;
}

// Listings
export type ListingStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';

export interface Listing {
  id: number;
  productId: number;
  title: string;
  slug: string;
  summary: string;
  tags: string[];
  featured: boolean;
  viewCount: number;
  weeklyViews: number;
  searchRank: number;
  price: number | null;
  sqft: number | null;
  layout: string | null;
  status: ListingStatus;
  publishedAt: string | null;
}

// Member Tiers
export interface MemberTier {
  id: number;
  name: string;
  rank: number;
  minSpend: number;
  maxSpend: number | null;
}

export interface BenefitPackage {
  id: number;
  tierId: number;
  name: string;
  description: string;
  active: boolean;
}

export interface BenefitItem {
  id: number;
  packageId: number;
  benefitType: string;
  benefitValue: string;
}

export interface MemberProfile {
  id: number;
  userId: number;
  tierId: number;
  tierName: string;
  totalSpend: number;
  phoneMasked: string | null;
  joinedAt: string;
}

export interface PointsLedgerEntry {
  id: number;
  amount: number;
  spendAfter: number;
  entryType: string;
  reference: string;
  createdAt: string;
}

// Warehouse & Inventory
export interface Warehouse {
  id: number;
  name: string;
  code: string;
  location: string;
  active: boolean;
}

export interface InventoryItem {
  id: number;
  productId: number;
  warehouseId: number;
  warehouseName: string;
  quantityOnHand: number;
  quantityReserved: number;
  quantityAvailable: number;
  lowStockThreshold: number;
  lowStock: boolean;
}

export interface InventoryMovement {
  id: number;
  inventoryItemId: number;
  warehouseId: number;
  movementType: string;
  quantity: number;
  balanceAfter: number;
  referenceDocument: string;
  operatorId: number;
  notes: string;
  createdAt: string;
}

export interface StockReservation {
  id: number;
  inventoryItemId: number;
  userId: number;
  quantity: number;
  status: string;
  idempotencyKey: string;
  expiresAt: string;
  createdAt: string;
  confirmedAt: string | null;
  cancelledAt: string | null;
}

// Incidents
export interface Incident {
  id: number;
  reporterId: number;
  assigneeId: number | null;
  incidentType: string;
  severity: string;
  title: string;
  description: string;
  address: string | null;
  crossStreet: string | null;
  status: string;
  slaAckDeadline: string | null;
  slaResolveDeadline: string | null;
  escalationLevel: number;
  createdAt: string;
  acknowledgedAt: string | null;
  resolvedAt: string | null;
}

export interface IncidentComment {
  id: number;
  incidentId: number;
  authorId: number;
  content: string;
  createdAt: string;
}

// Appeals
export interface Appeal {
  id: number;
  userId: number;
  relatedEntityType: string;
  relatedEntityId: number;
  reason: string;
  status: string;
  reviewerId: number | null;
  reviewNotes: string | null;
  createdAt: string;
  reviewedAt: string | null;
  resolvedAt: string | null;
}

// Fulfillment
export interface Fulfillment {
  id: number;
  orderId: number;
  warehouseId: number;
  status: string;
  operatorId: number | null;
  trackingInfo: string | null;
  idempotencyKey: string;
}

export interface FulfillmentStep {
  id: number;
  fulfillmentId: number;
  stepName: string;
  status: string;
  operatorId: number | null;
  notes: string | null;
  createdAt: string;
  completedAt: string | null;
}

// Risk Analytics
export interface RiskScore {
  id: number;
  userId: number;
  score: number;
  factors: string;
  computedAt: string;
}

export interface RiskEvent {
  id: number;
  userId: number;
  eventType: string;
  severity: string;
  details: string;
  createdAt: string;
}

// Audit
export interface AuditLogEntry {
  id: number;
  entityType: string;
  entityId: number;
  action: string;
  actorId: number | null;
  oldValue: string | null;
  newValue: string | null;
  ipAddress: string | null;
  createdAt: string;
  retentionExpiresAt: string;
}

// Account Deletion
export interface AccountDeletionStatus {
  id?: number;
  status: string;
  coolingOffEndsAt?: string;
  message?: string;
}
