import React, { useState, useEffect } from 'react';
import {
    Card,
    Form,
    Input,
    Select,
    Button,
    Space,
    Table,
    Row,
    Col,
    message,
    Tabs, Alert
} from 'antd';
import { DownloadOutlined, SearchOutlined, ClearOutlined, CodeOutlined } from '@ant-design/icons';
import { dataApi } from '../services/api';
import CodeMirror from '@uiw/react-codemirror';
import { sql } from '@codemirror/lang-sql';

const { TabPane } = Tabs;
const { Option } = Select;

const tables = [
    'Sellers', 'Products', 'Customers', 'Orders', 'OrderItems',
    'Categories', 'Reviews', 'Warehouse'
];

const filterOperators = [
    { value: '=', label: 'Равно' },
    { value: '!=', label: 'Не равно' },
    { value: '>', label: 'Больше' },
    { value: '<', label: 'Меньше' },
    { value: '>=', label: 'Больше или равно' },
    { value: '<=', label: 'Меньше или равно' },
    { value: 'LIKE', label: 'Содержит' },
];

interface Filter {
    field: string;
    operator: string;
    value: any;
}

export const QueryBuilder: React.FC = () => {
    const [form] = Form.useForm();
    const [activeTab, setActiveTab] = useState('builder');
    const [data, setData] = useState<any[]>([]);
    const [columns, setColumns] = useState<any[]>([]);
    const [availableColumns, setAvailableColumns] = useState<string[]>([]);
    const [filters, setFilters] = useState<Filter[]>([]);
    const [selectedTable, setSelectedTable] = useState<string>('');
    const [loading, setLoading] = useState(false);

    // SQL Editor State
    const [sqlQuery, setSqlQuery] = useState('SELECT * FROM Products LIMIT 10');
    const [sqlData, setSqlData] = useState<any[]>([]);
    const [sqlColumns, setSqlColumns] = useState<any[]>([]);
    const [sqlLoading, setSqlLoading] = useState(false);

    useEffect(() => {
        if (selectedTable) loadTableColumns(selectedTable);
    }, [selectedTable]);

    const [mainColumns, setMainColumns] = useState<string[]>([]);
    const [filterColumns, setFilterColumns] = useState<string[]>([]);

    const loadTableColumns = async (tableName: string) => {
        try {
            const response: string[] = await dataApi.getTableColumns(tableName);

            const mainCols: string[] = [];
            const allCols: string[] = [];

            response.forEach(col => {
                if (!col.includes(".")) {
                    mainCols.push(col);
                }
                allCols.push(col);
            });

            setMainColumns(mainCols);
            setFilterColumns(allCols);
            setAvailableColumns(mainCols);
        } catch (error) {
            console.error(error);
            message.error('Ошибка загрузки колонок');
        }
    };

    const columnIsString = (col: string) =>
        col.includes('name') || col.includes('title') || col.includes('desc');

    const addFilter = () => {
        setFilters([...filters, { field: '', operator: '=', value: '' }]);
        message.info('Добавлен новый фильтр');
    };

    const updateFilter = (index: number, field: keyof Filter, value: any) => {
        const newFilters = [...filters];
        newFilters[index] = { ...newFilters[index], [field]: value };
        setFilters(newFilters);
    };

    const removeFilter = (index: number) => {
        const newFilters = filters.filter((_, i) => i !== index);
        setFilters(newFilters);
    };

    const clearAll = () => {
        form.resetFields();
        setData([]);
        setFilters([]);
        setSelectedTable('');
        setAvailableColumns([]);
        message.warning('Все поля и фильтры очищены');
    };

    const handleTableChange = (value: string) => {
        setSelectedTable(value);
        form.setFieldsValue({ columns: [], sortBy: undefined });
    };

    const buildFilters = (filters: Filter[]): Record<string, any> => {
        const result: Record<string, any> = {};
        filters.forEach(f => {
            if (f.field && f.value !== '') {
                result[`${f.field}${f.operator}`] = f.value;
            }
        });
        return result;
    };

    const onExecute = async (values: any) => {
        const startTime = performance.now();
        setLoading(true);
        try {
            const request = {
                tableName: values.tableName,
                filters: buildFilters(filters),
                columns: values.columns || [],
                sortBy: values.sortBy,
                sortDirection: values.sortDirection || 'ASC'
            };
            const response = await dataApi.executeQuery(request);
            setData(response);

            if (response.length > 0) {
                const firstRow = response[0];
                const generatedColumns = Object.keys(firstRow).map(key => ({
                    title: key,
                    dataIndex: key,
                    key: key,
                    render: (value: any) => {
                        if (typeof value === 'boolean') return value ? 'Да' : 'Нет';
                        if (value instanceof Date || (typeof value === 'string' && !isNaN(Date.parse(value))))
                            return new Date(value).toLocaleDateString();
                        return value;
                    }
                }));
                setColumns(generatedColumns);
            }

            const duration = ((performance.now() - startTime) / 1000).toFixed(2);
            message.success(`Запрос к БД успешно выполнен (${duration} сек.)`);
        } catch (error) {
            console.error(error);
            message.error('Ошибка выполнения запроса');
        } finally {
            setLoading(false);
        }
    };

    const executeSQLQuery = async () => {
        const startTime = performance.now();
        setSqlLoading(true);
        try {
            const response = await dataApi.executeSQL({ query: sqlQuery });

            // Проверяем успешность запроса
            if (!response.success) {
                message.error(response.error || 'Ошибка выполнения SQL-запроса');
                setSqlData([]);
                setSqlColumns([]);
                return;
            }

            // Обрабатываем успешный результат
            const resultData = response.data || [];
            setSqlData(resultData);

            if (resultData.length > 0) {
                const firstRow = resultData[0];
                const generatedColumns = Object.keys(firstRow).map(key => ({
                    title: key,
                    dataIndex: key,
                    key: key,
                    render: (value: any) => {
                        if (typeof value === 'boolean') return value ? 'Да' : 'Нет';
                        if (value instanceof Date || (typeof value === 'string' && !isNaN(Date.parse(value))))
                            return new Date(value).toLocaleDateString('ru-RU');
                        return value;
                    }
                }));
                setSqlColumns(generatedColumns);
            } else {
                setSqlColumns([]);
                message.info('Запрос выполнен успешно, но не вернул данных');
            }

            const duration = ((performance.now() - startTime) / 1000).toFixed(2);
            message.success(`SQL-запрос выполнен успешно (${duration} сек.)`);
        } catch (error: any) {
            console.error('Ошибка выполнения SQL:', error);
            message.error(error.message || 'Ошибка выполнения SQL-запроса');
            setSqlData([]);
            setSqlColumns([]);
        } finally {
            setSqlLoading(false);
        }
    };

    const onExport = async (format: 'excel' | 'csv', isSql = false) => {
        const startTime = performance.now();
        try {
            const request = {
                query: isSql ? sqlQuery : undefined,
                tableName: !isSql ? form.getFieldValue('tableName') : undefined,
                filters: !isSql ? buildFilters(filters) : undefined,
                columns: !isSql ? form.getFieldValue('columns') || [] : undefined,
                sortBy: !isSql ? form.getFieldValue('sortBy') : undefined,
                sortDirection: !isSql ? form.getFieldValue('sortDirection') || 'ASC' : undefined,
                format: format === 'excel' ? 'xlsx' : 'csv'
            };

            const blob = await dataApi.exportData(request);
            const duration = ((performance.now() - startTime) / 1000).toFixed(2);

            const fileExtension = format === 'excel' ? 'xlsx' : 'csv';
            const contentType =
                format === 'excel'
                    ? 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
                    : 'text/csv';

            const url = window.URL.createObjectURL(new Blob([blob], { type: contentType }));
            const link = document.createElement('a');
            const tableName = isSql ? 'custom_query' : form.getFieldValue('tableName');
            link.href = url;
            link.setAttribute(
                'download',
                `export_${tableName}_${new Date().toISOString().slice(0, 19).replace(/:/g, '-')}.${fileExtension}`
            );
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);

            message.success(`Экспорт в ${format.toUpperCase()} завершён (${duration} сек.)`);
        } catch (error: any) {
            console.error(error);
            message.error(error.message || 'Ошибка при экспорте данных');
        }
    };

    return (
        <div style={{ padding: 20 }}>
            <Tabs activeKey={activeTab} onChange={setActiveTab} tabBarExtraContent={
                activeTab === 'sql' && (
                    <Button
                        type="primary"
                        icon={<SearchOutlined />}
                        onClick={executeSQLQuery}
                        loading={sqlLoading}
                    >
                        Выполнить SQL
                    </Button>
                )
            }>

                <TabPane tab="Конструктор запросов" key="builder">
                    <Alert
                        message="ВАЖНО!!!"
                        description={
                            <div>
                                <p>При использовании фильтрации <strong>могут не работать</strong> некоторые варианты(особенно <strong>'Содержит'(LIKE))</strong> поэтому такие запросы лучше через <strong>SQL Редактор</strong>.</p>

                                {/*<p><Text strong type="danger">Внимание:</Text> Операция необратима. Убедитесь в наличии резервной копии перед архивацией.</p>*/}
                            </div>
                        }
                        type="warning"
                        showIcon
                        style={{ marginTop: '16px', marginBottom: '16px' }}
                    />
                    <Card title="Построитель запросов" style={{ marginBottom: 20 }}>
                        <Form form={form} layout="vertical" onFinish={onExecute} initialValues={{ sortDirection: 'ASC' }}>
                            <Row gutter={16}>
                                <Col span={8}>
                                    <Form.Item
                                        name="tableName"
                                        label="Таблица"
                                        rules={[{ required: true, message: 'Выберите таблицу' }]}
                                    >
                                        <Select placeholder="Выберите таблицу" showSearch onChange={handleTableChange}>
                                            {tables.map(table => <Option key={table} value={table}>{table}</Option>)}
                                        </Select>
                                    </Form.Item>
                                </Col>
                                <Col span={8}>
                                    <Form.Item name="columns" label="Колонки">
                                        <Select mode="multiple" placeholder="Выберите колонки">
                                            {mainColumns.map(col => <Option key={col} value={col}>{col}</Option>)}
                                        </Select>
                                    </Form.Item>
                                </Col>
                                <Col span={4}>
                                    <Form.Item name="sortBy" label="Сортировка">
                                        <Select placeholder="Выберите колонку">
                                            {mainColumns.map(col => <Option key={col} value={col}>{col}</Option>)}
                                        </Select>
                                    </Form.Item>
                                </Col>
                                <Col span={4}>
                                    <Form.Item name="sortDirection" label="Направление">
                                        <Select>
                                            <Option value="ASC">По возрастанию</Option>
                                            <Option value="DESC">По убыванию</Option>
                                        </Select>
                                    </Form.Item>
                                </Col>
                            </Row>

                            <Card size="small" title="Фильтры" style={{ marginBottom: 16 }}>
                                {filters.map((filter, index) => (
                                    <Row gutter={8} key={index} style={{ marginBottom: 8 }}>
                                        <Col span={6}>
                                            <Select
                                                placeholder="Поле"
                                                value={filter.field}
                                                onChange={v => updateFilter(index, 'field', v)}
                                                style={{ width: '100%' }}
                                            >
                                                {filterColumns.map(col => <Option key={col} value={col}>{col}</Option>)}
                                            </Select>
                                        </Col>
                                        <Col span={4}>
                                            <Select
                                                placeholder="Оператор"
                                                value={filter.operator}
                                                onChange={v => updateFilter(index, 'operator', v)}
                                                style={{ width: '100%' }}
                                            >
                                                {filterOperators
                                                    .filter(op =>
                                                        !filter.field || columnIsString(filter.field)
                                                            ? ['=', '!=', 'LIKE'].includes(op.value)
                                                            : ['=', '!=', '>', '<', '>=', '<='].includes(op.value)
                                                    )
                                                    .map(op => <Option key={op.value} value={op.value}>{op.label}</Option>)}
                                            </Select>
                                        </Col>
                                        <Col span={8}>
                                            <Input
                                                placeholder="Значение"
                                                value={filter.value}
                                                onChange={e => updateFilter(index, 'value', e.target.value)}
                                            />
                                        </Col>
                                        <Col span={4}>
                                            <Button danger style={{ width: '100%' }} onClick={() => removeFilter(index)}>Удалить</Button>
                                        </Col>
                                    </Row>
                                ))}
                                <Button type="dashed" block onClick={addFilter}>+ Добавить фильтр</Button>
                            </Card>

                            <Form.Item>
                                <Space wrap>
                                    <Button type="primary" icon={<SearchOutlined />} htmlType="submit" loading={loading}>Выполнить запрос</Button>
                                    <Button icon={<ClearOutlined />} onClick={clearAll}>Очистить</Button>
                                    <Button icon={<DownloadOutlined />} onClick={() => onExport('excel')} disabled={data.length === 0}>Экспорт в Excel</Button>
                                    <Button onClick={() => onExport('csv')} disabled={data.length === 0}>Экспорт в CSV</Button>
                                </Space>
                            </Form.Item>
                        </Form>
                    </Card>

                    {data.length > 0 && (
                        <Card title={`Результаты (${data.length} записей)`}>
                            <Table
                                dataSource={data}
                                columns={columns}
                                rowKey={(record) => record.id || Math.random()}
                                pagination={{ pageSize: 10, showSizeChanger: true, showTotal: (total, range) => `Записи ${range[0]}-${range[1]} из ${total}` }}
                                scroll={{ x: true }}
                            />
                        </Card>
                    )}
                </TabPane>

                <TabPane tab="SQL Редактор" key="sql" forceRender>
                    <Alert
                        message="ВАЖНО!!!"
                        description={
                            <div>
                                <p>1. Не поддерживаются <strong>множественные</strong> запросы, поэтому смысла
                                    ставить <big><strong>;</strong></big> нет.</p>
                                <p><small><strong>Примечание:</strong> сложности при добавлении поддержки множественных
                                    запросов выше чем я могу закрыть по времени.</small></p>

                                <p>2. Подсказки отображают <strong>только синтаксис</strong> языка SQL, названия таблиц и полей в БД
                                    нужно <strong>писать вручную</strong>.</p>

                                {/*<p><Text strong type="danger">Внимание:</Text> Операция необратима. Убедитесь в наличии резервной копии перед архивацией.</p>*/}
                            </div>
                        }
                        type="warning"
                        showIcon
                        style={{marginTop: '16px', marginBottom: '16px' }}
                    />
                    <Card title="Редактор SQL-запросов" style={{ marginBottom: 20 }}>
                        <CodeMirror
                            value={sqlQuery}
                            height="400px"
                            extensions={[sql()]}
                            // theme={oneLight}
                            onChange={setSqlQuery}
                            options={{ lineNumbers: true }}
                            placeholder="Введите SQL-запрос здесь..."
                        />
                        <Space style={{ marginTop: 16, marginBottom: 16 }}>
                            <Button
                                type="primary"
                                icon={<SearchOutlined />}
                                onClick={executeSQLQuery}
                                loading={sqlLoading}
                            >
                                Выполнить запрос
                            </Button>
                            <Button icon={<ClearOutlined />} onClick={() => setSqlQuery('SELECT * FROM Products LIMIT 10')}>
                                Очистить
                            </Button>
                            <Button icon={<DownloadOutlined />} onClick={() => onExport('excel', true)} disabled={sqlData.length === 0}>
                                Экспорт в Excel
                            </Button>
                            <Button onClick={() => onExport('csv', true)} disabled={sqlData.length === 0}>
                                Экспорт в CSV
                            </Button>
                        </Space>
                    </Card>

                    {sqlData.length > 0 && (
                        <Card title={`Результаты запроса (${sqlData.length} записей)`}>
                            <Table
                                dataSource={sqlData}
                                columns={sqlColumns}
                                rowKey={() => Math.random()}
                                pagination={{ pageSize: 10, showSizeChanger: true, showTotal: (total, range) => `Записи ${range[0]}-${range[1]} из ${total}` }}
                                scroll={{ x: true }}
                            />
                        </Card>
                    )}
                </TabPane>
            </Tabs>
        </div>
    );
};

export default QueryBuilder;
