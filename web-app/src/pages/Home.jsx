import { Link } from 'react-router-dom';

export default function Home() {
  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      padding: 32,
      background: 'var(--bg)',
    }}>
      <div style={{ textAlign: 'center', marginBottom: 48 }}>
        <div style={{
          width: 80, height: 80, borderRadius: '50%',
          background: 'var(--yellow)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          margin: '0 auto 16px', fontSize: 32, fontWeight: 'bold', color: 'var(--black)', fontFamily: 'serif',
        }}>
          SM
        </div>
        <h1 style={{ fontSize: 28, color: 'var(--black)', marginBottom: 4 }}>Smart Matatu</h1>
        <p style={{ color: 'var(--text-secondary)', fontSize: 14 }}>Web Edition</p>
      </div>

      <Link to="/login" className="btn btn-primary" style={{ width: '100%', maxWidth: 360, height: 56, fontSize: 16, marginBottom: 12, textDecoration: 'none' }}>
        Sign In
      </Link>

      <Link to="/register" className="btn btn-secondary" style={{ width: '100%', maxWidth: 360, height: 56, fontSize: 16, textDecoration: 'none' }}>
        Create Account
      </Link>
    </div>
  );
}
