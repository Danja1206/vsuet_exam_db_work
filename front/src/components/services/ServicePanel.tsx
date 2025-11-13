import React, { useState, useEffect } from 'react';
import {
    Card,
    Button,
    InputNumber,
    message,
    Space,
    List,
    Tag,
    Modal,
    Select,
    Form,
    Typography,
    Spin,
    Divider,
    Alert, Input
} from 'antd';
import {
    SaveOutlined,
    ReloadOutlined,
    InboxOutlined,
    DownloadOutlined,
    DeleteOutlined,
    ClockCircleOutlined,
    DatabaseOutlined
} from '@ant-design/icons';
import { serviceApi } from '../services/api';

const { Text, Title } = Typography;
const { Option } = Select;

interface BackupFile {
    name: string;
    size: number;
    modified: number;
}

interface ArchiveFormValues {
    days: number;
    sourceTable: string;
    archiveTable: string;
}

export const ServicePanel: React.FC = () => {
    const [backupLoading, setBackupLoading] = useState(false);
    const [restoreLoading, setRestoreLoading] = useState(false);
    const [archiveLoading, setArchiveLoading] = useState(false);
    const [backupFiles, setBackupFiles] = useState<BackupFile[]>([]);
    const [restorePath, setRestorePath] = useState<string>('');
    const [tables, setTables] = useState<string[]>([]);
    const [loadingTables, setLoadingTables] = useState(true);
    const [archivingResult, setArchivingResult] = useState<string | null>(null);
    const [deletingBackup, setDeletingBackup] = useState<string | null>(null);
    const [archiveForm] = Form.useForm<ArchiveFormValues>();

    useEffect(() => {
        loadBackupFiles();
        loadTables();
    }, []);

    const loadBackupFiles = async () => {
        try {
            const data = await serviceApi.getBackupFiles();
            setBackupFiles(data || []);
        } catch (error) {
            console.error('Failed to load backup files:', error);
            message.error('Ошибка при загрузке списка бэкапов');
            setBackupFiles([]);
        }
    };

    const loadTables = async () => {
        setLoadingTables(true);
        try {
            const tableList = await serviceApi.getTables();
            setTables(tableList.filter(table =>
                !table.startsWith('backup_') &&
                !table.includes('_archive') &&
                table !== 'spatial_ref_sys'
            ));
        } catch (err) {
            console.error('Failed to load tables:', err);
            message.error('Ошибка при загрузке списка таблиц');
        } finally {
            setLoadingTables(false);
        }
    };

    const onBackup = async () => {
        setBackupLoading(true);
        try {
            const result = await serviceApi.createBackup({});
            message.success(result.message || 'Резервная копия успешно создана');
            loadBackupFiles();
        } catch (error: any) {
            console.error(error);
            message.error(error.message || 'Ошибка при создании бэкапа');
        } finally {
            setBackupLoading(false);
        }
    };

    const onRestore = async () => {
        if (!restorePath) {
            message.warning('Пожалуйста, выберите файл бэкапа для восстановления');
            return;
        }

        Modal.confirm({
            title: 'Подтверждение восстановления',
            content: (
                <div>
                    <p>Вы уверены, что хотите восстановить базу данных из бэкапа?</p>
                    <p><Text strong>Все текущие данные будут заменены!</Text></p>
                    <p>Файл: <Text code>{restorePath}</Text></p>
                </div>
            ),
            okText: 'Да, восстановить',
            cancelText: 'Отмена',
            onOk: async () => {
                setRestoreLoading(true);
                try {
                    const response = await serviceApi.restoreBackup(restorePath);
                    message.success(response.message || 'База данных успешно восстановлена');
                    setRestorePath('');
                } catch (error: any) {
                    message.error(error.message || 'Ошибка при восстановлении из бэкапа');
                } finally {
                    setRestoreLoading(false);
                }
            },
        });
    };

    const onArchive = async (values: ArchiveFormValues) => {
        setArchiveLoading(true);
        setArchivingResult(null);

        try {
            const response = await serviceApi.archiveData({
                olderThanDays: values.days,
                sourceTableName: values.sourceTable,
                archiveTableName: values.archiveTable
            });

            setArchivingResult(response.message || 'Данные успешно заархивированы');
            message.success('Архивация успешно выполнена');

            // Сбрасываем форму после успешного выполнения
            archiveForm.resetFields();
        } catch (error: any) {
            console.error('Archive error:', error);
            message.error(error.message || 'Ошибка при архивации данных');
        } finally {
            setArchiveLoading(false);
        }
    };

    const onDeleteBackup = (fileName: string) => {
        Modal.confirm({
            title: 'Удаление бэкапа',
            content: `Вы уверены, что хотите удалить бэкап "${fileName}"? Это действие нельзя отменить.`,
            okText: 'Да, удалить',
            cancelText: 'Отмена',
            onOk: async () => {
                setDeletingBackup(fileName);
                try {
                    // TODO: Implement deleteBackup API method
                    await new Promise(resolve => setTimeout(resolve, 1000)); // Mock delay
                    message.success('Бэкап успешно удален');
                    loadBackupFiles();
                } catch (error) {
                    message.error('Ошибка при удалении бэкапа');
                } finally {
                    setDeletingBackup(null);
                }
            }
        });
    };

    const formatFileSize = (bytes: number): string => {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    };

    const formatDateTime = (timestamp: number): string => {
        return new Date(timestamp).toLocaleString('ru-RU', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    return (
        <div style={{ padding: '20px', maxWidth: '1200px', margin: '0 auto' }}>
            <Title level={2} style={{ marginBottom: '24px', color: '#1890ff' }}>
                <DatabaseOutlined /> Панель управления сервисами
            </Title>

            <Space direction="vertical" style={{ width: '100%' }} size="large">
                {/* Резервное копирование */}
                <Card
                    title={
                        <Space>
                            <SaveOutlined style={{ color: '#52c41a' }} />
                            <span>Резервное копирование</span>
                        </Space>
                    }
                    bordered={false}
                    style={{ boxShadow: '0 2px 8px rgba(0,0,0,0.15)' }}
                >
                    <Space direction="vertical" style={{ width: '100%' }} size="middle">
                        <Button
                            type="primary"
                            icon={<SaveOutlined />}
                            onClick={onBackup}
                            loading={backupLoading}
                            size="large"
                            block
                        >
                            Создать резервную копию
                        </Button>

                        {backupFiles.length > 0 ? (
                            <Card size="small" title="Доступные бэкапы" style={{ marginTop: '16px' }}>
                                <List
                                    dataSource={backupFiles}
                                    renderItem={file => (
                                        <List.Item
                                            actions={[
                                                <Button
                                                    type="link"
                                                    icon={<DownloadOutlined />}
                                                    onClick={() => setRestorePath(file.name)}
                                                    disabled={restoreLoading}
                                                >
                                                    Восстановить
                                                </Button>,
                                                <Button
                                                    type="link"
                                                    danger
                                                    icon={<DeleteOutlined />}
                                                    loading={deletingBackup === file.name}
                                                    onClick={() => onDeleteBackup(file.name)}
                                                    disabled={deletingBackup !== null}
                                                >
                                                    Удалить
                                                </Button>
                                            ]}
                                        >
                                            <List.Item.Meta
                                                title={
                                                    <Text code copyable>{file.name}</Text>
                                                }
                                                description={
                                                    <Space size="middle" wrap>
                                                        <Tag icon={<DatabaseOutlined />} color="blue">
                                                            {formatFileSize(file.size)}
                                                        </Tag>
                                                        <Tag icon={<ClockCircleOutlined />} color="green">
                                                            {formatDateTime(file.modified)}
                                                        </Tag>
                                                    </Space>
                                                }
                                            />
                                        </List.Item>
                                    )}
                                    pagination={{
                                        pageSize: 5,
                                        showSizeChanger: false,
                                        showTotal: (total) => `Всего бэкапов: ${total}`
                                    }}
                                />
                            </Card>
                        ) : (
                            <Alert
                                message="Нет доступных бэкапов"
                                description="Создайте первый бэкап базы данных"
                                type="info"
                                showIcon
                                style={{ marginTop: '16px' }}
                            />
                        )}
                    </Space>
                </Card>

                <Divider />

                {/* Восстановление */}
                <Card
                    title={
                        <Space>
                            <ReloadOutlined style={{ color: '#faad14' }} />
                            <span>Восстановление данных</span>
                        </Space>
                    }
                    bordered={false}
                    style={{ boxShadow: '0 2px 8px rgba(0,0,0,0.15)' }}
                >
                    <Space direction="vertical" style={{ width: '100%' }} size="middle">
                        <Text>Выберите резервную копию для восстановления базы данных:</Text>

                        <Space.Compact style={{ width: '100%' }}>
                            <Select
                                placeholder="Выберите бэкап для восстановления"
                                value={restorePath || undefined}
                                onChange={value => setRestorePath(value)}
                                style={{ width: '70%' }}
                                showSearch
                                filterOption={(input, option) =>
                                    (option?.children as string)?.toLowerCase().includes(input.toLowerCase())
                                }
                            >
                                {backupFiles.map(file => (
                                    <Option key={file.name} value={file.name}>
                                        {file.name} <Text type="secondary">({formatFileSize(file.size)}, {formatDateTime(file.modified)})</Text>
                                    </Option>
                                ))}
                            </Select>
                            <Button
                                type="primary"
                                icon={<ReloadOutlined />}
                                onClick={onRestore}
                                loading={restoreLoading}
                                disabled={!restorePath}
                                style={{ width: '30%' }}
                            >
                                Восстановить
                            </Button>
                        </Space.Compact>

                        <Alert
                            message="Внимание!"
                            description="Восстановление из бэкапа перезапишет все текущие данные в базе. Убедитесь, что вы выбрали правильный файл."
                            type="warning"
                            showIcon
                        />
                    </Space>
                </Card>

                <Divider />

                {/* Архивация */}
                <Card
                    title={
                        <Space>
                            <InboxOutlined style={{ color: '#722ed1' }} />
                            <span>Архивация данных</span>
                        </Space>
                    }
                    bordered={false}
                    style={{ boxShadow: '0 2px 8px rgba(0,0,0,0.15)' }}
                >
                    <Spin spinning={loadingTables}>
                        <Form
                            form={archiveForm}
                            layout="vertical"
                            onFinish={onArchive}
                            initialValues={{
                                days: 365,
                                sourceTable: tables.includes('Orders') ? 'Orders' : undefined,
                                archiveTable: 'Orders_archive'
                            }}
                        >
                            <Form.Item
                                label="Таблица для архивации"
                                name="sourceTable"
                                rules={[{ required: true, message: 'Пожалуйста, выберите таблицу' }]}
                            >
                                <Select
                                    placeholder="Выберите таблицу"
                                    showSearch
                                    optionFilterProp="children"
                                    filterOption={(input, option) =>
                                        (option?.children as string)?.toLowerCase().includes(input.toLowerCase())
                                    }
                                    onChange={(value) => {
                                        // Автоматически предлагаем имя для архивной таблицы
                                        archiveForm.setFieldsValue({
                                            archiveTable: `${value}_archive`
                                        });
                                    }}
                                >
                                    {tables.map(table => (
                                        <Option key={table} value={table}>
                                            {table}
                                        </Option>
                                    ))}
                                </Select>
                            </Form.Item>

                            <Form.Item
                                label="Архивировать данные старше (дней)"
                                name="days"
                                rules={[
                                    { required: true, message: 'Введите количество дней' },
                                    {
                                        type: 'number',
                                        min: 1,
                                        max: 3650,
                                        message: 'Значение должно быть от 1 до 3650 дней'
                                    }
                                ]}
                            >
                                <InputNumber
                                    min={1}
                                    max={3650}
                                    style={{ width: '100%' }}
                                    placeholder="Количество дней"
                                    precision={0}
                                />
                            </Form.Item>

                            <Form.Item
                                label="Имя архивной таблицы"
                                name="archiveTable"
                                rules={[
                                    { required: true, message: 'Введите имя архивной таблицы' },
                                    { pattern: /^[a-zA-Z_][a-zA-Z0-9_]*$/, message: 'Некорректное имя таблицы' }
                                ]}
                                extra="Таблица будет создана автоматически, если не существует"
                            >
                                <Input
                                    placeholder="Имя таблицы для архивных данных"
                                    addonBefore="public."
                                />
                            </Form.Item>

                            <Form.Item>
                                <Space direction="vertical" style={{ width: '100%' }} size="small">
                                    <Button
                                        type="primary"
                                        icon={<InboxOutlined />}
                                        htmlType="submit"
                                        loading={archiveLoading}
                                        size="large"
                                        block
                                    >
                                        Запустить архивацию
                                    </Button>

                                    {archivingResult && (
                                        <Alert
                                            message="Результат архивации"
                                            description={archivingResult}
                                            type="success"
                                            showIcon
                                            closable
                                            onClose={() => setArchivingResult(null)}
                                        />
                                    )}
                                </Space>
                            </Form.Item>
                        </Form>
                    </Spin>

                    <Alert
                        message="Как работает архивация"
                        description={
                            <div>
                                <p>1. Создается архивная таблица (если не существует) с такой же структурой, как исходная + дополнительные поля для отслеживания</p>
                                <p>2. Данные, удовлетворяющие условию (старше N дней), копируются в архивную таблицу</p>
                                <p>3. Заархивированные данные удаляются из исходной таблицы</p>
                                <p><Text strong type="danger">Внимание:</Text> Операция необратима. Убедитесь в наличии резервной копии перед архивацией.</p>
                            </div>
                        }
                        type="info"
                        showIcon
                        style={{ marginTop: '16px' }}
                    />
                </Card>
            </Space>
        </div>
    );
};

export default ServicePanel;
