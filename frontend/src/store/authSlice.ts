import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';
import type { AuthResponse } from '../types';

export interface AuthState {
  token: string | null;
  username: string | null;
  email: string | null;
  role: string | null;
}

// 启动时从 localStorage 水合，localStorage 仍是持久化层（api.ts 负责写入），
// Redux 只是给组件用的单一数据源，避免各处散落 localStorage.getItem 调用。
function loadInitialState(): AuthState {
  return {
    token: localStorage.getItem('token'),
    username: localStorage.getItem('username'),
    email: localStorage.getItem('email'),
    role: localStorage.getItem('role'),
  };
}

const authSlice = createSlice({
  name: 'auth',
  initialState: loadInitialState(),
  reducers: {
    setCredentials(state, action: PayloadAction<AuthResponse>) {
      state.token = action.payload.token;
      state.username = action.payload.username;
      state.email = action.payload.email;
      state.role = action.payload.role;
    },
    clearCredentials(state) {
      state.token = null;
      state.username = null;
      state.email = null;
      state.role = null;
    },
  },
});

export const { setCredentials, clearCredentials } = authSlice.actions;
export default authSlice.reducer;
