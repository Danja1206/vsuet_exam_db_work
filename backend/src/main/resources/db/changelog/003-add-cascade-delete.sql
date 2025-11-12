-- 1. Категории → подкатегории
ALTER TABLE categories
DROP CONSTRAINT IF EXISTS categories_parent_category_id_fkey,
    ADD CONSTRAINT categories_parent_category_id_fkey
        FOREIGN KEY (parent_category_id)
        REFERENCES categories (category_id)
        ON DELETE CASCADE;

-- 2. Товары → продавцы
ALTER TABLE products
DROP CONSTRAINT IF EXISTS products_seller_id_fkey,
    ADD CONSTRAINT products_seller_id_fkey
        FOREIGN KEY (seller_id)
        REFERENCES sellers (seller_id)
        ON DELETE CASCADE;

-- 3. Товары → категории
ALTER TABLE products
DROP CONSTRAINT IF EXISTS products_category_id_fkey,
    ADD CONSTRAINT products_category_id_fkey
        FOREIGN KEY (category_id)
        REFERENCES categories (category_id)
        ON DELETE CASCADE;

-- 4. Заказы → покупатели
ALTER TABLE orders
DROP CONSTRAINT IF EXISTS orders_customer_id_fkey,
    ADD CONSTRAINT orders_customer_id_fkey
        FOREIGN KEY (customer_id)
        REFERENCES customers (customer_id)
        ON DELETE CASCADE;

-- 5. Позиции заказа → товары
ALTER TABLE order_items
DROP CONSTRAINT IF EXISTS order_items_product_id_fkey,
    ADD CONSTRAINT order_items_product_id_fkey
        FOREIGN KEY (product_id)
        REFERENCES products (product_id)
        ON DELETE CASCADE;

-- 6. Отзывы → товары
ALTER TABLE reviews
DROP CONSTRAINT IF EXISTS reviews_product_id_fkey,
    ADD CONSTRAINT reviews_product_id_fkey
        FOREIGN KEY (product_id)
        REFERENCES products (product_id)
        ON DELETE CASCADE;

-- 7. Отзывы → покупатели
ALTER TABLE reviews
DROP CONSTRAINT IF EXISTS reviews_customer_id_fkey,
    ADD CONSTRAINT reviews_customer_id_fkey
        FOREIGN KEY (customer_id)
        REFERENCES customers (customer_id)
        ON DELETE CASCADE;
