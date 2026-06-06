const express = require('express');
const router = express.Router();
const admin = require('firebase-admin');
const User = require('../models/User');

// Verify Firebase token and return/create user
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

module.exports = router;
