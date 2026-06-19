import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { logIn } from '../api';

export default function Login() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    if (!email.trim() || !password.trim()) { setError('Fill in all fields'); return; }
    setLoading(true);
    setError('');
    try {
      const data = await logIn(email.trim(), password);
      localStorage.setItem('auth_token', data.token);
      localStorage.setItem('auth_user', JSON.stringify(data.user));
      if (data.user.role === 'driver') navigate('/driver', { replace: true });
      else navigate('/passenger', { replace: true });
    } catch (err) {
      setError(err.message);
    }
    setLoading(false);
  }

  return (
    <div style={{ maxWidth: 400, margin: '0 auto', padding: 32, minHeight: '100vh', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
      <div style={{ textAlign: 'center', marginBottom: 32 }}>
        <div style={{ width: 64, height: 64, borderRadius: '50%', background: 'var(--yellow)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 16px', fontSize: 24, fontWeight: 'bold', color: 'var(--black)', fontFamily: 'serif' }}>SM</div>
        <h1 style={{ fontSize: 24, color: 'var(--black)' }}>Smart Matatu</h1>
        <p style={{ color: 'var(--text-secondary)', fontSize: 14 }}>Sign in to continue</p>
      </div>

      <form onSubmit={handleSubmit}>
        <div className="card" style={{ padding: 24 }}>
          {error && <p style={{ color: 'var(--red)', fontSize: 13, marginBottom: 12 }}>{error}</p>}
          <div style={{ marginBottom: 12 }}>
            <label>Email</label>
            <input type="email" placeholder="your@email.com" value={email} onChange={e => setEmail(e.target.value)} />
          </div>
          <div style={{ marginBottom: 20 }}>
            <label>Password</label>
            <input type="password" placeholder="Enter password" value={password} onChange={e => setPassword(e.target.value)} />
          </div>
          <button className="btn btn-primary" style={{ width: '100%' }} disabled={loading}>
            {loading ? 'Signing in...' : 'Sign In'}
          </button>
        </div>
      </form>

      <p style={{ textAlign: 'center', marginTop: 20, fontSize: 14, color: 'var(--text-secondary)' }}>
        Don't have an account? <Link to="/register" style={{ color: 'var(--blue)', fontWeight: 600 }}>Sign Up</Link>
      </p>
    </div>
  );
}
