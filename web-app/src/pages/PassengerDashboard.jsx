import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { searchTrips } from '../api';
import { getSocket } from '../socket';

export default function PassengerDashboard() {
  const navigate = useNavigate();
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [trips, setTrips] = useState([]);
  const [showResults, setShowResults] = useState(false);
  const [noResults, setNoResults] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const s = getSocket();
    s.connect();
    return () => { s.disconnect(); };
  }, []);

  async function handleSearch() {
    if (!from.trim() || !to.trim()) return alert('Enter both locations');
    setLoading(true);
    setShowResults(false);
    setNoResults(false);

    const query = `${from.trim()} ${to.trim()}`;
    try {
      let data = await searchTrips(query, null);
      if (!data || data.length === 0) {
        data = await searchTrips(to.trim(), null);
      }
      if (data && data.length > 0) {
        setTrips(data);
        setShowResults(true);
      } else {
        setNoResults(true);
      }
    } catch {
      setNoResults(true);
    }
    setLoading(false);
  }

  function goToMap(trip) {
    navigate(`/map/${trip.id}`, { state: { role: 'passenger', plate: trip.numberPlate, route: trip.route } });
  }

  return (
    <div style={{ maxWidth: 480, margin: '0 auto', padding: 24 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 24 }}>
        <span style={{ fontSize: 28 }}>🚐</span>
        <h1 style={{ fontSize: 24, color: 'var(--black)' }}>Find Your Ride</h1>
      </div>

      <div className="card" style={{ marginBottom: 16 }}>
        <div style={{ marginBottom: 12 }}>
          <label>From</label>
          <input placeholder="e.g. Nairobi" value={from} onChange={e => setFrom(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && handleSearch()} />
        </div>
        <div style={{ marginBottom: 16 }}>
          <label>To</label>
          <input placeholder="e.g. Nakuru" value={to} onChange={e => setTo(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && handleSearch()} />
        </div>
        <button className="btn btn-primary" style={{ width: '100%' }}
          onClick={handleSearch} disabled={loading}>
          {loading ? 'Searching...' : 'Search Available Vehicles'}
        </button>
      </div>

      {showResults && (
        <>
          <h3 style={{ fontSize: 16, marginBottom: 8, color: 'var(--text-primary)' }}>Active Vehicles</h3>
          {trips.map(t => (
            <div key={t.id} className="card" style={{ marginBottom: 8, cursor: 'pointer' }}
              onClick={() => goToMap(t)}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <span style={{ fontSize: 28 }}>🚐</span>
                <div style={{ flex: 1 }}>
                  <strong>{t.numberPlate}</strong>
                  <p style={{ fontSize: 13, color: 'var(--text-secondary)' }}>{t.route}</p>
                </div>
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: 20, fontWeight: 'bold', color: 'var(--green)' }}>{t.availableSeats}</div>
                  <div style={{ fontSize: 10, color: 'var(--text-secondary)' }}>seats</div>
                </div>
              </div>
            </div>
          ))}
        </>
      )}

      {noResults && (
        <p style={{ textAlign: 'center', color: 'var(--text-secondary)', marginTop: 32, padding: 32 }}>
          No vehicles found. Try a different route.
        </p>
      )}
    </div>
  );
}
