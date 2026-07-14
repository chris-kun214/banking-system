// API 服务
import type {
  LoginRequest,
  RegisterRequest,
  AuthResponse,
  ApiResponse,
  Account,
  CreateAccountRequest,
  Transaction,
  TransactionRequest,
} from './types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:9090/api';

// 通用请求函数
async function request<T>(
  url: string,
  options: RequestInit = {}
): Promise<ApiResponse<T>> {
  try {
    const token = localStorage.getItem('token');
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
      ...(token && { Authorization: `Bearer ${token}` }),
      ...options.headers,
    };

    const response = await fetch(`${API_BASE_URL}${url}`, {
      ...options,
      headers,
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.message || '请求失败');
    }

    return data;
  } catch (error) {
    if (error instanceof Error) {
      throw error;
    }
    throw new Error('网络请求失败');
  }
}

// 认证 API
export const authApi = {
  // 登录
  login: async (credentials: LoginRequest): Promise<AuthResponse> => {
    const response = await request<AuthResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(credentials),
    });
    
    if (response.data) {
      // 保存 token 到 localStorage
      localStorage.setItem('token', response.data.token);
      localStorage.setItem('username', response.data.username);
      localStorage.setItem('email', response.data.email);
      localStorage.setItem('role', response.data.role);
    }
    
    return response.data!;
  },

  // 注册
  register: async (userData: RegisterRequest): Promise<AuthResponse> => {
    const response = await request<AuthResponse>('/auth/register', {
      method: 'POST',
      body: JSON.stringify(userData),
    });
    
    if (response.data) {
      // 保存 token 到 localStorage
      localStorage.setItem('token', response.data.token);
      localStorage.setItem('username', response.data.username);
      localStorage.setItem('email', response.data.email);
      localStorage.setItem('role', response.data.role);
    }
    
    return response.data!;
  },

  // 登出
  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('email');
    localStorage.removeItem('role');
  },

  // 检查是否已登录
  isAuthenticated: (): boolean => {
    return !!localStorage.getItem('token');
  },

  // 获取当前用户信息
  getCurrentUser: () => {
    return {
      username: localStorage.getItem('username'),
      email: localStorage.getItem('email'),
      role: localStorage.getItem('role'),
    };
  },
};

// 账户 API
export const accountApi = {
  // 获取当前用户的所有账户
  getMyAccounts: async (): Promise<Account[]> => {
    const response = await request<Account[]>('/accounts/my');
    return response.data ?? [];
  },

  // 创建账户
  createAccount: async (payload: CreateAccountRequest): Promise<Account> => {
    const response = await request<Account>('/accounts', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
    return response.data!;
  },
};

// 交易 API
export const transactionApi = {
  deposit: async (payload: TransactionRequest): Promise<Transaction> => {
    const response = await request<Transaction>('/transactions/deposit', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
    return response.data!;
  },

  withdraw: async (payload: TransactionRequest): Promise<Transaction> => {
    const response = await request<Transaction>('/transactions/withdraw', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
    return response.data!;
  },

  transfer: async (payload: TransactionRequest): Promise<Transaction> => {
    const response = await request<Transaction>('/transactions/transfer', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
    return response.data!;
  },

  // 按账户 ID 查询交易记录
  getByAccountId: async (accountId: number): Promise<Transaction[]> => {
    const response = await request<Transaction[]>(`/transactions/account-id/${accountId}`);
    return response.data ?? [];
  },

  // 触发一次 LLM 交易描述推荐
  suggestDescription: async (transactionId: number): Promise<Transaction> => {
    const response = await request<Transaction>(`/transactions/${transactionId}/suggest-description`, {
      method: 'POST',
    });
    return response.data!;
  },
};

// 报表 API
export const reportApi = {
  // 下载月度对账单 PDF（用 fetch 而不是 <a href>，因为要带 Authorization header）
  downloadMonthlyStatement: async (accountNumber: string, month: string): Promise<void> => {
    const token = localStorage.getItem('token');
    const response = await fetch(
      `${API_BASE_URL}/reports/monthly?accountNumber=${encodeURIComponent(accountNumber)}&month=${encodeURIComponent(month)}`,
      {
        headers: { ...(token && { Authorization: `Bearer ${token}` }) },
      }
    );

    if (!response.ok) {
      const errorBody = await response.json().catch(() => null);
      throw new Error(errorBody?.message ?? '生成对账单失败');
    }

    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `statement-${accountNumber}-${month}.pdf`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },
};

