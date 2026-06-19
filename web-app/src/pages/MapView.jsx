import { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useLocation, useNavigate } from 'react-router-dom';
import { MapContainer, TileLayer, Marker, Polyline, Popup, useMapEvents, useMap } from 'react-leaflet';
import L from 'leaflet';
import { getSocket, joinTripRoom, sendLocationUpdate, sendReservationUpdate } from '../socket';
import { getTrip, getTripRequests, createTripRequest, updateRequestStatus } from '../api';

const NAIROBI = [-1.286389, 36.817223];
const EARTH_RADIUS_KM = 6371;
const SIM_SPEED_KPH = 80;

function haversineKm(lat1, lon1, lat2, lon2) {
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a = Math.sin(dLat / 2) ** 2 +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
    Math.sin(dLon / 2) ** 2;
  return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

const vehicleIcon = L.divIcon({
  className: '',
  html: '<div style="background:#FFC107;border:3px solid #212121;border-radius:50%;width:24px;height:24px;box-shadow:0 2px 6px rgba(0,0,0,0.3)"></div>',
  iconSize: [24, 24],
  iconAnchor: [12, 12],
});

const waypointIcon = L.divIcon({
  className: '',
  html: '<div style="background:#1565C0;border:2px solid white;border-radius:50%;width:16px;height:16px;box-shadow:0 1px 4px rgba(0,0,0,0.3)"></div>',
  iconSize: [16, 16],
  iconAnchor: [8, 8],
});

const pickupIcon = L.divIcon({
  className: '',
  html: '<div style="background:#43A047;border:2px solid white;border-radius:50%;width:20px;height:20px;box-shadow:0 1px 4px rgba(0,0,0,0.3);display:flex;align-items:center;justify-content:center"><div style="width:6px;height:6px;border-radius:50%;background:white"></div></div>',
  iconSize: [20, 20],
  iconAnchor: [10, 10],
});

const passengerIcon = L.divIcon({
  className: '',
  html: '<div style="background:#43A047;border:2px solid white;border-radius:4px;width:14px;height:14px;box-shadow:0 1px 4px rgba(0,0,0,0.3)"></div>',
  iconSize: [14, 14],
  iconAnchor: [7, 7],
});

const acceptedIcon = L.divIcon({
  className: '',
  html: '<div style="background:#FF9800;border:2px solid white;border-radius:4px;width:14px;height:14px;box-shadow:0 1px 4px rgba(0,0,0,0.3)"></div>',
  iconSize: [14, 14],
  iconAnchor: [7, 7],
});

function MapClickHandler({ waypointMode, selectingPickup, onAddWaypoint, onSetPickup }) {
  useMapEvents({
    click(e) {
      if (waypointMode) onAddWaypoint(e.latlng);
      else if (selectingPickup) onSetPickup(e.latlng);
    },
  });
  return null;
}

function FitBounds({ points }) {
  const map = useMap();
  useEffect(() => {
    if (points.length >= 2) {
      map.fitBounds(points.map(p => [p.lat, p.lng]), { padding: [50, 50] });
    }
  }, [points, map]);
  return null;
}

export default function MapView() {
  const { tripId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const role = location.state?.role || 'passenger';
  const plate = location.state?.plate || '';
  const routeName = location.state?.route || '';

  const [waypoints, setWaypoints] = useState([]);
  const [routePoints, setRoutePoints] = useState([]);
  const [waypointMode, setWaypointMode] = useState(false);
  const [selectingPickup, setSelectingPickup] = useState(false);
  const [simulating, setSimulating] = useState(false);
  const [currentPos, setCurrentPos] = useState(null);
  const [requests, setRequests] = useState([]);
  const [pickupPos, setPickupPos] = useState(null);
  const [status, setStatus] = useState(role === 'driver' ? 'Tap "Set WP" to plan route' : 'Tracking vehicle...');
  const [eta, setEta] = useState(null);
  const [distance, setDistance] = useState(null);
  const [speed, setSpeed] = useState(null);

  const simRef = useRef(null);
  const waypointIndexRef = useRef(0);
  const currentPosRef = useRef(null);
  const routePointsRef = useRef([]);
  const waypointsRef = useRef([]);
  const simulatingRef = useRef(false);
  const requestsRef = useRef([]);
  const getUser = () => { const s = localStorage.getItem('auth_user'); return s ? JSON.parse(s) : null; };
  const passengerIdRef = useRef(getUser()?.firebaseUid || getUser()?.email || `passenger_${Date.now()}`);

  // Keep refs in sync
  useEffect(() => { routePointsRef.current = routePoints; }, [routePoints]);
  useEffect(() => { waypointsRef.current = waypoints; }, [waypoints]);
  useEffect(() => { simulatingRef.current = simulating; }, [simulating]);
  useEffect(() => { requestsRef.current = requests; }, [requests]);

  // Load saved route and requests on mount
  useEffect(() => {
    async function load() {
      try {
        const trip = await getTrip(tripId);
        if (trip.routePath && trip.routePath.length > 0) {
          const pts = trip.routePath.map(p => ({ lat: p.lat, lng: p.lng }));
          setRoutePoints(pts);
          routePointsRef.current = pts;
          setStatus('Route loaded from server');
        }
      } catch {}
      try {
        const reqs = await getTripRequests(tripId);
        setRequests(reqs || []);
        requestsRef.current = reqs || [];
      } catch {}
    }
    load();
  }, [tripId]);

  // Socket setup
  useEffect(() => {
    const socket = getSocket();
    socket.connect();
    if (role === 'driver') joinTripRoom(tripId);

    function handleLocationUpdate(data) {
      if (!data || data.tripId !== tripId) return;
      if (data.type === 'route_broadcast' && data.routePath && role === 'passenger') {
        const pts = data.routePath.map(c => ({ lat: c[0], lng: c[1] }));
        setRoutePoints(pts);
        routePointsRef.current = pts;
        return;
      }
      if (data.latitude && data.longitude && role === 'passenger') {
        const pos = { lat: data.latitude, lng: data.longitude };
        setCurrentPos(pos);
        currentPosRef.current = pos;
      }
    }

    function handleReservation(data) {
      if (!data || data.tripId !== tripId) return;
      if (data.type === 'request_accepted' && role === 'passenger') {
        setStatus('Driver accepted! Matatu is coming');
        getTripRequests(tripId).then(reqs => {
          setRequests(reqs || []);
          requestsRef.current = reqs || [];
        }).catch(() => {});
      } else if (role === 'driver') {
        getTripRequests(tripId).then(reqs => {
          setRequests(reqs || []);
          requestsRef.current = reqs || [];
        }).catch(() => {});
      }
    }

    socket.on('vehicle-location', handleLocationUpdate);
    socket.on('location-update', handleLocationUpdate);
    socket.on('reservation-update', handleReservation);

    return () => {
      socket.off('vehicle-location', handleLocationUpdate);
      socket.off('location-update', handleLocationUpdate);
      socket.off('reservation-update', handleReservation);
    };
  }, [tripId, role]);

  function toggleWaypointMode() {
    const newMode = !waypointMode;
    setWaypointMode(newMode);
    setSelectingPickup(false);
    if (newMode) {
      setStatus('Tap map to place waypoints');
    } else {
      if (waypoints.length >= 2) {
        finishWaypoints();
      } else {
        setStatus(`Waypoints: ${waypoints.length}`);
      }
    }
  }

  function addWaypoint(latlng) {
    const pt = { lat: latlng.lat, lng: latlng.lng };
    const updated = [...waypoints, pt];
    setWaypoints(updated);
    waypointsRef.current = updated;
    setStatus(`Waypoints: ${updated.length} (tap Done WP when finished)`);
    sendRouteBroadcast(updated, routePointsRef.current);
  }

  function undoWaypoint() {
    if (waypoints.length === 0) return;
    const updated = waypoints.slice(0, -1);
    setWaypoints(updated);
    waypointsRef.current = updated;
    setStatus(`Waypoints: ${updated.length}`);
    sendRouteBroadcast(updated, routePointsRef.current);
  }

  async function finishWaypoints() {
    if (waypoints.length < 2) { alert('Place at least 2 waypoints'); return; }
    setStatus('Calculating road route...');
    try {
      const snapped = await fetchRoadRoute(waypoints);
      if (snapped && snapped.length > 0) {
        setRoutePoints(snapped);
        routePointsRef.current = snapped;
        setStatus('Road route created!');
        sendRouteBroadcast(waypoints, snapped);
        saveRouteToBackend(snapped);
      } else {
        setRoutePoints([...waypoints]);
        routePointsRef.current = [...waypoints];
        setStatus('Using straight-line route');
      }
    } catch {
      setRoutePoints([...waypoints]);
      routePointsRef.current = [...waypoints];
      setStatus('Road snap failed, using straight lines');
    }
    setWaypointMode(false);
  }

  async function fetchRoadRoute(wps) {
    const coords = wps.map(p => `${p.lng},${p.lat}`).join(';');
    const url = `https://routing.openstreetmap.de/routed-car/route/v1/driving/${coords}?overview=full&geometries=geojson`;
    const res = await fetch(url, { headers: { 'User-Agent': 'SmartMatatu/1.0' } });
    if (!res.ok) return null;
    const json = await res.json();
    if (!json.routes || json.routes.length === 0) return null;
    const coords2 = json.routes[0].geometry.coordinates;
    return coords2.map(c => ({ lat: c[1], lng: c[0] }));
  }

  function sendRouteBroadcast(wps, rps) {
    const socket = getSocket();
    const pts = (rps && rps.length > 0) ? rps : wps;
    if (!pts.length) return;
    socket.emit('location-update', {
      tripId,
      type: 'route_broadcast',
      routePath: pts.map(p => [p.lat, p.lng]),
    });
  }

  async function saveRouteToBackend(path) {
    try {
      const { updateTrip } = await import('../api');
      await updateTrip(tripId, { routePath: path });
    } catch {}
  }

  function clearRoute() {
    if (!confirm(role === 'driver' ? 'Clear map or delete trip?' : 'Clear map?')) return;
    if (simulating) stopSimulation();
    setWaypoints([]);
    setRoutePoints([]);
    waypointsRef.current = [];
    routePointsRef.current = [];
    setCurrentPos(null);
    currentPosRef.current = null;
    setStatus('Route cleared');
    sendRouteBroadcast([], []);
  }

  // Simulation
  function startSimulation() {
    const pts = routePoints.length > 0 ? routePoints : waypoints;
    if (pts.length < 2) { alert('Create a route first'); return; }
    setSimulating(true);
    simulatingRef.current = true;
    waypointIndexRef.current = 0;
    const start = pts[0];
    setCurrentPos(start);
    currentPosRef.current = start;
    setStatus('Simulating...');
    sendLocationUpdate(tripId, start.lat, start.lng);
    simRef.current = setInterval(() => stepSimulation(pts), 1500);
  }

  function stopSimulation() {
    setSimulating(false);
    simulatingRef.current = false;
    clearInterval(simRef.current);
    setStatus('Simulation stopped');
  }

  function stepSimulation(pts) {
    if (!simulatingRef.current) return;
    let idx = waypointIndexRef.current;
    if (idx >= pts.length - 1) {
      setStatus('Route complete!');
      stopSimulation();
      return;
    }
    const from = pts[idx];
    const to = pts[idx + 1];
    const segDistKm = haversineKm(from.lat, from.lng, to.lat, to.lng);
    const segSpeedKps = SIM_SPEED_KPH / 3600;
    const stepDistKm = segSpeedKps * 1.5;
    const fraction = stepDistKm / segDistKm;

    let newPos;
    if (fraction >= 1) {
      waypointIndexRef.current = ++idx;
      newPos = { lat: to.lat, lng: to.lng };
    } else {
      newPos = {
        lat: from.lat + (to.lat - from.lat) * fraction,
        lng: from.lng + (to.lng - from.lng) * fraction,
      };
    }
    setCurrentPos(newPos);
    currentPosRef.current = newPos;
    sendLocationUpdate(tripId, newPos.lat, newPos.lng);
    updateETA(newPos);
  }

  function updateETA(pos) {
    const accepted = requestsRef.current.find(r => r.status === 'ACCEPTED');
    if (accepted && (accepted.passengerLat || accepted.passengerLng)) {
      const dist = haversineKm(pos.lat, pos.lng, accepted.passengerLat, accepted.passengerLng);
      const etaMin = (dist / SIM_SPEED_KPH) * 60;
      setDistance(dist);
      setSpeed(SIM_SPEED_KPH);
      setEta(etaMin);
    }
  }

  function toggleSimulation() {
    if (simulating) stopSimulation();
    else startSimulation();
  }

  // Pickup selection
  function togglePickupSelection() {
    setSelectingPickup(prev => !prev);
    setWaypointMode(false);
    setStatus(selectingPickup ? 'Tap map to set pickup point' : 'Tracking vehicle...');
  }

  function snapToRoute(latlng) {
    const pts = routePoints.length > 0 ? routePoints : waypoints;
    if (pts.length === 0) return latlng;
    let nearest = null;
    let minDist = Infinity;
    for (const p of pts) {
      const d = haversineKm(latlng.lat, latlng.lng, p.lat, p.lng);
      if (d < minDist) { minDist = d; nearest = p; }
    }
    return nearest || latlng;
  }

  function setPickupPoint(latlng) {
    const pos = snapToRoute(latlng);
    setPickupPos(pos);
    setSelectingPickup(false);
    setStatus('Pickup point set on route!');
    handleRequestRide(pos);
  }

  async function handleRequestRide(pos) {
    try {
      const req = await createTripRequest(tripId, {
        passengerId: passengerIdRef.current,
        pickupPoint: 'Map Pickup',
        passengerLat: pos.lat,
        passengerLng: pos.lng,
      });
      sendReservationUpdate({
        tripId,
        pickupPoint: 'Map Pickup',
        type: 'passenger_request',
        passengerLat: pos.lat,
        passengerLng: pos.lng,
      });
      setStatus('Ride requested!');
      const reqs = await getTripRequests(tripId);
      setRequests(reqs || []);
      requestsRef.current = reqs || [];
    } catch (e) {
      alert('Request failed: ' + e.message);
    }
  }

  // Accept request
  async function handleAccept(req) {
    if (req.status !== 'WAITING') return;
    try {
      await updateRequestStatus(req.id, { status: 'ACCEPTED' });
      sendReservationUpdate({ tripId, requestId: req.id, status: 'ACCEPTED', type: 'request_accepted' });
      const reqs = await getTripRequests(tripId);
      setRequests(reqs || []);
      requestsRef.current = reqs || [];
    } catch (e) {
      alert('Accept failed: ' + e.message);
    }
  }

  // Map markers
  const routePointsForPolyline = routePoints.length > 1 ? routePoints : waypoints;

  return (
    <div style={{ height: '100vh', display: 'flex', flexDirection: 'column' }}>
      {/* Top bar */}
      <div style={{
        background: 'var(--surface)', padding: '8px 12px', display: 'flex',
        alignItems: 'center', gap: 6, boxShadow: '0 2px 4px rgba(0,0,0,0.1)', zIndex: 1000,
      }}>
        <button className="btn btn-small btn-tonal" onClick={() => navigate(-1)} style={{ padding: '4px 8px' }}>←</button>
        <span style={{ flex: 1, fontWeight: 600, fontSize: 14 }}>{role === 'driver' ? 'Driver' : 'Passenger'} - {plate}</span>
        {role === 'driver' && (
          <>
            <button className={`btn btn-small ${waypointMode ? 'btn-primary' : 'btn-tonal'}`}
              onClick={toggleWaypointMode}>
              {waypointMode ? 'Done WP' : 'Set WP'}
            </button>
            {waypointMode && waypoints.length > 0 && (
              <button className="btn btn-small btn-tonal" onClick={undoWaypoint}>Undo</button>
            )}
            <button className={`btn btn-small ${simulating ? 'btn-danger' : 'btn-primary'}`}
              onClick={toggleSimulation} disabled={routePoints.length < 2 && waypoints.length < 2}>
              {simulating ? 'Stop' : 'Start'}
            </button>
            <button className="btn btn-small btn-tonal" onClick={clearRoute}>Clear</button>
          </>
        )}
        {role === 'passenger' && (
          <button className={`btn btn-small ${selectingPickup ? 'btn-primary' : 'btn-tonal'}`}
            onClick={togglePickupSelection}>
            {selectingPickup ? 'Cancel' : 'Set Pickup'}
          </button>
        )}
      </div>

      {/* Status */}
      <div style={{ padding: '4px 12px', fontSize: 12, color: 'var(--text-secondary)', background: '#fafafa' }}>
        {status}
      </div>

      {/* Map */}
      <div style={{ flex: 1, position: 'relative' }}>
        <MapContainer center={NAIROBI} zoom={14} style={{ height: '100%', width: '100%' }}
          zoomControl={true}>
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a>'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <MapClickHandler
            waypointMode={waypointMode}
            selectingPickup={selectingPickup}
            onAddWaypoint={addWaypoint}
            onSetPickup={setPickupPoint}
          />
          {routePointsForPolyline.length > 1 && (
            <Polyline
              positions={routePointsForPolyline.map(p => [p.lat, p.lng])}
              pathOptions={{ color: '#1565C0', weight: 6, opacity: 0.8 }}
            />
          )}
          {waypoints.map((p, i) => (
            <Marker key={`wp-${i}`} position={[p.lat, p.lng]} icon={waypointIcon}>
              <Popup>WP {i + 1}</Popup>
            </Marker>
          ))}
          {currentPos && (
            <Marker position={[currentPos.lat, currentPos.lng]} icon={vehicleIcon}>
              <Popup>{role === 'driver' ? 'My Vehicle' : 'Matatu'}</Popup>
            </Marker>
          )}
          {pickupPos && (
            <Marker position={[pickupPos.lat, pickupPos.lng]} icon={pickupIcon}>
              <Popup>Your Pickup Point</Popup>
            </Marker>
          )}
          {requests.filter(r => r.passengerLat || r.passengerLng).map(req => (
            <Marker
              key={req.id}
              position={[req.passengerLat, req.passengerLng]}
              icon={req.status === 'ACCEPTED' ? acceptedIcon : passengerIcon}
            >
              <Popup>{req.pickupPoint}<br />{req.status || 'WAITING'}</Popup>
            </Marker>
          ))}
          {routePointsForPolyline.length >= 2 && (
            <FitBounds points={routePointsForPolyline} />
          )}
        </MapContainer>
      </div>

      {/* Bottom panel */}
      <div style={{
        background: 'var(--surface)', borderRadius: '12px 12px 0 0', boxShadow: '0 -4px 12px rgba(0,0,0,0.1)',
        padding: 12, zIndex: 1000, maxHeight: '40vh', overflow: 'auto',
      }}>
        {role === 'driver' && (
          <div>
            <h4 style={{ fontSize: 13, fontWeight: 600, marginBottom: 6 }}>Passenger Requests</h4>
            {requests.length === 0 ? (
              <p style={{ fontSize: 12, color: 'var(--text-secondary)' }}>No requests yet</p>
            ) : (
              requests.map(req => (
                <div key={req.id} style={{
                  display: 'flex', alignItems: 'center', gap: 8, padding: '6px 0',
                  borderBottom: '1px solid #eee', fontSize: 13,
                }}>
                  <span style={{ flex: 1 }}>{req.pickupPoint} <span style={{ color: 'var(--text-secondary)', fontSize: 11 }}>({req.status})</span></span>
                  {req.status === 'WAITING' && (
                    <button className="btn btn-small btn-primary"
                      onClick={() => handleAccept(req)}>Accept</button>
                  )}
                </div>
              ))
            )}
          </div>
        )}
        {role === 'passenger' && (
          <div>
            <h4 style={{ fontSize: 13, fontWeight: 600, marginBottom: 4 }}>Trip Status</h4>
            <p style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 8 }}>
              {requests.find(r => r.status === 'ACCEPTED') ? 'Driver accepted! Matatu is coming' : 'Request sent, waiting for driver...'}
            </p>
            <div style={{ display: 'flex', gap: 16, fontSize: 13 }}>
              <div><span style={{ color: 'var(--text-secondary)' }}>Distance: </span><strong>{distance != null ? `${distance.toFixed(1)} km` : '-- km'}</strong></div>
              <div><span style={{ color: 'var(--text-secondary)' }}>Speed: </span><strong>{speed != null ? `${speed} km/h` : '-- km/h'}</strong></div>
              <div><span style={{ color: 'var(--text-secondary)' }}>ETA: </span><strong>{eta != null ? `${Math.round(eta)} min` : '-- min'}</strong></div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
