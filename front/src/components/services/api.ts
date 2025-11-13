import axios from 'axios';
import { QueryRequest, ExportRequest, BackupRequest, ArchiveRequest } from '../../types';
const API_BASE_URL = 'http://localhost:8032/api';

export const entityApi = {
    getSellers: async () => {
        const response = await fetch(`${API_BASE_URL}/sellers`);
        if (!response.ok) throw new Error('Failed to fetch sellers');
        return response.json();
    },

    createSeller: async (sellerData) => {
        const response = await fetch(`${API_BASE_URL}/sellers`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(sellerData)
        });
        if (!response.ok) throw new Error('Failed to create seller');
        return response.json();
    },

    updateSeller: async (id, sellerData) => {
        const response = await fetch(`${API_BASE_URL}/sellers/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(sellerData)
        });
        if (!response.ok) throw new Error('Failed to update seller');
        return response.json();
    },

    deleteSeller: async (id) => {
        const response = await fetch(`${API_BASE_URL}/sellers/${id}`, {
            method: 'DELETE'
        });
        if (!response.ok) throw new Error('Failed to delete seller');
        return response.json();
    },

    // Аналогичные методы для products и orders
    getProducts: async () => {
        const response = await fetch(`${API_BASE_URL}/products`);
        if (!response.ok) throw new Error('Failed to fetch products');
        return response.json();
    },

    getCategories: async () => {
        const response = await fetch(`${API_BASE_URL}/categories`);
        if (!response.ok) throw new Error('Failed to fetch categories');
        return response.json();
    },

    getOrderItems: async () => {
        const response = await fetch(`${API_BASE_URL}/order-items`);
        if (!response.ok) throw new Error('Failed to fetch order-items');
        return response.json();
    },

    getReviews: async () => {
        const response = await fetch(`${API_BASE_URL}/reviews`);
        if (!response.ok) throw new Error('Failed to fetch order-items');
        return response.json();
    },

    getWarehouse: async () => {
        const response = await fetch(`${API_BASE_URL}/warehouses`);
        if (!response.ok) throw new Error('Failed to fetch order-items');
        return response.json();
    },


    getCustomers: async () => {
        const response = await fetch(`${API_BASE_URL}/customers`);
        if (!response.ok) throw new Error('Failed to fetch order-items');
        return response.json();
    },

    createProduct: async (productData) => {
        const response = await fetch(`${API_BASE_URL}/products`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(productData)
        });
        if (!response.ok) throw new Error('Failed to create product');
        return response.json();
    },

    updateProduct: async (id, productData) => {
        const response = await fetch(`${API_BASE_URL}/products/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(productData)
        });
        if (!response.ok) throw new Error('Failed to update product');
        return response.json();
    },

    deleteProduct: async (id) => {
        const response = await fetch(`${API_BASE_URL}/products/${id}`, {
            method: 'DELETE'
        });
        if (!response.ok) throw new Error('Failed to delete product');
        return response.json();
    },

    deleteWarehouse: async (id) => {
        const response = await fetch(`${API_BASE_URL}/warehouse/${id}`, {
            method: 'DELETE'
        });
        if (!response.ok) throw new Error('Failed to delete warehouse');
        return response.json();
    },

    deleteReview: async (id) => {
        const response = await fetch(`${API_BASE_URL}/review/${id}`, {
            method: 'DELETE'
        });
        if (!response.ok) throw new Error('Failed to delete review');
        return response.json();
    },

    deleteOrderItem: async (id) => {
        const response = await fetch(`${API_BASE_URL}/order-item/${id}`, {
            method: 'DELETE'
        });
        if (!response.ok) throw new Error('Failed to delete order-item');
        return response.json();
    },

    deleteCustomer: async (id) => {
        const response = await fetch(`${API_BASE_URL}/customer/${id}`, {
            method: 'DELETE'
        });
        if (!response.ok) throw new Error('Failed to delete customer');
        return response.json();
    },

    getOrders: async () => {
        const response = await fetch(`${API_BASE_URL}/orders`);
        if (!response.ok) throw new Error('Failed to fetch orders');
        return response.json();
    },

    deleteOrder: async (id) => {
        const response = await fetch(`${API_BASE_URL}/orders/${id}`, {
            method: 'DELETE'
        });
        if (!response.ok) throw new Error('Failed to delete order');
        return response.json();
    }
};

export const dataApi = {
    getTableColumns: async (tableName) => {
        const response = await fetch(`${API_BASE_URL}/data/columns/${tableName}`);
        if (!response.ok) throw new Error('Failed to fetch table columns');
        return response.json();
    },

    executeQuery: async (queryRequest) => {
        const response = await fetch(`${API_BASE_URL}/data/query`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(queryRequest)
        });
        if (!response.ok) throw new Error('Query execution failed');
        return response.json();
    },


    executeSQL: async (request: { query: string }) => {
        const response = await fetch(`${API_BASE_URL}/data/sql-command`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(request),
        });

        const result = await response.json();

        if (result.hasOwnProperty('success')) {
            return result;
        }

        if (!response.ok) {
            throw new Error(result.message || 'Ошибка выполнения SQL-запроса');
        }

        return {
            success: true,
            data: result,
            error: null
        };
    },

    exportData: async (exportRequest) => {
        const response = await fetch(`${API_BASE_URL}/data/export-sql`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(exportRequest)
        });

        if (!response.ok) {
            const errText = await response.text();
            throw new Error(`Export failed: ${errText}`);
        }

        return await response.blob();
    }
};

export const serviceApi = {
    getBackupFiles: async () => {
        const response = await fetch(`${API_BASE_URL}/service/backups`);
        if (!response.ok) throw new Error('Failed to load backup files');
        console.log(response)
        return response.json();
    },

    createBackup: async (backupRequest) => {
        const response = await fetch(`${API_BASE_URL}/service/backup`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(backupRequest)
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || 'Backup creation failed');
        }

        return response.json();
    },

    getTables: async (): Promise<string[]> => {
        const response = await fetch(`${API_BASE_URL}/service/tables`);
        if (!response.ok) throw new Error('Failed to load tables');
        return response.json();
    },


    restoreBackup: async (backupFile: string) => {
        const response = await fetch(`${API_BASE_URL}/service/restore`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ backupFile })  // <- здесь ключ должен совпадать
        });
        if (!response.ok) throw new Error('Restore failed');
        return response.json();
    },

    archiveData: async (archiveRequest) => {
        const response = await fetch(`${API_BASE_URL}/service/archive`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(archiveRequest)
        });
        if (!response.ok) throw new Error('Archive failed');
        return response.json();
    }
};
