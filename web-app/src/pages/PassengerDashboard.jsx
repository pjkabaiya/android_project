import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { searchTrips, getPassengerRequests, updateRequestStatus } from '../api';
import { getSocket } from '../socket';

function getPassengerId() {
  let id = sessionStorage.getItem('passengerId');
  if (!id) {
    id = 'passenger_' + Date.now();
    sessionStorage.setItem('passengerId', id);
  }
  return id;
}

export default function PassengerDashboard() {
  const navigate = useNavigate();
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [trips, setTrips] = useState([]);
  const [showResults, setShowResults] = useState(false);
  const [noResults, setNoResults] = useState(false);
  const [loading, setLoading] = useState(false);
  const [myRequests, setMyRequests] = useState([]);
  const [loadingRequests, setLoadingRequests] = useState(true);

  const passengerId = getPassengerId();

  const loadRequests = useCallback(async () => {
    setLoadingRequests(true);
    try {
      const data = await getPassengerRequests(passengerId);
      setMyRequests(data || []);
    } catch {
      setMyRequests([]);
    }
    setLoadingRequests(false);
  }, [passengerId]);

  useEffect(() => {
    const s = getSocket();
    s.connect();
    return () => { s.disconnect(); };
  }, []);

  useEffect(() => {
    loadRequests();
  }, [loadRequests]);

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

  function goToMap(tripId, plate, route) {
    navigate(`/map/${tripId}`, { state: { role: 'passenger', plate, route } });
  }

  async function handleCancel(req) {
    if (!confirm('Cancel this ride request?')) return;
    try {
      await updateRequestStatus(req.id, { status: 'CANCELLED' });
      loadRequests();
    } catch {
      alert('Failed to cancel request');
    }
  }

  function statusStyle(status) {
    switch (status) {
      case 'ACCEPTED': return { background: '#43A047', color: '#fff' };
      case 'REJECTED': return { background: '#D32F2F', color: '#fff' };
      case 'WAITING': return { background: '#FFC107', color: '#212121' };
      default: return { background: '#757575', color: '#fff' };
    }
  }

  const hasPendingStatus = (s) => s === 'WAITING' || s === 'ACCEPTED';

  return (
    <div style={{ maxWidth: 480, margin: '0 auto', padding: 24 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 24 }}>
        <span style={{ fontSize: 28 }}>🚐</span>
        <h1 style={{ fontSize: 24, color: 'var(--black)' }}>Find Your Ride</h1>
      </div>

      {myRequests.length > 0 && (
        <div style={{ marginBottom: 16 }}>
          <h3 style={{ fontSize: 16, marginBottom: 8, color: 'var(--text-primary)' }}>
            My Active Requests ({myRequests.length})
          </h3>
          {loadingRequests && <p style={{ color: 'var(--text-secondary)' }}>Loading...</p>}
          {myRequests.map(req => {
            const trip = req.tripId && typeof req.tripId === 'object' ? req.tripId : null;
            return (
              <div key={req.id} className="card"
                style={{ marginBottom: 8, display: 'flex', alignItems: 'center', gap: 8 }}>
                <div style={{ flex: 1, cursor: 'pointer' }}
                  onClick={() => goToMap(trip ? trip.id : req.tripId, trip?.numberPlate || '', trip?.route || '')}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ fontSize: 24 }}>🚐</span>
                    <div style={{ flex: 1 }}>
                      <strong>{trip ? `${trip.numberPlate}  |  ${trip.route}` : 'Loading...'}</strong>
                      <p style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
                        Pickup: {req.pickupPoint || 'set on map'}
                      </p>
                    </div>
                  </div>
                </div>
                <span style={{
                  fontSize: 11, fontWeight: 'bold', padding: '4px 10px', borderRadius: 12,
                  ...statusStyle(req.status)
                }}>
                  {req.status}
                </span>
                {hasPendingStatus(req.status) && (
                  <button className="btn btn-danger" style={{ padding: '4px 10px', fontSize: 12 }}
                    onClick={() => handleCancel(req)}>
                    Cancel
                  </button>
                )}
              </div>
            );
          })}
        </div>
      )}

      {!loadingRequests && myRequests.length === 0 && (
        <p style={{ textAlign: 'center', color: 'var(--text-secondary)', marginBottom: 16, fontSize: 13 }}>
          You have no active ride requests
        </p>
      )}

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
              onClick={() => goToMap(t.id, t.numberPlate, t.route)}>
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
