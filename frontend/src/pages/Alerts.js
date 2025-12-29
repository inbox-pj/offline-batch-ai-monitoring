import React, { useState, useEffect, useCallback } from 'react';
import {
  ExclamationTriangleIcon,
  ShieldExclamationIcon,
  CheckCircleIcon,
  ArrowPathIcon,
  BellIcon,
} from '@heroicons/react/24/outline';
import StatusBadge from '../components/StatusBadge';
import LoadingSpinner from '../components/LoadingSpinner';
import * as api from '../services/api';

const Alerts = () => {
  const [loading, setLoading] = useState(true);
  const [topRiskMerchants, setTopRiskMerchants] = useState([]);
  const [feedbackData, setFeedbackData] = useState(null);
  const [prediction, setPrediction] = useState(null);
  const [error, setError] = useState(null);

  const fetchAlertData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const [riskRes, feedbackRes, predRes] = await Promise.all([
        api.getTopRiskMerchants(10),
        api.getFeedbackLoopData(7),
        api.getPrediction(),
      ]);
      setTopRiskMerchants(riskRes.data || []);
      setFeedbackData(feedbackRes.data);
      setPrediction(predRes.data);
    } catch (err) {
      console.error('Failed to fetch alerts:', err);
      setError('Failed to load alerts data');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAlertData();
  }, [fetchAlertData]);

  const getAlertSeverity = (status) => {
    switch (status) {
      case 'CRITICAL': return 'bg-red-50 border-red-200 text-red-700';
      case 'WARNING': return 'bg-yellow-50 border-yellow-200 text-yellow-700';
      default: return 'bg-gray-50 border-gray-200 text-gray-700';
    }
  };

  if (loading) {
    return <LoadingSpinner text="Loading alerts..." />;
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Alerts & Notifications</h1>
          <p className="text-gray-500">Monitor system alerts and risk notifications</p>
        </div>
        <button onClick={fetchAlertData} className="btn-primary flex items-center">
          <ArrowPathIcon className="h-5 w-5 mr-2" />
          Refresh
        </button>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
          {error}
        </div>
      )}

      {/* Current System Alert */}
      {prediction && prediction.predictedStatus !== 'HEALTHY' && (
        <div className={`card border-2 ${getAlertSeverity(prediction.predictedStatus)}`}>
          <div className="flex items-center">
            <ShieldExclamationIcon className={`h-8 w-8 mr-4 ${
              prediction.predictedStatus === 'CRITICAL' ? 'text-red-600' : 'text-yellow-600'
            }`} />
            <div className="flex-1">
              <h2 className="text-lg font-semibold">
                System Alert: {prediction.predictedStatus}
              </h2>
              <p className="text-sm">
                Confidence: {((prediction.confidence || 0) * 100).toFixed(1)}% |
                Horizon: {prediction.timeHorizon || 6} hours
              </p>
            </div>
            <StatusBadge status={prediction.predictedStatus} size="lg" />
          </div>
          {prediction.keyFindings?.length > 0 && (
            <div className="mt-4 pt-4 border-t">
              <h3 className="text-sm font-semibold mb-2">Key Findings</h3>
              <ul className="space-y-1">
                {prediction.keyFindings.slice(0, 3).map((finding, i) => (
                  <li key={i} className="text-sm flex items-start">
                    <span className="mr-2">•</span>
                    {finding}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}

      {/* No Alerts Banner */}
      {prediction?.predictedStatus === 'HEALTHY' && (
        <div className="card bg-green-50 border border-green-200">
          <div className="flex items-center">
            <CheckCircleIcon className="h-8 w-8 text-green-600 mr-4" />
            <div>
              <h2 className="text-lg font-semibold text-green-800">All Systems Healthy</h2>
              <p className="text-sm text-green-600">No critical alerts at this time</p>
            </div>
          </div>
        </div>
      )}

      {/* Alert Statistics */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="card text-center">
          <BellIcon className="h-8 w-8 mx-auto text-gray-400 mb-2" />
          <p className="text-sm text-gray-500">At-Risk Merchants</p>
          <p className="text-2xl font-bold text-gray-900">{topRiskMerchants.length}</p>
        </div>
        <div className="card text-center">
          <ExclamationTriangleIcon className="h-8 w-8 mx-auto text-yellow-500 mb-2" />
          <p className="text-sm text-gray-500">High Confidence Errors</p>
          <p className="text-2xl font-bold text-gray-900">
            {feedbackData?.highConfidenceErrors?.length || 0}
          </p>
        </div>
        <div className="card text-center">
          <ShieldExclamationIcon className="h-8 w-8 mx-auto text-red-500 mb-2" />
          <p className="text-sm text-gray-500">Error Patterns</p>
          <p className="text-2xl font-bold text-gray-900">
            {feedbackData?.errorPatterns?.length || 0}
          </p>
        </div>
        <div className="card text-center">
          <div className={`h-8 w-8 mx-auto rounded-full mb-2 flex items-center justify-center ${
            feedbackData?.driftAnalysis?.driftDetected ? 'bg-red-100' : 'bg-green-100'
          }`}>
            <span className="text-lg">
              {feedbackData?.driftAnalysis?.driftDetected ? '⚠' : '✓'}
            </span>
          </div>
          <p className="text-sm text-gray-500">Model Drift</p>
          <p className="text-2xl font-bold text-gray-900">
            {feedbackData?.driftAnalysis?.driftDetected ? 'Detected' : 'None'}
          </p>
        </div>
      </div>

      {/* At-Risk Merchants */}
      <div className="card">
        <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
          <ExclamationTriangleIcon className="h-5 w-5 mr-2 text-yellow-500" />
          At-Risk Merchants
        </h3>
        {topRiskMerchants.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Rank</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Merchant</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Risk Score</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Risk Factors</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {topRiskMerchants.map((merchant, index) => (
                  <tr key={merchant.merchId} className="hover:bg-gray-50">
                    <td className="px-4 py-3">
                      <span className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold ${
                        index === 0 ? 'bg-red-100 text-red-600' :
                        index === 1 ? 'bg-yellow-100 text-yellow-600' :
                        'bg-gray-100 text-gray-600'
                      }`}>
                        {index + 1}
                      </span>
                    </td>
                    <td className="px-4 py-3 font-medium text-gray-900">{merchant.merchId}</td>
                    <td className="px-4 py-3">
                      <StatusBadge status={merchant.predictedStatus} />
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center">
                        <div className="w-20 bg-gray-200 rounded-full h-2 mr-2">
                          <div
                            className={`h-2 rounded-full ${
                              merchant.riskScore > 0.7 ? 'bg-red-500' :
                              merchant.riskScore > 0.4 ? 'bg-yellow-500' : 'bg-green-500'
                            }`}
                            style={{ width: `${(merchant.riskScore || 0) * 100}%` }}
                          ></div>
                        </div>
                        <span className="text-sm font-medium">
                          {((merchant.riskScore || 0) * 100).toFixed(0)}%
                        </span>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex flex-wrap gap-1">
                        {(merchant.riskFactors || []).slice(0, 2).map((factor, i) => (
                          <span key={i} className="px-2 py-0.5 bg-red-50 text-red-700 rounded text-xs">
                            {factor}
                          </span>
                        ))}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="text-center text-gray-500 py-8">No at-risk merchants</p>
        )}
      </div>

      {/* High Confidence Errors */}
      {feedbackData?.highConfidenceErrors?.length > 0 && (
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
            <ShieldExclamationIcon className="h-5 w-5 mr-2 text-red-500" />
            High Confidence Errors (Requiring Attention)
          </h3>
          <div className="space-y-3">
            {feedbackData.highConfidenceErrors.slice(0, 5).map((error, i) => (
              <div key={i} className="p-4 bg-red-50 rounded-lg border border-red-200">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm font-medium text-red-700">
                    Prediction #{error.predictionId}
                  </span>
                  <span className="text-sm text-red-600">
                    Confidence: {((error.confidence || 0) * 100).toFixed(0)}%
                  </span>
                </div>
                <p className="text-sm text-red-800">
                  Predicted: <strong>{error.predicted}</strong> →
                  Actual: <strong>{error.actual}</strong>
                </p>
                {error.possibleCauses?.length > 0 && (
                  <div className="mt-2">
                    <p className="text-xs text-red-600">Possible causes:</p>
                    <ul className="text-xs text-red-700">
                      {error.possibleCauses.map((cause, j) => (
                        <li key={j}>• {cause}</li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Improvement Recommendations */}
      {feedbackData?.improvementRecommendations?.length > 0 && (
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Improvement Recommendations</h3>
          <div className="space-y-3">
            {feedbackData.improvementRecommendations.map((rec, i) => (
              <div key={i} className="p-4 bg-blue-50 rounded-lg border border-blue-200 flex items-start">
                <span className={`px-2 py-1 rounded text-xs font-medium mr-3 ${
                  rec.priority === 'HIGH' ? 'bg-red-100 text-red-700' :
                  rec.priority === 'MEDIUM' ? 'bg-yellow-100 text-yellow-700' :
                  'bg-gray-100 text-gray-700'
                }`}>
                  {rec.priority}
                </span>
                <div className="flex-1">
                  <p className="font-medium text-gray-900">{rec.recommendation}</p>
                  <p className="text-sm text-gray-600 mt-1">
                    Area: {rec.area} | Expected improvement: {rec.expectedImprovementPercent}%
                  </p>
                  {rec.rationale && (
                    <p className="text-xs text-gray-500 mt-1">{rec.rationale}</p>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default Alerts;

