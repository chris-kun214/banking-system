import { useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../api';
import { setCredentials } from '../store/authSlice';
import { useAppDispatch } from '../store';
import type { RegisterRequest } from '../types';
import './Auth.css';

export default function Register() {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const [formData, setFormData] = useState<RegisterRequest>({
    username: '',
    password: '',
    email: '',
    fullName: '',
    phoneNumber: '',
  });
  const [confirmPassword, setConfirmPassword] = useState<string>('');
  const [error, setError] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);

  const validateForm = (): boolean => {
    if (formData.username.length < 3 || formData.username.length > 50) {
      setError('用户名长度必须在3-50个字符之间');
      return false;
    }

    if (formData.password.length < 6) {
      setError('密码长度至少6个字符');
      return false;
    }

    if (formData.password !== confirmPassword) {
      setError('两次输入的密码不一致');
      return false;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(formData.email)) {
      setError('邮箱格式不正确');
      return false;
    }

    if (!formData.fullName.trim()) {
      setError('全名不能为空');
      return false;
    }

    return true;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');

    if (!validateForm()) {
      return;
    }

    setLoading(true);

    try {
      const response = await authApi.register(formData);
      dispatch(setCredentials(response));
      navigate('/dashboard');
    } catch (err) {
      setError(err instanceof Error ? err.message : '注册失败，请重试');
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
          <h1>用户注册</h1>
          <p>创建您的银行账户</p>
        </div>

        <form onSubmit={handleSubmit} className="auth-form">
          {error && (
            <div className="error-message">
              <span>⚠️ {error}</span>
            </div>
          )}

          <div className="form-group">
            <label htmlFor="username">用户名 *</label>
            <input
              type="text"
              id="username"
              name="username"
              value={formData.username}
              onChange={handleChange}
              placeholder="3-50个字符"
              required
              autoComplete="username"
            />
          </div>

          <div className="form-group">
            <label htmlFor="fullName">全名 *</label>
            <input
              type="text"
              id="fullName"
              name="fullName"
              value={formData.fullName}
              onChange={handleChange}
              placeholder="请输入您的全名"
              required
              autoComplete="name"
            />
          </div>

          <div className="form-group">
            <label htmlFor="email">邮箱 *</label>
            <input
              type="email"
              id="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              placeholder="example@email.com"
              required
              autoComplete="email"
            />
          </div>

          <div className="form-group">
            <label htmlFor="phoneNumber">电话号码</label>
            <input
              type="tel"
              id="phoneNumber"
              name="phoneNumber"
              value={formData.phoneNumber}
              onChange={handleChange}
              placeholder="选填"
              autoComplete="tel"
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">密码 *</label>
            <input
              type="password"
              id="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              placeholder="至少6个字符"
              required
              autoComplete="new-password"
            />
          </div>

          <div className="form-group">
            <label htmlFor="confirmPassword">确认密码 *</label>
            <input
              type="password"
              id="confirmPassword"
              name="confirmPassword"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="再次输入密码"
              required
              autoComplete="new-password"
            />
          </div>

          <button type="submit" className="auth-button" disabled={loading}>
            {loading ? '注册中...' : '注册'}
          </button>
        </form>

        <div className="auth-footer">
          <p>
            已有账户？
            <button 
              type="button" 
              className="link-button" 
              onClick={() => navigate('/login')}
            >
              立即登录
            </button>
          </p>
        </div>
      </div>
    </div>
  );
}

