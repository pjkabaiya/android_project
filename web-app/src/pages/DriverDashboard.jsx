import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { searchTrips, createTrip, updateTrip } from '../api';

function getUser() {
  const s = localStorage.getItem('auth_user');
  return s ? JSON.parse(s) : null;
}

export default function DriverDashboard() {
  const navigate = useNavigate();
  const user = getUser();
  const driverId = user?.firebaseUid || user?.email || 'driver_001';

  const [plate, setPlate] = useState('');
  const [route, setRoute] = useState('');
  const [seats, setSeats] = useState('14');
  const [trips, setTrips] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => { loadTrips(); }, []);

  async function loadTrips() {
    try {
      const data = await searchTrips(null, driverId);
      setTrips(data || []);
    } catch {}
  }

  function logout() {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('auth_user');
    navigate('/login', { replace: true });
  }

  async function handleCreate() {
    if (!plate.trim() || !route.trim()) return alert('Fill in plate and route');
    setLoading(true);
    try {
      const trip = await createTrip({
        numberPlate: plate.trim(),
        route: route.trim(),
        availableSeats: parseInt(seats) || 14,
        driverId: driverId,
        status: 'ON_ROUTE',
      });
      navigate(`/map/${trip.id}`, { state: { role: 'driver', plate: plate.trim(), route: route.trim(), createRouteDirect: true } });
    } catch (e) { alert('Failed: ' + e.message); }
    setLoading(false);
  }

  async function deleteTrip(id) {
    if (!confirm('Delete this trip?')) return;
    try {
      await updateTrip(id, { status: 'CANCELLED' });
      loadTrips();
    } catch {}
  }

  return (
    <div style={{ maxWidth: 480, margin: '0 auto', padding: 24 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 24 }}>
        <span style={{ width: 36, height: 36, borderRadius: '50%', background: 'var(--yellow)', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', fontSize: 14, fontWeight: 'bold', color: 'var(--black)', fontFamily: 'serif' }}>SM</span>
        <h1 style={{ fontSize: 24, color: 'var(--black)', flex: 1 }}>Driver Dashboard</h1>
        <button className="btn btn-danger btn-small" onClick={logout}>Logout</button>
      </div>
      <p style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 16 }}>{user?.email || ''}</p>

      {trips.length > 0 && (
        <div style={{ marginBottom: 16 }}>
          <h3 style={{ fontSize: 16, marginBottom: 8 }}>Current Routes</h3>
          {trips.map(t => (
            <div key={t.id} className="card" style={{ marginBottom: 8, display: 'flex', alignItems: 'center', gap: 8 }}>
              <div style={{ flex: 1, cursor: 'pointer' }} onClick={() => navigate(`/map/${t.id}`, { state: { role: 'driver', plate: t.numberPlate, route: t.route } })}>
                <strong>{t.numberPlate}</strong>
                <p style={{ fontSize: 13, color: 'var(--text-secondary)' }}>{t.route} — {t.availableSeats} seats</p>
              </div>
              <button className="btn btn-danger btn-small" onClick={() => deleteTrip(t.id)}>Delete</button>
            </div>
          ))}
        </div>
      )}

      <div className="card" style={{ marginBottom: 16 }}>
        <h3 style={{ fontSize: 16, marginBottom: 12 }}>Start New Trip</h3>
        <div style={{ marginBottom: 12 }}>
          <label>Number Plate</label>
          <input placeholder="e.g. KAA 001A" value={plate} onChange={e => setPlate(e.target.value)} />
        </div>
        <div style={{ marginBottom: 12 }}>
          <label>Route</label>
          <input placeholder="e.g. Nairobi → Nakuru" value={route} onChange={e => setRoute(e.target.value)} />
        </div>
        <div style={{ marginBottom: 16 }}>
          <label>Available Seats</label>
          <input type="number" value={seats} onChange={e => setSeats(e.target.value)} />
        </div>
        <button className="btn btn-primary" style={{ width: '100%' }} onClick={handleCreate} disabled={loading}>
          {loading ? 'Creating...' : 'Start Trip'}
        </button>
      </div>
    </div>
  );
}
