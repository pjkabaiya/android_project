import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { signUp } from '../api';

export default function Register() {
  const navigate = useNavigate();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState('passenger');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    if (!name.trim() || !email.trim() || !password.trim()) { setError('Fill in all fields'); return; }
    setLoading(true);
    setError('');
    try {
      const data = await signUp(email.trim(), password, name.trim(), role);
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
        <h1 style={{ fontSize: 24, color: 'var(--black)' }}>Create Account</h1>
        <p style={{ color: 'var(--text-secondary)', fontSize: 14 }}>Choose your role to get started</p>
      </div>

      <form onSubmit={handleSubmit}>
        <div className="card" style={{ padding: 24 }}>
          {error && <p style={{ color: 'var(--red)', fontSize: 13, marginBottom: 12 }}>{error}</p>}
          <div style={{ marginBottom: 12 }}>
            <label>Full Name</label>
            <input type="text" placeholder="Your name" value={name} onChange={e => setName(e.target.value)} />
          </div>
          <div style={{ marginBottom: 12 }}>
            <label>Email</label>
            <input type="email" placeholder="your@email.com" value={email} onChange={e => setEmail(e.target.value)} />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label>Password</label>
            <input type="password" placeholder="At least 6 characters" value={password} onChange={e => setPassword(e.target.value)} />
          </div>

          <div style={{ marginBottom: 20 }}>
            <label>I am a...</label>
            <div style={{ display: 'flex', gap: 12, marginTop: 4 }}>
              <label className="card" style={{ flex: 1, padding: 12, textAlign: 'center', cursor: 'pointer', border: role === 'passenger' ? '2px solid var(--yellow)' : '2px solid transparent' }}>
                <input type="radio" name="role" value="passenger" checked={role === 'passenger'} onChange={() => setRole('passenger')} style={{ display: 'none' }} />
                <div style={{ width: 36, height: 36, borderRadius: '50%', background: '#E8E8E8', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 16, fontWeight: 'bold', color: 'var(--text-secondary)' }}>P</div>
                <div style={{ fontSize: 13, fontWeight: 600, marginTop: 4 }}>Passenger</div>
              </label>
              <label className="card" style={{ flex: 1, padding: 12, textAlign: 'center', cursor: 'pointer', border: role === 'driver' ? '2px solid var(--yellow)' : '2px solid transparent' }}>
                <input type="radio" name="role" value="driver" checked={role === 'driver'} onChange={() => setRole('driver')} style={{ display: 'none' }} />
                <div style={{ width: 36, height: 36, borderRadius: '50%', background: '#E8E8E8', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 16, fontWeight: 'bold', color: 'var(--text-secondary)' }}>D</div>
                <div style={{ fontSize: 13, fontWeight: 600, marginTop: 4 }}>Driver</div>
              </label>
            </div>
          </div>

          <button className="btn btn-primary" style={{ width: '100%' }} disabled={loading}>
            {loading ? 'Creating account...' : 'Sign Up'}
          </button>
        </div>
      </form>

      <p style={{ textAlign: 'center', marginTop: 20, fontSize: 14, color: 'var(--text-secondary)' }}>
        Already have an account? <Link to="/login" style={{ color: 'var(--blue)', fontWeight: 600 }}>Sign In</Link>
      </p>
    </div>
  );
}
