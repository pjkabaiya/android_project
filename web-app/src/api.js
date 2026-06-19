const API = '/api';

async function req(url, opts = {}) {
  const res = await fetch(`${API}${url}`, {
    headers: { 'Content-Type': 'application/json' },
    ...opts,
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.error || `Request failed: ${res.status}`);
  }
  return res.json();
}

export function searchTrips(route, driverId) {
  const params = new URLSearchParams();
  if (route) params.set('route', route);
  if (driverId) params.set('driverId', driverId);
  return req(`/trips?${params}`);
}

export function getTrip(id) {
  return req(`/trips/${id}`);
}

export function createTrip(trip) {
  return req('/trips', { method: 'POST', body: JSON.stringify(trip) });
}

export function updateTrip(id, updates) {
  return req(`/trips/${id}`, { method: 'PATCH', body: JSON.stringify(updates) });
}

export function getTripRequests(tripId) {
  return req(`/trips/${tripId}/requests?includeProcessed=true`);
}

export function createTripRequest(tripId, request) {
  return req(`/trips/${tripId}/requests`, { method: 'POST', body: JSON.stringify(request) });
}

export function updateRequestStatus(requestId, updates) {
  return req(`/trips/requests/${requestId}`, { method: 'PATCH', body: JSON.stringify(updates) });
}

export function acceptRequest(tripId, requestId) {
  return req(`/trips/${tripId}/requests/${requestId}/accept`, { method: 'POST' });
}

export function rejectRequest(tripId, requestId) {
  return req(`/trips/${tripId}/requests/${requestId}/reject`, { method: 'POST' });
}

export function getPassengerRequests(passengerId) {
  const params = new URLSearchParams();
  if (passengerId) params.set('passengerId', passengerId);
  params.set('includeProcessed', 'true');
  params.set('populate', 'true');
  return req(`/trips/requests?${params}`);
}

export function registerUser(user) {
  return req('/users/register', { method: 'POST', body: JSON.stringify(user) });
}
