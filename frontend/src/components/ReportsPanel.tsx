import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { accountApi, reportApi } from '../api';
import type { Account } from '../types';

function currentYearMonth(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
}

export default function ReportsPanel() {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [selectedAccountNumber, setSelectedAccountNumber] = useState('');
  const [month, setMonth] = useState(currentYearMonth());
  const [downloading, setDownloading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    accountApi
      .getMyAccounts()
      .then((result) => {
        setAccounts(result);
        if (result.length > 0) {
          setSelectedAccountNumber(result[0].accountNumber);
        }
      })
      .catch((err) => setError(err instanceof Error ? err.message : '加载账户失败'));
  }, []);

  const handleDownload = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setDownloading(true);
    try {
      await reportApi.downloadMonthlyStatement(selectedAccountNumber, month);
      setSuccess('对账单已生成并下载（Apache PDFBox 实时生成）');
    } catch (err) {
      setError(err instanceof Error ? err.message : '生成对账单失败');
    } finally {
      setDownloading(false);
    }
  };

  return (
    <div className="panel">
      <div className="panel-header">
        <h2>财务报表 · 月度对账单</h2>
      </div>

      {accounts.length === 0 ? (
        <p className="panel-empty">请先在"账户管理"里创建一个账户</p>
      ) : (
        <>
          {error && (
            <div className="error-message">
              <span>⚠️ {error}</span>
            </div>
          )}
          {success && <div className="success-message">✅ {success}</div>}

          <form onSubmit={handleDownload} className="inline-form">
            <div className="form-group">
              <label>账户</label>
              <select
                value={selectedAccountNumber}
                onChange={(e) => setSelectedAccountNumber(e.target.value)}
              >
                {accounts.map((account) => (
                  <option key={account.id} value={account.accountNumber}>
                    {account.accountName}（{account.accountNumber}）
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label>月份</label>
              <input type="month" value={month} onChange={(e) => setMonth(e.target.value)} required />
            </div>

            <button type="submit" className="panel-action-button" disabled={downloading}>
              {downloading ? '生成中...' : '📄 下载 PDF 对账单'}
            </button>
          </form>
        </>
      )}
    </div>
  );
}
