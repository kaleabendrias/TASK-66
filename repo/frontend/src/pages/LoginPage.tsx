import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '@/features/auth/useAuth';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';
import Card from '@/components/ui/Card';

const LoginPage: React.FC = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const { login, loading, error } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await login(username, password);
      navigate('/dashboard');
    } catch {
      // error is set in store
    }
  };

  return (
    <div className="auth-page">
      <Card title="Login" className="auth-card">
        <form onSubmit={handleSubmit}>
          {error && <div className="alert alert-danger">{error}</div>}
          <Input
            label="Username"
            name="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
          />
          <Input
            label="Password"
            name="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
          <Button type="submit" loading={loading} className="btn-block">
            Login
          </Button>
        </form>
        <div className="auth-footer">
          <p>
            Don't have an account? <Link to="/register">Register</Link>
          </p>
          <div className="demo-credentials">
            <p><strong>Demo credentials</strong> (all use password: <code>password123</code>):</p>
            <ul>
              <li><code>admin</code> - Administrator</li>
              <li><code>seller1</code> - Seller</li>
              <li><code>member1</code> - Member</li>
              <li><code>warehouse1</code> - Warehouse Staff</li>
              <li><code>moderator1</code> - Moderator</li>
            </ul>
          </div>
        </div>
      </Card>
    </div>
  );
};

export default LoginPage;
