-- Sử dụng database ecommerce
USE ecommerce;

-- Thêm dữ liệu vào bảng users
INSERT INTO users (username, email, password, full_name, phone_number, address, role) VALUES
('admin', 'admin@example.com', '$2a$10$yfIHMg3xrCZYqYUqvQ1preS.fnM.1iqnZpE33.hYGBBF9kQ2LB/Oe', 'Admin User', '0987654321', '123 Admin St, Admin City', 'ADMIN'),
('seller1', 'seller1@example.com', '$2a$10$yfIHMg3xrCZYqYUqvQ1preS.fnM.1iqnZpE33.hYGBBF9kQ2LB/Oe', 'Seller One', '0123456789', '456 Seller St, Seller City', 'SELLER'),
('seller2', 'seller2@example.com', '$2a$10$yfIHMg3xrCZYqYUqvQ1preS.fnM.1iqnZpE33.hYGBBF9kQ2LB/Oe', 'Seller Two', '0123456788', '789 Seller St, Seller City', 'SELLER'),
('user1', 'user1@example.com', '$2a$10$yfIHMg3xrCZYqYUqvQ1preS.fnM.1iqnZpE33.hYGBBF9kQ2LB/Oe', 'User One', '0123456787', '101 User St, User City', 'USER'),
('user2', 'user2@example.com', '$2a$10$yfIHMg3xrCZYqYUqvQ1preS.fnM.1iqnZpE33.hYGBBF9kQ2LB/Oe', 'User Two', '0123456786', '202 User St, User City', 'USER');

-- Thêm dữ liệu vào bảng categories
INSERT INTO categories (name, description, image_url, parent_id) VALUES
('Electronics', 'Electronic devices and gadgets', 'electronics.jpg', NULL),
('Clothing', 'Fashion and apparel', 'clothing.jpg', NULL),
('Home & Kitchen', 'Home appliances and kitchenware', 'home_kitchen.jpg', NULL),
('Smartphones', 'Mobile phones and accessories', 'smartphones.jpg', 1),
('Laptops', 'Notebook computers and accessories', 'laptops.jpg', 1),
('Men''s Clothing', 'Clothing for men', 'mens_clothing.jpg', 2),
('Women''s Clothing', 'Clothing for women', 'womens_clothing.jpg', 2),
('Kitchen Appliances', 'Appliances for kitchen use', 'kitchen_appliances.jpg', 3),
('Furniture', 'Home furniture', 'furniture.jpg', 3);

-- Thêm dữ liệu vào bảng products
INSERT INTO products (name, description, price, stock, average_rating, category_id, seller_id) VALUES
('iPhone 13 Pro', 'Latest Apple smartphone with advanced features', 999.99, 50, 4.8, 4, 2),
('Samsung Galaxy S21', 'Flagship Android smartphone', 899.99, 45, 4.7, 4, 2),
('Dell XPS 13', 'Premium ultrabook with InfinityEdge display', 1299.99, 30, 4.9, 5, 2),
('MacBook Pro M1', 'Apple laptop with M1 chip', 1499.99, 25, 4.8, 5, 2),
('Men''s Casual Shirt', 'Comfortable cotton casual shirt', 29.99, 100, 4.5, 6, 3),
('Women''s Summer Dress', 'Lightweight summer dress', 39.99, 80, 4.6, 7, 3),
('Stand Mixer', 'Professional kitchen stand mixer', 199.99, 20, 4.7, 8, 3),
('Coffee Maker', 'Programmable coffee maker', 89.99, 35, 4.4, 8, 3),
('Sofa Set', 'Modern 3-piece sofa set', 699.99, 10, 4.3, 9, 3),
('Dining Table', 'Wooden dining table for 6 people', 349.99, 15, 4.5, 9, 3);

-- Thêm dữ liệu vào bảng product_images
INSERT INTO product_images (product_id, image_url) VALUES
(1, 'iphone13pro_1.jpg'),
(1, 'iphone13pro_2.jpg'),
(2, 'galaxys21_1.jpg'),
(2, 'galaxys21_2.jpg'),
(3, 'dellxps13_1.jpg'),
(3, 'dellxps13_2.jpg'),
(4, 'macbookpro_1.jpg'),
(4, 'macbookpro_2.jpg'),
(5, 'mens_shirt_1.jpg'),
(5, 'mens_shirt_2.jpg'),
(6, 'womens_dress_1.jpg'),
(6, 'womens_dress_2.jpg'),
(7, 'stand_mixer_1.jpg'),
(7, 'stand_mixer_2.jpg'),
(8, 'coffee_maker_1.jpg'),
(8, 'coffee_maker_2.jpg'),
(9, 'sofa_set_1.jpg'),
(9, 'sofa_set_2.jpg'),
(10, 'dining_table_1.jpg'),
(10, 'dining_table_2.jpg');

-- Thêm dữ liệu vào bảng carts
INSERT INTO carts (user_id) VALUES
(4),
(5);

-- Thêm dữ liệu vào bảng cart_items
INSERT INTO cart_items (cart_id, product_id, quantity, price) VALUES
(1, 1, 1, 999.99),
(1, 3, 1, 1299.99),
(2, 2, 1, 899.99),
(2, 6, 2, 39.99);

-- Thêm dữ liệu vào bảng orders
INSERT INTO orders (order_number, user_id, total_amount, status, shipping_address, payment_method, order_date, delivery_date) VALUES
('ORD-2023-001', 4, 1329.98, 'DELIVERED', '101 User St, User City', 'CREDIT_CARD', '2023-01-15 10:30:00', '2023-01-20 14:45:00'),
('ORD-2023-002', 4, 699.99, 'SHIPPED', '101 User St, User City', 'PAYPAL', '2023-02-10 11:15:00', NULL),
('ORD-2023-003', 5, 939.98, 'PROCESSING', '202 User St, User City', 'CREDIT_CARD', '2023-03-05 09:45:00', NULL),
('ORD-2023-004', 5, 1499.99, 'PENDING', '202 User St, User City', 'BANK_TRANSFER', '2023-03-10 16:20:00', NULL);

-- Thêm dữ liệu vào bảng order_items
INSERT INTO order_items (order_id, product_id, quantity, price) VALUES
(1, 5, 1, 29.99),
(1, 8, 1, 89.99),
(1, 10, 1, 349.99),
(1, 7, 1, 199.99),
(1, 6, 2, 39.99),
(2, 9, 1, 699.99),
(3, 2, 1, 899.99),
(3, 5, 1, 29.99),
(3, 8, 1, 89.99),
(4, 4, 1, 1499.99);

-- Thêm dữ liệu vào bảng reviews
INSERT INTO reviews (user_id, product_id, rating, comment, created_at) VALUES
(4, 1, 5, 'Excellent phone! The camera quality is amazing.', '2023-01-25 10:15:00'),
(4, 3, 5, 'Best laptop I''ve ever owned. Fast and reliable.', '2023-01-26 14:30:00'),
(4, 7, 4, 'Good mixer, but a bit noisy.', '2023-01-27 16:45:00'),
(5, 2, 4, 'Great phone, but battery life could be better.', '2023-02-15 09:20:00'),
(5, 6, 5, 'Perfect fit and very comfortable!', '2023-02-16 11:10:00'),
(5, 9, 4, 'Comfortable sofa, but delivery took longer than expected.', '2023-02-17 13:25:00');

-- Thêm dữ liệu vào bảng review_images
INSERT INTO review_images (review_id, image_url) VALUES
(1, 'review_iphone_1.jpg'),
(1, 'review_iphone_2.jpg'),
(2, 'review_dell_1.jpg'),
(4, 'review_samsung_1.jpg'),
(5, 'review_dress_1.jpg'),
(5, 'review_dress_2.jpg');