import React, { useState, useEffect } from 'react';
import { Table, Button, Space, Card, Tabs, message, Modal, Tag, Typography } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons';
import { Seller, Product, Order, OrderItem, Review, Warehouse, Customer } from '../../types';
import { entityApi } from '../services/api';
import { SellerForm, ProductForm } from './DataForms';

const { TabPane } = Tabs;
const { Text } = Typography;

export const EntityManager: React.FC = () => {
    const [activeTab, setActiveTab] = useState('sellers');
    const [sellers, setSellers] = useState<Seller[]>([]);
    const [products, setProducts] = useState<Product[]>([]);
    const [orders, setOrders] = useState<Order[]>([]);
    const [orderItems, setOrderItems] = useState<OrderItem[]>([]);
    const [reviews, setReviews] = useState<Review[]>([]);
    const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
    const [customers, setCustomers] = useState<Customer[]>([]);
    const [loading, setLoading] = useState(false);
    const [modalVisible, setModalVisible] = useState(false);
    const [editingRecord, setEditingRecord] = useState<any>(null);

    useEffect(() => {
        loadData();
    }, [activeTab]);

    const loadData = async () => {
        setLoading(true);
        try {
            switch (activeTab) {
                case 'sellers':
                    const sellersData = await entityApi.getSellers();
                    setSellers(sellersData);
                    break;
                case 'products':
                    const productsData = await entityApi.getProducts();
                    setProducts(productsData);
                    break;
                case 'orders':
                    const ordersData = await entityApi.getOrders();
                    setOrders(ordersData);
                    break;
                case 'order-items':
                    const orderItemsData = await entityApi.getOrderItems();
                    setOrderItems(orderItemsData);
                    break;
                case 'reviews':
                    const reviewsData = await entityApi.getReviews();
                    setReviews(reviewsData);
                    break;
                case 'warehouse':
                    const warehouseData = await entityApi.getWarehouse();
                    setWarehouses(warehouseData);
                    break;
                case 'customers':
                    const customersData = await entityApi.getCustomers();
                    setCustomers(customersData);
                    break;
            }
        } catch (error) {
            console.error('Error loading data:', error);
            message.error('Ошибка загрузки данных');
        } finally {
            setLoading(false);
        }
    };

    const handleAdd = () => {
        setEditingRecord(null);
        setModalVisible(true);
    };

    const handleEdit = (record: any) => {
        setEditingRecord(record);
        setModalVisible(true);
    };

    const handleDelete = async (id: number, entityType: string) => {
        Modal.confirm({
            title: 'Подтверждение удаления',
            content: 'Вы уверены, что хотите удалить эту запись?',
            onOk: async () => {
                try {
                    switch (entityType) {
                        case 'sellers':
                            await entityApi.deleteSeller(id);
                            message.success('Продавец удален');
                            break;
                        case 'products':
                            await entityApi.deleteProduct(id);
                            message.success('Товар удален');
                            break;
                        case 'orders':
                            await entityApi.deleteOrder(id);
                            message.success('Заказ удален');
                            break;
                        case 'order-items':
                            await entityApi.deleteOrderItem(id);
                            message.success('Позиция заказа удалена');
                            break;
                        case 'reviews':
                            await entityApi.deleteReview(id);
                            message.success('Отзыв удален');
                            break;
                        case 'warehouse':
                            await entityApi.deleteWarehouse(id);
                            message.success('Запись склада удалена');
                            break;
                        case 'customers':
                            await entityApi.deleteCustomer(id);
                            message.success('Клиент удален');
                            break;
                    }
                    loadData();
                } catch (error) {
                    console.error('Delete error:', error);
                    message.error('Ошибка при удалении');
                }
            },
        });
    };

    const handleModalClose = () => {
        setModalVisible(false);
        setEditingRecord(null);
    };

    const handleSave = () => {
        setModalVisible(false);
        setEditingRecord(null);
        loadData();
    };

    const sellerColumns = [
        { title: 'ID', dataIndex: 'sellerId', key: 'sellerId' },
        { title: 'Компания', dataIndex: 'companyName', key: 'companyName' },
        { title: 'Email', dataIndex: 'contactEmail', key: 'contactEmail' },
        { title: 'Телефон', dataIndex: 'contactPhone', key: 'contactPhone' },
        { title: 'ИНН', dataIndex: 'taxIdentificationNumber', key: 'taxIdentificationNumber' },
        {
            title: 'Активен',
            dataIndex: 'isActive',
            key: 'isActive',
            render: (value: boolean) => value ?
                <Tag icon={<CheckCircleOutlined />} color="success">Да</Tag> :
                <Tag icon={<CloseCircleOutlined />} color="error">Нет</Tag>
        },
        {
            title: 'Действия',
            key: 'actions',
            render: (_, record) => (
                <Space>
                    <Button
                        type="primary"
                        icon={<EditOutlined />}
                        onClick={() => handleEdit(record)}
                        size="small"
                    >
                        Редактировать
                    </Button>
                    <Button
                        danger
                        icon={<DeleteOutlined />}
                        onClick={() => handleDelete(record.sellerId, 'sellers')}
                        size="small"
                    >
                        Удалить
                    </Button>
                </Space>
            ),
        },
    ];

    const productColumns = [
        { title: 'ID', dataIndex: 'productId', key: 'productId' },
        { title: 'Название', dataIndex: 'productName', key: 'productName' },
        {
            title: 'Цена',
            dataIndex: 'price',
            key: 'price',
            render: (value: number) => `₽${value.toFixed(2)}`
        },
        { title: 'Описание', dataIndex: 'description', key: 'description', ellipsis: true },
        {
            title: 'Продавец',
            dataIndex: ['seller', 'companyName'],
            key: 'seller',
            render: (_, record) => record.seller?.companyName || record.sellerId
        },
        {
            title: 'Действия',
            key: 'actions',
            render: (_, record) => (
                <Space>
                    <Button
                        type="primary"
                        icon={<EditOutlined />}
                        onClick={() => handleEdit(record)}
                        size="small"
                    >
                        Редактировать
                    </Button>
                    <Button
                        danger
                        icon={<DeleteOutlined />}
                        onClick={() => handleDelete(record.productId, 'products')}
                        size="small"
                    >
                        Удалить
                    </Button>
                </Space>
            ),
        },
    ];

    const orderColumns = [
        { title: 'ID', dataIndex: 'orderId', key: 'orderId' },
        {
            title: 'Дата',
            dataIndex: 'orderDate',
            key: 'orderDate',
            render: (value: string) => new Date(value).toLocaleDateString()
        },
        {
            title: 'Статус',
            dataIndex: 'status',
            key: 'status',
            render: (status: string) => {
                let color = 'blue';
                if (status === 'Доставлен') color = 'green';
                if (status === 'Отменен') color = 'red';
                return <Tag color={color}>{status}</Tag>;
            }
        },
        {
            title: 'Сумма',
            dataIndex: 'totalAmount',
            key: 'totalAmount',
            render: (value: number) => `₽${value.toFixed(2)}`
        },
        {
            title: 'Адрес доставки',
            dataIndex: 'shippingAddress',
            key: 'shippingAddress',
            ellipsis: true
        },
        {
            title: 'Действия',
            key: 'actions',
            render: (_, record) => (
                <Space>
                    <Button
                        type="primary"
                        icon={<EditOutlined />}
                        onClick={() => handleEdit(record)}
                        size="small"
                        disabled
                    >
                        Редактировать
                    </Button>
                    <Button
                        danger
                        icon={<DeleteOutlined />}
                        onClick={() => handleDelete(record.orderId, 'orders')}
                        size="small"
                    >
                        Удалить
                    </Button>
                </Space>
            ),
        },
    ];

    const orderItemColumns = [
        { title: 'ID', dataIndex: 'orderItemId', key: 'orderItemId' },
        {
            title: 'Заказ',
            dataIndex: ['order', 'orderId'],
            key: 'orderId',
            render: (_, record) => record.order?.orderId || record.orderId
        },
        {
            title: 'Товар',
            dataIndex: ['product', 'productName'],
            key: 'productId',
            render: (_, record) => record.product?.productName || record.productId
        },
        { title: 'Количество', dataIndex: 'quantity', key: 'quantity' },
        {
            title: 'Цена за ед.',
            dataIndex: 'unitPrice',
            key: 'unitPrice',
            render: (value: number) => `₽${value.toFixed(2)}`
        },
        {
            title: 'Общая стоимость',
            key: 'total',
            render: (_, record) => {
                const total = record.quantity * record.unitPrice;
                return `₽${total.toFixed(2)}`;
            }
        },
        {
            title: 'Действия',
            key: 'actions',
            render: (_, record) => (
                <Space>
                    <Button
                        type="primary"
                        icon={<EditOutlined />}
                        onClick={() => handleEdit(record)}
                        size="small"
                        disabled
                    >
                        Редактировать
                    </Button>
                    <Button
                        danger
                        icon={<DeleteOutlined />}
                        onClick={() => handleDelete(record.orderItemId, 'order-items')}
                        size="small"
                    >
                        Удалить
                    </Button>
                </Space>
            ),
        },
    ];

    const reviewColumns = [
        { title: 'ID', dataIndex: 'reviewId', key: 'reviewId' },
        {
            title: 'Товар',
            dataIndex: ['product', 'productName'],
            key: 'productId',
            render: (_, record) => record.product?.productName || record.productId
        },
        {
            title: 'Клиент',
            dataIndex: ['customer', 'name'],
            key: 'customerId',
            render: (_, record) => record.customer?.name || record.customerId
        },
        {
            title: 'Рейтинг',
            dataIndex: 'rating',
            key: 'rating',
            render: (rating: number) => (
                <Space>
                    {Array.from({ length: 5 }, (_, i) => (
                        <span key={i} style={{ color: i < rating ? '#fadb14' : '#d9d9d9' }}>★</span>
                    ))}
                    <span>({rating})</span>
                </Space>
            )
        },
        {
            title: 'Комментарий',
            dataIndex: 'commentText',
            key: 'commentText',
            ellipsis: true,
            render: (text: string) => text || <span style={{ color: '#888' }}>Без комментария</span>
        },
        {
            title: 'Дата',
            dataIndex: 'reviewDate',
            key: 'reviewDate',
            render: (value: string) => new Date(value).toLocaleDateString()
        },
        {
            title: 'Опубликован',
            dataIndex: 'isPublished',
            key: 'isPublished',
            render: (value: boolean) => value ?
                <Tag icon={<CheckCircleOutlined />} color="success">Да</Tag> :
                <Tag icon={<CloseCircleOutlined />} color="error">Нет</Tag>
        },
        {
            title: 'Действия',
            key: 'actions',
            render: (_, record) => (
                <Space>
                    <Button
                        type="primary"
                        icon={<EditOutlined />}
                        onClick={() => handleEdit(record)}
                        size="small"
                    >
                        Редактировать
                    </Button>
                    <Button
                        danger
                        icon={<DeleteOutlined />}
                        onClick={() => handleDelete(record.reviewId, 'reviews')}
                        size="small"
                    >
                        Удалить
                    </Button>
                </Space>
            ),
        },
    ];

    const warehouseColumns = [
        { title: 'ID', dataIndex: 'warehouseId', key: 'warehouseId' },
        {
            title: 'Товар',
            dataIndex: ['product', 'productName'],
            key: 'productId',
            render: (_, record) => record.product?.productName || record.productId
        },
        {
            title: 'Количество на складе',
            dataIndex: 'quantityInStock',
            key: 'quantityInStock',
            render: (value: number) => {
                let color = 'green';
                if (value < 10) color = 'orange';
                if (value === 0) color = 'red';
                return <Tag color={color}>{value} шт.</Tag>;
            }
        },
        {
            title: 'Последнее пополнение',
            dataIndex: 'lastRestockDate',
            key: 'lastRestockDate',
            render: (value: string) => value ? new Date(value).toLocaleDateString() : <span style={{ color: '#888' }}>Не указано</span>
        },
        {
            title: 'Действия',
            key: 'actions',
            render: (_, record) => (
                <Space>
                    <Button
                        type="primary"
                        icon={<EditOutlined />}
                        onClick={() => handleEdit(record)}
                        size="small"
                    >
                        Редактировать
                    </Button>
                    <Button
                        danger
                        icon={<DeleteOutlined />}
                        onClick={() => handleDelete(record.warehouseId, 'warehouse')}
                        size="small"
                    >
                        Удалить
                    </Button>
                </Space>
            ),
        },
    ];

    const customerColumns = [
        { title: 'ID', dataIndex: 'customerId', key: 'customerId' },
        {
            title: 'Имя',
            dataIndex: 'firstName',
            key: 'firstName',
            render: (firstName: string, record: Customer) => (
                <Space direction="vertical" size={0}>
                    <Text strong>{firstName} {record.lastName}</Text>
                    <Text type="secondary" style={{ fontSize: '12px' }}>
                        ID: {record.customerId}
                    </Text>
                </Space>
            )
        },
        { title: 'Email', dataIndex: 'email', key: 'email', ellipsis: true },
        { title: 'Телефон', dataIndex: 'phoneNumber', key: 'phoneNumber', render: (value: string) => value || <span style={{ color: '#888' }}>Не указан</span> },
        {
            title: 'Регистрация',
            dataIndex: 'registrationDate',
            key: 'registrationDate',
            render: (value: string) => new Date(value).toLocaleDateString()
        },
        {
            title: 'Адрес',
            dataIndex: 'address',
            key: 'address',
            render: (_, record: Customer) => {
                if (!record.address && !record.city && !record.postalCode) return <span style={{ color: '#888' }}>Не указан</span>;
                return (
                    <div>
                        {record.address && <div>{record.address}</div>}
                        {(record.city || record.postalCode) && (
                            <div>
                                {record.city && `${record.city}`}
                                {record.city && record.postalCode && ', '}
                                {record.postalCode && `${record.postalCode}`}
                            </div>
                        )}
                    </div>
                );
            }
        },
        {
            title: 'Активен',
            dataIndex: 'isActive',
            key: 'isActive',
            render: (value: boolean) => value ?
                <Tag icon={<CheckCircleOutlined />} color="success">Да</Tag> :
                <Tag icon={<CloseCircleOutlined />} color="error">Нет</Tag>
        },
        {
            title: 'Действия',
            key: 'actions',
            render: (_, record) => (
                <Space>
                    <Button
                        type="primary"
                        icon={<EditOutlined />}
                        onClick={() => handleEdit(record)}
                        size="small"
                    >
                        Редактировать
                    </Button>
                    <Button
                        danger
                        icon={<DeleteOutlined />}
                        onClick={() => handleDelete(record.customerId, 'customers')}
                        size="small"
                    >
                        Удалить
                    </Button>
                </Space>
            ),
        },
    ];

    const renderForm = () => {
        switch (activeTab) {
            case 'sellers':
                return <SellerForm seller={editingRecord} onSave={handleSave} onCancel={handleModalClose} />;
            case 'products':
                return <ProductForm product={editingRecord} onSave={handleSave} onCancel={handleModalClose} />;
            case 'reviews':
                return <div>Форма для редактирования отзывов будет реализована позже</div>;
            case 'order-items':
                return <div>Форма для редактирования позиций заказа будет реализована позже</div>;
            case 'warehouse':
                return <div>Форма для редактирования склада будет реализована позже</div>;
            case 'customers':
                return <div>Форма для редактирования клиентов будет реализована позже</div>;
            default:
                return <div>Редактирование для этого раздела не поддерживается</div>;
        }
    };

    const canAddRecord = () => {
        return activeTab === 'sellers' || activeTab === 'products';
    };

    return (
        <div style={{ padding: '20px' }}>
            <Card>
                <Tabs activeKey={activeTab} onChange={setActiveTab}>
                    <TabPane tab="Продавцы" key="sellers">
                        {canAddRecord() && (
                            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd} style={{ marginBottom: 16 }}>
                                Добавить продавца
                            </Button>
                        )}
                        <Table
                            columns={sellerColumns}
                            dataSource={sellers}
                            loading={loading}
                            rowKey="sellerId"
                            pagination={{ pageSize: 10, showSizeChanger: true }}
                            scroll={{ x: true }}
                        />
                    </TabPane>

                    <TabPane tab="Товары" key="products">
                        {canAddRecord() && (
                            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd} style={{ marginBottom: 16 }}>
                                Добавить товар
                            </Button>
                        )}
                        <Table
                            columns={productColumns}
                            dataSource={products}
                            loading={loading}
                            rowKey="productId"
                            pagination={{ pageSize: 10, showSizeChanger: true }}
                            scroll={{ x: true }}
                        />
                    </TabPane>

                    <TabPane tab="Заказы" key="orders">
                        <Table
                            columns={orderColumns}
                            dataSource={orders}
                            loading={loading}
                            rowKey="orderId"
                            pagination={{ pageSize: 10, showSizeChanger: true }}
                            scroll={{ x: true }}
                        />
                    </TabPane>

                    <TabPane tab="Позиции заказов" key="order-items">
                        <Table
                            columns={orderItemColumns}
                            dataSource={orderItems}
                            loading={loading}
                            rowKey="orderItemId"
                            pagination={{ pageSize: 10, showSizeChanger: true }}
                            scroll={{ x: true }}
                        />
                    </TabPane>

                    <TabPane tab="Отзывы" key="reviews">
                        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd} style={{ marginBottom: 16 }}>
                            Добавить отзыв
                        </Button>
                        <Table
                            columns={reviewColumns}
                            dataSource={reviews}
                            loading={loading}
                            rowKey="reviewId"
                            pagination={{ pageSize: 10, showSizeChanger: true }}
                            scroll={{ x: true }}
                        />
                    </TabPane>

                    <TabPane tab="Склад" key="warehouse">
                        <Table
                            columns={warehouseColumns}
                            dataSource={warehouses}
                            loading={loading}
                            rowKey="warehouseId"
                            pagination={{ pageSize: 10, showSizeChanger: true }}
                            scroll={{ x: true }}
                        />
                    </TabPane>

                    <TabPane tab="Клиенты" key="customers">
                        <Table
                            columns={customerColumns}
                            dataSource={customers}
                            loading={loading}
                            rowKey="customerId"
                            pagination={{ pageSize: 10, showSizeChanger: true }}
                            scroll={{ x: true }}
                        />
                    </TabPane>
                </Tabs>
            </Card>

            <Modal
                title={editingRecord ? 'Редактирование' : 'Создание'}
                open={modalVisible}
                onCancel={handleModalClose}
                footer={null}
                width={600}
            >
                {renderForm()}
            </Modal>
        </div>
    );
};

export default EntityManager;
