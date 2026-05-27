-- liquibase formatted sql

-- changeset aweem:ca-09 splitStatements:false runOnChange:false
-- comment: Seeding standard workflow operational states data-driven constraints.
-- rollback TRUNCATE TABLE order_statuses RESTART IDENTITY;
INSERT INTO order_statuses (status_code, description, is_active)
VALUES ('PENDING', 'Initial generated context state waiting execution workflows.', TRUE),
       ('PAID', 'Verified financial ledger payment validation accepted.', TRUE),
       ('FULFILLED', 'Inventory packed and handed down to shipping dispatch carriers.', TRUE),
       ('CANCELLED', 'Order drop execution path triggered. Reserved inventory restored.', TRUE);

-- changeset aweem:ca-09b splitStatements:false runOnChange:false
-- comment: Seeding order item status codes for item-level state tracking.
-- rollback TRUNCATE TABLE order_item_statuses RESTART IDENTITY;
INSERT INTO order_item_statuses (status_code, description, is_active)
VALUES ('PENDING', 'Initial state when item is added to order', TRUE),
       ('CONFIRMED', 'Item inventory is confirmed after payment and pending fulfillment', TRUE),
       ('FULFILLED', 'Item has been packed and handed to shipping', TRUE),
       ('CANCELLED', 'Item is temporarily out of stock or cancelled', TRUE);

-- changeset aweem:ca-10 splitStatements:false runOnChange:false
-- comment: Hydrating permissible workflow state transition direction paths rules maps.
-- rollback TRUNCATE TABLE update_status_restriction RESTART IDENTITY;
INSERT INTO update_status_restriction (current_status, allowed_next_status, is_active)
VALUES ('PENDING', 'PAID', TRUE),
       ('PENDING', 'CANCELLED', TRUE),
       ('PAID', 'FULFILLED', TRUE);

-- changeset aweem:ca-11 splitStatements:false runOnChange:false
-- comment: Seeding multi-tiered retail catalog assortment variants data structures.
-- rollback DELETE FROM products;
INSERT INTO products (product_id, sku, name, description, price, stock_quantity, version)
VALUES ('a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d', 'SKU-CA-DENIM-001', 'Classic Relaxed Fit Denim Pants', 'Comfortable and versatile relaxed fit denim for everyday wear', 49.99, 1500, 0),
       ('11111111-2222-3333-4444-555555555555', 'SKU-CA-DENIM-002', 'High-Waist Skinny Dark Indigo Denim', 'Premium dark indigo denim with high-waist cut for a flattering fit', 59.90, 850, 0),
       ('22222222-3333-4444-5555-666666666666', 'SKU-CA-DENIM-003', 'Premium Selvedge Slim Fit Denim Jeans', 'Authentic selvedge denim with superior craftsmanship and durability', 119.00, 300, 0),
       ('33333333-4444-5555-6666-777777777777', 'SKU-CA-DENIM-004', 'Distressed Vintage Denim Trucker Jacket', 'Stylish vintage-inspired trucker jacket with distressed details', 79.95, 400, 0),
       ('c3d4e5f6-a7b8-9c0d-1e2f-3a4b5c6d7e8f', 'SKU-CA-SHIRT-003', 'Organic Slim Linen Blend Button Up', 'Lightweight and breathable linen blend perfect for warm weather', 29.00, 3000, 0),
       ('44444444-5555-6666-7777-888888888888', 'SKU-CA-SHIRT-004', 'Essential Everyday Cotton Crewneck Tee White', 'Classic white cotton tee suitable for any casual occasion', 14.99, 10000, 0),
       ('55555555-6666-7777-8888-999999999999', 'SKU-CA-SHIRT-005', 'Essential Everyday Cotton Crewneck Tee Black', 'Versatile black cotton tee for a timeless wardrobe staple', 14.99, 12000, 0),
       ('66666666-7777-8888-9999-aaaaaaaaaaaa', 'SKU-CA-KNIT-001', 'Fine Knit Merino Wool Lightweight Sweater', 'Premium merino wool sweater offering warmth and breathability', 69.50, 650, 0),
       ('b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e', 'SKU-CA-JACKET-002', 'Premium Bomber Winter Shell Jacket', 'Insulated bomber jacket designed for harsh winter conditions', 89.50, 450, 0),
       ('77777777-8888-9999-aaaa-bbbbbbbbbbbb', 'SKU-CA-COAT-001', 'Classic Double-Breasted Wool Blend Trench Coat', 'Elegant and timeless trench coat in luxurious wool blend', 149.00, 200, 0),
       ('88888888-9999-aaaa-bbbb-cccccccccccc', 'SKU-CA-JACKET-006', 'Water-Repellent Technical Hooded Parka', 'Advanced weatherproof parka with technical performance features', 129.95, 500, 0),
       ('99999999-aaaa-bbbb-cccc-dddddddddddd', 'SKU-CA-ACC-001', 'Unisex Recycled Cotton Ribbed Beanie', 'Eco-friendly beanie made from sustainable recycled cotton', 19.99, 2500, 0),
       ('00000000-bbbb-cccc-dddd-eeeeeeeeeeee', 'SKU-CA-ACC-002', 'Genuine Textured Leather Dress Belt', 'Premium leather belt perfect for formal and casual wear', 34.50, 1200, 0);
