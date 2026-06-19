import { io } from 'socket.io-client';

const SOCKET_URL = 'https://android-project-gb6e.onrender.com';

let socket = null;

export function getSocket() {
  if (!socket) {
    socket = io(SOCKET_URL, { transports: ['websocket', 'polling'] });
  }
  return socket;
}

export function joinTripRoom(tripId) {
  const s = getSocket();
  s.emit('driver-join', { tripId });
}

export function sendLocationUpdate(tripId, lat, lng) {
  const s = getSocket();
  s.emit('location-update', { tripId, latitude: lat, longitude: lng });
}

export function sendReservationUpdate(data) {
  const s = getSocket();
  s.emit('reservation-update', data);
}

export function disconnectSocket() {
  if (socket) {
    socket.disconnect();
    socket = null;
  }
}
