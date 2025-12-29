import React from 'react';

const StatusBadge = ({ status, size = 'md' }) => {
  const getStatusClasses = () => {
    const baseClasses = size === 'lg'
      ? 'px-4 py-2 text-base font-semibold'
      : 'px-3 py-1 text-sm font-medium';

    switch (status?.toUpperCase()) {
      case 'HEALTHY':
        return `${baseClasses} bg-green-100 text-green-800 rounded-full`;
      case 'WARNING':
        return `${baseClasses} bg-yellow-100 text-yellow-800 rounded-full`;
      case 'CRITICAL':
        return `${baseClasses} bg-red-100 text-red-800 rounded-full`;
      default:
        return `${baseClasses} bg-gray-100 text-gray-800 rounded-full`;
    }
  };

  const getStatusIcon = () => {
    switch (status?.toUpperCase()) {
      case 'HEALTHY':
        return '✓';
      case 'WARNING':
        return '⚠';
      case 'CRITICAL':
        return '✕';
      default:
        return '?';
    }
  };

  return (
    <span className={getStatusClasses()}>
      <span className="mr-1">{getStatusIcon()}</span>
      {status || 'UNKNOWN'}
    </span>
  );
};

export default StatusBadge;

