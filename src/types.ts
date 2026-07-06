// 认证相关类型定义

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
  email: string;
  fullName: string;
  phoneNumber?: string;
}

export interface AuthResponse {
  token: string;
  type: string;
  username: string;
  email: string;
  role: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data?: T;
}

export interface User {
  username: string;
  email: string;
  role: string;
}

