import { useState } from 'react';
import type { FormEvent } from 'react';
import { authApi } from '../api';
import type { LoginRequest } from '../types';
import './Auth.css';

interface LoginProps {
  onLoginSuccess: () => void;
  onSwitchToRegister: () => void;
}

export default function Login({ onLoginSuccess, onSwitchToRegister }: LoginProps) {
  const [formData, setFormData] = useState<LoginRequest>({
    username: '',
    password: '',
  });
  const [error, setError] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await authApi.login(formData);
      onLoginSuccess();
    } catch (err) {
      setError(err instanceof Error ? err.message : '登录失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-header">
          <h1>银行系统登录</h1>
          <p>欢迎回来，请登录您的账户</p>
        </div>

        <form onSubmit={handleSubmit} className="auth-form">
          {error && (
            <div className="error-message">
              <span>⚠️ {error}</span>
            </div>
          )}

          <div className="form-group">
            <label htmlFor="username">用户名</label>
            <input
              type="text"
              id="username"
              name="username"
              value={formData.username}
              onChange={handleChange}
              placeholder="请输入用户名"
              required
              autoComplete="username"
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">密码</label>
            <input
              type="password"
              id="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              placeholder="请输入密码"
              required
              autoComplete="current-password"
            />
          </div>

          <button type="submit" className="auth-button" disabled={loading}>
            {loading ? '登录中...' : '登录'}
          </button>
        </form>

        <div className="auth-footer">
          <p>
            还没有账户？
            <button 
              type="button" 
              className="link-button" 
              onClick={onSwitchToRegister}
            >
              立即注册
            </button>
          </p>
        </div>
      </div>
    </div>
  );
}

