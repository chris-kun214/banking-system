import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../api';
import { clearCredentials } from '../store/authSlice';
import { useAppDispatch, useAppSelector } from '../store';
import AccountsPanel from './AccountsPanel';
import TransactionsPanel from './TransactionsPanel';
import ReportsPanel from './ReportsPanel';
import './Dashboard.css';

type Tab = 'overview' | 'accounts' | 'transactions' | 'reports';

const TABS: { key: Tab; icon: string; label: string; description: string }[] = [
  { key: 'accounts', icon: '💰', label: '账户管理', description: '查看和管理您的银行账户' },
  { key: 'transactions', icon: '💸', label: '转账服务 · 交易记录', description: '存取款、转账、查看交易历史' },
  { key: 'reports', icon: '📈', label: '财务报表', description: '生成并下载月度对账单 PDF' },
];

export default function Dashboard() {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const user = useAppSelector((state) => state.auth);
  const [activeTab, setActiveTab] = useState<Tab>('overview');

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
        {activeTab === 'overview' ? (
          <>
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
                  <span className={`role-badge ${user.role?.toLowerCase()}`}>{user.role}</span>
                </div>
              </div>
            </div>

            <div className="features-grid">
              {TABS.map((tab) => (
                <button key={tab.key} className="feature-card" onClick={() => setActiveTab(tab.key)}>
                  <div className="feature-icon">{tab.icon}</div>
                  <h3>{tab.label}</h3>
                  <p>{tab.description}</p>
                </button>
              ))}
            </div>
          </>
        ) : (
          <>
            <button className="back-button" onClick={() => setActiveTab('overview')}>
              ← 返回概览
            </button>
            {activeTab === 'accounts' && <AccountsPanel />}
            {activeTab === 'transactions' && <TransactionsPanel />}
            {activeTab === 'reports' && <ReportsPanel />}
          </>
        )}
      </div>
    </div>
  );
}
