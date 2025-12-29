import React, { useState, useEffect, useCallback } from 'react';
import { ArrowPathIcon, BeakerIcon, CheckCircleIcon } from '@heroicons/react/24/outline';
import LoadingSpinner from '../components/LoadingSpinner';
import { BarChart } from '../components/Charts';
import * as api from '../services/api';

const ABTesting = () => {
  const [loading, setLoading] = useState(true);
  const [days, setDays] = useState(30);
  const [abResult, setAbResult] = useState(null);
  const [feedbackData, setFeedbackData] = useState(null);
  const [error, setError] = useState(null);

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const [abRes, feedbackRes] = await Promise.all([
        api.getABTestResults(days),
        api.getFeedbackLoopData(days),
      ]);
      setAbResult(abRes.data);
      setFeedbackData(feedbackRes.data);
    } catch (err) {
      console.error('Failed to fetch A/B test data:', err);
      setError('Failed to load A/B test data');
    } finally {
      setLoading(false);
    }
  }, [days]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const getComparisonChartData = () => {
    if (!abResult) return null;

    return {
      labels: ['Accuracy', 'F1 Score', 'Confidence'],
      datasets: [
        {
          label: 'AI Model',
          data: [
            (abResult.aiModelPerformance?.accuracy || 0) * 100,
            (abResult.aiModelPerformance?.f1Score || 0) * 100,
            (abResult.aiModelPerformance?.averageConfidence || 0) * 100,
          ],
          backgroundColor: 'rgba(59, 130, 246, 0.8)',
        },
        {
          label: 'Rule-Based',
          data: [
            (abResult.ruleBasedPerformance?.accuracy || 0) * 100,
            (abResult.ruleBasedPerformance?.f1Score || 0) * 100,
            (abResult.ruleBasedPerformance?.averageConfidence || 0) * 100,
          ],
          backgroundColor: 'rgba(156, 163, 175, 0.8)',
        },
      ],
    };
  };

  if (loading && !abResult) {
    return <LoadingSpinner text="Loading A/B test results..." />;
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">A/B Testing</h1>
          <p className="text-gray-500">Compare AI vs Rule-Based model performance</p>
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
          <button onClick={fetchData} className="btn-primary flex items-center">
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

      {/* Winner Banner */}
      {abResult && (
        <div className={`card ${
          abResult.winner === 'AI' ? 'bg-blue-50 border-blue-200' :
          abResult.winner === 'RULE_BASED' ? 'bg-gray-50 border-gray-200' :
          'bg-yellow-50 border-yellow-200'
        } border`}>
          <div className="flex items-center justify-between">
            <div className="flex items-center">
              <BeakerIcon className={`h-12 w-12 mr-4 ${
                abResult.winner === 'AI' ? 'text-blue-600' :
                abResult.winner === 'RULE_BASED' ? 'text-gray-600' : 'text-yellow-600'
              }`} />
              <div>
                <h2 className="text-xl font-semibold text-gray-900">
                  {abResult.winner === 'TIE' ? 'Models Performing Similarly' :
                   `${abResult.winner === 'AI' ? 'AI Model' : 'Rule-Based'} Wins!`}
                </h2>
                <p className="text-gray-600">
                  {abResult.isStatisticallySignificant
                    ? '✓ Results are statistically significant'
                    : '⚠ More data needed for significance'}
                </p>
              </div>
            </div>
            <div className="text-right">
              <p className="text-3xl font-bold text-gray-900">
                {abResult.winner !== 'TIE' && (
                  <>+{((abResult.winMargin || 0) * 100).toFixed(1)}%</>
                )}
                {abResult.winner === 'TIE' && '~'}
              </p>
              <p className="text-sm text-gray-500">Win Margin</p>
            </div>
          </div>
        </div>
      )}

      {/* Model Comparison Cards */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* AI Model */}
        <div className={`card ${abResult?.winner === 'AI' ? 'ring-2 ring-blue-500' : ''}`}>
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold text-gray-900">AI Model</h3>
            {abResult?.winner === 'AI' && (
              <span className="px-3 py-1 bg-blue-100 text-blue-700 rounded-full text-sm font-medium">
                Winner
              </span>
            )}
          </div>
          <div className="space-y-4">
            <div className="flex justify-between items-center">
              <span className="text-gray-500">Accuracy</span>
              <span className="text-xl font-bold text-blue-600">
                {((abResult?.aiModelPerformance?.accuracy || 0) * 100).toFixed(1)}%
              </span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-gray-500">F1 Score</span>
              <span className="font-semibold">
                {((abResult?.aiModelPerformance?.f1Score || 0) * 100).toFixed(1)}%
              </span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-gray-500">Avg Confidence</span>
              <span className="font-semibold">
                {((abResult?.aiModelPerformance?.averageConfidence || 0) * 100).toFixed(1)}%
              </span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-gray-500">Avg Response Time</span>
              <span className="font-semibold">
                {abResult?.aiModelPerformance?.averageResponseTimeMs?.toFixed(0) || 0}ms
              </span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-gray-500">Predictions</span>
              <span className="font-semibold">
                {abResult?.aiModelPerformance?.evaluatedPredictions || 0}
              </span>
            </div>
          </div>
        </div>

        {/* Rule-Based Model */}
        <div className={`card ${abResult?.winner === 'RULE_BASED' ? 'ring-2 ring-gray-500' : ''}`}>
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold text-gray-900">Rule-Based Model</h3>
            {abResult?.winner === 'RULE_BASED' && (
              <span className="px-3 py-1 bg-gray-100 text-gray-700 rounded-full text-sm font-medium">
                Winner
              </span>
            )}
          </div>
          <div className="space-y-4">
            <div className="flex justify-between items-center">
              <span className="text-gray-500">Accuracy</span>
              <span className="text-xl font-bold text-gray-600">
                {((abResult?.ruleBasedPerformance?.accuracy || 0) * 100).toFixed(1)}%
              </span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-gray-500">F1 Score</span>
              <span className="font-semibold">
                {((abResult?.ruleBasedPerformance?.f1Score || 0) * 100).toFixed(1)}%
              </span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-gray-500">Avg Confidence</span>
              <span className="font-semibold">
                {((abResult?.ruleBasedPerformance?.averageConfidence || 0) * 100).toFixed(1)}%
              </span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-gray-500">Avg Response Time</span>
              <span className="font-semibold">
                {abResult?.ruleBasedPerformance?.averageResponseTimeMs?.toFixed(0) || 0}ms
              </span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-gray-500">Predictions</span>
              <span className="font-semibold">
                {abResult?.ruleBasedPerformance?.evaluatedPredictions || 0}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Comparison Chart */}
      <div className="card">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Performance Comparison</h3>
        {getComparisonChartData() && (
          <BarChart data={getComparisonChartData()} height={300} />
        )}
      </div>

      {/* Statistical Details */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="card text-center">
          <p className="text-sm text-gray-500">Accuracy Difference</p>
          <p className={`text-2xl font-bold ${
            (abResult?.accuracyDifference || 0) > 0 ? 'text-blue-600' : 'text-gray-600'
          }`}>
            {(abResult?.accuracyDifference || 0) > 0 ? '+' : ''}
            {((abResult?.accuracyDifference || 0) * 100).toFixed(1)}%
          </p>
          <p className="text-xs text-gray-400">AI - Rule-Based</p>
        </div>
        <div className="card text-center">
          <p className="text-sm text-gray-500">P-Value</p>
          <p className="text-2xl font-bold text-gray-900">
            {abResult?.pValue?.toFixed(4) || 'N/A'}
          </p>
          <p className="text-xs text-gray-400">
            {(abResult?.pValue || 1) < 0.05 ? 'Significant' : 'Not Significant'}
          </p>
        </div>
        <div className="card text-center">
          <p className="text-sm text-gray-500">Effect Size</p>
          <p className="text-2xl font-bold text-gray-900">
            {abResult?.effectSize?.toFixed(2) || 'N/A'}
          </p>
          <p className="text-xs text-gray-400">Cohen's d</p>
        </div>
      </div>

      {/* Insights and Recommendations */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Insights</h3>
          <ul className="space-y-2">
            {(abResult?.insights || []).map((insight, i) => (
              <li key={i} className="flex items-start text-sm">
                <span className="mr-2">📊</span>
                {insight}
              </li>
            ))}
          </ul>
        </div>
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Recommendations</h3>
          <ul className="space-y-2">
            {(abResult?.recommendations || []).map((rec, i) => (
              <li key={i} className="flex items-start text-sm">
                <span className="mr-2">💡</span>
                {rec}
              </li>
            ))}
          </ul>
        </div>
      </div>

      {/* Model Drift Detection */}
      {feedbackData?.driftAnalysis && (
        <div className={`card ${
          feedbackData.driftAnalysis.driftDetected 
            ? 'bg-red-50 border-red-200 border' 
            : 'bg-green-50 border-green-200 border'
        }`}>
          <h3 className="text-lg font-semibold text-gray-900 mb-2">Model Drift Analysis</h3>
          <div className="flex items-center">
            <CheckCircleIcon className={`h-8 w-8 mr-3 ${
              feedbackData.driftAnalysis.driftDetected ? 'text-red-500' : 'text-green-500'
            }`} />
            <div>
              <p className="font-medium">
                {feedbackData.driftAnalysis.driftDetected
                  ? `⚠️ Drift Detected: ${feedbackData.driftAnalysis.driftType}`
                  : '✅ No Drift Detected'}
              </p>
              <p className="text-sm text-gray-600">{feedbackData.driftAnalysis.description}</p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ABTesting;

