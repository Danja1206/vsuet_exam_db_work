-- 1. Продавцы
CREATE TABLE sellers
(
    seller_id                BIGSERIAL PRIMARY KEY,
    company_name             VARCHAR(255) NOT NULL,
    contact_email            VARCHAR(100) NOT NULL,
    contact_phone            VARCHAR(20),
    tax_identification_number VARCHAR(20)  NOT NULL UNIQUE,
    registration_date        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active                BOOLEAN      NOT NULL DEFAULT TRUE
);

-- 2. Покупатели
CREATE TABLE customers
(
    customer_id       BIGSERIAL PRIMARY KEY,
    first_name        VARCHAR(50)  NOT NULL,
    last_name         VARCHAR(50)  NOT NULL,
    email             VARCHAR(100) NOT NULL UNIQUE,
    phone_number      VARCHAR(20),
    registration_date TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    address           VARCHAR(500),
    city              VARCHAR(100),
    postal_code       VARCHAR(20),
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE
);

-- 3. Категории
CREATE TABLE categories
(
    category_id        BIGSERIAL PRIMARY KEY,
    category_name      VARCHAR(100) NOT NULL,
    parent_category_id BIGINT NULL,
    FOREIGN KEY (parent_category_id) REFERENCES categories (category_id)
);

-- 4. Товары
CREATE TABLE products
(
    product_id    BIGSERIAL PRIMARY KEY,
    seller_id     BIGINT         NOT NULL,
    category_id   BIGINT         NOT NULL,
    product_name  VARCHAR(255)   NOT NULL,
    description   TEXT,
    price         DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
    created_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (seller_id) REFERENCES sellers (seller_id),
    FOREIGN KEY (category_id) REFERENCES categories (category_id)
);

-- 5. Заказы
CREATE TABLE orders
(
    order_id          BIGSERIAL PRIMARY KEY,
    customer_id       BIGINT         NOT NULL,
    order_date        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status            VARCHAR(50)    NOT NULL DEFAULT 'Оформлен',
    total_amount      DECIMAL(10, 2) NOT NULL CHECK (total_amount >= 0),
    shipping_address  VARCHAR(500)   NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customers (customer_id)
);

-- 6. Позиции заказа
CREATE TABLE order_items
(
    order_item_id BIGSERIAL PRIMARY KEY,
    order_id      BIGINT         NOT NULL,
    product_id    BIGINT         NOT NULL,
    quantity      INT            NOT NULL CHECK (quantity > 0),
    unit_price    DECIMAL(10, 2) NOT NULL CHECK (unit_price >= 0),
    FOREIGN KEY (order_id) REFERENCES orders (order_id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products (product_id)
);

-- 7. Отзывы
CREATE TABLE reviews
(
    review_id     BIGSERIAL PRIMARY KEY,
    product_id    BIGINT      NOT NULL,
    customer_id   BIGINT      NOT NULL,
    rating        INTEGER    NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment_text  TEXT,
    review_date   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_published  BOOLEAN     NOT NULL DEFAULT TRUE,
    FOREIGN KEY (product_id) REFERENCES products (product_id),
    FOREIGN KEY (customer_id) REFERENCES customers (customer_id)
);

-- 8. Склад
CREATE TABLE warehouse
(
    warehouse_id      BIGSERIAL PRIMARY KEY,
    product_id        BIGINT    NOT NULL,
    quantity_in_stock INT NOT NULL CHECK (quantity_in_stock >= 0),
    last_restock_date TIMESTAMP NULL,
    FOREIGN KEY (product_id) REFERENCES products (product_id) ON DELETE CASCADE
);

-- Индексы
CREATE INDEX idx_products_seller_id ON products (seller_id);
CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_orders_customer_id ON orders (customer_id);
CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_order_items_product_id ON order_items (product_id);
CREATE INDEX idx_reviews_product_id ON reviews (product_id);
CREATE INDEX idx_reviews_customer_id ON reviews (customer_id);
CREATE INDEX idx_sellers_tax_number ON sellers (tax_identification_number);
CREATE INDEX idx_customers_email ON customers (email);
