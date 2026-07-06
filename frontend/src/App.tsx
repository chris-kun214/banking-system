import { useState } from 'react';
import Login from './components/Login';
import Register from './components/Register';
import Dashboard from './components/Dashboard';
import { authApi } from './api';

type Page = 'login' | 'register' | 'dashboard';

function App() {
  // 用惰性初始值代替"挂载后在 effect 里检查登录态再 setState"，避免多余的一次级联渲染
  const [currentPage, setCurrentPage] = useState<Page>(() =>
    authApi.isAuthenticated() ? 'dashboard' : 'login'
  );

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
