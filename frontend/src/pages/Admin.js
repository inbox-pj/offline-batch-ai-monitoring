import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
  CogIcon,
  PlusIcon,
  PencilIcon,
  TrashIcon,
  CheckIcon,
  XMarkIcon,
  ArrowPathIcon,
  CpuChipIcon,
  ChartBarIcon,
  ShieldCheckIcon,
  ServerIcon,
  DocumentTextIcon,
  CloudArrowUpIcon,
  DocumentIcon,
} from '@heroicons/react/24/outline';
import LoadingSpinner from '../components/LoadingSpinner';
import * as api from '../services/api';
import { getBackendUrl } from '../services/api';

const Admin = () => {
  const [activeTab, setActiveTab] = useState('thresholds');
  const [loading, setLoading] = useState(true);
  const [thresholds, setThresholds] = useState([]);
  const [editingThreshold, setEditingThreshold] = useState(null);
  const [showAddModal, setShowAddModal] = useState(false);
  const [newThreshold, setNewThreshold] = useState(getDefaultThreshold());
  const [saveStatus, setSaveStatus] = useState(null);
  const [aiUsage, setAiUsage] = useState(null);
  const [aiHealth, setAiHealth] = useState(null);
  const [systemHealth, setSystemHealth] = useState(null);

  function getDefaultThreshold() {
    return {
      merchId: '',
      merchantName: '',
      hsn: '',
      errorRateWarningThreshold: 0.02,
      errorRateCriticalThreshold: 0.05,
      processingTimeWarningMs: 5000,
      processingTimeCriticalMs: 10000,
      riskScoreWarningThreshold: 0.4,
      riskScoreCriticalThreshold: 0.7,
      minDailyBatches: 10,
      maxDailyErrors: 100,
      alertsEnabled: true,
      emailNotificationEnabled: false,
      slackNotificationEnabled: false,
      notificationEmail: '',
      notificationSlackChannel: '',
      priorityLevel: 1,
      notes: '',
    };
  }

  const fetchThresholds = useCallback(async () => {
    try {
      setLoading(true);
      const res = await api.getAllMerchantThresholds();
      setThresholds(res.data || []);
    } catch (err) {
      console.error('Failed to fetch thresholds:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchAIStatus = useCallback(async () => {
    try {
      const [usageRes, healthRes, sysHealthRes] = await Promise.all([
        api.getAIUsage().catch(() => ({ data: null })),
        api.getAIHealth().catch(() => ({ data: null })),
        api.getSystemHealth().catch(() => ({ data: null })),
      ]);
      setAiUsage(usageRes.data);
      setAiHealth(healthRes.data);
      setSystemHealth(sysHealthRes.data);
    } catch (err) {
      console.error('Failed to fetch AI status:', err);
    }
  }, []);

  useEffect(() => {
    if (activeTab === 'thresholds') fetchThresholds();
    else if (activeTab === 'ai-usage' || activeTab === 'system') fetchAIStatus();
  }, [activeTab, fetchThresholds, fetchAIStatus]);

  const handleSaveThreshold = async (threshold) => {
    try {
      setSaveStatus('saving');
      if (threshold.id) {
        await api.updateMerchantThreshold(threshold.merchId, threshold);
      } else {
        await api.saveMerchantThreshold(threshold);
      }
      setSaveStatus('success');
      fetchThresholds();
      setEditingThreshold(null);
      setShowAddModal(false);
      setNewThreshold(getDefaultThreshold());
      setTimeout(() => setSaveStatus(null), 2000);
    } catch (err) {
      console.error('Failed to save threshold:', err);
      setSaveStatus('error');
    }
  };

  const handleDeleteThreshold = async (merchId) => {
    if (!window.confirm(`Are you sure you want to delete threshold for ${merchId}?`)) return;
    try {
      await api.deleteMerchantThreshold(merchId);
      fetchThresholds();
    } catch (err) {
      console.error('Failed to delete threshold:', err);
    }
  };

  const tabs = [
    { id: 'thresholds', name: 'Merchant Thresholds', icon: CogIcon },
    { id: 'ai-usage', name: 'AI Usage & RAG', icon: CpuChipIcon },
    { id: 'system', name: 'System Health', icon: ServerIcon },
    { id: 'api', name: 'API Configuration', icon: DocumentTextIcon },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Admin Configuration</h1>
          <p className="text-gray-500">Manage system settings, AI configuration, and merchant thresholds</p>
        </div>
      </div>

      {/* Save Status Toast */}
      {saveStatus && (
        <div className={`fixed top-4 right-4 px-4 py-2 rounded-lg shadow-lg z-50 ${
          saveStatus === 'success' ? 'bg-green-500 text-white' :
          saveStatus === 'error' ? 'bg-red-500 text-white' :
          'bg-blue-500 text-white'
        }`}>
          {saveStatus === 'success' && '✓ Saved successfully'}
          {saveStatus === 'error' && '✕ Failed to save'}
          {saveStatus === 'saving' && 'Saving...'}
        </div>
      )}

      {/* Tabs */}
      <div className="border-b border-gray-200">
        <nav className="-mb-px flex space-x-8">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`flex items-center py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === tab.id
                  ? 'border-primary-500 text-primary-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              <tab.icon className="h-5 w-5 mr-2" />
              {tab.name}
            </button>
          ))}
        </nav>
      </div>

      {/* Merchant Thresholds Tab */}
      {activeTab === 'thresholds' && (
        <ThresholdsTab
          loading={loading}
          thresholds={thresholds}
          onRefresh={fetchThresholds}
          onAdd={() => setShowAddModal(true)}
          onEdit={setEditingThreshold}
          onDelete={handleDeleteThreshold}
        />
      )}

      {/* AI Usage & RAG Tab */}
      {activeTab === 'ai-usage' && (
        <AIUsageTab
          aiUsage={aiUsage}
          aiHealth={aiHealth}
          onRefresh={fetchAIStatus}
        />
      )}

      {/* System Health Tab */}
      {activeTab === 'system' && (
        <SystemHealthTab
          systemHealth={systemHealth}
          onRefresh={fetchAIStatus}
        />
      )}

      {/* API Configuration Tab */}
      {activeTab === 'api' && (
        <APIConfigurationTab />
      )}

      {/* Add/Edit Modal */}
      {(showAddModal || editingThreshold) && (
        <ThresholdModal
          threshold={editingThreshold || newThreshold}
          onSave={handleSaveThreshold}
          onClose={() => {
            setShowAddModal(false);
            setEditingThreshold(null);
            setNewThreshold(getDefaultThreshold());
          }}
          isEditing={!!editingThreshold}
        />
      )}
    </div>
  );
};

// ==================== Thresholds Tab ====================
const ThresholdsTab = ({ loading, thresholds, onRefresh, onAdd, onEdit, onDelete }) => (
  <div className="space-y-6">
    <div className="flex items-center justify-between">
      <h2 className="text-lg font-semibold text-gray-900">Merchant Alert Thresholds</h2>
      <div className="flex gap-2">
        <button onClick={onRefresh} className="btn-secondary flex items-center">
          <ArrowPathIcon className="h-5 w-5 mr-2" />
          Refresh
        </button>
        <button onClick={onAdd} className="btn-primary flex items-center">
          <PlusIcon className="h-5 w-5 mr-2" />
          Add Threshold
        </button>
      </div>
    </div>

    {loading ? (
      <LoadingSpinner text="Loading thresholds..." />
    ) : (
      <div className="card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Merchant</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Error Rate</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Processing Time</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Risk Score</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Priority</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Alerts</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {thresholds.map((threshold) => (
                <tr key={threshold.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3">
                    <div>
                      <p className="font-medium text-gray-900">{threshold.merchId}</p>
                      <p className="text-sm text-gray-500">{threshold.merchantName || 'N/A'}</p>
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    <span className="text-yellow-600">{(threshold.errorRateWarningThreshold * 100).toFixed(1)}%</span>
                    {' / '}
                    <span className="text-red-600">{(threshold.errorRateCriticalThreshold * 100).toFixed(1)}%</span>
                  </td>
                  <td className="px-4 py-3">
                    <span className="text-yellow-600">{threshold.processingTimeWarningMs}ms</span>
                    {' / '}
                    <span className="text-red-600">{threshold.processingTimeCriticalMs}ms</span>
                  </td>
                  <td className="px-4 py-3">
                    <span className="text-yellow-600">{(threshold.riskScoreWarningThreshold * 100).toFixed(0)}%</span>
                    {' / '}
                    <span className="text-red-600">{(threshold.riskScoreCriticalThreshold * 100).toFixed(0)}%</span>
                  </td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-1 rounded text-sm ${
                      threshold.priorityLevel >= 4 ? 'bg-red-100 text-red-700' :
                      threshold.priorityLevel >= 2 ? 'bg-yellow-100 text-yellow-700' :
                      'bg-gray-100 text-gray-700'
                    }`}>
                      P{threshold.priorityLevel}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex gap-1">
                      {threshold.alertsEnabled && (
                        <span className="px-2 py-1 bg-green-100 text-green-700 rounded text-xs">ON</span>
                      )}
                      {threshold.emailNotificationEnabled && (
                        <span className="px-2 py-1 bg-blue-100 text-blue-700 rounded text-xs">📧</span>
                      )}
                      {threshold.slackNotificationEnabled && (
                        <span className="px-2 py-1 bg-purple-100 text-purple-700 rounded text-xs">💬</span>
                      )}
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex gap-2">
                      <button onClick={() => onEdit(threshold)} className="text-primary-600 hover:text-primary-800">
                        <PencilIcon className="h-5 w-5" />
                      </button>
                      <button onClick={() => onDelete(threshold.merchId)} className="text-red-600 hover:text-red-800">
                        <TrashIcon className="h-5 w-5" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {thresholds.length === 0 && (
            <p className="text-center text-gray-500 py-8">
              No custom thresholds configured. Click "Add Threshold" to create one.
            </p>
          )}
        </div>
      </div>
    )}
  </div>
);

// ==================== AI Usage & RAG Tab ====================
const AIUsageTab = ({ aiUsage, aiHealth, onRefresh }) => {
  const [ragStats, setRagStats] = useState(null);
  const [documents, setDocuments] = useState([]);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState(null);
  const [uploadSuccess, setUploadSuccess] = useState(null);
  const [showTextUpload, setShowTextUpload] = useState(false);
  const [textTitle, setTextTitle] = useState('');
  const [textContent, setTextContent] = useState('');
  const [description, setDescription] = useState('');
  const fileInputRef = useRef(null);

  const fetchRAGData = useCallback(async () => {
    try {
      const [statsRes, docsRes] = await Promise.all([
        api.getRAGStats().catch(() => ({ data: null })),
        api.getRAGDocuments().catch(() => ({ data: { documents: [] } })),
      ]);
      setRagStats(statsRes.data);
      setDocuments(docsRes.data?.documents || []);
    } catch (err) {
      console.error('Failed to fetch RAG data:', err);
    }
  }, []);

  useEffect(() => {
    fetchRAGData();
  }, [fetchRAGData]);

  const handleFileUpload = async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    setUploading(true);
    setUploadError(null);
    setUploadSuccess(null);

    try {
      await api.uploadRAGDocument(file, description);
      setUploadSuccess(`Document "${file.name}" uploaded successfully!`);
      setDescription('');
      fetchRAGData();
      if (fileInputRef.current) fileInputRef.current.value = '';
    } catch (err) {
      setUploadError(err.response?.data?.message || 'Failed to upload document');
    } finally {
      setUploading(false);
      setTimeout(() => { setUploadSuccess(null); setUploadError(null); }, 5000);
    }
  };

  const handleTextUpload = async () => {
    if (!textContent.trim()) {
      setUploadError('Content cannot be empty');
      return;
    }

    setUploading(true);
    setUploadError(null);
    setUploadSuccess(null);

    try {
      await api.uploadRAGTextContent(textTitle, textContent);
      setUploadSuccess('Text content uploaded successfully!');
      setTextTitle('');
      setTextContent('');
      setShowTextUpload(false);
      fetchRAGData();
    } catch (err) {
      setUploadError(err.response?.data?.message || 'Failed to upload content');
    } finally {
      setUploading(false);
      setTimeout(() => { setUploadSuccess(null); setUploadError(null); }, 5000);
    }
  };

  const handleDeleteDocument = async (id, filename) => {
    if (!window.confirm(`Delete document "${filename}"?`)) return;
    try {
      await api.deleteRAGDocument(id);
      fetchRAGData();
    } catch (err) {
      setUploadError('Failed to delete document');
    }
  };

  const handleClearAll = async () => {
    if (!window.confirm('Are you sure you want to delete ALL documents from the RAG store?')) return;
    try {
      await api.clearAllRAGDocuments();
      fetchRAGData();
      setUploadSuccess('All documents cleared');
    } catch (err) {
      setUploadError('Failed to clear documents');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-gray-900">AI Usage & RAG Configuration</h2>
        <button onClick={() => { onRefresh(); fetchRAGData(); }} className="btn-secondary flex items-center">
          <ArrowPathIcon className="h-5 w-5 mr-2" />
          Refresh
        </button>
      </div>

      {/* Upload Status Messages */}
      {uploadSuccess && (
        <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg">
          ✓ {uploadSuccess}
        </div>
      )}
      {uploadError && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
          ✕ {uploadError}
        </div>
      )}

      {/* AI Status */}
      <div className="card">
        <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
          <CpuChipIcon className="h-6 w-6 mr-2 text-primary-600" />
          AI Service Status
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="space-y-4">
            <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
              <span className="text-gray-600">Status</span>
              <span className={`font-semibold ${aiHealth?.status === 'UP' ? 'text-green-600' : 'text-red-600'}`}>
                {aiHealth?.status || 'Unknown'}
              </span>
            </div>
            <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
              <span className="text-gray-600">AI Enabled</span>
              <span className={`font-semibold ${aiHealth?.aiEnabled === 'true' ? 'text-green-600' : 'text-yellow-600'}`}>
                {aiHealth?.aiEnabled === 'true' ? 'Yes' : 'No'}
              </span>
            </div>
          </div>
          <div className="space-y-4">
            <div className="p-4 bg-blue-50 rounded-lg">
              <p className="text-sm text-blue-600 font-medium">Model</p>
              <p className="text-lg font-semibold text-blue-800">Ollama (llama3.2)</p>
            </div>
            <div className="p-4 bg-purple-50 rounded-lg">
              <p className="text-sm text-purple-600 font-medium">RAG Status</p>
              <p className="text-lg font-semibold text-purple-800">
                {ragStats?.enabled ? `${ragStats.documentCount} documents indexed` : 'Not enabled'}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Usage Statistics */}
      <div className="card">
        <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
          <ChartBarIcon className="h-6 w-6 mr-2 text-green-600" />
          Daily Usage Statistics
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div className="p-4 bg-gray-50 rounded-lg text-center">
            <p className="text-sm text-gray-500">Daily Requests</p>
            <p className="text-3xl font-bold text-gray-900">{aiUsage?.dailyRequests || 0}</p>
          </div>
          <div className="p-4 bg-gray-50 rounded-lg text-center">
            <p className="text-sm text-gray-500">Remaining</p>
            <p className="text-3xl font-bold text-green-600">{aiUsage?.remainingRequests || 0}</p>
          </div>
          <div className="p-4 bg-gray-50 rounded-lg text-center">
            <p className="text-sm text-gray-500">Daily Cost</p>
            <p className="text-3xl font-bold text-gray-900">${((aiUsage?.dailyCostCents || 0) / 100).toFixed(2)}</p>
          </div>
          <div className="p-4 bg-gray-50 rounded-lg text-center">
            <p className="text-sm text-gray-500">Budget Remaining</p>
            <p className="text-3xl font-bold text-blue-600">${((aiUsage?.remainingBudgetCents || 0) / 100).toFixed(2)}</p>
          </div>
        </div>
      </div>

      {/* RAG Document Upload */}
      <div className="card">
        <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
          <CloudArrowUpIcon className="h-6 w-6 mr-2 text-purple-600" />
          Upload Documents to RAG
        </h3>
        <p className="text-sm text-gray-600 mb-4">
          Upload documents to enrich the AI's knowledge base. Supported formats: .txt, .md, .json, .csv, .log, .xml
        </p>

        {/* Document Schema & Content Guidelines */}
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
          <h4 className="font-semibold text-blue-900 mb-3 flex items-center">
            <DocumentTextIcon className="h-5 w-5 mr-2" />
            Document Schema & Content Guidelines
          </h4>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            {/* Supported File Types */}
            <div className="bg-white rounded-lg p-4 border border-blue-100">
              <h5 className="font-medium text-gray-900 mb-2">Supported File Types</h5>
              <div className="space-y-2 text-sm">
                <div className="flex items-center">
                  <span className="w-16 px-2 py-0.5 bg-green-100 text-green-700 rounded text-xs font-mono">.txt</span>
                  <span className="ml-2 text-gray-600">Plain text documents</span>
                </div>
                <div className="flex items-center">
                  <span className="w-16 px-2 py-0.5 bg-blue-100 text-blue-700 rounded text-xs font-mono">.md</span>
                  <span className="ml-2 text-gray-600">Markdown documentation</span>
                </div>
                <div className="flex items-center">
                  <span className="w-16 px-2 py-0.5 bg-yellow-100 text-yellow-700 rounded text-xs font-mono">.json</span>
                  <span className="ml-2 text-gray-600">JSON configuration/data</span>
                </div>
                <div className="flex items-center">
                  <span className="w-16 px-2 py-0.5 bg-purple-100 text-purple-700 rounded text-xs font-mono">.csv</span>
                  <span className="ml-2 text-gray-600">Tabular data files</span>
                </div>
                <div className="flex items-center">
                  <span className="w-16 px-2 py-0.5 bg-gray-100 text-gray-700 rounded text-xs font-mono">.log</span>
                  <span className="ml-2 text-gray-600">System/application logs</span>
                </div>
                <div className="flex items-center">
                  <span className="w-16 px-2 py-0.5 bg-orange-100 text-orange-700 rounded text-xs font-mono">.xml</span>
                  <span className="ml-2 text-gray-600">XML configuration files</span>
                </div>
              </div>
            </div>

            {/* Recommended Content Types */}
            <div className="bg-white rounded-lg p-4 border border-blue-100">
              <h5 className="font-medium text-gray-900 mb-2">Recommended Content Types</h5>
              <ul className="space-y-1.5 text-sm text-gray-600">
                <li className="flex items-start">
                  <span className="text-green-500 mr-2">✓</span>
                  <span><strong>Runbooks:</strong> Incident response procedures</span>
                </li>
                <li className="flex items-start">
                  <span className="text-green-500 mr-2">✓</span>
                  <span><strong>SOP Documents:</strong> Standard operating procedures</span>
                </li>
                <li className="flex items-start">
                  <span className="text-green-500 mr-2">✓</span>
                  <span><strong>Error Catalogs:</strong> Known issues & resolutions</span>
                </li>
                <li className="flex items-start">
                  <span className="text-green-500 mr-2">✓</span>
                  <span><strong>System Architecture:</strong> Component documentation</span>
                </li>
                <li className="flex items-start">
                  <span className="text-green-500 mr-2">✓</span>
                  <span><strong>Historical Incidents:</strong> Post-mortem reports</span>
                </li>
                <li className="flex items-start">
                  <span className="text-green-500 mr-2">✓</span>
                  <span><strong>Merchant Configs:</strong> Custom merchant settings</span>
                </li>
              </ul>
            </div>
          </div>

          {/* Document Schema Examples */}
          <div className="mt-4 bg-white rounded-lg p-4 border border-blue-100">
            <h5 className="font-medium text-gray-900 mb-3">Document Schema Examples</h5>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* JSON Schema Example */}
              <div>
                <p className="text-xs font-semibold text-gray-500 mb-1">JSON Document Schema:</p>
                <pre className="bg-gray-900 text-green-400 p-3 rounded text-xs overflow-x-auto">
{`{
  "type": "runbook",
  "title": "High Error Rate Response",
  "category": "incident",
  "severity": "critical",
  "symptoms": ["error_rate > 5%", "timeouts"],
  "steps": [
    "Check database connections",
    "Verify API gateway status",
    "Review recent deployments"
  ],
  "resolution": "Restart affected services",
  "escalation": "On-call DBA"
}`}
                </pre>
              </div>

              {/* CSV Schema Example */}
              <div>
                <p className="text-xs font-semibold text-gray-500 mb-1">CSV Document Schema:</p>
                <pre className="bg-gray-900 text-green-400 p-3 rounded text-xs overflow-x-auto">
{`error_code,description,resolution,severity
E001,Database timeout,Restart connection pool,HIGH
E002,API rate limit exceeded,Increase limits,MEDIUM
E003,Invalid merchant ID,Verify merchant config,LOW
E004,Payment gateway error,Failover to backup,CRITICAL
E005,Batch processing delay,Scale workers,MEDIUM`}
                </pre>
              </div>
            </div>

            {/* Markdown Schema Example */}
            <div className="mt-4">
              <p className="text-xs font-semibold text-gray-500 mb-1">Markdown/Text Document Structure:</p>
              <pre className="bg-gray-900 text-green-400 p-3 rounded text-xs overflow-x-auto">
{`# Incident Response: High Latency

## Symptoms
- Processing time > 10 seconds
- Queue backlog increasing
- Merchant complaints

## Root Causes
1. Database connection pool exhausted
2. Memory pressure on batch servers
3. Network congestion

## Resolution Steps
1. Check database metrics in Grafana
2. Scale up batch processing pods
3. Clear stale connections

## Escalation
Contact: on-call-team@company.com
PagerDuty: High Priority`}
              </pre>
            </div>
          </div>

          {/* Best Practices */}
          <div className="mt-4 grid grid-cols-1 md:grid-cols-3 gap-3">
            <div className="bg-white rounded-lg p-3 border border-blue-100">
              <p className="text-xs font-semibold text-gray-700 mb-1">📄 Max File Size</p>
              <p className="text-sm text-gray-600">10 MB per document</p>
            </div>
            <div className="bg-white rounded-lg p-3 border border-blue-100">
              <p className="text-xs font-semibold text-gray-700 mb-1">🔢 Chunk Size</p>
              <p className="text-sm text-gray-600">1,000 chars with 200 overlap</p>
            </div>
            <div className="bg-white rounded-lg p-3 border border-blue-100">
              <p className="text-xs font-semibold text-gray-700 mb-1">🧠 Embedding Model</p>
              <p className="text-sm text-gray-600">OpenAI text-embedding-3-small</p>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* File Upload */}
          <div className="border-2 border-dashed border-gray-300 rounded-lg p-6 text-center hover:border-primary-500 transition-colors">
            <CloudArrowUpIcon className="h-12 w-12 mx-auto text-gray-400 mb-3" />
            <p className="text-gray-600 mb-2">Drop a file here or click to browse</p>
            <input
              type="file"
              ref={fileInputRef}
              onChange={handleFileUpload}
              accept=".txt,.md,.json,.csv,.log,.xml"
              className="hidden"
              id="file-upload"
            />
            <label htmlFor="file-upload" className="btn-primary cursor-pointer inline-block">
              Choose File
            </label>
            <div className="mt-3">
              <input
                type="text"
                placeholder="Optional description..."
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="input-field w-full"
              />
            </div>
            {uploading && <p className="text-primary-600 mt-2">Uploading...</p>}
          </div>

          {/* Text Upload */}
          <div className="border border-gray-200 rounded-lg p-6">
            <h4 className="font-medium text-gray-900 mb-3">Or paste text content</h4>
            {!showTextUpload ? (
              <button onClick={() => setShowTextUpload(true)} className="btn-secondary w-full">
                <PlusIcon className="h-5 w-5 mr-2 inline" />
                Add Text Content
              </button>
            ) : (
              <div className="space-y-3">
                <input
                  type="text"
                  placeholder="Title (optional)"
                  value={textTitle}
                  onChange={(e) => setTextTitle(e.target.value)}
                  className="input-field w-full"
                />
                <textarea
                  placeholder="Paste your text content here..."
                  value={textContent}
                  onChange={(e) => setTextContent(e.target.value)}
                  rows={4}
                  className="input-field w-full resize-none"
                />
                <div className="flex gap-2">
                  <button onClick={handleTextUpload} className="btn-primary flex-1" disabled={uploading}>
                    {uploading ? 'Uploading...' : 'Upload Text'}
                  </button>
                  <button onClick={() => { setShowTextUpload(false); setTextTitle(''); setTextContent(''); }} className="btn-secondary">
                    Cancel
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Document List */}
      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-gray-900 flex items-center">
            <DocumentIcon className="h-6 w-6 mr-2 text-gray-600" />
            Indexed Documents ({documents.length})
          </h3>
          {documents.length > 0 && (
            <button onClick={handleClearAll} className="text-red-600 hover:text-red-800 text-sm">
              Clear All
            </button>
          )}
        </div>

        {documents.length === 0 ? (
          <p className="text-gray-500 text-center py-8">
            No documents uploaded yet. Upload documents above to enrich the AI's knowledge.
          </p>
        ) : (
          <div className="space-y-3 max-h-96 overflow-y-auto">
            {documents.map((doc) => (
              <div key={doc.id} className="flex items-start justify-between p-4 bg-gray-50 rounded-lg">
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <DocumentTextIcon className="h-5 w-5 text-gray-500" />
                    <span className="font-medium text-gray-900">{doc.filename || 'Untitled'}</span>
                    <span className="text-xs text-gray-500">({(doc.contentLength / 1024).toFixed(1)} KB)</span>
                  </div>
                  <p className="text-sm text-gray-600 mt-1 line-clamp-2">{doc.preview}</p>
                  <p className="text-xs text-gray-400 mt-1">
                    Added: {new Date(doc.createdAt).toLocaleString()}
                  </p>
                </div>
                <button
                  onClick={() => handleDeleteDocument(doc.id, doc.filename)}
                  className="text-red-500 hover:text-red-700 ml-4"
                >
                  <TrashIcon className="h-5 w-5" />
                </button>
              </div>
            ))}
          </div>
        )}

        {ragStats && (
          <div className="mt-4 pt-4 border-t border-gray-200">
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-center text-sm">
              <div>
                <p className="text-gray-500">Total Documents</p>
                <p className="font-semibold text-lg">{ragStats.documentCount}</p>
              </div>
              <div>
                <p className="text-gray-500">Total Characters</p>
                <p className="font-semibold text-lg">{(ragStats.totalCharacters / 1000).toFixed(1)}K</p>
              </div>
              <div>
                <p className="text-gray-500">Vector Dimension</p>
                <p className="font-semibold text-lg">{ragStats.embeddingDimension}</p>
              </div>
              <div>
                <p className="text-gray-500">Similarity Metric</p>
                <p className="font-semibold text-lg">Cosine</p>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* AI Configuration */}
      <div className="card">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">AI Configuration</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="space-y-4">
            <div className="flex justify-between items-center p-3 border-b">
              <span className="text-gray-600">Analysis Window</span>
              <span className="font-semibold">24 hours</span>
            </div>
            <div className="flex justify-between items-center p-3 border-b">
              <span className="text-gray-600">Forecast Horizon</span>
              <span className="font-semibold">6 hours</span>
            </div>
            <div className="flex justify-between items-center p-3 border-b">
              <span className="text-gray-600">Scheduler</span>
              <span className="font-semibold">Every 5 minutes</span>
            </div>
          </div>
          <div className="space-y-4">
            <div className="flex justify-between items-center p-3 border-b">
              <span className="text-gray-600">Daily Request Limit</span>
              <span className="font-semibold">1,000</span>
            </div>
            <div className="flex justify-between items-center p-3 border-b">
              <span className="text-gray-600">Daily Budget</span>
              <span className="font-semibold">$10.00</span>
            </div>
            <div className="flex justify-between items-center p-3 border-b">
              <span className="text-gray-600">A/B Testing</span>
              <span className="font-semibold">10% Rule-Based</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

// ==================== System Health Tab ====================
const SystemHealthTab = ({ systemHealth, onRefresh }) => {
  const getStatusColor = (status) => {
    switch (status?.toUpperCase()) {
      case 'UP': return 'text-green-600 bg-green-100';
      case 'DOWN': return 'text-red-600 bg-red-100';
      default: return 'text-gray-600 bg-gray-100';
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-gray-900">System Health</h2>
        <button onClick={onRefresh} className="btn-secondary flex items-center">
          <ArrowPathIcon className="h-5 w-5 mr-2" />
          Refresh
        </button>
      </div>

      {/* Overall Status */}
      <div className={`card ${
        systemHealth?.status === 'UP' ? 'bg-green-50 border border-green-200' : 'bg-red-50 border border-red-200'
      }`}>
        <div className="flex items-center">
          <ShieldCheckIcon className={`h-12 w-12 mr-4 ${
            systemHealth?.status === 'UP' ? 'text-green-600' : 'text-red-600'
          }`} />
          <div>
            <h3 className="text-xl font-semibold text-gray-900">
              System Status: {systemHealth?.status || 'Unknown'}
            </h3>
            <p className="text-gray-600">All components are monitored in real-time</p>
          </div>
        </div>
      </div>

      {/* Component Health */}
      <div className="card">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Component Health</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {systemHealth?.components && Object.entries(systemHealth.components).map(([name, component]) => (
            <div key={name} className="p-4 border border-gray-200 rounded-lg">
              <div className="flex items-center justify-between mb-2">
                <span className="font-medium text-gray-900 capitalize">{name.replace(/([A-Z])/g, ' $1').trim()}</span>
                <span className={`px-2 py-1 rounded text-sm ${getStatusColor(component?.status)}`}>
                  {component?.status || 'Unknown'}
                </span>
              </div>
              {component?.details && (
                <div className="text-sm text-gray-500">
                  {Object.entries(component.details).slice(0, 2).map(([key, value]) => (
                    <div key={key} className="flex justify-between">
                      <span>{key}:</span>
                      <span className="font-mono">{String(value).substring(0, 30)}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}
          {!systemHealth?.components && (
            <p className="col-span-3 text-center text-gray-500">No component data available</p>
          )}
        </div>
      </div>

      {/* Quick Actions */}
      <div className="card">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Quick Actions</h3>
        <div className="flex flex-wrap gap-4">
          <a
            href={getBackendUrl('/actuator/health')}
            target="_blank"
            rel="noopener noreferrer"
            className="btn-secondary"
          >
            View Full Health
          </a>
          <a
            href={getBackendUrl('/actuator/prometheus')}
            target="_blank"
            rel="noopener noreferrer"
            className="btn-secondary"
          >
            View Prometheus Metrics
          </a>
          <a
            href={getBackendUrl('/swagger-ui.html')}
            target="_blank"
            rel="noopener noreferrer"
            className="btn-secondary"
          >
            API Documentation
          </a>
        </div>
      </div>
    </div>
  );
};

// ==================== API Configuration Tab ====================
const APIConfigurationTab = () => {
  const [apiKey, setApiKey] = useState(localStorage.getItem('apiKey') || '');
  const [saved, setSaved] = useState(false);
  const [expandedCategories, setExpandedCategories] = useState({});

  const handleSaveApiKey = () => {
    localStorage.setItem('apiKey', apiKey);
    setSaved(true);
    setTimeout(() => setSaved(false), 2000);
  };

  const toggleCategory = (category) => {
    setExpandedCategories(prev => ({
      ...prev,
      [category]: !prev[category]
    }));
  };

  const endpointCategories = [
    {
      name: 'AI Prediction',
      endpoints: [
        { method: 'GET', path: '/api/ai/predict', desc: 'Get AI health prediction' },
        { method: 'POST', path: '/api/ai/chat', desc: 'Chat with AI assistant' },
        { method: 'GET', path: '/api/ai/usage', desc: 'Get AI usage statistics' },
        { method: 'GET', path: '/api/ai/health', desc: 'Get AI health status' },
      ]
    },
    {
      name: 'Merchant Predictions',
      endpoints: [
        { method: 'GET', path: '/api/ai/merchant/{id}/predict', desc: 'Get merchant prediction' },
        { method: 'GET', path: '/api/ai/merchant/predict/all', desc: 'Get all merchant predictions' },
        { method: 'GET', path: '/api/ai/merchant/compare', desc: 'Compare all merchants' },
        { method: 'GET', path: '/api/ai/merchant/risk-ranking', desc: 'Get risk rankings' },
        { method: 'GET', path: '/api/ai/merchant/top-risk', desc: 'Get top risk merchants' },
      ]
    },
    {
      name: 'Merchant Thresholds',
      endpoints: [
        { method: 'GET', path: '/api/ai/merchant/{id}/thresholds', desc: 'Get merchant thresholds' },
        { method: 'GET', path: '/api/ai/merchant/thresholds', desc: 'Get all thresholds' },
        { method: 'POST', path: '/api/ai/merchant/thresholds', desc: 'Create threshold' },
        { method: 'PUT', path: '/api/ai/merchant/{id}/thresholds', desc: 'Update threshold' },
        { method: 'DELETE', path: '/api/ai/merchant/{id}/thresholds', desc: 'Delete threshold' },
      ]
    },
    {
      name: 'Accuracy & A/B Testing',
      endpoints: [
        { method: 'GET', path: '/api/ai/accuracy/metrics', desc: 'Get accuracy metrics' },
        { method: 'GET', path: '/api/ai/accuracy/summary', desc: 'Get accuracy summary' },
        { method: 'GET', path: '/api/ai/accuracy/ab-test', desc: 'Get A/B test results' },
        { method: 'GET', path: '/api/ai/accuracy/ab-test/summary', desc: 'Get A/B test summary' },
        { method: 'GET', path: '/api/ai/accuracy/feedback', desc: 'Get feedback loop data' },
        { method: 'GET', path: '/api/ai/accuracy/feedback/drift', desc: 'Get drift analysis' },
        { method: 'GET', path: '/api/ai/accuracy/feedback/errors', desc: 'Get high confidence errors' },
        { method: 'GET', path: '/api/ai/accuracy/feedback/recommendations', desc: 'Get improvement recommendations' },
        { method: 'POST', path: '/api/ai/accuracy/outcomes/{id}', desc: 'Record prediction outcome' },
        { method: 'POST', path: '/api/ai/accuracy/evaluate', desc: 'Trigger evaluation' },
      ]
    },
    {
      name: 'Reports',
      endpoints: [
        { method: 'GET', path: '/api/reports/daily', desc: 'Get daily report' },
        { method: 'GET', path: '/api/reports/daily/export', desc: 'Export daily report CSV' },
        { method: 'GET', path: '/api/reports/weekly', desc: 'Get weekly report' },
        { method: 'GET', path: '/api/reports/weekly/export', desc: 'Export weekly report CSV' },
        { method: 'GET', path: '/api/reports/dashboard', desc: 'Get dashboard summary' },
      ]
    },
    {
      name: 'Merchant Scorecards',
      endpoints: [
        { method: 'GET', path: '/api/reports/merchants/scorecard/{id}', desc: 'Get merchant scorecard' },
        { method: 'GET', path: '/api/reports/merchants/scorecard/{id}/export', desc: 'Export scorecard CSV' },
        { method: 'GET', path: '/api/reports/merchants/scorecards', desc: 'Get all scorecards' },
        { method: 'GET', path: '/api/reports/merchants/scorecards/export', desc: 'Export all scorecards CSV' },
        { method: 'GET', path: '/api/reports/merchants/at-risk', desc: 'Get at-risk merchants' },
        { method: 'GET', path: '/api/reports/merchants/at-risk/export', desc: 'Export at-risk CSV' },
      ]
    },
    {
      name: 'RAG Documents',
      endpoints: [
        { method: 'GET', path: '/api/rag/documents', desc: 'List all RAG documents' },
        { method: 'GET', path: '/api/rag/stats', desc: 'Get RAG statistics' },
        { method: 'POST', path: '/api/rag/documents', desc: 'Upload document file' },
        { method: 'POST', path: '/api/rag/documents/text', desc: 'Add text content' },
        { method: 'DELETE', path: '/api/rag/documents/{id}', desc: 'Delete document' },
        { method: 'DELETE', path: '/api/rag/documents', desc: 'Clear all documents' },
      ]
    },
    {
      name: 'Health & Monitoring',
      endpoints: [
        { method: 'GET', path: '/actuator/health', desc: 'System health status' },
        { method: 'GET', path: '/actuator/prometheus', desc: 'Prometheus metrics' },
        { method: 'GET', path: '/actuator/info', desc: 'Application info' },
      ]
    },
  ];

  const getMethodColor = (method) => {
    switch (method) {
      case 'GET': return 'bg-green-100 text-green-700';
      case 'POST': return 'bg-blue-100 text-blue-700';
      case 'PUT': return 'bg-yellow-100 text-yellow-700';
      case 'DELETE': return 'bg-red-100 text-red-700';
      default: return 'bg-gray-100 text-gray-700';
    }
  };

  return (
    <div className="space-y-6">
      <div className="card">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">API Authentication</h3>
        <div className="space-y-4">
          <div>
            <label className="label">API Key</label>
            <div className="flex gap-2">
              <input
                type="password"
                value={apiKey}
                onChange={(e) => setApiKey(e.target.value)}
                placeholder="Enter your API key"
                className="input-field flex-1"
              />
              <button onClick={handleSaveApiKey} className="btn-primary">
                {saved ? '✓ Saved' : 'Save'}
              </button>
            </div>
            <p className="text-xs text-gray-500 mt-1">
              This key will be sent with all API requests as X-API-KEY header
            </p>
          </div>
        </div>
      </div>

      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-gray-900">API Endpoints</h3>
          <span className="text-sm text-gray-500">
            {endpointCategories.reduce((acc, cat) => acc + cat.endpoints.length, 0)} endpoints
          </span>
        </div>
        <div className="space-y-3">
          {endpointCategories.map((category, catIndex) => (
            <div key={catIndex} className="border border-gray-200 rounded-lg overflow-hidden">
              <button
                onClick={() => toggleCategory(category.name)}
                className="w-full flex items-center justify-between p-3 bg-gray-50 hover:bg-gray-100 transition-colors"
              >
                <div className="flex items-center">
                  <span className="font-medium text-gray-900">{category.name}</span>
                  <span className="ml-2 px-2 py-0.5 bg-gray-200 text-gray-600 text-xs rounded-full">
                    {category.endpoints.length}
                  </span>
                </div>
                <svg
                  className={`w-5 h-5 text-gray-500 transform transition-transform ${
                    expandedCategories[category.name] ? 'rotate-180' : ''
                  }`}
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                </svg>
              </button>
              {expandedCategories[category.name] && (
                <div className="p-2 space-y-1">
                  {category.endpoints.map((ep, i) => (
                    <div key={i} className="flex items-center p-2 bg-white rounded hover:bg-gray-50">
                      <span className={`px-2 py-0.5 rounded text-xs font-mono mr-3 ${getMethodColor(ep.method)}`}>
                        {ep.method}
                      </span>
                      <span className="font-mono text-sm text-gray-700 flex-1">{ep.path}</span>
                      <span className="text-xs text-gray-500">{ep.desc}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>

      <div className="card">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Rate Limiting</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="p-4 bg-gray-50 rounded-lg">
            <p className="text-sm text-gray-500">Requests per minute</p>
            <p className="text-2xl font-bold text-gray-900">100</p>
          </div>
          <div className="p-4 bg-gray-50 rounded-lg">
            <p className="text-sm text-gray-500">Daily AI Request Limit</p>
            <p className="text-2xl font-bold text-gray-900">1,000</p>
          </div>
        </div>
      </div>
    </div>
  );
};

// ==================== Threshold Modal ====================
const ThresholdModal = ({ threshold, onSave, onClose, isEditing }) => {
  const [formData, setFormData] = useState(threshold);

  const handleChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    onSave(formData);
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-4xl w-full max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between p-6 border-b">
          <h2 className="text-xl font-semibold text-gray-900">
            {isEditing ? 'Edit Merchant Threshold' : 'Add Merchant Threshold'}
          </h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
            <XMarkIcon className="h-6 w-6" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-6">
          {/* Basic Info */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="label">Merchant ID *</label>
              <input
                type="text"
                value={formData.merchId}
                onChange={(e) => handleChange('merchId', e.target.value)}
                className="input-field"
                required
                disabled={isEditing}
              />
            </div>
            <div>
              <label className="label">Merchant Name</label>
              <input
                type="text"
                value={formData.merchantName}
                onChange={(e) => handleChange('merchantName', e.target.value)}
                className="input-field"
              />
            </div>
            <div>
              <label className="label">HSN</label>
              <input
                type="text"
                value={formData.hsn}
                onChange={(e) => handleChange('hsn', e.target.value)}
                className="input-field"
              />
            </div>
          </div>

          {/* Thresholds */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <h4 className="text-sm font-semibold text-gray-700 mb-3">Error Rate Thresholds (%)</h4>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="label">Warning</label>
                  <input
                    type="number"
                    step="0.1"
                    value={(formData.errorRateWarningThreshold * 100).toFixed(1)}
                    onChange={(e) => handleChange('errorRateWarningThreshold', parseFloat(e.target.value) / 100)}
                    className="input-field"
                  />
                </div>
                <div>
                  <label className="label">Critical</label>
                  <input
                    type="number"
                    step="0.1"
                    value={(formData.errorRateCriticalThreshold * 100).toFixed(1)}
                    onChange={(e) => handleChange('errorRateCriticalThreshold', parseFloat(e.target.value) / 100)}
                    className="input-field"
                  />
                </div>
              </div>
            </div>
            <div>
              <h4 className="text-sm font-semibold text-gray-700 mb-3">Processing Time (ms)</h4>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="label">Warning</label>
                  <input
                    type="number"
                    value={formData.processingTimeWarningMs}
                    onChange={(e) => handleChange('processingTimeWarningMs', parseInt(e.target.value))}
                    className="input-field"
                  />
                </div>
                <div>
                  <label className="label">Critical</label>
                  <input
                    type="number"
                    value={formData.processingTimeCriticalMs}
                    onChange={(e) => handleChange('processingTimeCriticalMs', parseInt(e.target.value))}
                    className="input-field"
                  />
                </div>
              </div>
            </div>
          </div>

          {/* Priority & Notifications */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="label">Priority (1-10)</label>
              <input
                type="number"
                min="1"
                max="10"
                value={formData.priorityLevel}
                onChange={(e) => handleChange('priorityLevel', parseInt(e.target.value))}
                className="input-field"
              />
            </div>
            <div className="flex items-center pt-6">
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={formData.alertsEnabled}
                  onChange={(e) => handleChange('alertsEnabled', e.target.checked)}
                  className="w-4 h-4"
                />
                <span>Alerts Enabled</span>
              </label>
            </div>
            <div className="flex items-center pt-6">
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={formData.emailNotificationEnabled}
                  onChange={(e) => handleChange('emailNotificationEnabled', e.target.checked)}
                  className="w-4 h-4"
                />
                <span>Email Notifications</span>
              </label>
            </div>
          </div>

          {/* Actions */}
          <div className="flex justify-end gap-3 pt-4 border-t">
            <button type="button" onClick={onClose} className="btn-secondary">
              Cancel
            </button>
            <button type="submit" className="btn-primary flex items-center">
              <CheckIcon className="h-5 w-5 mr-2" />
              {isEditing ? 'Update' : 'Create'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default Admin;

