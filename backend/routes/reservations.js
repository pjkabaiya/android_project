const express = require('express');
const router = express.Router();
const Reservation = require('../models/Reservation');

router.post('/', async (req, res) => {
  const doc = await Reservation.create(req.body);
  res.json(doc);
});

router.get('/', async (req, res) => {
  const docs = await Reservation.find();
  res.json(docs);
});

router.patch('/:id/status', async (req, res) => {
  const doc = await Reservation.findByIdAndUpdate(req.params.id, { status: req.body }, { new: true });
  if (!doc) return res.status(404).json({ error: 'Reservation not found' });
  res.json(doc);
});

module.exports = router;
