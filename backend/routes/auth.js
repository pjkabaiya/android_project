const express = require('express');
const router = express.Router();
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const admin = require('firebase-admin');
const User = require('../models/User');
const Trip = require('../models/Trip');
const TripRequest = require('../models/TripRequest');

const JWT_SECRET = process.env.JWT_SECRET || 'smart-matatu-jwt-secret-dev';

// Sign up with email/password (web-app)
router.post('/signup', async (req, res) => {
  try {
    const { email, password, name, role } = req.body;
    if (!email || !password || !name) {
      return res.status(400).json({ error: 'email, password, name required' });
    }
    const existing = await User.findOne({ email });
    if (existing) return res.status(400).json({ error: 'Email already registered' });

    const hashedPassword = await bcrypt.hash(password, 10);
    const user = await User.create({
      email,
      name,
      password: hashedPassword,
      role: role || 'passenger',
      firebaseUid: 'web_' + Date.now()
    });
    const token = jwt.sign({ uid: user.firebaseUid, email: user.email, role: user.role }, JWT_SECRET, { expiresIn: '30d' });
    res.status(201).json({ token, user });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// Login with email/password (web-app)
router.post('/login', async (req, res) => {
  try {
    const { email, password } = req.body;
    if (!email || !password) return res.status(400).json({ error: 'email and password required' });

    const user = await User.findOne({ email });
    if (!user) return res.status(401).json({ error: 'Invalid email or password' });

    if (!user.password) return res.status(401).json({ error: 'This account uses Google sign-in' });

    const valid = await bcrypt.compare(password, user.password);
    if (!valid) return res.status(401).json({ error: 'Invalid email or password' });

    const token = jwt.sign({ uid: user.firebaseUid, email: user.email, role: user.role }, JWT_SECRET, { expiresIn: '30d' });
    res.json({ token, user });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// Verify Firebase token and return/create user (Android)
router.post('/verify', async (req, res) => {
  const idToken = req.body.token;
  if (!idToken) return res.status(400).json({ error: 'token required' });

  try {
    const decoded = await admin.auth().verifyIdToken(idToken);
    const firebaseUid = decoded.uid;
    let user = await User.findOne({ firebaseUid });
    if (!user) {
      user = await User.create({ firebaseUid, email: decoded.email, name: decoded.name || '', role: 'passenger' });
    }
    res.json({ user });
  } catch (err) {
    res.status(401).json({ error: 'invalid token', details: err.message });
  }
});

// Get user by email (for web session restore)
router.get('/me', async (req, res) => {
  try {
    const token = req.headers.authorization?.split(' ')[1];
    if (!token) return res.status(401).json({ error: 'token required' });
    const decoded = jwt.verify(token, JWT_SECRET);
    const user = await User.findOne({ firebaseUid: decoded.uid });
    if (!user) return res.status(404).json({ error: 'User not found' });
    res.json(user);
  } catch (err) {
    res.status(401).json({ error: 'invalid token' });
  }
});

// Get enhanced user profile with stats
router.get('/profile', async (req, res) => {
  try {
    const token = req.headers.authorization?.split(' ')[1];
    if (!token) return res.status(401).json({ error: 'token required' });
    const decoded = jwt.verify(token, JWT_SECRET);
    const user = await User.findOne({ firebaseUid: decoded.uid });
    if (!user) return res.status(404).json({ error: 'User not found' });

    const profile = { name: user.name, email: user.email, role: user.role, memberSince: user.createdAt };

    if (user.role === 'driver') {
      const trips = await Trip.find({ driverId: user.firebaseUid });
      profile.totalTrips = trips.length;
      const tripIds = trips.map(t => t._id);
      const accepted = await TripRequest.countDocuments({ tripId: { $in: tripIds }, status: 'ACCEPTED' });
      profile.totalPassengers = accepted;
    } else {
      const requests = await TripRequest.find({ passengerId: user.firebaseUid });
      profile.totalRequests = requests.length;
      profile.accepted = requests.filter(r => r.status === 'ACCEPTED').length;
      profile.rejected = requests.filter(r => r.status === 'REJECTED').length;
      profile.cancelled = requests.filter(r => r.status === 'CANCELLED').length;
    }

    res.json(profile);
  } catch (err) {
    res.status(401).json({ error: 'invalid token' });
  }
});

module.exports = router;
