-- Runs once when the Postgres container is first created.
-- Each microservice gets its own database (database-per-service pattern).
CREATE DATABASE ecommerce_user_db;
CREATE DATABASE ecommerce_product_db;
CREATE DATABASE ecommerce_inventory_db;
CREATE DATABASE ecommerce_order_db;
