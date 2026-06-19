import { useNavigate } from 'react-router-dom';

export default function Home() {
  const navigate = useNavigate();

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
          margin: '0 auto 16px', fontSize: 36, fontWeight: 'bold', color: 'var(--black)',
        }}>
          🚐
        </div>
        <h1 style={{ fontSize: 28, color: 'var(--black)', marginBottom: 4 }}>Smart Matatu</h1>
        <p style={{ color: 'var(--text-secondary)', fontSize: 14 }}>Web Edition</p>
      </div>

      <button className="btn btn-primary" style={{ width: '100%', maxWidth: 360, height: 72, fontSize: 18, marginBottom: 16 }}
        onClick={() => navigate('/passenger')}>
        🧑 Find Your Ride
      </button>

      <button className="btn btn-secondary" style={{ width: '100%', maxWidth: 360, height: 72, fontSize: 18 }}
        onClick={() => navigate('/driver')}>
        🚐 Driver Dashboard
      </button>

      <p style={{ marginTop: 32, color: 'var(--text-secondary)', fontSize: 12 }}>
        (Authentication Bypassed)
      </p>
    </div>
  );
}
