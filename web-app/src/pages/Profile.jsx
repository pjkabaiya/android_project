import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getProfile } from '../api';

function getUser() {
  const s = localStorage.getItem('auth_user');
  return s ? JSON.parse(s) : null;
}

export default function Profile() {
  const navigate = useNavigate();
  const user = getUser();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      try {
        const data = await getProfile();
        setProfile(data);
      } catch {}
      setLoading(false);
    }
    load();
  }, []);

  function logout() {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('auth_user');
    navigate('/login', { replace: true });
  }

  const joinDate = profile?.memberSince
    ? new Date(profile.memberSince).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' })
    : '—';

  return (
    <div style={{ maxWidth: 480, margin: '0 auto', padding: 24 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 24 }}>
        <button className="btn btn-tonal btn-small" onClick={() => navigate(-1)}>Back</button>
        <h1 style={{ fontSize: 24, color: 'var(--black)', flex: 1 }}>Profile</h1>
        <button className="btn btn-danger btn-small" onClick={logout}>Logout</button>
      </div>

      {loading ? (
        <p style={{ textAlign: 'center', color: 'var(--text-secondary)', padding: 32 }}>Loading...</p>
      ) : !profile ? (
        <p style={{ textAlign: 'center', color: 'var(--red)', padding: 32 }}>Failed to load profile</p>
      ) : (
        <>
          <div style={{ textAlign: 'center', marginBottom: 24 }}>
            <div style={{
              width: 72, height: 72, borderRadius: '50%',
              background: 'var(--yellow)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              margin: '0 auto 12px', fontSize: 28, fontWeight: 'bold', color: 'var(--black)', fontFamily: 'serif',
            }}>SM</div>
            <h2 style={{ fontSize: 20, color: 'var(--black)' }}>{profile.name}</h2>
            <p style={{ fontSize: 13, color: 'var(--text-secondary)' }}>{profile.email}</p>
          </div>

          <div className="card" style={{ marginBottom: 16 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid #f0f0f0' }}>
              <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>Role</span>
              <span style={{ fontSize: 14, fontWeight: 600, textTransform: 'capitalize' }}>{profile.role}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid #f0f0f0' }}>
              <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>Member since</span>
              <span style={{ fontSize: 14, fontWeight: 600 }}>{joinDate}</span>
            </div>
            {profile.role === 'driver' ? (
              <>
                <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid #f0f0f0' }}>
                  <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>Trips completed</span>
                  <span style={{ fontSize: 14, fontWeight: 600 }}>{profile.totalTrips}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0' }}>
                  <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>Passengers carried</span>
                  <span style={{ fontSize: 14, fontWeight: 600 }}>{profile.totalPassengers}</span>
                </div>
              </>
            ) : (
              <>
                <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid #f0f0f0' }}>
                  <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>Total requests</span>
                  <span style={{ fontSize: 14, fontWeight: 600 }}>{profile.totalRequests}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid #f0f0f0' }}>
                  <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>Accepted</span>
                  <span style={{ fontSize: 14, fontWeight: 600, color: 'var(--green)' }}>{profile.accepted}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid #f0f0f0' }}>
                  <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>Rejected</span>
                  <span style={{ fontSize: 14, fontWeight: 600, color: 'var(--red)' }}>{profile.rejected}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0' }}>
                  <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>Cancelled</span>
                  <span style={{ fontSize: 14, fontWeight: 600, color: 'var(--orange)' }}>{profile.cancelled}</span>
                </div>
              </>
            )}
          </div>

          <button className="btn btn-secondary" style={{ width: '100%' }}
            onClick={() => navigate(profile.role === 'driver' ? '/driver' : '/passenger')}>
            Back to Dashboard
          </button>
        </>
      )}
    </div>
  );
}