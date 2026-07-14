import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { accountApi, transactionApi } from '../api';
import type { Account, Transaction, TransactionType } from '../types';

type OperationType = 'DEPOSIT' | 'WITHDRAW' | 'TRANSFER';

const TYPE_LABELS: Record<TransactionType, string> = {
  DEPOSIT: '存款',
  WITHDRAW: '取款',
  TRANSFER_IN: '转入',
  TRANSFER_OUT: '转出',
};

export default function TransactionsPanel() {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [selectedAccountId, setSelectedAccountId] = useState<number | null>(null);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [operation, setOperation] = useState<OperationType>('DEPOSIT');
  const [amount, setAmount] = useState('');
  const [targetAccountNumber, setTargetAccountNumber] = useState('');
  const [note, setNote] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [suggestingId, setSuggestingId] = useState<number | null>(null);

  const selectedAccount = accounts.find((a) => a.id === selectedAccountId) ?? null;

  const loadAccounts = async () => {
    try {
      const result = await accountApi.getMyAccounts();
      setAccounts(result);
      if (result.length > 0 && selectedAccountId === null) {
        setSelectedAccountId(result[0].id);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载账户失败');
    }
  };

  const loadTransactions = async (accountId: number) => {
    setLoading(true);
    try {
      const result = await transactionApi.getByAccountId(accountId);
      setTransactions(
        [...result].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载交易记录失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAccounts();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (selectedAccountId !== null) {
      loadTransactions(selectedAccountId);
    }
  }, [selectedAccountId]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!selectedAccount) return;
    setError('');
    setSuccess('');
    setSubmitting(true);

    try {
      const payload = {
        accountNumber: selectedAccount.accountNumber,
        amount: parseFloat(amount),
        targetAccountNumber: operation === 'TRANSFER' ? targetAccountNumber : undefined,
        description: note || undefined,
      };

      if (operation === 'DEPOSIT') {
        await transactionApi.deposit(payload);
      } else if (operation === 'WITHDRAW') {
        await transactionApi.withdraw(payload);
      } else {
        await transactionApi.transfer(payload);
      }

      setSuccess(`${operation === 'DEPOSIT' ? '存款' : operation === 'WITHDRAW' ? '取款' : '转账'}成功`);
      setAmount('');
      setTargetAccountNumber('');
      setNote('');
      await Promise.all([loadAccounts(), loadTransactions(selectedAccount.id)]);
    } catch (err) {
      setError(err instanceof Error ? err.message : '操作失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleSuggestDescription = async (transactionId: number) => {
    setSuggestingId(transactionId);
    setError('');
    try {
      const updated = await transactionApi.suggestDescription(transactionId);
      setTransactions((prev) => prev.map((t) => (t.id === transactionId ? updated : t)));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'AI 描述推荐失败');
    } finally {
      setSuggestingId(null);
    }
  };

  return (
    <div className="panel">
      <div className="panel-header">
        <h2>转账服务 · 交易记录</h2>
      </div>

      {accounts.length === 0 ? (
        <p className="panel-empty">请先在"账户管理"里创建一个账户</p>
      ) : (
        <>
          <div className="form-group">
            <label>选择账户</label>
            <select
              value={selectedAccountId ?? ''}
              onChange={(e) => setSelectedAccountId(Number(e.target.value))}
            >
              {accounts.map((account) => (
                <option key={account.id} value={account.id}>
                  {account.accountName}（{account.accountNumber}）— ${account.balance.toFixed(2)}
                </option>
              ))}
            </select>
          </div>

          {error && (
            <div className="error-message">
              <span>⚠️ {error}</span>
            </div>
          )}
          {success && <div className="success-message">✅ {success}</div>}

          <form onSubmit={handleSubmit} className="inline-form">
            <div className="form-group">
              <label>操作类型</label>
              <select value={operation} onChange={(e) => setOperation(e.target.value as OperationType)}>
                <option value="DEPOSIT">存款</option>
                <option value="WITHDRAW">取款</option>
                <option value="TRANSFER">转账</option>
              </select>
            </div>

            {operation === 'TRANSFER' && (
              <div className="form-group">
                <label>目标账号</label>
                <input
                  type="text"
                  value={targetAccountNumber}
                  onChange={(e) => setTargetAccountNumber(e.target.value)}
                  required
                  placeholder="收款账号"
                />
              </div>
            )}

            <div className="form-group">
              <label>金额</label>
              <input
                type="number"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                min="0.01"
                step="0.01"
                required
              />
            </div>

            <div className="form-group">
              <label>备注（可选，供 AI 描述推荐参考）</label>
              <input
                type="text"
                value={note}
                onChange={(e) => setNote(e.target.value)}
                placeholder="例如：Woolworths 超市购物"
              />
            </div>

            <button type="submit" className="panel-action-button" disabled={submitting}>
              {submitting ? '处理中...' : '提交'}
            </button>
          </form>

          {loading ? (
            <p className="panel-empty">加载中...</p>
          ) : transactions.length === 0 ? (
            <p className="panel-empty">这个账户还没有交易记录</p>
          ) : (
            <table className="data-table">
              <thead>
                <tr>
                  <th>类型</th>
                  <th>金额</th>
                  <th>余额变化</th>
                  <th>时间</th>
                  <th>描述</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {transactions.map((tx) => (
                  <tr key={tx.id}>
                    <td>
                      <span className={`type-badge type-${tx.transactionType.toLowerCase()}`}>
                        {TYPE_LABELS[tx.transactionType]}
                      </span>
                    </td>
                    <td className="amount">${tx.amount.toFixed(2)}</td>
                    <td className="mono">
                      ${tx.balanceBefore.toFixed(2)} → ${tx.balanceAfter.toFixed(2)}
                    </td>
                    <td>{new Date(tx.createdAt).toLocaleString()}</td>
                    <td>
                      {tx.aiDescription ? (
                        <div className="ai-description">
                          <span>{tx.aiDescription}</span>
                          {tx.aiCategory && <span className="ai-category">{tx.aiCategory}</span>}
                        </div>
                      ) : (
                        tx.description || <span className="panel-empty-inline">—</span>
                      )}
                    </td>
                    <td>
                      {!tx.aiDescription && (
                        <button
                          className="ai-suggest-button"
                          onClick={() => handleSuggestDescription(tx.id)}
                          disabled={suggestingId === tx.id}
                        >
                          {suggestingId === tx.id ? '生成中...' : '✨ AI 描述推荐'}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}
    </div>
  );
}
