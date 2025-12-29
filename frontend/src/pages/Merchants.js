import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { MagnifyingGlassIcon, FunnelIcon, ArrowPathIcon } from '@heroicons/react/24/outline';
import StatusBadge from '../components/StatusBadge';
import LoadingSpinner from '../components/LoadingSpinner';
import * as api from '../services/api';

const Merchants = () => {
  const [loading, setLoading] = useState(true);
  const [merchants, setMerchants] = useState([]);
  const [riskRanking, setRiskRanking] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [selectedMerchant, setSelectedMerchant] = useState(null);
  const [merchantPrediction, setMerchantPrediction] = useState(null);

  const [error, setError] = useState(null);

  const fetchMerchants = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const [compRes, rankRes] = await Promise.all([
        api.compareMerchants(),
        api.getMerchantRiskRanking(),
      ]);
      setMerchants(compRes.data?.merchantDetails || []);
      setRiskRanking(rankRes.data);
    } catch (err) {
      console.error('Failed to fetch merchants:', err);
      setError('Failed to load merchant data');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchMerchants();
  }, [fetchMerchants]);

  const fetchMerchantDetails = async (merchId) => {
    try {
      const res = await api.getMerchantPrediction(merchId);
      setMerchantPrediction(res.data);
      setSelectedMerchant(merchId);
    } catch (err) {
      console.error('Failed to fetch merchant prediction:', err);
    }
  };

  const filteredMerchants = useMemo(() => {
    return merchants.filter((m) => {
      const matchesSearch = m.merchId.toLowerCase().includes(searchTerm.toLowerCase());
      const matchesStatus = statusFilter === 'ALL' || m.healthStatus === statusFilter;
      return matchesSearch && matchesStatus;
    });
  }, [merchants, searchTerm, statusFilter]);

  if (loading) {
    return <LoadingSpinner text="Loading merchants..." />;
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Merchant Analysis</h1>
          <p className="text-gray-500">View and analyze merchant-specific predictions</p>
        </div>
        <button onClick={fetchMerchants} className="btn-primary flex items-center">
          <ArrowPathIcon className="h-5 w-5 mr-2" />
          Refresh
        </button>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
          {error}
        </div>
      )}

      {/* Risk Summary Cards */}
      {riskRanking && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div className="card bg-green-50 border border-green-200">
            <p className="text-sm text-green-600 font-medium">Low Risk</p>
            <p className="text-3xl font-bold text-green-700">{riskRanking.lowRiskCount || 0}</p>
          </div>
          <div className="card bg-yellow-50 border border-yellow-200">
            <p className="text-sm text-yellow-600 font-medium">Medium Risk</p>
            <p className="text-3xl font-bold text-yellow-700">{riskRanking.mediumRiskCount || 0}</p>
          </div>
          <div className="card bg-red-50 border border-red-200">
            <p className="text-sm text-red-600 font-medium">High Risk</p>
            <p className="text-3xl font-bold text-red-700">{riskRanking.highRiskCount || 0}</p>
          </div>
          <div className="card bg-blue-50 border border-blue-200">
            <p className="text-sm text-blue-600 font-medium">Avg Risk Score</p>
            <p className="text-3xl font-bold text-blue-700">
              {((riskRanking.avgRiskScore || 0) * 100).toFixed(0)}%
            </p>
          </div>
        </div>
      )}

      {/* Filters */}
      <div className="card">
        <div className="flex flex-col md:flex-row gap-4">
          <div className="flex-1 relative">
            <MagnifyingGlassIcon className="h-5 w-5 absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              placeholder="Search merchants..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="input-field pl-10"
            />
          </div>
          <div className="flex items-center gap-2">
            <FunnelIcon className="h-5 w-5 text-gray-400" />
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="input-field w-40"
            >
              <option value="ALL">All Status</option>
              <option value="HEALTHY">Healthy</option>
              <option value="WARNING">Warning</option>
              <option value="CRITICAL">Critical</option>
            </select>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Merchant List */}
        <div className="lg:col-span-2">
          <div className="card">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">
              Merchants ({filteredMerchants.length})
            </h3>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Merchant ID</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Risk Score</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Error Rate</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {filteredMerchants.map((merchant) => (
                    <tr
                      key={merchant.merchId}
                      className={`hover:bg-gray-50 cursor-pointer ${selectedMerchant === merchant.merchId ? 'bg-blue-50' : ''}`}
                      onClick={() => fetchMerchantDetails(merchant.merchId)}
                    >
                      <td className="px-4 py-3 whitespace-nowrap">
                        <span className="font-medium text-gray-900">{merchant.merchId}</span>
                      </td>
                      <td className="px-4 py-3 whitespace-nowrap">
                        <StatusBadge status={merchant.healthStatus} />
                      </td>
                      <td className="px-4 py-3 whitespace-nowrap">
                        <div className="flex items-center">
                          <div className="w-16 bg-gray-200 rounded-full h-2 mr-2">
                            <div
                              className={`h-2 rounded-full ${
                                merchant.riskScore > 0.7 ? 'bg-red-500' :
                                merchant.riskScore > 0.4 ? 'bg-yellow-500' : 'bg-green-500'
                              }`}
                              style={{ width: `${(merchant.riskScore || 0) * 100}%` }}
                            ></div>
                          </div>
                          <span className="text-sm text-gray-600">
                            {((merchant.riskScore || 0) * 100).toFixed(0)}%
                          </span>
                        </div>
                      </td>
                      <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-600">
                        {((merchant.errorRate || 0) * 100).toFixed(2)}%
                      </td>
                      <td className="px-4 py-3 whitespace-nowrap">
                        <button
                          className="text-primary-600 hover:text-primary-800 text-sm font-medium"
                          onClick={(e) => {
                            e.stopPropagation();
                            fetchMerchantDetails(merchant.merchId);
                          }}
                        >
                          View Details
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {filteredMerchants.length === 0 && (
                <p className="text-center text-gray-500 py-8">No merchants found</p>
              )}
            </div>
          </div>
        </div>

        {/* Merchant Details Panel */}
        <div className="lg:col-span-1">
          <div className="card sticky top-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Merchant Details</h3>
            {selectedMerchant && merchantPrediction ? (
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <span className="text-sm text-gray-500">Merchant ID</span>
                  <span className="font-semibold">{merchantPrediction.merchId}</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm text-gray-500">Status</span>
                  <StatusBadge status={merchantPrediction.predictedStatus} />
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm text-gray-500">Confidence</span>
                  <span>{((merchantPrediction.confidence || 0) * 100).toFixed(1)}%</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm text-gray-500">Risk Score</span>
                  <span className={`font-semibold ${
                    merchantPrediction.riskScore > 0.7 ? 'text-red-600' :
                    merchantPrediction.riskScore > 0.4 ? 'text-yellow-600' : 'text-green-600'
                  }`}>
                    {((merchantPrediction.riskScore || 0) * 100).toFixed(0)}%
                  </span>
                </div>

                {/* Metrics Summary */}
                {merchantPrediction.metricsSummary && (
                  <div className="pt-4 border-t">
                    <h4 className="font-medium text-gray-900 mb-2">Metrics</h4>
                    <div className="space-y-2 text-sm">
                      <div className="flex justify-between">
                        <span className="text-gray-500">Total Batches</span>
                        <span>{merchantPrediction.metricsSummary.totalBatches}</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-gray-500">Error Rate</span>
                        <span>{((merchantPrediction.metricsSummary.errorRate || 0) * 100).toFixed(2)}%</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-gray-500">Avg Processing Time</span>
                        <span>{merchantPrediction.metricsSummary.avgProcessingTimeMs?.toFixed(0)}ms</span>
                      </div>
                    </div>
                  </div>
                )}

                {/* Risk Factors */}
                {merchantPrediction.riskFactors?.length > 0 && (
                  <div className="pt-4 border-t">
                    <h4 className="font-medium text-gray-900 mb-2">Risk Factors</h4>
                    <ul className="space-y-1">
                      {merchantPrediction.riskFactors.map((factor, i) => (
                        <li key={i} className="text-sm text-red-600 flex items-start">
                          <span className="mr-2">⚠</span>
                          {factor}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}

                {/* Recommendations */}
                {merchantPrediction.recommendations?.length > 0 && (
                  <div className="pt-4 border-t">
                    <h4 className="font-medium text-gray-900 mb-2">Recommendations</h4>
                    <ul className="space-y-1">
                      {merchantPrediction.recommendations.map((rec, i) => (
                        <li key={i} className="text-sm text-green-600 flex items-start">
                          <span className="mr-2">✓</span>
                          {rec}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            ) : (
              <p className="text-gray-500 text-center py-8">
                Select a merchant to view details
              </p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Merchants;

