const express = require('express');
const router = express.Router();
const Reservation = require('../models/Reservation');

router.post('/', async (req, res) => {
  try {
    const doc = await Reservation.create(req.body);
    res.json(doc);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

router.get('/', async (req, res) => {
  try {
    const filter = {};
    if (!req.query.includeProcessed || req.query.includeProcessed !== 'true') filter.status = 'pending';
    const docs = await Reservation.find(filter);
    res.json(docs);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

router.patch('/:id/status', async (req, res) => {
  try {
    const status = req.body && req.body.status ? req.body.status : req.body;
    if (typeof status !== 'string') return res.status(400).json({ error: 'status must be a string' });
    const doc = await Reservation.findByIdAndUpdate(req.params.id, { status }, { new: true });
    if (!doc) return res.status(404).json({ error: 'Reservation not found' });
    const io = req.app && req.app.get && req.app.get('io');
    if (io) io.emit('reservation-update', { id: doc.id, status: doc.status });
    res.json(doc);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

router.post('/:id/accept', async (req, res) => {
  try {
    const doc = await Reservation.findByIdAndUpdate(req.params.id, { status: 'accepted' }, { new: true });
    if (!doc) return res.status(404).json({ error: 'Reservation not found' });
    const io = req.app && req.app.get && req.app.get('io');
    if (io) io.emit('reservation-update', { id: doc.id, status: doc.status });
    res.json(doc);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

router.post('/:id/reject', async (req, res) => {
  try {
    const doc = await Reservation.findByIdAndUpdate(req.params.id, { status: 'rejected' }, { new: true });
    if (!doc) return res.status(404).json({ error: 'Reservation not found' });
    const io = req.app && req.app.get && req.app.get('io');
    if (io) io.emit('reservation-update', { id: doc.id, status: doc.status });
    res.json(doc);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

module.exports = router;
