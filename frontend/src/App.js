import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Sidebar from './components/Sidebar';
import Dashboard from './pages/Dashboard';
import Merchants from './pages/Merchants';
import Accuracy from './pages/Accuracy';
import ABTesting from './pages/ABTesting';
import Reports from './pages/Reports';
import Alerts from './pages/Alerts';
import AIChat from './pages/AIChat';
import Admin from './pages/Admin';

function App() {
  return (
    <Router>
      <div className="min-h-screen bg-gray-100">
        <Sidebar />
        <div className="lg:pl-64">
          {/* Mobile Header */}
          <div className="lg:hidden sticky top-0 z-10 bg-primary-800 px-4 py-3">
            <h1 className="text-xl font-bold text-white">AI Monitor</h1>
          </div>

          {/* Main Content */}
          <main className="p-6">
            <Routes>
              <Route path="/" element={<Dashboard />} />
              <Route path="/merchants" element={<Merchants />} />
              <Route path="/accuracy" element={<Accuracy />} />
              <Route path="/ab-testing" element={<ABTesting />} />
              <Route path="/reports" element={<Reports />} />
              <Route path="/alerts" element={<Alerts />} />
              <Route path="/chat" element={<AIChat />} />
              <Route path="/admin" element={<Admin />} />
            </Routes>
          </main>
        </div>
      </div>
    </Router>
  );
}

export default App;

