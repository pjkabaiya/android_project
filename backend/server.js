const express = require('express');
const http = require('http');
const cors = require('cors');
const dotenv = require('dotenv');
const mongoose = require('mongoose');
const { Server } = require('socket.io');
const admin = require('firebase-admin');
const User = require('./models/User');

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

// expose io through app to avoid circular require issues
app.set('io', io);

// Basic routes
app.get('/', (req, res) => res.json({ ok: true, name: 'Smart Matatu Backend' }));

// User routes (called by Android app)
app.post('/users/register', async (req, res) => {
  try {
    const { firebaseUid, name, email, role } = req.body;
    let user = await User.findOne({ firebaseUid });
    if (user) return res.status(200).json(user);
    user = await User.create({ firebaseUid, name, email, role: role || 'passenger' });
    res.status(201).json(user);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

app.get('/users/:firebaseUid', async (req, res) => {
  try {
    const user = await User.findOne({ firebaseUid: req.params.firebaseUid });
    if (!user) return res.status(404).json({ error: 'User not found' });
    res.json(user);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// Mount API routes (they'll be added in routes folder)
app.use('/api/auth', require('./routes/auth'));
app.use('/api/vehicles', require('./routes/vehicles'));
app.use('/api/routes', require('./routes/routes'));
app.use('/api/reservations', require('./routes/reservations'));
app.use('/api/trips', require('./routes/trips'));

// Socket.IO for real-time updates
io.on('connection', (socket) => {
  socket.on('driver-join', (data) => {
    if (data && data.tripId) socket.join(`trip_${data.tripId}`);
  });

  socket.on('location-update', (payload) => {
    if (payload && payload.tripId) {
      io.to(`trip_${payload.tripId}`).emit('location-update', payload);
      io.emit('vehicle-location', payload);
    }
  });

  socket.on('reservation-update', (payload) => {
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
