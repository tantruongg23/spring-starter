INSERT INTO users (id, username, email, password, role, version, created_at, updated_at) VALUES
                                                                                             ('11111111-1111-1111-1111-111111111111', 'admin', 'admin@example.com', 'password', 'ADMIN', 0, NOW(), NOW()),
                                                                                             ('22222222-2222-2222-2222-222222222222', 'customer', 'customer@example.com', 'password', 'CUSTOMER', 0, NOW(), NOW());

INSERT INTO categories (id, name, description, version, created_at, updated_at) VALUES
                                                                                    ('33333333-3333-3333-3333-333333333333', 'Electronics', 'Electronic Items', 0, NOW(), NOW()),
                                                                                    ('44444444-4444-4444-4444-444444444444', 'Books', 'Books and Literature', 0, NOW(), NOW());

INSERT INTO products (id, name, description, price, stock_quantity, version, created_at, updated_at) VALUES
                                                                                                         ('55555555-5555-5555-5555-555555555555', 'Laptop', 'High performance laptop', 1200.00, 10, 0, NOW(), NOW()),
                                                                                                         ('66666666-6666-6666-6666-666666666666', 'Smartphone', 'Latest model smartphone', 800.00, 20, 0, NOW(), NOW()),
                                                                                                         ('77777777-7777-7777-7777-777777777777', 'Novel', 'Bestseller Novel', 15.50, 100, 0, NOW(), NOW());

INSERT INTO product_categories (product_id, category_id) VALUES
                                                             ('55555555-5555-5555-5555-555555555555', '33333333-3333-3333-3333-333333333333'),
                                                             ('66666666-6666-6666-6666-666666666666', '33333333-3333-3333-3333-333333333333'),
                                                             ('77777777-7777-7777-7777-777777777777', '44444444-4444-4444-4444-444444444444');
