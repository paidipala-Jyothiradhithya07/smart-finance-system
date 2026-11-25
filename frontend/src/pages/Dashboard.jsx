import { useState } from 'react';
import { useAuth } from '../auth/useAuth';

export default function Dashboard() {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState('overview');
  const [showBalances, setShowBalances] = useState(true);
  const [timeRange, setTimeRange] = useState('month');
  const [targetSavings, setTargetSavings] = useState(2500);
  const [activityFilter, setActivityFilter] = useState('all');

  const currencySymbol = user?.currency === 'INR' ? '₹' : user?.currency === 'GBP' ? '£' : '$';

  const annualSavings = targetSavings * 12;
  const suggestedDailyBudget = Math.max(10, Math.round((targetSavings * 1.5) / 30));

  let savingsTier = 'Balanced Growth';
  let savingsBadgeColor = 'primary';
  if (targetSavings < 1500) {
    savingsTier = 'Steady Saver';
    savingsBadgeColor = 'secondary';
  } else if (targetSavings > 4500) {
    savingsTier = 'Aggressive Wealth Builder';
    savingsBadgeColor = 'success';
  }

  return (
    <div className="py-2 animate-fade-in">
      {/* Hero Welcome Banner */}
      <div className="hero-welcome-banner mb-4">
        <div className="row align-items-center">
          <div className="col-lg-8">
            <div className="d-flex align-items-center gap-2 mb-2">
              <span className="badge bg-white bg-opacity-20 text-white rounded-pill px-3 py-1 fw-normal">
                Active Session &bull; {user?.currency || 'USD'}
              </span>
              <button
                type="button"
                className="btn btn-sm btn-outline-light rounded-pill px-2 py-0 d-inline-flex align-items-center gap-1"
                style={{ fontSize: '0.75rem' }}
                onClick={() => setShowBalances((prev) => !prev)}
                title={showBalances ? 'Hide balances for privacy' : 'Show balances'}
              >
                {showBalances ? (
                  <>
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                      <line x1="1" y1="1" x2="23" y2="23"></line>
                    </svg>
                    <span>Hide Balances</span>
                  </>
                ) : (
                  <>
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                      <circle cx="12" cy="12" r="3"></circle>
                    </svg>
                    <span>Show Balances</span>
                  </>
                )}
              </button>
            </div>

            <h1 className="h2 fw-bold text-white mb-2">Welcome, {user?.name || 'User'}</h1>
            <p className="text-white-50 mb-0" style={{ maxWidth: '600px' }}>
              Your financial dashboard is ready. Track transactions, monitor statements, and optimize your monthly savings with real-time financial intelligence.
            </p>
          </div>
          <div className="col-lg-4 text-lg-end mt-3 mt-lg-0">
            <div className="d-inline-flex flex-column align-items-lg-end gap-1">
              <div className="d-inline-flex align-items-center gap-2 bg-white bg-opacity-10 rounded-3 p-2 text-white">
                <span className="small">Today:</span>
                <span className="fw-semibold small">
                  {new Date().toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })}
                </span>
              </div>
              <span className="text-white-50 small" style={{ fontSize: '0.75rem' }}>
                System Status: All services healthy
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Interactive Controls Bar: Time Range and Tabs */}
      <div className="d-flex flex-column flex-md-row align-items-start align-items-md-center justify-content-between gap-3 mb-4">
        {/* Navigation Tabs */}
        <ul className="nav nav-pills bg-white p-1 rounded-3 border shadow-sm">
          <li className="nav-item">
            <button
              className={`nav-link rounded-2 py-1 px-3 small ${activeTab === 'overview' ? 'active' : ''}`}
              onClick={() => setActiveTab('overview')}
            >
              Overview
            </button>
          </li>
          <li className="nav-item">
            <button
              className={`nav-link rounded-2 py-1 px-3 small ${activeTab === 'planner' ? 'active' : ''}`}
              onClick={() => setActiveTab('planner')}
            >
              Savings Planner
            </button>
          </li>
          <li className="nav-item">
            <button
              className={`nav-link rounded-2 py-1 px-3 small ${activeTab === 'activity' ? 'active' : ''}`}
              onClick={() => setActiveTab('activity')}
            >
              Activity Feed
            </button>
          </li>
        </ul>

        {/* Dynamic Time Range Filter Pills */}
        <div className="btn-group btn-group-sm bg-white p-1 rounded-3 border shadow-sm" role="group">
          <button
            type="button"
            className={`btn btn-sm ${timeRange === 'month' ? 'btn-primary' : 'btn-light text-muted'} rounded-2`}
            onClick={() => setTimeRange('month')}
          >
            This Month
          </button>
          <button
            type="button"
            className={`btn btn-sm ${timeRange === 'quarter' ? 'btn-primary' : 'btn-light text-muted'} rounded-2`}
            onClick={() => setTimeRange('quarter')}
          >
            Last 3 Months
          </button>
          <button
            type="button"
            className={`btn btn-sm ${timeRange === 'ytd' ? 'btn-primary' : 'btn-light text-muted'} rounded-2`}
            onClick={() => setTimeRange('ytd')}
          >
            YTD
          </button>
        </div>
      </div>

      {/* Tab 1: Overview */}
      {activeTab === 'overview' && (
        <div className="animate-fade-in">
          {/* KPI Stat Cards */}
          <div className="row g-3 mb-4">
            <div className="col-12 col-sm-6 col-lg-3">
              <div className="stat-card">
                <div className="d-flex justify-content-between align-items-start">
                  <div>
                    <span className="text-muted small fw-medium">Total Balance</span>
                    <h3 className="h4 fw-bold text-dark mt-1 mb-0">
                      {showBalances ? `${currencySymbol} 0.00` : '••••••••'}
                    </h3>
                  </div>
                  <div className="stat-icon-wrapper bg-primary bg-opacity-10 text-primary">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <rect x="2" y="4" width="20" height="16" rx="2"></rect>
                      <line x1="2" y1="10" x2="22" y2="10"></line>
                    </svg>
                  </div>
                </div>
                <div className="mt-2 text-muted small d-flex align-items-center justify-content-between">
                  <span className="badge bg-light text-secondary">
                    {timeRange === 'month' ? 'Current Period' : timeRange === 'quarter' ? 'Last 3M' : 'Full Year'}
                  </span>
                  <span style={{ fontSize: '0.75rem' }}>Closing Balance</span>
                </div>
              </div>
            </div>

            <div className="col-12 col-sm-6 col-lg-3">
              <div className="stat-card">
                <div className="d-flex justify-content-between align-items-start">
                  <div>
                    <span className="text-muted small fw-medium">Monthly Inflow</span>
                    <h3 className="h4 fw-bold text-success mt-1 mb-0">
                      {showBalances ? `${currencySymbol} 0.00` : '••••••••'}
                    </h3>
                  </div>
                  <div className="stat-icon-wrapper bg-success bg-opacity-10 text-success">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <line x1="12" y1="19" x2="12" y2="5"></line>
                      <polyline points="5 12 12 5 19 12"></polyline>
                    </svg>
                  </div>
                </div>
                <div className="mt-2 text-muted small d-flex align-items-center justify-content-between">
                  <span className="badge bg-success bg-opacity-10 text-success">Credits</span>
                  <span style={{ fontSize: '0.75rem' }}>+0 transactions</span>
                </div>
              </div>
            </div>

            <div className="col-12 col-sm-6 col-lg-3">
              <div className="stat-card">
                <div className="d-flex justify-content-between align-items-start">
                  <div>
                    <span className="text-muted small fw-medium">Monthly Outflow</span>
                    <h3 className="h4 fw-bold text-danger mt-1 mb-0">
                      {showBalances ? `${currencySymbol} 0.00` : '••••••••'}
                    </h3>
                  </div>
                  <div className="stat-icon-wrapper bg-danger bg-opacity-10 text-danger">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <line x1="12" y1="5" x2="12" y2="19"></line>
                      <polyline points="19 12 12 19 5 12"></polyline>
                    </svg>
                  </div>
                </div>
                <div className="mt-2 text-muted small d-flex align-items-center justify-content-between">
                  <span className="badge bg-danger bg-opacity-10 text-danger">Debits</span>
                  <span style={{ fontSize: '0.75rem' }}>-0 transactions</span>
                </div>
              </div>
            </div>

            <div className="col-12 col-sm-6 col-lg-3">
              <div className="stat-card">
                <div className="d-flex justify-content-between align-items-start">
                  <div>
                    <span className="text-muted small fw-medium">Net Savings</span>
                    <h3 className="h4 fw-bold text-primary mt-1 mb-0">
                      {showBalances ? `${currencySymbol} 0.00` : '••••••••'}
                    </h3>
                  </div>
                  <div className="stat-icon-wrapper bg-primary bg-opacity-10 text-primary">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline>
                    </svg>
                  </div>
                </div>
                <div className="mt-2 text-muted small d-flex align-items-center justify-content-between">
                  <span className="badge bg-primary bg-opacity-10 text-primary">Savings Rate</span>
                  <span style={{ fontSize: '0.75rem' }}>0.0%</span>
                </div>
              </div>
            </div>
          </div>

          {/* Quick Action Cards */}
          <div className="row g-3 mb-4">
            <div className="col-12 col-md-4">
              <div className="premium-card p-3 premium-card-interactive h-100">
                <div className="d-flex align-items-center gap-3">
                  <div className="stat-icon-wrapper bg-primary bg-opacity-10 text-primary">
                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                      <polyline points="17 8 12 3 7 8"></polyline>
                      <line x1="12" y1="3" x2="12" y2="15"></line>
                    </svg>
                  </div>
                  <div>
                    <h4 className="h6 fw-bold mb-1">Import Bank Statement</h4>
                    <p className="text-muted small mb-0">Upload .csv or .xlsx to parse transactions</p>
                  </div>
                </div>
              </div>
            </div>

            <div className="col-12 col-md-4">
              <div className="premium-card p-3 premium-card-interactive h-100">
                <div className="d-flex align-items-center gap-3">
                  <div className="stat-icon-wrapper bg-success bg-opacity-10 text-success">
                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <line x1="12" y1="5" x2="12" y2="19"></line>
                      <line x1="5" y1="12" x2="19" y2="12"></line>
                    </svg>
                  </div>
                  <div>
                    <h4 className="h6 fw-bold mb-1">Add Manual Entry</h4>
                    <p className="text-muted small mb-0">Record a single income or expense</p>
                  </div>
                </div>
              </div>
            </div>

            <div className="col-12 col-md-4">
              <div className="premium-card p-3 premium-card-interactive h-100">
                <div className="d-flex align-items-center gap-3">
                  <div className="stat-icon-wrapper bg-warning bg-opacity-10 text-warning">
                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <line x1="18" y1="20" x2="18" y2="10"></line>
                      <line x1="12" y1="20" x2="12" y2="4"></line>
                      <line x1="6" y1="20" x2="6" y2="14"></line>
                    </svg>
                  </div>
                  <div>
                    <h4 className="h6 fw-bold mb-1">Analyze Trends</h4>
                    <p className="text-muted small mb-0">Monthly breakdown and category ratios</p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Empty State / Getting Started */}
          <div className="premium-card p-4 text-center py-5">
            <div
              className="mx-auto mb-3 d-flex align-items-center justify-content-center rounded-circle bg-primary bg-opacity-10 text-primary"
              style={{ width: '64px', height: '64px' }}
            >
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <line x1="12" y1="2" x2="12" y2="6"></line>
                <line x1="12" y1="18" x2="12" y2="22"></line>
                <line x1="4.93" y1="4.93" x2="7.76" y2="7.76"></line>
                <line x1="16.24" y1="16.24" x2="19.07" y2="19.07"></line>
                <line x1="2" y1="12" x2="6" y2="12"></line>
                <line x1="18" y1="12" x2="22" y2="12"></line>
                <line x1="4.93" y1="19.07" x2="7.76" y2="16.24"></line>
                <line x1="16.24" y1="7.76" x2="19.07" y2="4.93"></line>
              </svg>
            </div>
            <h2 className="h4 fw-bold text-dark mb-2">No Financial Data Recorded Yet</h2>
            <p className="text-muted mx-auto mb-3" style={{ maxWidth: '480px' }}>
              Your financial dashboard is ready. Upload your bank statements or record transactions manually to see real-time analytics.
            </p>
          </div>
        </div>
      )}

      {/* Tab 2: Interactive Savings Planner */}
      {activeTab === 'planner' && (
        <div className="premium-card p-4 p-md-5 animate-fade-in">
          <div className="row align-items-center mb-4">
            <div className="col-md-8">
              <h2 className="h4 fw-bold text-dark mb-1">Interactive Monthly Savings Simulator</h2>
              <p className="text-muted small mb-0">
                Adjust your monthly savings target to visualize projected annual accumulation and daily budget thresholds.
              </p>
            </div>
            <div className="col-md-4 text-md-end mt-3 mt-md-0">
              <span className={`badge bg-${savingsBadgeColor} px-3 py-2 rounded-pill fw-semibold`}>
                {savingsTier}
              </span>
            </div>
          </div>

          <div className="bg-light p-4 rounded-3 border mb-4">
            <div className="d-flex justify-content-between align-items-center mb-2">
              <label htmlFor="savings-slider" className="fw-semibold text-dark mb-0">
                Monthly Target Savings
              </label>
              <span className="h4 fw-bold text-primary mb-0">
                {currencySymbol} {targetSavings.toLocaleString()}
              </span>
            </div>
            <input
              id="savings-slider"
              type="range"
              className="interactive-slider"
              min="500"
              max="10000"
              step="250"
              value={targetSavings}
              onChange={(e) => setTargetSavings(Number(e.target.value))}
            />
            <div className="d-flex justify-content-between text-muted small mt-1">
              <span>{currencySymbol} 500</span>
              <span>{currencySymbol} 5,000</span>
              <span>{currencySymbol} 10,000</span>
            </div>
          </div>

          <div className="row g-3">
            <div className="col-12 col-md-4">
              <div className="stat-card bg-primary bg-opacity-10 border-primary border-opacity-25">
                <span className="text-primary fw-medium small">Projected 1-Year Savings</span>
                <h3 className="h4 fw-bold text-primary mt-1 mb-0">
                  {currencySymbol} {annualSavings.toLocaleString()}
                </h3>
                <span className="text-muted small">Based on 12 months</span>
              </div>
            </div>
            <div className="col-12 col-md-4">
              <div className="stat-card bg-success bg-opacity-10 border-success border-opacity-25">
                <span className="text-success fw-medium small">Projected 3-Year Wealth</span>
                <h3 className="h4 fw-bold text-success mt-1 mb-0">
                  {currencySymbol} {(annualSavings * 3).toLocaleString()}
                </h3>
                <span className="text-muted small">Compounded potential</span>
              </div>
            </div>
            <div className="col-12 col-md-4">
              <div className="stat-card bg-dark bg-opacity-10 border-dark border-opacity-25">
                <span className="text-dark fw-medium small">Recommended Daily Allowance</span>
                <h3 className="h4 fw-bold text-dark mt-1 mb-0">
                  {currencySymbol} {suggestedDailyBudget.toLocaleString()} / day
                </h3>
                <span className="text-muted small">Discretionary spending buffer</span>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Tab 3: Activity Feed */}
      {activeTab === 'activity' && (
        <div className="premium-card p-4 animate-fade-in">
          <div className="d-flex justify-content-between align-items-center mb-3 pb-2 border-bottom">
            <h2 className="h5 fw-bold text-dark mb-0">Transaction Activity</h2>
            <div className="btn-group btn-group-sm">
              <button
                type="button"
                className={`btn btn-sm ${activityFilter === 'all' ? 'btn-primary' : 'btn-outline-secondary'}`}
                onClick={() => setActivityFilter('all')}
              >
                All
              </button>
              <button
                type="button"
                className={`btn btn-sm ${activityFilter === 'credits' ? 'btn-primary' : 'btn-outline-secondary'}`}
                onClick={() => setActivityFilter('credits')}
              >
                Credits
              </button>
              <button
                type="button"
                className={`btn btn-sm ${activityFilter === 'debits' ? 'btn-primary' : 'btn-outline-secondary'}`}
                onClick={() => setActivityFilter('debits')}
              >
                Debits
              </button>
            </div>
          </div>

          <div className="text-center py-5">
            <div className="text-muted mb-2">
              <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="8" y1="12" x2="16" y2="12"></line>
              </svg>
            </div>
            <p className="text-muted small mb-0">
              No recent activity found for filter: <strong className="text-dark">{activityFilter.toUpperCase()}</strong>
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
