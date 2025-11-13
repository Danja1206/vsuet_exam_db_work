export interface QueryRequest {
    tableName: string;
    filters: Record<string, any>;
    columns: string[];
    sortBy?: string;
    sortDirection: string;
}

export interface ExportRequest {
    queryRequest: QueryRequest;
    format: string;
}

export interface BackupRequest {
    backupPath?: string;
    fileName?: string;
}

export interface ArchiveRequest {
    olderThanDays: number;
    archiveTableName: string;
}

export interface TableData {
    columns: Array<{
        title: string;
        dataIndex: string;
        key: string;
    }>;
    data: any[];
}

export interface Seller {
    sellerId: number;
    companyName: string;
    contactEmail: string;
    contactPhone?: string;
    taxIdentificationNumber: string;
    registrationDate: string;
    isActive: boolean;
}

export interface Product {
    productId: number;
    sellerId: number;
    categoryId: number;
    productName: string;
    description?: string;
    price: number;
    createdDate: string;
}

export interface Order {
    orderId: number;
    customerId: number;
    orderDate: string;
    status: string;
    totalAmount: number;
    shippingAddress: string;
}
