import React, { useState, useEffect, useCallback } from 'react';
import {
  ShieldCheckIcon,
  ExclamationTriangleIcon,
  XCircleIcon,
  UserGroupIcon,
  ChartBarIcon,
  ClockIcon,
  ArrowTrendingUpIcon,
} from '@heroicons/react/24/outline';
import StatCard from '../components/StatCard';
import StatusBadge from '../components/StatusBadge';
import LoadingSpinner from '../components/LoadingSpinner';
import { HealthDistributionChart, RiskScoreChart } from '../components/Charts';
import * as api from '../services/api';

const Dashboard = () => {
  const [loading, setLoading] = useState(true);
  const [prediction, setPrediction] = useState(null);
  const [accuracy, setAccuracy] = useState(null);
  const [merchantComparison, setMerchantComparison] = useState(null);
  const [topRiskMerchants, setTopRiskMerchants] = useState([]);
  const [error, setError] = useState(null);

  const fetchDashboardData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const [predRes, accRes, compRes, riskRes] = await Promise.all([
        api.getPrediction().catch(() => ({ data: null })),
        api.getAccuracySummary(7).catch(() => ({ data: null })),
        api.compareMerchants().catch(() => ({ data: null })),
        api.getTopRiskMerchants(5).catch(() => ({ data: [] })),
      ]);

      setPrediction(predRes.data);
      setAccuracy(accRes.data);
      setMerchantComparison(compRes.data);
      setTopRiskMerchants(riskRes.data || []);
    } catch (err) {
      setError('Failed to fetch dashboard data');
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDashboardData();
    const interval = setInterval(fetchDashboardData, 60000); // Refresh every minute
    return () => clearInterval(interval);
  }, [fetchDashboardData]);

  if (loading && !prediction) {
    return <LoadingSpinner text="Loading dashboard..." />;
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">AI Monitoring Dashboard</h1>
          <p className="text-gray-500">Real-time health monitoring and predictions</p>
        </div>
        <button
          onClick={fetchDashboardData}
          className="btn-primary flex items-center"
        >
          <ClockIcon className="h-5 w-5 mr-2" />
          Refresh
        </button>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
          {error}
        </div>
      )}

      {/* Current Status Banner */}
      <div className={`card ${
        prediction?.predictedStatus === 'HEALTHY' ? 'bg-green-50 border-green-200' :
        prediction?.predictedStatus === 'WARNING' ? 'bg-yellow-50 border-yellow-200' :
        prediction?.predictedStatus === 'CRITICAL' ? 'bg-red-50 border-red-200' :
        'bg-gray-50 border-gray-200'
      } border`}>
        <div className="flex items-center justify-between">
          <div className="flex items-center">
            {prediction?.predictedStatus === 'HEALTHY' && <ShieldCheckIcon className="h-12 w-12 text-green-600 mr-4" />}
            {prediction?.predictedStatus === 'WARNING' && <ExclamationTriangleIcon className="h-12 w-12 text-yellow-600 mr-4" />}
            {prediction?.predictedStatus === 'CRITICAL' && <XCircleIcon className="h-12 w-12 text-red-600 mr-4" />}
            <div>
              <h2 className="text-xl font-semibold text-gray-900">System Status</h2>
              <p className="text-gray-600">AI Prediction for next {prediction?.timeHorizon || 6} hours</p>
            </div>
          </div>
          <div className="text-right">
            <StatusBadge status={prediction?.predictedStatus} size="lg" />
            <p className="mt-2 text-sm text-gray-500">
              Confidence: {((prediction?.confidence || 0) * 100).toFixed(1)}%
            </p>
          </div>
        </div>
      </div>

      {/* Key Metrics */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard
          title="Overall Accuracy"
          value={`${((accuracy?.overallAccuracy || 0) * 100).toFixed(1)}%`}
          subtitle="Last 7 days"
          icon={ChartBarIcon}
          color={accuracy?.overallAccuracy >= 0.8 ? 'success' : accuracy?.overallAccuracy >= 0.7 ? 'warning' : 'danger'}
          trend={accuracy?.accuracyTrend > 0 ? 'up' : 'down'}
          trendValue={`${((accuracy?.accuracyTrend || 0) * 100).toFixed(1)}%`}
        />
        <StatCard
          title="Total Merchants"
          value={merchantComparison?.totalMerchantsAnalyzed || 0}
          subtitle="Active in last 24h"
          icon={UserGroupIcon}
          color="primary"
        />
        <StatCard
          title="At Risk"
          value={(merchantComparison?.warningCount || 0) + (merchantComparison?.criticalCount || 0)}
          subtitle="Warning + Critical"
          icon={ExclamationTriangleIcon}
          color="warning"
        />
        <StatCard
            title="F1 Score"
            value={`${((accuracy?.weightedF1Score || 0) * 100).toFixed(1)}%`}
            subtitle="Weighted average"
            icon={ArrowTrendingUpIcon}
            color="primary"
        />
      </div>

      {/* Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Health Distribution */}
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Merchant Health Distribution</h3>
          <HealthDistributionChart
            healthy={merchantComparison?.healthyCount || 0}
            warning={merchantComparison?.warningCount || 0}
            critical={merchantComparison?.criticalCount || 0}
            unknown={merchantComparison?.unknownCount || 0}
          />
        </div>

        {/* Top Risk Merchants */}
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Top Risk Merchants</h3>
          {topRiskMerchants.length > 0 ? (
            <RiskScoreChart merchants={topRiskMerchants} />
          ) : (
            <p className="text-gray-500 text-center py-8">No at-risk merchants</p>
          )}
        </div>
      </div>

      {/* Key Findings & Recommendations */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Key Findings */}
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Key Findings</h3>
          <ul className="space-y-3">
            {(prediction?.keyFindings || []).slice(0, 5).map((finding, index) => (
              <li key={index} className="flex items-start">
                <span className="flex-shrink-0 w-6 h-6 bg-primary-100 text-primary-600 rounded-full flex items-center justify-center text-sm font-medium mr-3">
                  {index + 1}
                </span>
                <span className="text-gray-700">{finding}</span>
              </li>
            ))}
            {(!prediction?.keyFindings || prediction.keyFindings.length === 0) && (
              <li className="text-gray-500">No findings available</li>
            )}
          </ul>
        </div>

        {/* Recommendations */}
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Recommendations</h3>
          <ul className="space-y-3">
            {(prediction?.recommendations || []).slice(0, 5).map((rec, index) => (
              <li key={index} className="flex items-start">
                <span className="flex-shrink-0 w-6 h-6 bg-green-100 text-green-600 rounded-full flex items-center justify-center text-sm mr-3">
                  ✓
                </span>
                <span className="text-gray-700">{rec}</span>
              </li>
            ))}
            {(!prediction?.recommendations || prediction.recommendations.length === 0) && (
              <li className="text-gray-500">No recommendations available</li>
            )}
          </ul>
        </div>
      </div>

      {/* Insights from Accuracy */}
      {accuracy?.insights && accuracy.insights.length > 0 && (
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Accuracy Insights</h3>
          <div className="flex flex-wrap gap-2">
            {accuracy.insights.map((insight, index) => (
              <span
                key={index}
                className="px-3 py-1 bg-blue-50 text-blue-700 rounded-full text-sm"
              >
                {insight}
              </span>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default Dashboard;

