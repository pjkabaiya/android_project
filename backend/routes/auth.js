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

    // Create user in Firebase Auth so Android can sign in with same credentials
    let firebaseUid;
    try {
      const fbUser = await admin.auth().createUser({
        email,
        password,
        displayName: name,
      });
      firebaseUid = fbUser.uid;
    } catch (fbErr) {
      return res.status(400).json({ error: 'Failed to create authentication account: ' + fbErr.message });
    }

    const hashedPassword = await bcrypt.hash(password, 10);
    const user = await User.create({
      email,
      name,
      password: hashedPassword,
      role: role || 'passenger',
      firebaseUid
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

    let user = await User.findOne({ email });
    if (!user) return res.status(401).json({ error: 'Invalid email or password' });

    if (!user.password) return res.status(401).json({ error: 'This account uses Google sign-in. Please use the Android app.' });

    const valid = await bcrypt.compare(password, user.password);
    if (!valid) return res.status(401).json({ error: 'Invalid email or password' });

    // Migrate legacy web users (firebaseUid starts with 'web_') to real Firebase Auth
    if (user.firebaseUid && user.firebaseUid.startsWith('web_')) {
      try {
        const fbUser = await admin.auth().createUser({
          email: user.email,
          password,
          displayName: user.name,
        });
        user.firebaseUid = fbUser.uid;
        await user.save();
      } catch (fbErr) {
        // If user already exists in Firebase Auth (e.g. from earlier migration attempt),
        // try to find them by email and update the UID
        if (fbErr.code === 'auth/email-already-exists') {
          try {
            const fbUser = await admin.auth().getUserByEmail(user.email);
            user.firebaseUid = fbUser.uid;
            await user.save();
          } catch (getErr) {
            // ignore
          }
        }
      }
    }

    const token = jwt.sign({ uid: user.firebaseUid, email: user.email, role: user.role }, JWT_SECRET, { expiresIn: '30d' });
    res.json({ token, user });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// Verify Firebase token and return/create user (Android + future web Firebase Auth)
router.post('/verify', async (req, res) => {
  const idToken = req.body.token;
  if (!idToken) return res.status(400).json({ error: 'token required' });

  try {
    const decoded = await admin.auth().verifyIdToken(idToken);
    const firebaseUid = decoded.uid;
    let user = await User.findOne({ firebaseUid });
    if (!user) {
      const { name, role } = req.body;
      user = await User.create({
        firebaseUid,
        email: decoded.email || '',
        name: name || decoded.name || '',
        role: role || 'passenger',
      });
    }
    const token = jwt.sign({ uid: user.firebaseUid, email: user.email, role: user.role }, JWT_SECRET, { expiresIn: '30d' });
    res.json({ token, user });
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
