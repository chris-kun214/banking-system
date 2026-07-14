import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { accountApi } from '../api';
import type { Account } from '../types';

function generateAccountNumber(): string {
  return 'ACC' + Math.floor(100000000 + Math.random() * 900000000).toString();
}

interface AccountsPanelProps {
  onAccountsChanged?: (accounts: Account[]) => void;
}

export default function AccountsPanel({ onAccountsChanged }: AccountsPanelProps) {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [accountName, setAccountName] = useState('');
  const [initialBalance, setInitialBalance] = useState('0');
  const [creating, setCreating] = useState(false);

  const loadAccounts = async () => {
    setLoading(true);
    setError('');
    try {
      const result = await accountApi.getMyAccounts();
      setAccounts(result);
      onAccountsChanged?.(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载账户失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAccounts();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleCreate = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setCreating(true);
    try {
      await accountApi.createAccount({
        accountName,
        accountNumber: generateAccountNumber(),
        initialBalance: parseFloat(initialBalance) || 0,
      });
      setAccountName('');
      setInitialBalance('0');
      setShowForm(false);
      await loadAccounts();
    } catch (err) {
      setError(err instanceof Error ? err.message : '创建账户失败');
    } finally {
      setCreating(false);
    }
  };

  return (
    <div className="panel">
      <div className="panel-header">
        <h2>账户管理</h2>
        <button className="panel-action-button" onClick={() => setShowForm((v) => !v)}>
          {showForm ? '取消' : '+ 新建账户'}
        </button>
      </div>

      {error && (
        <div className="error-message">
          <span>⚠️ {error}</span>
        </div>
      )}

      {showForm && (
        <form onSubmit={handleCreate} className="inline-form">
          <div className="form-group">
            <label>账户名</label>
            <input
              type="text"
              value={accountName}
              onChange={(e) => setAccountName(e.target.value)}
              placeholder="例如：日常账户"
              required
              maxLength={20}
            />
          </div>
          <div className="form-group">
            <label>初始余额</label>
            <input
              type="number"
              value={initialBalance}
              onChange={(e) => setInitialBalance(e.target.value)}
              min="0"
              step="0.01"
              required
            />
          </div>
          <button type="submit" className="panel-action-button" disabled={creating}>
            {creating ? '创建中...' : '确认创建'}
          </button>
        </form>
      )}

      {loading ? (
        <p className="panel-empty">加载中...</p>
      ) : accounts.length === 0 ? (
        <p className="panel-empty">还没有账户，点击"新建账户"创建一个</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>账户名</th>
              <th>账号</th>
              <th>余额</th>
              <th>创建时间</th>
            </tr>
          </thead>
          <tbody>
            {accounts.map((account) => (
              <tr key={account.id}>
                <td>{account.accountName}</td>
                <td className="mono">{account.accountNumber}</td>
                <td className="amount">${account.balance.toFixed(2)}</td>
                <td>{new Date(account.createdAt).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
