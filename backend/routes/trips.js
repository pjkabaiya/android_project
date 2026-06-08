const express = require('express');
const router = express.Router();
const Trip = require('../models/Trip');
const TripRequest = require('../models/TripRequest');

router.post('/', async (req, res) => {
  try {
    const trip = await Trip.create(req.body);
    res.status(201).json(trip);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

const escapeRegex = (str) => str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

router.get('/', async (req, res) => {
  try {
    const filter = { status: 'ON_ROUTE' };
    if (req.query.driverId) {
      filter.driverId = { $regex: '^' + escapeRegex(req.query.driverId) + '$', $options: 'i' };
    }
    if (req.query.route) {
      const keywords = req.query.route.split(/\s+/).filter(k => /[a-zA-Z0-9]/.test(k));
      if (keywords.length > 0) {
        filter.$and = keywords.map(k => ({
          route: { $regex: escapeRegex(k), $options: 'i' }
        }));
      }
    }
    const trips = await Trip.find(filter);
    res.json(trips);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

router.get('/requests', async (req, res) => {
  try {
    const filter = {};
    if (req.query.passengerId) {
      filter.passengerId = { $regex: '^' + escapeRegex(req.query.passengerId) + '$', $options: 'i' };
    }
    const requests = await TripRequest.find(filter);
    res.json(requests);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

router.get('/:id', async (req, res) => {
  try {
    const trip = await Trip.findById(req.params.id);
    if (!trip) return res.status(404).json({ error: 'Trip not found' });
    res.json(trip);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

router.patch('/:id', async (req, res) => {
  try {
    const trip = await Trip.findByIdAndUpdate(req.params.id, req.body, { new: true });
    if (!trip) return res.status(404).json({ error: 'Trip not found' });
    res.json(trip);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

router.get('/:id/requests', async (req, res) => {
  try {
    const requests = await TripRequest.find({ tripId: req.params.id });
    res.json(requests);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

router.post('/:id/requests', async (req, res) => {
  try {
    const request = await TripRequest.create({ ...req.body, tripId: req.params.id });
    res.status(201).json(request);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

module.exports = router;
