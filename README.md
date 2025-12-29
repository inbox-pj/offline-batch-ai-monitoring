# AI Monitoring Platform

> AI-powered predictive monitoring system for offline batch processing health analysis with React dashboard and Spring Boot backend.

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org)
[![React](https://img.shields.io/badge/React-18-blue)](https://reactjs.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Quick Start](#-quick-start)
- [Architecture](#-architecture)
- [Backend (Spring Boot)](#-backend-spring-boot)
  - [Features](#backend-features)
  - [API Endpoints](#api-endpoints)
  - [Configuration](#backend-configuration)
  - [Database](#database)
- [Frontend (React)](#-frontend-react)
  - [Features](#frontend-features)
  - [Pages](#pages)
  - [Components](#components)
- [RAG Document Management](#-rag-document-management)
- [Observability & Monitoring](#-observability--monitoring)
  - [Metrics](#metrics)
  - [Dashboards](#dashboards)
  - [Alerts](#alerts)
- [Docker Deployment](#-docker-deployment)
- [Security](#-security)
- [Testing](#-testing)
- [Troubleshooting](#-troubleshooting)

---

## 🎯 Overview

### What is "Offline Batch Processing"?

In payment processing, transactions are sometimes processed in groups (called "batches") rather than one at a time. Think of it like:
- **Real-time processing**: A cashier ringing up each item as you hand it to them
- **Batch processing**: Putting all your items in a basket and ringing them up all at once at the end

Offline batch processing handles these grouped transactions, typically for:
- Credit card authorizations
- Settlement processing
- Merchant transaction summaries
- End-of-day reconciliation

The **AI Monitoring Platform** is an intelligent health monitoring system for **payment processing batch systems**. It acts as a "doctor" for batch operations:

- **Monitors** batch processing health continuously
- **Detects** problems before they become critical
- **Predicts** future issues using AI (Ollama/llama3.2)
- **Recommends** actions to resolve issues
- **Tracks** prediction accuracy over time

### How Does It Work?

1. **Data Collection**: The system continuously collects metrics about batch processing:
- How many transactions were processed
- How many errors occurred
- How long processing took
- Which merchants are affected

2. **AI Analysis**: Using **Ollama** (a local AI engine), the system analyzes patterns:
- Compares current metrics to historical data
- Identifies trends (is error rate increasing?)
- Detects anomalies (unusual patterns)

3. **Prediction**: The AI predicts future health status:
- **HEALTHY**: Everything is working normally
- **WARNING**: Potential issues detected, monitor closely
- **CRITICAL**: Immediate action required

4. **Recommendations**: The system provides actionable advice:
- What to investigate
- Which merchants to check
- What actions to take

### The Problem This Solves

| Without Monitoring | With AI Monitoring |
|--------------------|-------------------|
| Issues discovered when merchants complain | Early warnings hours before critical |
| Reactive firefighting | Proactive prevention |
| Manual threshold checking | AI-powered pattern recognition |
| No historical learning | Continuous accuracy improvement |

### Real-World Example

| Time | Error Rate | AI Prediction |
|------|------------|---------------|
| 8 AM | 0.1% | ✅ HEALTHY - Normal operations |
| 10 AM | 0.5% | ✅ HEALTHY - Slight increase noted |
| 12 PM | 2% | ⚠️ WARNING - Trend detected, monitor MERCH001 |
| 2 PM | 8% | 🚨 CRITICAL - Investigate immediately |

**The AI predicted the problem at 12 PM**, 2 hours before it became critical.

---

## 🚀 Quick Start

### Prerequisites

- Java 21+
- Node.js 18+
- Docker & Docker Compose
- [Ollama](https://ollama.ai/) (for AI features)

### Option 1: Docker Compose (Recommended)

```bash
# Clone the repository
git clone <repository-url>
cd offline-batch-ai-monitoring

# Start all services
docker-compose up --build

# With local Ollama AI server
docker-compose --profile with-ollama up --build
```

### Option 2: Run Locally

**1. Start Ollama:**
```bash
ollama pull llama3.2
ollama serve
```

**2. Start Backend:**
```bash
mvn spring-boot:run
```

**3. Start Frontend:**
```bash
cd frontend
npm install --legacy-peer-deps
npm start
```

### Service URLs

| Service | URL | Description |
|---------|-----|-------------|
| **Frontend Dashboard** | http://localhost:3001 | React web dashboard |
| **Backend API** | http://localhost:8080 | Spring Boot REST API |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | API documentation |
| **Grafana** | http://localhost:3000 | Metrics dashboards (admin/admin) |
| **Prometheus** | http://localhost:9090 | Metrics database |
| **Jaeger** | http://localhost:16686 | Distributed tracing |
| **Alertmanager** | http://localhost:9093 | Alert management |

### Quick Commands

```bash
# Get AI prediction
curl http://localhost:8080/api/ai/predict

# Get merchant prediction
curl http://localhost:8080/api/ai/merchant/MERCH001/predict

# Chat with AI assistant
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "What is the current batch health?"}'

# Check AI usage
curl http://localhost:8080/api/ai/usage
```

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         AI Monitoring Platform                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────┐         ┌─────────────────┐         ┌────────────┐ │
│  │    Frontend     │────────▶│     Backend     │────────▶│  Database  │ │
│  │    (React)      │         │  (Spring Boot)  │         │ PostgreSQL │ │
│  │    :3001        │         │     :8080       │         │   :5432    │ │
│  └─────────────────┘         └────────┬────────┘         └────────────┘ │
│                                       │                                 │
│                              ┌────────▼────────┐                        │
│                              │     Ollama      │                        │
│                              │   (llama3.2)    │                        │
│                              │    :11434       │                        │
│                              └─────────────────┘                        │
│                                                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                        Observability Stack                              │
│  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌─────────────┐           │
│  │Prometheus │  │  Grafana  │  │  Jaeger   │  │Alertmanager │           │
│  │  :9090    │  │   :3000   │  │  :16686   │  │   :9093     │           │
│  └───────────┘  └───────────┘  └───────────┘  └─────────────┘           │
└─────────────────────────────────────────────────────────────────────────┘
```

### Technology Stack

| Layer | Technology |
|-------|------------|
| **Frontend** | React 18, Tailwind CSS, Chart.js, React Router |
| **Backend** | Spring Boot 3.5, Spring AI, Spring Data JPA |
| **AI Engine** | Ollama (llama3.2), Spring AI integration |
| **Database** | PostgreSQL 15 (prod), H2 (dev) |
| **Caching** | Caffeine |
| **Resilience** | Resilience4j (Circuit Breaker, Retry, Bulkhead) |
| **Observability** | Prometheus, Grafana, Jaeger, Micrometer |

---

## ⚙️ Backend (Spring Boot)

### Backend Features

| Feature | Description |
|---------|-------------|
| **AI Predictions** | Ollama-powered health predictions with confidence scores |
| **RAG Integration** | Vector store for historical pattern learning |
| **Merchant Predictions** | Per-merchant health analysis and risk scoring |
| **Prediction Accuracy** | Track precision, recall, F1 score over time |
| **A/B Testing** | Compare AI vs rule-based model performance |
| **Feedback Loop** | Automatic drift detection and improvement recommendations |
| **Hybrid Analysis** | Falls back to rule-based when AI unavailable |
| **Analytics Reports** | Daily summaries, weekly trends, merchant scorecards |
| **CSV Export** | Download reports for offline analysis |
| **Circuit Breaker** | Resilience4j protection for AI service |
| **Rate Limiting** | Configurable request limits |
| **Caching** | Caffeine-based prediction caching |

### API Endpoints

#### AI Prediction APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/ai/predict` | Get system health prediction |
| `POST` | `/api/ai/chat` | Chat with AI assistant |
| `GET` | `/api/ai/usage` | Get AI usage statistics |
| `GET` | `/api/ai/health` | AI service health check |

**Example Response - `/api/ai/predict`:**
```json
{
  "predictedStatus": "WARNING",
  "confidence": 0.78,
  "timeHorizon": 6,
  "keyFindings": [
    "Error rate increased from 2% to 5.2% over 4 hours",
    "Merchant MERCH004 accounts for 60% of errors"
  ],
  "trendAnalysis": {
    "errorRateTrend": "INCREASING",
    "processingTimeTrend": "STABLE",
    "anomalyDetected": true
  },
  "recommendations": [
    "Investigate MERCH004 transaction failures",
    "Check database connection pool"
  ]
}
```

#### Merchant Prediction APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/ai/merchant/{merchId}/predict` | Get merchant prediction |
| `GET` | `/api/ai/merchant/predict/all` | Get all merchant predictions |
| `GET` | `/api/ai/merchant/compare` | Compare all merchants |
| `GET` | `/api/ai/merchant/risk-ranking` | Get risk rankings |
| `GET` | `/api/ai/merchant/top-risk?limit=10` | Get top at-risk merchants |

**Example Response - `/api/ai/merchant/compare`:**
```json
{
  "comparisonTime": "2024-12-29T10:30:00",
  "totalMerchantsAnalyzed": 25,
  "healthyCount": 18,
  "warningCount": 5,
  "criticalCount": 2,
  "merchantDetails": [
    {
      "merchId": "MERCH001",
      "healthStatus": "WARNING",
      "riskScore": 0.55,
      "errorRate": 0.035,
      "overallRiskRank": 2
    }
  ]
}
```

#### Merchant Threshold APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/ai/merchant/{id}/thresholds` | Get merchant thresholds |
| `GET` | `/api/ai/merchant/thresholds` | Get all thresholds |
| `POST` | `/api/ai/merchant/thresholds` | Create threshold |
| `PUT` | `/api/ai/merchant/{id}/thresholds` | Update threshold |
| `DELETE` | `/api/ai/merchant/{id}/thresholds` | Delete threshold |

**Threshold Configuration Example:**
```json
{
  "merchId": "MERCH001",
  "merchantName": "Acme Corporation",
  "errorRateWarningThreshold": 0.02,
  "errorRateCriticalThreshold": 0.05,
  "processingTimeWarningMs": 5000,
  "processingTimeCriticalMs": 10000,
  "riskScoreWarningThreshold": 0.4,
  "riskScoreCriticalThreshold": 0.7,
  "alertsEnabled": true,
  "emailNotificationEnabled": true,
  "priorityLevel": 5
}
```

#### Accuracy Tracking APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/ai/accuracy/metrics?days=30` | Get accuracy metrics |
| `GET` | `/api/ai/accuracy/summary?days=7` | Get accuracy summary |
| `GET` | `/api/ai/accuracy/ab-test?days=30` | Get A/B test results |
| `GET` | `/api/ai/accuracy/feedback?days=30` | Get feedback loop data |
| `GET` | `/api/ai/accuracy/feedback/drift?days=30` | Get drift analysis |
| `POST` | `/api/ai/accuracy/outcomes/{id}` | Record prediction outcome |
| `POST` | `/api/ai/accuracy/evaluate` | Trigger evaluation |

**Example Response - `/api/ai/accuracy/metrics`:**
```json
{
  "totalPredictions": 500,
  "evaluatedPredictions": 450,
  "correctPredictions": 382,
  "overallAccuracy": 0.849,
  "weightedF1Score": 0.845,
  "metricsByStatus": {
    "HEALTHY": { "precision": 0.92, "recall": 0.88, "f1Score": 0.90 },
    "WARNING": { "precision": 0.78, "recall": 0.82, "f1Score": 0.80 },
    "CRITICAL": { "precision": 0.85, "recall": 0.90, "f1Score": 0.87 }
  },
  "confusionMatrix": {
    "truePositives": 382,
    "falsePositives": 35,
    "trueNegatives": 0,
    "falseNegatives": 33
  }
}
```

#### RAG Document APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/rag/documents` | List all RAG documents |
| `POST` | `/api/rag/documents` | Upload document (multipart) |
| `POST` | `/api/rag/documents/text` | Upload text content |
| `DELETE` | `/api/rag/documents/{id}` | Delete document |
| `DELETE` | `/api/rag/documents` | Clear all documents |
| `GET` | `/api/rag/stats` | Get RAG statistics |

#### Analytics & Reports APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/reports/daily?date=2024-12-29` | Daily summary report |
| `GET` | `/api/reports/daily/export` | Export daily report CSV |
| `GET` | `/api/reports/weekly?weekOf=2024-12-23` | Weekly trend report |
| `GET` | `/api/reports/weekly/export` | Export weekly report CSV |
| `GET` | `/api/reports/dashboard` | Dashboard summary |
| `GET` | `/api/reports/merchants/scorecard/{id}` | Merchant scorecard |
| `GET` | `/api/reports/merchants/scorecards` | All merchant scorecards |
| `GET` | `/api/reports/merchants/at-risk` | At-risk merchants |

#### Health Check APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/actuator/health` | System health |
| `GET` | `/actuator/health/liveness` | Kubernetes liveness |
| `GET` | `/actuator/health/readiness` | Kubernetes readiness |
| `GET` | `/actuator/prometheus` | Prometheus metrics |
| `GET` | `/actuator/info` | Application info |

### Backend Configuration

#### Essential Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `dev` | Active profile (dev/prod) |
| `SERVER_PORT` | `8080` | Server port |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama service URL |
| `OLLAMA_CHAT_MODEL` | `llama3.2` | Chat model |
| `AI_PREDICTION_ENABLED` | `true` | Enable AI predictions |
| `DATABASE_URL` | H2 in-memory | Database JDBC URL |
| `DATABASE_USERNAME` | `sa` | Database username |
| `DATABASE_PASSWORD` | (empty) | Database password |

#### AI Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `AI_ANALYSIS_WINDOW_HOURS` | `24` | Analysis lookback hours |
| `AI_FORECAST_HORIZON_HOURS` | `6` | Forecast horizon hours |
| `AI_CONFIDENCE_THRESHOLD` | `0.75` | Minimum confidence |
| `AI_MAX_DAILY_REQUESTS` | `1000` | Max daily AI requests |
| `AI_MAX_DAILY_COST_CENTS` | `10000` | Max daily cost (cents) |
| `AI_CACHE_TTL_MINUTES` | `5` | Cache TTL minutes |
| `AI_FALLBACK_ENABLED` | `true` | Enable rule-based fallback |

#### Scheduler Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `AI_SCHEDULER_ENABLED` | `true` | Enable scheduler |
| `AI_SCHEDULER_INTERVAL_MS` | `300000` | Interval (5 min) |
| `AI_WEEKLY_SUMMARY_CRON` | `0 0 8 * * MON` | Weekly summary cron |

#### Security Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `API_SECURITY_ENABLED` | `false` | Enable API security |
| `API_KEYS` | `demo-key-123` | Comma-separated API keys |
| `API_RATE_LIMIT_ENABLED` | `true` | Enable rate limiting |
| `API_RATE_LIMIT_RPM` | `100` | Requests per minute |

#### Resilience4j Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `CB_SLIDING_WINDOW` | `20` | Circuit breaker window size |
| `CB_FAILURE_THRESHOLD` | `60` | Failure threshold (%) |
| `CB_WAIT_DURATION` | `30s` | Wait duration when open |
| `RETRY_MAX_ATTEMPTS` | `3` | Max retry attempts |
| `TIMEOUT_DURATION` | `30s` | Request timeout |

### Database

#### Development (H2)
```properties
spring.datasource.url=jdbc:h2:mem:offline_batch_ai
spring.h2.console.enabled=true
```
Access H2 Console: http://localhost:8080/h2-console

#### Production (PostgreSQL)
```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/offline_batch_ai
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=postgres
```

#### Database Schema

| Table | Description |
|-------|-------------|
| `offline_batch_metrics` | Batch processing metrics |
| `ai_prediction_audit` | Prediction history and outcomes |
| `merchant_alert_threshold` | Custom merchant thresholds |
| `flyway_schema_history` | Database migrations |

---

## 🎨 Frontend (React)

### Frontend Features

| Feature | Description |
|---------|-------------|
| **Interactive Dashboard** | Real-time system health overview with charts |
| **Merchant Analysis** | View, filter, and compare merchant predictions |
| **Accuracy Metrics** | Track prediction accuracy with confusion matrix |
| **A/B Testing Dashboard** | Compare AI vs rule-based performance |
| **Reports & Analytics** | Daily/weekly reports with CSV export |
| **Alerts & Notifications** | Monitor at-risk merchants and system alerts |
| **AI Chat** | Interactive conversation with AI assistant |
| **Admin Configuration** | Manage thresholds, upload RAG documents |

### Pages

| Page | Path | Description |
|------|------|-------------|
| **Dashboard** | `/` | Main overview with health status, predictions, key metrics |
| **Merchants** | `/merchants` | Merchant list with risk scores, filtering, predictions |
| **Accuracy** | `/accuracy` | Prediction accuracy metrics, confusion matrix, trends |
| **A/B Testing** | `/ab-testing` | AI vs rule-based comparison with statistical analysis |
| **Reports** | `/reports` | Daily/weekly reports, merchant scorecards, exports |
| **Alerts** | `/alerts` | System alerts, at-risk merchants, recommendations |
| **AI Chat** | `/chat` | Chat interface with AI assistant |
| **Admin** | `/admin` | Thresholds, AI usage, RAG documents, system health |

### Components

| Component | File | Description |
|-----------|------|-------------|
| **Sidebar** | `Sidebar.js` | Navigation sidebar with all page links |
| **StatusBadge** | `StatusBadge.js` | Health status indicator (HEALTHY/WARNING/CRITICAL) |
| **LoadingSpinner** | `LoadingSpinner.js` | Loading state indicator |
| **Charts** | `Charts.js` | Chart.js wrappers (Line, Bar, Doughnut) |

### Frontend Project Structure

```
frontend/
├── public/
│   └── index.html
├── src/
│   ├── components/
│   │   ├── Charts.js           # Chart components
│   │   ├── LoadingSpinner.js   # Loading indicator
│   │   ├── Sidebar.js          # Navigation sidebar
│   │   └── StatusBadge.js      # Status badges
│   ├── pages/
│   │   ├── Dashboard.js        # Main dashboard
│   │   ├── Merchants.js        # Merchant analysis
│   │   ├── Accuracy.js         # Accuracy metrics
│   │   ├── ABTesting.js        # A/B testing
│   │   ├── Reports.js          # Analytics reports
│   │   ├── Alerts.js           # Alerts & notifications
│   │   ├── AIChat.js           # AI chat interface
│   │   └── Admin.js            # Admin configuration
│   ├── services/
│   │   └── api.js              # API service layer
│   ├── App.js                  # Main app with routing
│   └── index.js                # Entry point
├── package.json
├── tailwind.config.js
└── nginx.conf                  # Production nginx config
```

### Frontend Configuration

#### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `REACT_APP_API_URL` | (empty) | Backend API URL |

#### Development

```bash
cd frontend
npm install --legacy-peer-deps
npm start
```
Runs at http://localhost:3000 with hot reload.

#### Production Build

```bash
npm run build
```
Creates optimized build in `build/` folder.

#### Docker Build

```bash
docker build --target frontend -t ai-monitor-frontend .
```

---

## 📚 RAG Document Management

### What is RAG?

**RAG (Retrieval-Augmented Generation)** enhances AI predictions by:

1. **Storing** historical analysis results and documents
2. **Retrieving** relevant past patterns when making predictions
3. **Augmenting** AI context with historical knowledge

### Uploading Documents

#### Via Admin UI

1. Navigate to **Admin** → **AI Usage & RAG** tab
2. Upload files (.txt, .md, .json, .csv, .log, .xml)
3. Or paste text content directly
4. View and manage indexed documents

#### Via API

**Upload File:**
```bash
curl -X POST http://localhost:8080/api/rag/documents \
  -F "file=@document.txt" \
  -F "description=Historical batch patterns"
```

**Upload Text:**
```bash
curl -X POST http://localhost:8080/api/rag/documents/text \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Error Patterns",
    "content": "When error rate exceeds 5%, check database connections..."
  }'
```

### RAG Statistics

```bash
curl http://localhost:8080/api/rag/stats
```

Response:
```json
{
  "enabled": true,
  "documentCount": 15,
  "totalCharacters": 45000,
  "vectorStoreType": "SimpleDocumentStore (In-Memory)",
  "embeddingDimension": 384,
  "similarityMetric": "Cosine Similarity"
}
```

---

## 📊 Observability & Monitoring

### Metrics

#### AI Metrics

| Metric | Description |
|--------|-------------|
| `ai_predictions_total` | Total predictions made |
| `ai_predictions_by_status` | Predictions by status |
| `ai_prediction_confidence` | Confidence distribution |
| `ai_prediction_latency` | Prediction response time |
| `ai_usage_daily_requests` | Daily request count |
| `ai_usage_daily_cost_cents` | Daily cost |

#### Batch Metrics

| Metric | Description |
|--------|-------------|
| `offline_batch_total` | Total batches processed |
| `offline_batch_errors` | Error count |
| `offline_batch_processing_time` | Processing time histogram |

#### Application Metrics

| Metric | Description |
|--------|-------------|
| `http_server_requests` | HTTP request metrics |
| `jvm_memory_used` | JVM memory usage |
| `db_pool_active` | Database connection pool |

### Dashboards

#### Pre-built Grafana Dashboards

| Dashboard | Description |
|-----------|-------------|
| **AI Monitoring Overview** | System health, predictions, accuracy |
| **Merchant Analysis** | Merchant-level metrics and trends |
| **Prediction Accuracy** | Accuracy metrics, confusion matrix |

Access: http://localhost:3000 (admin/admin)

#### Dashboard Panels

- System Health Status (gauge)
- Predictions Over Time (line chart)
- Error Rate Trend (line chart)
- Merchant Risk Distribution (pie chart)
- Processing Time Histogram (histogram)
- AI Usage Statistics (stat panel)

### Alerts

#### Prometheus Alert Rules

| Alert | Condition | Severity |
|-------|-----------|----------|
| `HighErrorRate` | Error rate > 10% for 5m | Critical |
| `AIServiceDown` | AI service unavailable | Warning |
| `HighPredictionLatency` | Latency > 30s | Warning |
| `LowAccuracy` | Accuracy < 70% | Warning |
| `DriftDetected` | Model drift score > 0.3 | Warning |

#### Alertmanager Configuration

Located at `docker/alertmanager/alertmanager.yml`

```yaml
route:
  receiver: 'default-receiver'
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h

receivers:
  - name: 'default-receiver'
    # Configure email, Slack, PagerDuty, etc.
```

---

## 🐳 Docker Deployment

### Docker Commands

```bash
# Build all images
docker-compose build

# Start all services
docker-compose up -d

# Start with local Ollama
docker-compose --profile with-ollama up -d

# View logs
docker-compose logs -f backend
docker-compose logs -f frontend

# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### Building Individual Images

```bash
# Build backend image
docker build --target backend -t ai-monitor-backend .

# Build frontend image
docker build --target frontend -t ai-monitor-frontend .
```

### Docker Compose Services

| Service | Image | Ports | Description |
|---------|-------|-------|-------------|
| `backend` | Custom | 8080 | Spring Boot API |
| `frontend` | Custom | 3001 | React dashboard |
| `postgres` | postgres:15-alpine | 5432 | Database |
| `prometheus` | prom/prometheus | 9090 | Metrics |
| `grafana` | grafana/grafana | 3000 | Dashboards |
| `jaeger` | jaegertracing/all-in-one | 16686 | Tracing |
| `alertmanager` | prom/alertmanager | 9093 | Alerts |
| `ollama` | ollama/ollama | 11434 | AI (optional) |

### Production Deployment

```bash
# Set production environment
export SPRING_PROFILES_ACTIVE=prod
export DATABASE_URL=jdbc:postgresql://prod-db:5432/offline_batch_ai
export DATABASE_USERNAME=app_user
export DATABASE_PASSWORD=secure_password
export API_SECURITY_ENABLED=true
export API_KEYS=production-key-1,production-key-2
export AI_PREDICTION_ENABLED=true
export OLLAMA_BASE_URL=http://ollama-prod:11434

# Deploy
docker-compose -f docker-compose.prod.yml up -d
```

---

## 🔐 Security

### API Authentication

Enable API key authentication:

```bash
export API_SECURITY_ENABLED=true
export API_KEYS=your-secure-api-key
```

Include API key in requests:
```bash
curl -H "X-API-KEY: your-secure-api-key" http://localhost:8080/api/ai/predict
```

### Rate Limiting

Configure rate limits:

```bash
export API_RATE_LIMIT_ENABLED=true
export API_RATE_LIMIT_RPM=100  # Requests per minute
```

### AI Cost Control

Set daily limits:

```bash
export AI_MAX_DAILY_REQUESTS=1000
export AI_MAX_DAILY_COST_CENTS=10000  # $100/day
```

### Security Headers

The application includes:
- CORS configuration
- CSRF protection (configurable)
- Security headers (X-Frame-Options, etc.)

---

## 🧪 Testing

### Backend Tests

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### Frontend Tests

```bash
cd frontend

# Run tests
npm test

# Run with coverage
npm test -- --coverage
```

### Integration Tests

```bash
# Requires PostgreSQL running
mvn verify -DskipUnitTests
```

### Test Coverage

| Module | Coverage |
|--------|----------|
| Services | >80% |
| Controllers | >70% |
| Models | >90% |

---

## ❓ Troubleshooting

### Common Issues

#### Ollama Not Responding

```bash
# Check if Ollama is running
curl http://localhost:11434/api/tags

# Start Ollama
ollama serve

# Pull required model
ollama pull llama3.2
```

#### Database Connection Failed

```bash
# Check PostgreSQL is running
docker-compose ps postgres

# View logs
docker-compose logs postgres
```

#### Frontend Cannot Connect to Backend

1. Check backend is running: `curl http://localhost:8080/actuator/health`
2. Check CORS configuration
3. Verify proxy settings in `package.json` or `nginx.conf`

#### High Memory Usage

```bash
# Increase JVM heap
export JAVA_OPTS="-Xmx2g -Xms1g"
```

### Logs

```bash
# Backend logs
docker-compose logs -f backend

# Frontend logs
docker-compose logs -f frontend

# All logs
docker-compose logs -f
```

### Health Checks

```bash
# System health
curl http://localhost:8080/actuator/health

# AI health
curl http://localhost:8080/api/ai/health

# Database health
curl http://localhost:8080/actuator/health/db
```

---

## 📁 Project Structure

```
offline-batch-ai-monitoring/
├── src/main/java/                  # Backend Java source
│   └── com/cardconnect/bolt/ai/
│       ├── config/                 # Configuration classes
│       ├── controller/             # REST controllers
│       ├── exception/              # Exception handlers
│       ├── health/                 # Health indicators
│       ├── model/                  # Domain models
│       ├── repository/             # JPA repositories
│       ├── scheduler/              # Scheduled tasks
│       ├── security/               # Security filters
│       ├── service/                # Business logic
│       └── util/                   # Utilities
├── src/main/resources/
│   ├── application.properties      # Main config
│   ├── db/migration/               # Flyway migrations
│   ├── prompts/                    # AI prompt templates
│   └── sample-data/                # Test data
├── src/test/java/                  # Backend tests
├── frontend/                       # React frontend
│   ├── src/
│   │   ├── components/             # React components
│   │   ├── pages/                  # Page components
│   │   └── services/               # API services
│   ├── package.json
│   └── nginx.conf
├── docker/                         # Docker configs
│   ├── prometheus/
│   ├── grafana/
│   └── alertmanager/
├── .github/workflows/              # CI/CD
│   └── ci.yml
├── Dockerfile                      # Multi-stage build
├── docker-compose.yml
├── pom.xml
└── README.md
```

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📝 License

MIT License - see [LICENSE](LICENSE) for details.

---

## 📞 Support

- **Documentation**: This README
- **API Docs**: http://localhost:8080/swagger-ui.html
- **Issues**: GitHub Issues

---

*Built with ❤️ using Spring Boot, React, and Ollama AI*

