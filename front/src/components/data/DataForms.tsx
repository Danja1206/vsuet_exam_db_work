import React, { useState, useEffect } from 'react';
import { Card, Form, Input, InputNumber, Button, Select, Switch, message, Space, Table, Spin } from 'antd';
import { Seller, Product, Order, OrderItem } from '../../types';
import { entityApi } from '../services/api';

const { Option } = Select;
const { TextArea } = Input;

interface SellerFormProps {
    seller?: Seller;
    onSave: () => void;
    onCancel: () => void;
}

export const SellerForm: React.FC<SellerFormProps> = ({ seller, onSave, onCancel }) => {
    const [form] = Form.useForm();
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (seller) {
            form.setFieldsValue(seller);
        }
    }, [seller, form]);

    const onFinish = async (values: any) => {
        setLoading(true);
        try {
            if (seller) {
                await entityApi.updateSeller(seller.sellerId, values);
                message.success('Продавец обновлен');
            } else {
                await entityApi.createSeller(values);
                message.success('Продавец создан');
            }
            onSave();
        } catch (error) {
            message.error('Ошибка при сохранении');
        } finally {
            setLoading(false);
        }
    };

    return (
        <Card title={seller ? 'Редактирование продавца' : 'Новый продавец'}>
            <Form form={form} layout="vertical" onFinish={onFinish}>
                <Form.Item name="companyName" label="Название компании" rules={[{ required: true }]}>
                    <Input />
                </Form.Item>

                <Form.Item name="contactEmail" label="Email" rules={[{ required: true, type: 'email' }]}>
                    <Input />
                </Form.Item>

                <Form.Item name="contactPhone" label="Телефон">
                    <Input />
                </Form.Item>

                <Form.Item name="taxIdentificationNumber" label="ИНН" rules={[{ required: true }]}>
                    <Input />
                </Form.Item>

                <Form.Item name="isActive" label="Активен" valuePropName="checked">
                    <Switch defaultChecked={true} />
                </Form.Item>

                <Form.Item>
                    <Space>
                        <Button type="primary" htmlType="submit" loading={loading}>
                            Сохранить
                        </Button>
                        <Button onClick={onCancel}>
                            Отмена
                        </Button>
                    </Space>
                </Form.Item>
            </Form>
        </Card>
    );
};

interface ProductFormProps {
    product?: Product;
    onSave: () => void;
    onCancel: () => void;
}

export const ProductForm: React.FC<ProductFormProps> = ({ product, onSave, onCancel }) => {
    const [form] = Form.useForm();
    const [loading, setLoading] = useState(false);
    const [sellers, setSellers] = useState<Seller[]>([]);
    const [categories, setCategories] = useState<any[]>([]); // Категории товаров
    const [loadingSellers, setLoadingSellers] = useState(true);
    const [loadingCategories, setLoadingCategories] = useState(true);

    useEffect(() => {
        loadSellers();
        loadCategories();
        if (product) {
            form.setFieldsValue({
                ...product,
                // Убеждаемся, что categoryId правильно установлен при редактировании
                categoryId: product.categoryId
            });
        }
    }, [product, form]);

    const loadSellers = async () => {
        setLoadingSellers(true);
        try {
            const data = await entityApi.getSellers();
            setSellers(data);
        } catch (error) {
            console.error('Ошибка загрузки продавцов:', error);
            message.error('Ошибка загрузки продавцов');
            setSellers([]);
        } finally {
            setLoadingSellers(false);
        }
    };

    const loadCategories = async () => {
        setLoadingCategories(true);
        try {
            const data = await entityApi.getCategories();
            setCategories(data);
        } catch (error) {
            console.error('Ошибка загрузки категорий:', error);
            message.error('Ошибка загрузки категорий');
            setCategories([]);
        } finally {
            setLoadingCategories(false);
        }
    };

    const onFinish = async (values: any) => {
        setLoading(true);
        try {
            // Данные уже содержат categoryId как значение, так как мы используем name="categoryId"
            const productData = {
                ...values,
                // categoryId уже содержит правильный ID благодаря Form.Item с name="categoryId"
            };

            if (product) {
                await entityApi.updateProduct(product.productId, productData);
                message.success('Товар обновлен');
            } else {
                await entityApi.createProduct(productData);
                message.success('Товар создан');
            }
            onSave();
        } catch (error) {
            console.error('Ошибка при сохранении товара:', error);
            message.error('Ошибка при сохранении');
        } finally {
            setLoading(false);
        }
    };

    return (
        <Card title={product ? 'Редактирование товара' : 'Новый товар'}>
            <Form form={form} layout="vertical" onFinish={onFinish} initialValues={{ categoryId: 1 }}>
                <Form.Item name="sellerId" label="Продавец" rules={[{ required: true, message: 'Пожалуйста, выберите продавца' }]}>
                    <Select
                        loading={loadingSellers}
                        placeholder="Выберите продавца"
                        showSearch
                        optionFilterProp="children"
                        filterOption={(input, option) =>
                            (option?.children as string).toLowerCase().includes(input.toLowerCase())
                        }
                    >
                        {sellers.length === 0 ? (
                            <Option disabled>Нет доступных продавцов</Option>
                        ) : (
                            sellers.map(seller => (
                                <Option key={seller.sellerId} value={seller.sellerId}>
                                    {seller.companyName}
                                </Option>
                            ))
                        )}
                    </Select>
                </Form.Item>

                <Form.Item
                    name="categoryId"
                    label="Категория"
                    rules={[{ required: true, message: 'Пожалуйста, выберите категорию' }]}
                >
                    <Select
                        loading={loadingCategories}
                        placeholder="Выберите категорию"
                        showSearch
                        optionFilterProp="children"
                        filterOption={(input, option) =>
                            (option?.children as string).toLowerCase().includes(input.toLowerCase())
                        }
                    >
                        {categories.length === 0 ? (
                            <Option disabled>Нет доступных категорий</Option>
                        ) : (
                            categories.map(category => (
                                <Option
                                    key={category.categoryId}
                                    value={category.categoryId}
                                >
                                    {category.categoryName}
                                </Option>
                            ))
                        )}
                    </Select>
                </Form.Item>

                <Form.Item name="productName" label="Название товара" rules={[{ required: true }]}>
                    <Input />
                </Form.Item>

                <Form.Item name="description" label="Описание">
                    <TextArea rows={4} />
                </Form.Item>

                <Form.Item
                    name="price"
                    label="Цена"
                    rules={[
                        { required: true, message: 'Пожалуйста, укажите цену' },
                        { type: 'number', min: 0, message: 'Цена не может быть отрицательной' }
                    ]}
                >
                    <InputNumber
                        min={0}
                        style={{ width: '100%' }}
                        formatter={value => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ' ')}
                        parser={value => value!.replace(/\s?/g, '')}
                    />
                </Form.Item>

                <Form.Item>
                    <Space>
                        <Button type="primary" htmlType="submit" loading={loading}>
                            Сохранить
                        </Button>
                        <Button onClick={onCancel}>
                            Отмена
                        </Button>
                    </Space>
                </Form.Item>
            </Form>
        </Card>
    );
};

interface OrderItemFormProps {
    orderItem?: OrderItem;
    onSave: () => void;
    onCancel: () => void;
}

export const OrderItemForm: React.FC<OrderItemFormProps> = ({ orderItem, onSave, onCancel }) => {
    const [form] = Form.useForm();
    const [loading, setLoading] = useState(false);
    const [orders, setOrders] = useState<Order[]>([]);
    const [products, setProducts] = useState<Product[]>([]);
    const [loadingOrders, setLoadingOrders] = useState(true);
    const [loadingProducts, setLoadingProducts] = useState(true);

    useEffect(() => {
        loadOrders();
        loadProducts();
        if (orderItem) {
            form.setFieldsValue({
                ...orderItem,
                orderId: orderItem.order?.orderId || orderItem.orderId,
                productId: orderItem.product?.productId || orderItem.productId
            });
        }
    }, [orderItem, form]);

    const loadOrders = async () => {
        setLoadingOrders(true);
        try {
            const data = await entityApi.getOrders();
            setOrders(data);
        } catch (error) {
            console.error('Ошибка загрузки заказов:', error);
            message.error('Ошибка загрузки заказов');
            setOrders([]);
        } finally {
            setLoadingOrders(false);
        }
    };

    const loadProducts = async () => {
        setLoadingProducts(true);
        try {
            const data = await entityApi.getProducts();
            setProducts(data);
        } catch (error) {
            console.error('Ошибка загрузки товаров:', error);
            message.error('Ошибка загрузки товаров');
            setProducts([]);
        } finally {
            setLoadingProducts(false);
        }
    };

    const onFinish = async (values: any) => {
        setLoading(true);
        try {
            const orderItemData = {
                ...values,
                orderId: values.orderId,
                productId: values.productId
            };

            if (orderItem) {
                await entityApi.updateOrderItem(orderItem.orderItemId, orderItemData);
                message.success('Позиция заказа обновлена');
            } else {
                await entityApi.createOrderItem(orderItemData);
                message.success('Позиция заказа создана');
            }
            onSave();
        } catch (error) {
            console.error('Ошибка при сохранении позиции заказа:', error);
            message.error('Ошибка при сохранении');
        } finally {
            setLoading(false);
        }
    };

    return (
        <Card title={orderItem ? 'Редактирование позиции заказа' : 'Новая позиция заказа'}>
            <Form form={form} layout="vertical" onFinish={onFinish}>
                <Form.Item
                    name="orderId"
                    label="Заказ"
                    rules={[{ required: true, message: 'Пожалуйста, выберите заказ' }]}
                >
                    <Select
                        loading={loadingOrders}
                        placeholder="Выберите заказ"
                        showSearch
                        optionFilterProp="children"
                        filterOption={(input, option) =>
                            (option?.children as string).toLowerCase().includes(input.toLowerCase())
                        }
                        disabled={!!orderItem} // Запрещаем изменение заказа при редактировании
                    >
                        {orders.length === 0 ? (
                            <Option disabled>Нет доступных заказов</Option>
                        ) : (
                            orders.map(order => (
                                <Option key={order.orderId} value={order.orderId}>
                                    Заказ #{order.orderId} от {new Date(order.orderDate).toLocaleDateString()}
                                </Option>
                            ))
                        )}
                    </Select>
                </Form.Item>

                <Form.Item
                    name="productId"
                    label="Товар"
                    rules={[{ required: true, message: 'Пожалуйста, выберите товар' }]}
                >
                    <Select
                        loading={loadingProducts}
                        placeholder="Выберите товар"
                        showSearch
                        optionFilterProp="children"
                        filterOption={(input, option) =>
                            (option?.children as string).toLowerCase().includes(input.toLowerCase())
                        }
                    >
                        {products.length === 0 ? (
                            <Option disabled>Нет доступных товаров</Option>
                        ) : (
                            products.map(product => (
                                <Option key={product.productId} value={product.productId}>
                                    {product.productName} (₽{product.price.toFixed(2)})
                                </Option>
                            ))
                        )}
                    </Select>
                </Form.Item>

                <Form.Item
                    name="quantity"
                    label="Количество"
                    rules={[
                        { required: true, message: 'Укажите количество' },
                        { type: 'number', min: 1, message: 'Количество должно быть больше 0' }
                    ]}
                >
                    <InputNumber
                        min={1}
                        style={{ width: '100%' }}
                    />
                </Form.Item>

                <Form.Item
                    name="unitPrice"
                    label="Цена за единицу"
                    rules={[
                        { required: true, message: 'Укажите цену за единицу' },
                        { type: 'number', min: 0.01, message: 'Цена должна быть положительной' }
                    ]}
                >
                    <InputNumber
                        min={0.01}
                        step={0.01}
                        style={{ width: '100%' }}
                        formatter={value => `₽ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ' ')}
                        parser={value => value!.replace(/\₽\s?|( )/g, '')}
                    />
                </Form.Item>

                <Form.Item>
                    <Space>
                        <Button type="primary" htmlType="submit" loading={loading}>
                            Сохранить
                        </Button>
                        <Button onClick={onCancel}>
                            Отмена
                        </Button>
                    </Space>
                </Form.Item>
            </Form>
        </Card>
    );
};
