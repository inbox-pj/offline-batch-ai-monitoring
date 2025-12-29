import React, { useState, useEffect, useCallback } from 'react';
import {
  DocumentTextIcon,
  CalendarDaysIcon,
  ArrowDownTrayIcon,
  ArrowPathIcon,
  ChartBarIcon,
  UserGroupIcon,
} from '@heroicons/react/24/outline';
import LoadingSpinner from '../components/LoadingSpinner';
import StatusBadge from '../components/StatusBadge';
import * as api from '../services/api';

const Reports = () => {
  const [activeTab, setActiveTab] = useState('daily');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [dailyReport, setDailyReport] = useState(null);
  const [weeklyReport, setWeeklyReport] = useState(null);
  const [merchantScorecards, setMerchantScorecards] = useState([]);
  const [atRiskMerchants, setAtRiskMerchants] = useState([]);
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);
  const [selectedWeek, setSelectedWeek] = useState('');
  const [days, setDays] = useState(30);

  const tabs = [
    { id: 'daily', name: 'Daily Summary', icon: CalendarDaysIcon },
    { id: 'weekly', name: 'Weekly Trend', icon: ChartBarIcon },
    { id: 'scorecards', name: 'Merchant Scorecards', icon: UserGroupIcon },
    { id: 'at-risk', name: 'At-Risk Merchants', icon: DocumentTextIcon },
  ];

  const fetchDailyReport = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await api.getDailyReport(selectedDate);
      setDailyReport(res.data);
    } catch (err) {
      setError('Failed to load daily report');
    } finally {
      setLoading(false);
    }
  }, [selectedDate]);

  const fetchWeeklyReport = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await api.getWeeklyReport(selectedWeek || undefined);
      setWeeklyReport(res.data);
    } catch (err) {
      setError('Failed to load weekly report');
    } finally {
      setLoading(false);
    }
  }, [selectedWeek]);

  const fetchMerchantScorecards = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await api.getAllMerchantScorecards(days);
      setMerchantScorecards(res.data || []);
    } catch (err) {
      setError('Failed to load merchant scorecards');
    } finally {
      setLoading(false);
    }
  }, [days]);

  const fetchAtRiskMerchants = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await api.getAtRiskMerchants(days);
      setAtRiskMerchants(res.data || []);
    } catch (err) {
      setError('Failed to load at-risk merchants');
    } finally {
      setLoading(false);
    }
  }, [days]);

  useEffect(() => {
    if (activeTab === 'daily') fetchDailyReport();
    else if (activeTab === 'weekly') fetchWeeklyReport();
    else if (activeTab === 'scorecards') fetchMerchantScorecards();
    else if (activeTab === 'at-risk') fetchAtRiskMerchants();
  }, [activeTab, fetchDailyReport, fetchWeeklyReport, fetchMerchantScorecards, fetchAtRiskMerchants]);

  const handleExport = async (type) => {
    try {
      let response, filename;
      switch (type) {
        case 'daily':
          response = await api.exportDailyReportCsv(selectedDate);
          filename = `daily-report-${selectedDate}.csv`;
          break;
        case 'weekly':
          response = await api.exportWeeklyReportCsv(selectedWeek);
          filename = `weekly-report-${selectedWeek || 'current'}.csv`;
          break;
        default:
          return;
      }
      api.downloadBlob(response.data, filename);
    } catch (err) {
      setError('Failed to export report');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Reports & Analytics</h1>
          <p className="text-gray-500">Generate and export analytical reports</p>
        </div>
      </div>

      <div className="border-b border-gray-200">
        <nav className="-mb-px flex space-x-8">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`flex items-center py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === tab.id
                  ? 'border-primary-500 text-primary-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              <tab.icon className="h-5 w-5 mr-2" />
              {tab.name}
            </button>
          ))}
        </nav>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
          {error}
        </div>
      )}

      {/* Daily Summary Tab */}
      {activeTab === 'daily' && (
        <div className="space-y-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <input
                type="date"
                value={selectedDate}
                onChange={(e) => setSelectedDate(e.target.value)}
                className="input-field w-48"
              />
              <button onClick={fetchDailyReport} className="btn-secondary flex items-center">
                <ArrowPathIcon className="h-5 w-5 mr-2" />
                Refresh
              </button>
            </div>
            <button onClick={() => handleExport('daily')} className="btn-primary flex items-center">
              <ArrowDownTrayIcon className="h-5 w-5 mr-2" />
              Export CSV
            </button>
          </div>

          {loading ? (
            <LoadingSpinner text="Loading daily report..." />
          ) : dailyReport ? (
            <div className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <div className="card text-center">
                  <p className="text-sm text-gray-500">Status</p>
                  <div className="mt-2">
                    <StatusBadge status={dailyReport.overallHealthStatus} size="lg" />
                  </div>
                </div>
                <div className="card text-center">
                  <p className="text-sm text-gray-500">Total Batches</p>
                  <p className="text-3xl font-bold text-gray-900">
                    {dailyReport.totalBatches?.toLocaleString() || 0}
                  </p>
                </div>
                <div className="card text-center">
                  <p className="text-sm text-gray-500">Total Transactions</p>
                  <p className="text-3xl font-bold text-gray-900">
                    {dailyReport.totalTransactions?.toLocaleString() || 0}
                  </p>
                </div>
                <div className="card text-center">
                  <p className="text-sm text-gray-500">Error Rate</p>
                  <p className={`text-3xl font-bold ${
                    (dailyReport.errorRate || 0) > 0.05 ? 'text-red-600' : 'text-green-600'
                  }`}>
                    {((dailyReport.errorRate || 0) * 100).toFixed(2)}%
                  </p>
                </div>
              </div>

              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <div className="card">
                  <h3 className="text-lg font-semibold text-gray-900 mb-4">Key Findings</h3>
                  <ul className="space-y-2">
                    {(dailyReport.keyFindings || []).map((finding, i) => (
                      <li key={i} className="flex items-start text-sm">
                        <span className="mr-2 text-blue-500">•</span>
                        {finding}
                      </li>
                    ))}
                    {(!dailyReport.keyFindings || dailyReport.keyFindings.length === 0) && (
                      <li className="text-gray-500">No findings for this date</li>
                    )}
                  </ul>
                </div>
                <div className="card">
                  <h3 className="text-lg font-semibold text-gray-900 mb-4">Recommendations</h3>
                  <ul className="space-y-2">
                    {(dailyReport.recommendations || []).map((rec, i) => (
                      <li key={i} className="flex items-start text-sm">
                        <span className="mr-2 text-green-500">✓</span>
                        {rec}
                      </li>
                    ))}
                    {(!dailyReport.recommendations || dailyReport.recommendations.length === 0) && (
                      <li className="text-gray-500">No recommendations</li>
                    )}
                  </ul>
                </div>
              </div>
            </div>
          ) : (
            <div className="card text-center py-12">
              <p className="text-gray-500">No data available for selected date</p>
            </div>
          )}
        </div>
      )}

      {/* Weekly Trend Tab */}
      {activeTab === 'weekly' && (
        <div className="space-y-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <input
                type="date"
                value={selectedWeek}
                onChange={(e) => setSelectedWeek(e.target.value)}
                className="input-field w-48"
              />
              <button onClick={fetchWeeklyReport} className="btn-secondary flex items-center">
                <ArrowPathIcon className="h-5 w-5 mr-2" />
                Refresh
              </button>
            </div>
            <button onClick={() => handleExport('weekly')} className="btn-primary flex items-center">
              <ArrowDownTrayIcon className="h-5 w-5 mr-2" />
              Export CSV
            </button>
          </div>

          {loading ? (
            <LoadingSpinner text="Loading weekly report..." />
          ) : weeklyReport ? (
            <div className="card">
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Weekly Summary</h3>
              <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <div className="p-4 bg-gray-50 rounded-lg">
                  <p className="text-sm text-gray-500">Period</p>
                  <p className="font-semibold">{weeklyReport.weekStart} - {weeklyReport.weekEnd}</p>
                </div>
                <div className="p-4 bg-gray-50 rounded-lg">
                  <p className="text-sm text-gray-500">Total Batches</p>
                  <p className="text-2xl font-bold">{weeklyReport.totalBatches || 0}</p>
                </div>
                <div className="p-4 bg-gray-50 rounded-lg">
                  <p className="text-sm text-gray-500">Avg Error Rate</p>
                  <p className="text-2xl font-bold">{((weeklyReport.avgErrorRate || 0) * 100).toFixed(2)}%</p>
                </div>
                <div className="p-4 bg-gray-50 rounded-lg">
                  <p className="text-sm text-gray-500">Week-over-Week</p>
                  <p className={`text-2xl font-bold ${
                    (weeklyReport.weekOverWeekChange || 0) < 0 ? 'text-green-600' : 'text-red-600'
                  }`}>
                    {(weeklyReport.weekOverWeekChange || 0) > 0 ? '+' : ''}
                    {((weeklyReport.weekOverWeekChange || 0) * 100).toFixed(1)}%
                  </p>
                </div>
              </div>
            </div>
          ) : (
            <div className="card text-center py-12">
              <p className="text-gray-500">No data available</p>
            </div>
          )}
        </div>
      )}

      {/* Merchant Scorecards Tab */}
      {activeTab === 'scorecards' && (
        <div className="space-y-6">
          <div className="flex items-center gap-4">
            <select
              value={days}
              onChange={(e) => setDays(Number(e.target.value))}
              className="input-field w-40"
            >
              <option value={7}>Last 7 days</option>
              <option value={30}>Last 30 days</option>
              <option value={90}>Last 90 days</option>
            </select>
            <button onClick={fetchMerchantScorecards} className="btn-secondary flex items-center">
              <ArrowPathIcon className="h-5 w-5 mr-2" />
              Refresh
            </button>
          </div>

          {loading ? (
            <LoadingSpinner text="Loading scorecards..." />
          ) : (
            <div className="card overflow-hidden">
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Merchant</th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Health Score</th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Error Rate</th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Batches</th>
                    </tr>
                  </thead>
                  <tbody className="bg-white divide-y divide-gray-200">
                    {merchantScorecards.map((scorecard) => (
                      <tr key={scorecard.merchId} className="hover:bg-gray-50">
                        <td className="px-4 py-3 font-medium text-gray-900">{scorecard.merchId}</td>
                        <td className="px-4 py-3">
                          <div className="flex items-center">
                            <div className="w-16 bg-gray-200 rounded-full h-2 mr-2">
                              <div
                                className={`h-2 rounded-full ${
                                  scorecard.healthScore >= 80 ? 'bg-green-500' :
                                  scorecard.healthScore >= 60 ? 'bg-yellow-500' : 'bg-red-500'
                                }`}
                                style={{ width: `${scorecard.healthScore || 0}%` }}
                              ></div>
                            </div>
                            <span className="text-sm">{scorecard.healthScore?.toFixed(0) || 0}</span>
                          </div>
                        </td>
                        <td className="px-4 py-3">
                          <StatusBadge status={scorecard.healthStatus} />
                        </td>
                        <td className="px-4 py-3 text-sm">
                          {((scorecard.errorRate || 0) * 100).toFixed(2)}%
                        </td>
                        <td className="px-4 py-3 text-sm">
                          {scorecard.totalBatches?.toLocaleString() || 0}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {merchantScorecards.length === 0 && (
                  <p className="text-center text-gray-500 py-8">No merchant data available</p>
                )}
              </div>
            </div>
          )}
        </div>
      )}

      {/* At-Risk Merchants Tab */}
      {activeTab === 'at-risk' && (
        <div className="space-y-6">
          <div className="flex items-center gap-4">
            <select
              value={days}
              onChange={(e) => setDays(Number(e.target.value))}
              className="input-field w-40"
            >
              <option value={7}>Last 7 days</option>
              <option value={30}>Last 30 days</option>
              <option value={90}>Last 90 days</option>
            </select>
            <button onClick={fetchAtRiskMerchants} className="btn-secondary flex items-center">
              <ArrowPathIcon className="h-5 w-5 mr-2" />
              Refresh
            </button>
          </div>

          {loading ? (
            <LoadingSpinner text="Loading at-risk merchants..." />
          ) : (
            <div className="space-y-4">
              {atRiskMerchants.map((merchant, index) => (
                <div
                  key={merchant.merchId}
                  className={`card border-l-4 ${
                    merchant.riskLevel === 'CRITICAL'
                      ? 'border-red-500 bg-red-50'
                      : merchant.riskLevel === 'HIGH'
                      ? 'border-orange-500 bg-orange-50'
                      : 'border-yellow-500 bg-yellow-50'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center">
                      <span
                        className={`w-8 h-8 rounded-full flex items-center justify-center text-white font-bold mr-4 ${
                          index === 0 ? 'bg-red-500' : index < 3 ? 'bg-orange-500' : 'bg-yellow-500'
                        }`}
                      >
                        {index + 1}
                      </span>
                      <div>
                        <p className="font-semibold text-gray-900">{merchant.merchId}</p>
                        <p className="text-sm text-gray-600">{merchant.merchantName || 'Unknown'}</p>
                      </div>
                    </div>
                    <div className="text-right">
                      <p
                        className={`font-bold ${
                          merchant.riskLevel === 'CRITICAL'
                            ? 'text-red-600'
                            : merchant.riskLevel === 'HIGH'
                            ? 'text-orange-600'
                            : 'text-yellow-600'
                        }`}
                      >
                        {merchant.riskLevel}
                      </p>
                      <p className="text-sm text-gray-500">
                        Risk: {((merchant.riskScore || 0) * 100).toFixed(0)}%
                      </p>
                    </div>
                  </div>
                  {merchant.riskFactors && merchant.riskFactors.length > 0 && (
                    <div className="mt-3 pt-3 border-t border-gray-200">
                      <p className="text-sm font-medium text-gray-700 mb-1">Risk Factors:</p>
                      <div className="flex flex-wrap gap-2">
                        {merchant.riskFactors.map((factor, i) => (
                          <span key={i} className="px-2 py-1 bg-white rounded text-xs text-gray-700">
                            {factor}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              ))}
              {atRiskMerchants.length === 0 && (
                <div className="card text-center py-12">
                  <p className="text-gray-500">🎉 No at-risk merchants found!</p>
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default Reports;

