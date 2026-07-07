import { useNavigate } from 'react-router-dom';
import { authApi } from '../api';
import { clearCredentials } from '../store/authSlice';
import { useAppDispatch, useAppSelector } from '../store';
import './Dashboard.css';

export default function Dashboard() {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const user = useAppSelector((state) => state.auth);

  const handleLogout = () => {
    authApi.logout();
    dispatch(clearCredentials());
    navigate('/login');
  };

  return (
    <div className="dashboard-container">
      <nav className="dashboard-nav">
        <div className="nav-brand">
          <h2>银行系统</h2>
        </div>
        <div className="nav-user">
          <span>欢迎，{user.username}</span>
          <button onClick={handleLogout} className="logout-button">
            退出登录
          </button>
        </div>
      </nav>

      <div className="dashboard-content">
        <div className="welcome-card">
          <h1>欢迎使用银行系统！</h1>
          <div className="user-info">
            <div className="info-item">
              <strong>用户名：</strong>
              <span>{user.username}</span>
            </div>
            <div className="info-item">
              <strong>邮箱：</strong>
              <span>{user.email}</span>
            </div>
            <div className="info-item">
              <strong>角色：</strong>
              <span className={`role-badge ${user.role?.toLowerCase()}`}>
                {user.role}
              </span>
            </div>
          </div>
        </div>

        <div className="features-grid">
          <div className="feature-card">
            <div className="feature-icon">💰</div>
            <h3>账户管理</h3>
            <p>查看和管理您的银行账户</p>
          </div>

          <div className="feature-card">
            <div className="feature-icon">💸</div>
            <h3>转账服务</h3>
            <p>快速安全的转账功能</p>
          </div>

          <div className="feature-card">
            <div className="feature-icon">📊</div>
            <h3>交易记录</h3>
            <p>查看您的交易历史</p>
          </div>

          <div className="feature-card">
            <div className="feature-icon">📈</div>
            <h3>财务报表</h3>
            <p>详细的财务分析报告</p>
          </div>
        </div>
      </div>
    </div>
  );
}

