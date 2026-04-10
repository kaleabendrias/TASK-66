-- Enforce strict minimum low_stock_threshold of 5 on all inventory items
UPDATE inventory_item SET low_stock_threshold = 5 WHERE low_stock_threshold < 5;
ALTER TABLE inventory_item ADD CONSTRAINT chk_min_threshold CHECK (low_stock_threshold >= 5);
