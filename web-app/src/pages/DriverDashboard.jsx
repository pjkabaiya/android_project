import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { searchTrips, createTrip, updateTrip } from '../api';

const DRIVER_ID = 'driver_001';

export default function DriverDashboard() {
  const navigate = useNavigate();
  const [plate, setPlate] = useState('');
  const [route, setRoute] = useState('');
  const [seats, setSeats] = useState('14');
  const [trips, setTrips] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => { loadTrips(); }, []);

  async function loadTrips() {
    try {
      const data = await searchTrips(null, DRIVER_ID);
      setTrips(data || []);
    } catch { setTrips([]); }
  }

  async function handleStart() {
    if (!plate.trim() || !route.trim()) return alert('Fill in plate and route');
    setLoading(true);
    try {
      const trip = await createTrip({
        numberPlate: plate.trim(),
        route: route.trim(),
        availableSeats: parseInt(seats) || 14,
        driverId: DRIVER_ID,
      });
      navigate(`/map/${trip.id}`, { state: { role: 'driver', plate: plate.trim(), route: route.trim() } });
    } catch (e) { alert(e.message); }
    setLoading(false);
  }

  async function handleDelete(tripId) {
    if (!confirm('Delete this trip?')) return;
    try {
      await updateTrip(tripId, { status: 'CANCELLED' });
      loadTrips();
    } catch (e) { alert(e.message); }
  }

  return (
    <div style={{ maxWidth: 480, margin: '0 auto', padding: 24 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 24 }}>
        <span style={{ fontSize: 28 }}>🚐</span>
        <h1 style={{ fontSize: 24, color: 'var(--black)' }}>Driver Dashboard</h1>
      </div>

      {trips.length > 0 && (
        <div className="card" style={{ marginBottom: 16 }}>
          <h3 style={{ fontSize: 16, marginBottom: 8 }}>Current Routes</h3>
          {trips.map(t => (
            <div key={t.id} style={{
              display: 'flex', alignItems: 'center', gap: 8,
              padding: '10px 0', borderBottom: '1px solid #eee',
            }}>
              <div style={{ flex: 1 }}>
                <strong>{t.numberPlate}</strong>
                <span style={{ color: 'var(--text-secondary)', marginLeft: 8, fontSize: 13 }}>{t.route}</span>
                <span style={{ color: 'var(--green)', marginLeft: 8, fontSize: 13 }}>{t.availableSeats} seats</span>
              </div>
              <button className="btn btn-small btn-tonal"
                onClick={() => navigate(`/map/${t.id}`, { state: { role: 'driver', plate: t.numberPlate, route: t.route } })}>
                Map
              </button>
              <button className="btn btn-small btn-danger" onClick={() => handleDelete(t.id)}>✕</button>
            </div>
          ))}
        </div>
      )}

      <div className="card">
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
        <button className="btn btn-primary" style={{ width: '100%' }}
          onClick={handleStart} disabled={loading}>
          {loading ? 'Creating...' : 'Start Trip'}
        </button>
      </div>
    </div>
  );
}
