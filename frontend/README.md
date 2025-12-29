# AI Monitoring Dashboard - Frontend

React-based web dashboard for the AI Monitoring Platform.

> **Note**: For complete documentation, see the main [README.md](../README.md) in the project root.

---

## 🚀 Quick Start

```bash
# Install dependencies
npm install --legacy-peer-deps

# Start development server
npm start
```

The dashboard will be available at **http://localhost:3000**.

---

## 📱 Pages

| Page | Path | Description |
|------|------|-------------|
| **Dashboard** | `/` | Main system health overview with charts |
| **Merchants** | `/merchants` | Merchant analysis with risk scores |
| **Accuracy** | `/accuracy` | Prediction accuracy metrics |
| **A/B Testing** | `/ab-testing` | AI vs rule-based comparison |
| **Reports** | `/reports` | Daily/weekly reports with CSV export |
| **Alerts** | `/alerts` | System alerts and notifications |
| **AI Chat** | `/chat` | Chat with AI assistant |
| **Admin** | `/admin` | Configuration, thresholds, RAG documents |

---

## 🧩 Components

| Component | Description |
|-----------|-------------|
| `Sidebar` | Navigation sidebar with all page links |
| `StatusBadge` | Health status indicator (HEALTHY/WARNING/CRITICAL) |
| `LoadingSpinner` | Loading state indicator |
| `Charts` | Chart.js wrappers (Line, Bar, Doughnut) |

---

## 📁 Project Structure

```
frontend/
├── public/
│   └── index.html
├── src/
│   ├── components/           # Reusable UI components
│   │   ├── Charts.js
│   │   ├── LoadingSpinner.js
│   │   ├── Sidebar.js
│   │   └── StatusBadge.js
│   ├── pages/                # Page components
│   │   ├── Dashboard.js
│   │   ├── Merchants.js
│   │   ├── Accuracy.js
│   │   ├── ABTesting.js
│   │   ├── Reports.js
│   │   ├── Alerts.js
│   │   ├── AIChat.js
│   │   └── Admin.js
│   ├── services/             # API services
│   │   └── api.js
│   ├── App.js
│   └── index.js
├── package.json
├── tailwind.config.js
└── nginx.conf
```

---

## 🛠️ Tech Stack

| Technology | Purpose |
|------------|---------|
| React 18 | UI framework |
| React Router 6 | Routing |
| Tailwind CSS | Styling |
| Chart.js | Charts |
| Axios | HTTP client |
| Heroicons | Icons |

---

## 🐳 Docker

The frontend is built as part of the unified Docker setup. From the project root:

```bash
# Build frontend image
docker build --target frontend -t ai-monitor-frontend .

# Run with Docker Compose
docker-compose up frontend
```

---

## 🔧 Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `REACT_APP_API_URL` | (empty) | Backend API base URL |

### API Proxy

- **Development**: Requests proxied to `http://localhost:8080` (via `package.json`)
- **Production**: Nginx proxies to `backend:8080` (via `nginx.conf`)

---

## 📝 Available Scripts

```bash
# Start development server
npm start

# Build for production
npm run build

# Run tests
npm test

# Run linting
npm run lint
```

---

## 🎨 Styling

Uses **Tailwind CSS** with custom primary color palette:

```javascript
// tailwind.config.js
primary: {
  50: '#eff6ff',
  500: '#3b82f6',
  900: '#1e3a8a',
}
```

Custom utility classes in `src/index.css`:
- `.btn-primary` - Primary button
- `.btn-secondary` - Secondary button
- `.card` - Card container
- `.input-field` - Form inputs
- `.label` - Form labels

---

## 🔗 API Integration

All API calls are centralized in `src/services/api.js`:

```javascript
// AI Predictions
getPrediction()
chatWithAI(message)
getAIUsage()
getAIHealth()

// Merchant Predictions
getMerchantPrediction(merchId)
getAllMerchantPredictions()
compareMerchants()
getMerchantRiskRanking()

// Accuracy
getAccuracyMetrics(days)
getABTestResults(days)
getFeedbackLoopData(days)

// Reports
getDailyReport(date)
getWeeklyReport(weekOf)
getMerchantScorecard(merchId)

// RAG Documents
getRAGDocuments()
uploadRAGDocument(file, description)
uploadRAGTextContent(title, content)
deleteRAGDocument(id)

// Thresholds
getAllMerchantThresholds()
saveMerchantThreshold(threshold)
updateMerchantThreshold(merchId, threshold)
deleteMerchantThreshold(merchId)
```

---

*Part of the [AI Monitoring Platform](../README.md)*

