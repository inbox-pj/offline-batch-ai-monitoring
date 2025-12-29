import React, { useState, useEffect, useCallback } from 'react';
import { ArrowPathIcon } from '@heroicons/react/24/outline';
import LoadingSpinner from '../components/LoadingSpinner';
import { BarChart } from '../components/Charts';
import * as api from '../services/api';

const Accuracy = () => {
  const [loading, setLoading] = useState(true);
  const [days, setDays] = useState(30);
  const [metrics, setMetrics] = useState(null);
  const [error, setError] = useState(null);

  const fetchAccuracyData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await api.getAccuracyMetrics(days);
      setMetrics(res.data);
    } catch (err) {
      console.error('Failed to fetch accuracy:', err);
      setError('Failed to load accuracy data');
    } finally {
      setLoading(false);
    }
  }, [days]);

  useEffect(() => {
    fetchAccuracyData();
  }, [fetchAccuracyData]);

  const getConfusionMatrixData = () => {
    if (!metrics?.confusionMatrix?.matrix) return null;

    const labels = metrics.confusionMatrix.labels || [];
    const matrix = metrics.confusionMatrix.matrix;

    return { labels, matrix };
  };

  const getStatusMetricsData = () => {
    if (!metrics?.metricsByStatus) return null;

    const statuses = Object.keys(metrics.metricsByStatus);
    return {
      labels: statuses,
      datasets: [
        {
          label: 'Precision',
          data: statuses.map(s => (metrics.metricsByStatus[s]?.precision || 0) * 100),
          backgroundColor: 'rgba(59, 130, 246, 0.8)',
        },
        {
          label: 'Recall',
          data: statuses.map(s => (metrics.metricsByStatus[s]?.recall || 0) * 100),
          backgroundColor: 'rgba(16, 185, 129, 0.8)',
        },
        {
          label: 'F1 Score',
          data: statuses.map(s => (metrics.metricsByStatus[s]?.f1Score || 0) * 100),
          backgroundColor: 'rgba(139, 92, 246, 0.8)',
        },
      ],
    };
  };

  if (loading && !metrics) {
    return <LoadingSpinner text="Loading accuracy data..." />;
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Prediction Accuracy</h1>
          <p className="text-gray-500">Model performance metrics and analysis</p>
        </div>
        <div className="flex items-center gap-4">
          <select
            value={days}
            onChange={(e) => setDays(Number(e.target.value))}
            className="input-field w-40"
          >
            <option value={7}>Last 7 days</option>
            <option value={14}>Last 14 days</option>
            <option value={30}>Last 30 days</option>
            <option value={90}>Last 90 days</option>
          </select>
          <button onClick={fetchAccuracyData} className="btn-primary flex items-center">
            <ArrowPathIcon className="h-5 w-5 mr-2" />
            Refresh
          </button>
        </div>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
          {error}
        </div>
      )}

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
        <div className="card text-center">
          <p className="text-sm text-gray-500">Overall Accuracy</p>
          <p className={`text-3xl font-bold ${
            (metrics?.overallAccuracy || 0) >= 0.8 ? 'text-green-600' :
            (metrics?.overallAccuracy || 0) >= 0.7 ? 'text-yellow-600' : 'text-red-600'
          }`}>
            {((metrics?.overallAccuracy || 0) * 100).toFixed(1)}%
          </p>
        </div>
        <div className="card text-center">
          <p className="text-sm text-gray-500">Weighted Precision</p>
          <p className="text-3xl font-bold text-blue-600">
            {((metrics?.weightedPrecision || 0) * 100).toFixed(1)}%
          </p>
        </div>
        <div className="card text-center">
          <p className="text-sm text-gray-500">Weighted Recall</p>
          <p className="text-3xl font-bold text-green-600">
            {((metrics?.weightedRecall || 0) * 100).toFixed(1)}%
          </p>
        </div>
        <div className="card text-center">
          <p className="text-sm text-gray-500">Weighted F1</p>
          <p className="text-3xl font-bold text-purple-600">
            {((metrics?.weightedF1Score || 0) * 100).toFixed(1)}%
          </p>
        </div>
        <div className="card text-center">
          <p className="text-sm text-gray-500">Calibration</p>
          <p className={`text-3xl font-bold ${
            (metrics?.confidenceCalibration || 0) >= 0.9 ? 'text-green-600' : 'text-yellow-600'
          }`}>
            {((metrics?.confidenceCalibration || 0) * 100).toFixed(1)}%
          </p>
        </div>
      </div>

      {/* Prediction Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="card">
          <p className="text-sm text-gray-500">Total Predictions</p>
          <p className="text-2xl font-bold text-gray-900">{metrics?.totalPredictions || 0}</p>
        </div>
        <div className="card">
          <p className="text-sm text-gray-500">Evaluated</p>
          <p className="text-2xl font-bold text-blue-600">{metrics?.evaluatedPredictions || 0}</p>
        </div>
        <div className="card">
          <p className="text-sm text-gray-500">Correct</p>
          <p className="text-2xl font-bold text-green-600">{metrics?.correctPredictions || 0}</p>
        </div>
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Per-Status Metrics */}
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Metrics by Status</h3>
          {getStatusMetricsData() && (
            <BarChart data={getStatusMetricsData()} height={300} />
          )}
        </div>

        {/* Confusion Matrix Visualization */}
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Confusion Matrix</h3>
          {getConfusionMatrixData() && (
            <div className="overflow-x-auto">
              <table className="min-w-full">
                <thead>
                  <tr>
                    <th className="px-2 py-2 text-xs text-gray-500">Predicted ↓ / Actual →</th>
                    {getConfusionMatrixData().labels.map((label) => (
                      <th key={label} className="px-2 py-2 text-xs text-gray-500">{label}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {getConfusionMatrixData().matrix.map((row, i) => (
                    <tr key={i}>
                      <td className="px-2 py-2 text-xs font-medium text-gray-700">
                        {getConfusionMatrixData().labels[i]}
                      </td>
                      {row.map((cell, j) => (
                        <td
                          key={j}
                          className={`px-2 py-2 text-center text-sm font-medium ${
                            i === j ? 'bg-green-100 text-green-800' : 
                            cell > 0 ? 'bg-red-50 text-red-800' : 'bg-gray-50 text-gray-600'
                          }`}
                        >
                          {cell}
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* Insights and Recommendations */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Insights</h3>
          <ul className="space-y-2">
            {(metrics?.insights || []).map((insight, i) => (
              <li key={i} className="flex items-start text-sm">
                <span className="mr-2 text-blue-500">💡</span>
                {insight}
              </li>
            ))}
            {(!metrics?.insights || metrics.insights.length === 0) && (
              <li className="text-gray-500">No insights available</li>
            )}
          </ul>
        </div>
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Recommendations</h3>
          <ul className="space-y-2">
            {(metrics?.recommendations || []).map((rec, i) => (
              <li key={i} className="flex items-start text-sm">
                <span className="mr-2 text-green-500">✓</span>
                {rec}
              </li>
            ))}
            {(!metrics?.recommendations || metrics.recommendations.length === 0) && (
              <li className="text-gray-500">No recommendations available</li>
            )}
          </ul>
        </div>
      </div>

      {/* Trend Indicator */}
      {metrics?.accuracyTrend !== undefined && (
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-2">Accuracy Trend</h3>
          <div className="flex items-center">
            <span className={`text-2xl mr-2 ${metrics.accuracyTrend >= 0 ? 'text-green-500' : 'text-red-500'}`}>
              {metrics.accuracyTrend >= 0 ? '📈' : '📉'}
            </span>
            <span className={`text-lg font-semibold ${metrics.accuracyTrend >= 0 ? 'text-green-600' : 'text-red-600'}`}>
              {metrics.accuracyTrend >= 0 ? '+' : ''}{(metrics.accuracyTrend * 100).toFixed(1)}%
            </span>
            <span className="ml-2 text-gray-500">
              {metrics.accuracyTrend >= 0 ? 'Improving' : 'Declining'} compared to previous period
            </span>
          </div>
        </div>
      )}
    </div>
  );
};

export default Accuracy;

