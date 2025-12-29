import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || '';

// Export the base URL for use in components that need direct links to backend
export const getBackendUrl = (path = '') => {
  const baseUrl = API_BASE_URL || 'http://localhost:8080';
  return `${baseUrl}${path}`;
};

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor for API key
api.interceptors.request.use((config) => {
  const apiKey = localStorage.getItem('apiKey');
  if (apiKey) {
    config.headers['X-API-KEY'] = apiKey;
  }
  return config;
});

// Response interceptor for error handling
api.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', error.response?.data || error.message);
    return Promise.reject(error);
  }
);

// ==================== AI Prediction APIs ====================

export const getPrediction = () => api.get('/api/ai/predict');

export const getAIUsage = () => api.get('/api/ai/usage');

export const getAIHealth = () => api.get('/api/ai/health');

export const chatWithAI = (message) =>
  api.post('/api/ai/chat', { message });

// ==================== Merchant Prediction APIs ====================

export const getMerchantPrediction = (merchId) =>
  api.get(`/api/ai/merchant/${merchId}/predict`);

export const getAllMerchantPredictions = () =>
  api.get('/api/ai/merchant/predict/all');

export const compareMerchants = () =>
  api.get('/api/ai/merchant/compare');

export const getMerchantRiskRanking = () =>
  api.get('/api/ai/merchant/risk-ranking');

export const getTopRiskMerchants = (limit = 10) =>
  api.get(`/api/ai/merchant/top-risk?limit=${limit}`);

// ==================== Merchant Threshold APIs ====================

export const getMerchantThresholds = (merchId) =>
  api.get(`/api/ai/merchant/${merchId}/thresholds`);

export const getAllMerchantThresholds = () =>
  api.get('/api/ai/merchant/thresholds');

export const saveMerchantThreshold = (threshold) =>
  api.post('/api/ai/merchant/thresholds', threshold);

export const updateMerchantThreshold = (merchId, threshold) =>
  api.put(`/api/ai/merchant/${merchId}/thresholds`, threshold);

export const deleteMerchantThreshold = (merchId) =>
  api.delete(`/api/ai/merchant/${merchId}/thresholds`);

// ==================== Accuracy APIs ====================

export const getAccuracyMetrics = (days = 30) =>
  api.get(`/api/ai/accuracy/metrics?days=${days}`);

export const getAccuracySummary = (days = 7) =>
  api.get(`/api/ai/accuracy/summary?days=${days}`);

export const getABTestResults = (days = 30) =>
  api.get(`/api/ai/accuracy/ab-test?days=${days}`);

export const getABTestSummary = (days = 7) =>
  api.get(`/api/ai/accuracy/ab-test/summary?days=${days}`);

export const getFeedbackLoopData = (days = 30) =>
  api.get(`/api/ai/accuracy/feedback?days=${days}`);

export const getDriftAnalysis = (days = 30) =>
  api.get(`/api/ai/accuracy/feedback/drift?days=${days}`);

export const getHighConfidenceErrors = (days = 30) =>
  api.get(`/api/ai/accuracy/feedback/errors?days=${days}`);

export const getImprovementRecommendations = (days = 30) =>
  api.get(`/api/ai/accuracy/feedback/recommendations?days=${days}`);

export const recordOutcome = (predictionId, outcome) =>
  api.post(`/api/ai/accuracy/outcomes/${predictionId}`, outcome);

export const triggerEvaluation = () =>
  api.post('/api/ai/accuracy/evaluate');

// ==================== Reports APIs ====================

export const getDailyReport = (date) => {
  const params = date ? `?date=${date}` : '';
  return api.get(`/api/reports/daily${params}`);
};

export const exportDailyReportCsv = (date) => {
  const params = date ? `?date=${date}` : '';
  return api.get(`/api/reports/daily/export${params}`, { responseType: 'blob' });
};

export const getWeeklyReport = (weekOf) => {
  const params = weekOf ? `?weekOf=${weekOf}` : '';
  return api.get(`/api/reports/weekly${params}`);
};

export const exportWeeklyReportCsv = (weekOf) => {
  const params = weekOf ? `?weekOf=${weekOf}` : '';
  return api.get(`/api/reports/weekly/export${params}`, { responseType: 'blob' });
};

export const getDashboardSummary = () =>
  api.get('/api/reports/dashboard');

export const getMerchantScorecard = (merchId, hsn = '', days = 30) =>
  api.get(`/api/reports/merchants/scorecard/${merchId}?hsn=${hsn}&days=${days}`);

export const exportMerchantScorecardCsv = (merchId, hsn = '', days = 30) =>
  api.get(`/api/reports/merchants/scorecard/${merchId}/export?hsn=${hsn}&days=${days}`, { responseType: 'blob' });

export const getAllMerchantScorecards = (days = 30) =>
  api.get(`/api/reports/merchants/scorecards?days=${days}`);

export const exportAllMerchantScorecardsCsv = (days = 30) =>
  api.get(`/api/reports/merchants/scorecards/export?days=${days}`, { responseType: 'blob' });

export const getAtRiskMerchants = (days = 30) =>
  api.get(`/api/reports/merchants/at-risk?days=${days}`);

export const exportAtRiskMerchantsCsv = (days = 30) =>
  api.get(`/api/reports/merchants/at-risk/export?days=${days}`, { responseType: 'blob' });

// ==================== Health Check APIs ====================

export const getSystemHealth = () =>
  api.get('/actuator/health');

export const getPrometheusMetrics = () =>
  api.get('/actuator/prometheus', { responseType: 'text' });

export const getAppInfo = () =>
  api.get('/actuator/info');

// ==================== RAG Document APIs ====================

export const getRAGDocuments = () =>
  api.get('/api/rag/documents');

export const getRAGStats = () =>
  api.get('/api/rag/stats');

export const uploadRAGDocument = (file, description) => {
  const formData = new FormData();
  formData.append('file', file);
  if (description) {
    formData.append('description', description);
  }
  return api.post('/api/rag/documents', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};

export const uploadRAGTextContent = (title, content) =>
  api.post('/api/rag/documents/text', { title, content });

export const deleteRAGDocument = (id) =>
  api.delete(`/api/rag/documents/${id}`);

export const clearAllRAGDocuments = () =>
  api.delete('/api/rag/documents');

// ==================== Utility Functions ====================

export const downloadBlob = (blob, filename) => {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', filename);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};

export default api;

