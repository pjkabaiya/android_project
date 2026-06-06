const express = require('express');
const router = express.Router();
const Reservation = require('../models/Reservation');

router.post('/', async (req, res) => {
  const doc = await Reservation.create(req.body);
  res.json(doc);
});

router.get('/', async (req, res) => {
  const docs = await Reservation.find().populate('passengerId vehicleId');
  res.json(docs);
});

router.patch('/:id', async (req, res) => {
  const doc = await Reservation.findByIdAndUpdate(req.params.id, req.body, { new: true });
  res.json(doc);
});

module.exports = router;
