// API 服务
import type { LoginRequest, RegisterRequest, AuthResponse, ApiResponse } from './types';

const API_BASE_URL = 'http://localhost:8080/api';

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

