-- Seed demo users (password: password123)
INSERT INTO app_user (username, email, password_hash, display_name, role) VALUES
  ('guest',      'guest@demo.local',      '$2a$10$CDdcj12dr65C27ckLRMFQevdNud3wkqYzCcyk5iCsqCihFJDF1Ol2', 'Demo Guest',           'GUEST'),
  ('member',     'member@demo.local',     '$2a$10$CDdcj12dr65C27ckLRMFQevdNud3wkqYzCcyk5iCsqCihFJDF1Ol2', 'Demo Member',          'MEMBER'),
  ('seller',     'seller@demo.local',     '$2a$10$CDdcj12dr65C27ckLRMFQevdNud3wkqYzCcyk5iCsqCihFJDF1Ol2', 'Demo Seller',          'SELLER'),
  ('warehouse',  'warehouse@demo.local',  '$2a$10$CDdcj12dr65C27ckLRMFQevdNud3wkqYzCcyk5iCsqCihFJDF1Ol2', 'Demo Warehouse Staff', 'WAREHOUSE_STAFF'),
  ('moderator',  'moderator@demo.local',  '$2a$10$CDdcj12dr65C27ckLRMFQevdNud3wkqYzCcyk5iCsqCihFJDF1Ol2', 'Demo Moderator',       'MODERATOR'),
  ('admin',      'admin@demo.local',      '$2a$10$CDdcj12dr65C27ckLRMFQevdNud3wkqYzCcyk5iCsqCihFJDF1Ol2', 'Demo Administrator',   'ADMINISTRATOR');

INSERT INTO category (name, description) VALUES
  ('Electronics',   'Gadgets, devices, and accessories'),
  ('Books',         'Physical and digital books'),
  ('Clothing',      'Apparel and fashion items'),
  ('Home & Garden', 'Furniture, decor, and gardening');

INSERT INTO product (name, description, price, stock_quantity, category_id, seller_id, status) VALUES
  ('Wireless Headphones',  'Bluetooth over-ear headphones with ANC',       79.99,  150, 1, 3, 'APPROVED'),
  ('USB-C Hub',            '7-in-1 USB-C dock for laptops',                34.50,  300, 1, 3, 'APPROVED'),
  ('Domain-Driven Design', 'Eric Evans classic on DDD',                    42.00,   80, 2, 3, 'APPROVED'),
  ('Cotton T-Shirt',       'Unisex crew-neck cotton tee',                  15.99,  500, 3, 3, 'APPROVED'),
  ('Standing Desk',        'Electric sit-stand desk, 60x30 inches',       349.00,   25, 4, 3, 'PENDING'),
  ('Mechanical Keyboard',  'Cherry MX Brown switches, TKL layout',        89.00,  200, 1, 3, 'APPROVED');

INSERT INTO product_order (buyer_id, product_id, quantity, total_price, status) VALUES
  (2, 1, 1,  79.99, 'PLACED'),
  (2, 3, 2,  84.00, 'SHIPPED'),
  (2, 4, 3,  47.97, 'DELIVERED');
