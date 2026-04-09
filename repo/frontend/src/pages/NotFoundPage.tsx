import React from 'react';
import { Link } from 'react-router-dom';
import Card from '@/components/ui/Card';

const NotFoundPage: React.FC = () => {
  return (
    <div className="auth-page">
      <Card title="404 - Not Found">
        <p>The page you are looking for does not exist.</p>
        <Link to="/dashboard" className="btn btn-primary">Go to Dashboard</Link>
      </Card>
    </div>
  );
};

export default NotFoundPage;
