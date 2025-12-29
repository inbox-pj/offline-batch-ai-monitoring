import React from 'react';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  ArcElement,
  Title,
  Tooltip,
  Legend,
  Filler,
} from 'chart.js';
import { Line, Bar, Doughnut } from 'react-chartjs-2';

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  ArcElement,
  Title,
  Tooltip,
  Legend,
  Filler
);

export const LineChart = ({ data, options = {}, height = 300 }) => {
  const defaultOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { position: 'top' },
    },
    scales: {
      y: { beginAtZero: true },
    },
    ...options,
  };

  return (
    <div style={{ height }}>
      <Line data={data} options={defaultOptions} />
    </div>
  );
};

export const BarChart = ({ data, options = {}, height = 300 }) => {
  const defaultOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { position: 'top' },
    },
    ...options,
  };

  return (
    <div style={{ height }}>
      <Bar data={data} options={defaultOptions} />
    </div>
  );
};

export const DoughnutChart = ({ data, options = {}, height = 300 }) => {
  const defaultOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { position: 'right' },
    },
    ...options,
  };

  return (
    <div style={{ height }}>
      <Doughnut data={data} options={defaultOptions} />
    </div>
  );
};

// Health Status Distribution Chart
export const HealthDistributionChart = ({ healthy = 0, warning = 0, critical = 0, unknown = 0 }) => {
  const data = {
    labels: ['Healthy', 'Warning', 'Critical', 'Unknown'],
    datasets: [
      {
        data: [healthy, warning, critical, unknown],
        backgroundColor: ['#22c55e', '#f59e0b', '#ef4444', '#9ca3af'],
        borderColor: ['#16a34a', '#d97706', '#dc2626', '#6b7280'],
        borderWidth: 2,
      },
    ],
  };

  return <DoughnutChart data={data} height={250} />;
};

// Accuracy Trend Chart
export const AccuracyTrendChart = ({ labels = [], accuracy = [], confidence = [] }) => {
  const data = {
    labels,
    datasets: [
      {
        label: 'Accuracy',
        data: accuracy,
        borderColor: '#3b82f6',
        backgroundColor: 'rgba(59, 130, 246, 0.1)',
        fill: true,
        tension: 0.4,
      },
      {
        label: 'Confidence',
        data: confidence,
        borderColor: '#8b5cf6',
        backgroundColor: 'rgba(139, 92, 246, 0.1)',
        fill: true,
        tension: 0.4,
      },
    ],
  };

  return <LineChart data={data} height={300} />;
};

// Risk Score Bar Chart
export const RiskScoreChart = ({ merchants = [] }) => {
  const data = {
    labels: merchants.map(m => m.merchId),
    datasets: [
      {
        label: 'Risk Score',
        data: merchants.map(m => m.riskScore * 100),
        backgroundColor: merchants.map(m =>
          m.riskScore > 0.7 ? '#ef4444' : m.riskScore > 0.4 ? '#f59e0b' : '#22c55e'
        ),
        borderRadius: 4,
      },
    ],
  };

  const options = {
    indexAxis: 'y',
    scales: {
      x: {
        max: 100,
        title: { display: true, text: 'Risk Score (%)' },
      },
    },
  };

  return <BarChart data={data} options={options} height={Math.max(200, merchants.length * 40)} />;
};

const Charts = { LineChart, BarChart, DoughnutChart, HealthDistributionChart, AccuracyTrendChart, RiskScoreChart };
export default Charts;

