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

// 账户相关类型定义

export interface Account {
  id: number;
  accountName: string;
  accountNumber: string;
  balance: number;
  userId: number;
  username: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAccountRequest {
  accountName: string;
  accountNumber: string;
  initialBalance: number;
}

// 交易相关类型定义

export type TransactionType = 'DEPOSIT' | 'WITHDRAW' | 'TRANSFER_IN' | 'TRANSFER_OUT';

export interface Transaction {
  id: number;
  transactionId: string;
  accountId: number;
  accountNumber: string;
  transactionType: TransactionType;
  amount: number;
  balanceBefore: number;
  balanceAfter: number;
  createdAt: string;
  description?: string;
  aiDescription?: string;
  aiCategory?: string;
}

export interface TransactionRequest {
  accountNumber: string;
  amount: number;
  targetAccountNumber?: string;
  description?: string;
}

