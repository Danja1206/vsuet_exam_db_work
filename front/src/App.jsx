import React, { useState } from 'react';
import { Layout, Menu, theme, ConfigProvider } from 'antd';
import {
    DatabaseOutlined,
    ToolOutlined,
    TableOutlined
} from '@ant-design/icons';
import { QueryBuilder } from './components/query/QueryBuilder';
import { ServicePanel } from './components/services/ServicePanel';
import { EntityManager } from './components/data/EntityManager';
import ruRU from 'antd/locale/ru_RU';
import 'antd/dist/reset.css';

const { Header, Content, Sider } = Layout;

function App() {
    const [selectedKey, setSelectedKey] = useState('1');
    const {
        token: { colorBgContainer },
    } = theme.useToken();

    const renderContent = () => {
        switch (selectedKey) {
            case '1':
                return <EntityManager />;
            case '2':
                return <QueryBuilder />;
            case '3':
                return <ServicePanel />;
            default:
                return <div>Выберите раздел</div>;
        }
    };

    const menuItems = [
        {
            key: '1',
            icon: <TableOutlined />,
            label: 'Управление данными',
        },
        {
            key: '2',
            icon: <DatabaseOutlined />,
            label: 'Конструктор запросов',
        },
        {
            key: '3',
            icon: <ToolOutlined />,
            label: 'Сервисные функции',
        },
    ];

    return (
        <ConfigProvider locale={ruRU}>
            <Layout style={{ minHeight: '100vh' }}>
                <Header style={{
                    display: 'flex',
                    alignItems: 'center',
                    background: '#001529'
                }}>
                    <div style={{
                        display: 'flex',
                        alignItems: 'center',
                        color: 'white',
                        fontSize: '18px',
                        fontWeight: 'bold'
                    }}>
                        <DatabaseOutlined style={{ fontSize: '24px', marginRight: '12px' }} />
                        Управление Маркетплейсом
                    </div>
                </Header>

                <Layout>
                    <Sider
                        width={250}
                        style={{
                            background: colorBgContainer,
                            boxShadow: '2px 0 6px rgba(0,0,0,0.1)'
                        }}
                    >
                        <Menu
                            mode="inline"
                            selectedKeys={[selectedKey]}
                            style={{
                                height: '100%',
                                borderRight: 0,
                                paddingTop: '16px'
                            }}
                            items={menuItems}
                            onSelect={({ key }) => setSelectedKey(key)}
                        />
                    </Sider>

                    <Layout style={{ padding: '0' }}>
                        <Content
                            style={{
                                padding: '24px',
                                margin: 0,
                                minHeight: 280,
                                background: colorBgContainer,
                                overflow: 'auto'
                            }}
                        >
                            {renderContent()}
                        </Content>
                    </Layout>
                </Layout>
            </Layout>
        </ConfigProvider>
    );
}

export default App;
