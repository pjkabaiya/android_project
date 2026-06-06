const express = require('express');
const http = require('http');
const cors = require('cors');
const dotenv = require('dotenv');
const mongoose = require('mongoose');
const { Server } = require('socket.io');
const admin = require('firebase-admin');

dotenv.config();

// Initialize Firebase Admin if service account is provided via env var
try {
  if (process.env.FIREBASE_SERVICE_ACCOUNT) {
    const sa = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
    admin.initializeApp({ credential: admin.credential.cert(sa) });
    console.log('Firebase admin initialized from FIREBASE_SERVICE_ACCOUNT env var');
  } else if (process.env.FIREBASE_SERVICE_ACCOUNT_PATH) {
    const saPath = process.env.FIREBASE_SERVICE_ACCOUNT_PATH;
    try {
      const sa = require(saPath);
      admin.initializeApp({ credential: admin.credential.cert(sa) });
      console.log('Firebase admin initialized from FIREBASE_SERVICE_ACCOUNT_PATH');
    } catch (e) {
      console.warn('Could not load service account path, falling back to default app credentials');
      admin.initializeApp();
    }
  } else {
    // Will use GOOGLE_APPLICATION_CREDENTIALS or default app credentials if available
    admin.initializeApp();
    console.log('Firebase admin initialized with default credentials');
  }
} catch (e) {
  console.warn('Firebase admin initialization warning:', e && e.message);
}

const app = express();
app.use(cors());
app.use(express.json());

const server = http.createServer(app);
const io = new Server(server, {
  cors: { origin: '*' }
});

// Basic routes
app.get('/', (req, res) => res.json({ ok: true, name: 'Smart Matatu Backend' }));

// Mount API routes (they'll be added in routes folder)
app.use('/api/auth', require('./routes/auth'));
app.use('/api/vehicles', require('./routes/vehicles'));
app.use('/api/routes', require('./routes/routes'));
app.use('/api/reservations', require('./routes/reservations'));

// Socket.IO for real-time updates
io.on('connection', (socket) => {
  socket.on('driver-join', (data) => {
    // driver joins a room for their vehicle id
    if (data && data.vehicleId) socket.join(`vehicle_${data.vehicleId}`);
  });

  socket.on('location-update', (payload) => {
    // payload: { vehicleId, latitude, longitude, timestamp }
    if (payload && payload.vehicleId) {
      io.to(`vehicle_${payload.vehicleId}`).emit('location-update', payload);
      // also broadcast to passengers subscribed to the route
      io.emit('vehicle-location', payload);
    }
  });

  socket.on('reservation-update', (payload) => {
    // passenger/driver reservation events
    io.emit('reservation-update', payload);
  });
});

// DB connect + start
const PORT = process.env.PORT || 4000;
mongoose.connect(process.env.MONGO_URI || '', { connectTimeoutMS: 10000 })
  .then(() => {
    server.listen(PORT, () => console.log(`Server listening on ${PORT}`));
  })
  .catch((err) => {
    console.error('MongoDB connection error:', err.message);
    server.listen(PORT, () => console.log(`Server listening on ${PORT} (no DB)`));
  });

module.exports = { app, server, io };
