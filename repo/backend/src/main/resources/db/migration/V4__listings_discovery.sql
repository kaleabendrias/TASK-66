-- Listing discovery metadata
CREATE TABLE listing (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT       NOT NULL REFERENCES product(id),
    title           VARCHAR(300) NOT NULL,
    slug            VARCHAR(300) NOT NULL UNIQUE,
    summary         VARCHAR(500),
    tags            TEXT[],
    featured        BOOLEAN      NOT NULL DEFAULT FALSE,
    view_count      BIGINT       NOT NULL DEFAULT 0,
    search_rank     DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    metadata        JSONB,
    status          VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    published_at    TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_listing_product ON listing(product_id);
CREATE INDEX idx_listing_status ON listing(status);
CREATE INDEX idx_listing_tags ON listing USING GIN(tags);
CREATE INDEX idx_listing_metadata ON listing USING GIN(metadata);
CREATE INDEX idx_listing_search ON listing(search_rank DESC, published_at DESC);

-- Seed listings for existing products
INSERT INTO listing (product_id, title, slug, summary, tags, featured, view_count, search_rank, status, published_at) VALUES
  (1, 'Wireless Headphones - Premium ANC', 'wireless-headphones-premium-anc', 'High-quality bluetooth headphones', ARRAY['audio','bluetooth','anc'], TRUE, 1250, 8.5, 'PUBLISHED', NOW()),
  (2, 'USB-C Hub 7-in-1', 'usb-c-hub-7in1', 'Essential laptop dock', ARRAY['usb-c','dock','laptop'], FALSE, 830, 7.2, 'PUBLISHED', NOW()),
  (3, 'Domain-Driven Design by Eric Evans', 'domain-driven-design-eric-evans', 'Classic software architecture book', ARRAY['books','software','ddd'], FALSE, 620, 6.8, 'PUBLISHED', NOW()),
  (4, 'Cotton T-Shirt Unisex', 'cotton-tshirt-unisex', 'Comfortable everyday wear', ARRAY['clothing','cotton','unisex'], FALSE, 2100, 9.1, 'PUBLISHED', NOW()),
  (5, 'Electric Standing Desk', 'electric-standing-desk', 'Ergonomic sit-stand desk', ARRAY['furniture','desk','ergonomic'], TRUE, 450, 5.5, 'DRAFT', NULL),
  (6, 'Mechanical Keyboard TKL', 'mechanical-keyboard-tkl', 'Cherry MX Brown tactile keyboard', ARRAY['keyboard','mechanical','cherry-mx'], TRUE, 980, 8.0, 'PUBLISHED', NOW());
