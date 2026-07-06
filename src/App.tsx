import { useState, useEffect } from 'react';
import Login from './components/Login';
import Register from './components/Register';
import Dashboard from './components/Dashboard';
import { authApi } from './api';

type Page = 'login' | 'register' | 'dashboard';

function App() {
  const [currentPage, setCurrentPage] = useState<Page>('login');

  // 检查用户是否已登录
  useEffect(() => {
    if (authApi.isAuthenticated()) {
      setCurrentPage('dashboard');
    }
  }, []);

  const handleLoginSuccess = () => {
    setCurrentPage('dashboard');
  };

  const handleRegisterSuccess = () => {
    setCurrentPage('dashboard');
  };

  const handleLogout = () => {
    setCurrentPage('login');
  };

  const switchToRegister = () => {
    setCurrentPage('register');
  };

  const switchToLogin = () => {
    setCurrentPage('login');
  };

  return (
    <>
      {currentPage === 'login' && (
        <Login 
          onLoginSuccess={handleLoginSuccess}
          onSwitchToRegister={switchToRegister}
        />
      )}
      {currentPage === 'register' && (
        <Register 
          onRegisterSuccess={handleRegisterSuccess}
          onSwitchToLogin={switchToLogin}
        />
      )}
      {currentPage === 'dashboard' && (
        <Dashboard onLogout={handleLogout} />
      )}
    </>
  );
}

export default App;
