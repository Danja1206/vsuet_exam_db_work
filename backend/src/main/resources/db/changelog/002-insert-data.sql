-- Продавцы
INSERT INTO sellers (company_name, contact_email, contact_phone, tax_identification_number)
VALUES
    ('ООО Электроникс', 'contact@electronics.ru', '+79990001101', '1234567890'),
    ('ИП Иванов', 'ip-ivanov@mail.ru', '+79990001102', '0987654321');

-- Покупатели
INSERT INTO customers (first_name, last_name, email, phone_number, registration_date)
VALUES
    ('Пётр', 'Сергеев', 'petr.s@yandex.ru', '+79991112233', CURRENT_TIMESTAMP),
    ('Анна', 'Козлова', 'anna.k@gmail.com', '+79994445566', CURRENT_TIMESTAMP);

-- Категории
INSERT INTO categories (category_name, parent_category_id)
VALUES
    ('электроника', NULL),
    ('смартфоны', 1),
    ('бытовая техника', NULL),
    ('холодильники', 3);

-- Товары
INSERT INTO products (seller_id, category_id, product_name, description, price)
VALUES
    (1, 2, 'Смартфон SuperPhone X', 'Новейший флагман с потрясающей камерой.', 79999.99),
    (1, 2, 'Смартфон BasicPhone 10', 'Надёжный телефон для повседневных задач.', 24999.50),
    (2, 4, 'Холодильник "Морозко"', 'Двухкамерный холодильник с системой No Frost.', 45999.00);

-- Склад
INSERT INTO warehouse (product_id, quantity_in_stock, last_restock_date)
VALUES
    (1, 15, CURRENT_TIMESTAMP),
    (2, 30, CURRENT_TIMESTAMP),
    (3, 5, CURRENT_TIMESTAMP);

-- Заказы
INSERT INTO orders (customer_id, total_amount, status, shipping_address)
VALUES
    (1, 104999.49, 'формируется', 'г. Москва, ул. Примерная, д. 10, кв. 25');

-- Позиции заказов
INSERT INTO order_items (order_id, product_id, quantity, unit_price)
VALUES
    (1, 1, 1, 79999.99),
    (1, 2, 1, 24999.50);

-- Отзывы
INSERT INTO reviews (product_id, customer_id, rating, comment_text)
VALUES
    (1, 2, 5, 'Отличный телефон! Батарея держит очень долго. Рекомендую!');
