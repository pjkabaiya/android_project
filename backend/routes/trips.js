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
    // By default return only waiting requests (queue)
    if (!req.query.includeProcessed || req.query.includeProcessed !== 'true') {
      filter.status = 'WAITING';
    }

    let query = TripRequest.find(filter);

    if (req.query.populate === 'true') {
      query = query.populate('tripId');
    }

    if (req.query.groupBy === 'vehicle') {
      const populated = await query;
      const grouped = {};
      populated.forEach(r => {
        const key = (r.tripId && r.tripId.numberPlate) || 'unknown';
        grouped[key] = grouped[key] || [];
        grouped[key].push(r);
      });
      return res.json(grouped);
    }

    const requests = await query;
    res.json(requests);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

router.patch('/requests/:id', async (req, res) => {
  try {
    const request = await TripRequest.findById(req.params.id);
    if (!request) return res.status(404).json({ error: 'Request not found' });

    const wasAccept = req.body.status === 'ACCEPTED' && request.status === 'WAITING';

    Object.assign(request, req.body);
    await request.save();

    if (wasAccept) {
      const trip = await Trip.findById(request.tripId);
      if (trip && typeof trip.availableSeats === 'number') {
        trip.availableSeats = Math.max(0, trip.availableSeats - 1);
        await trip.save();
      }
      const io = req.app && req.app.get && req.app.get('io');
      if (io) io.to(`trip_${request.tripId}`).emit('request-accepted', { requestId: request.id, tripId: request.tripId });
    }

    res.json(request);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// Cancel a trip with a reason
router.post('/:id/cancel', async (req, res) => {
  try {
    const { reason } = req.body;
    const trip = await Trip.findByIdAndUpdate(req.params.id, { status: 'CANCELLED', cancellationReason: reason || 'No reason provided' }, { new: true });
    if (!trip) return res.status(404).json({ error: 'Trip not found' });

    // Cancel all pending requests for this trip with the reason
    await TripRequest.updateMany(
      { tripId: req.params.id, status: { $in: ['WAITING', 'ACCEPTED'] } },
      { status: 'CANCELLED', cancellationReason: trip.cancellationReason }
    );

    const io = req.app && req.app.get && req.app.get('io');
    if (io) {
      io.to(`trip_${req.params.id}`).emit('trip-cancelled', { tripId: req.params.id, reason: trip.cancellationReason });
    }

    res.json({ ok: true, trip });
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
    const filter = { tripId: req.params.id };
    // By default only return waiting requests (queue)
    if (!req.query.includeProcessed || req.query.includeProcessed !== 'true') {
      filter.status = 'WAITING';
    }
    const requests = await TripRequest.find(filter);
    // Optionally group/sort by vehicle (numberPlate) if requested
    if (req.query.groupBy === 'vehicle') {
      // populate trip to read vehicle info
      const populated = await TripRequest.find(filter).populate('tripId');
      const grouped = {};
      populated.forEach(r => {
        const key = (r.tripId && r.tripId.numberPlate) || 'unknown';
        grouped[key] = grouped[key] || [];
        grouped[key].push(r);
      });
      return res.json(grouped);
    }

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

// Accept a passenger request
router.post('/:tripId/requests/:requestId/accept', async (req, res) => {
  try {
    const request = await TripRequest.findById(req.params.requestId);
    if (!request) return res.status(404).json({ error: 'Request not found' });
    if (request.status !== 'WAITING') return res.status(400).json({ error: 'Request already processed' });
    request.status = 'ACCEPTED';
    await request.save();
    // decrement available seats on trip if present
    const trip = await Trip.findById(req.params.tripId);
    if (trip && typeof trip.availableSeats === 'number') {
      trip.availableSeats = Math.max(0, trip.availableSeats - 1);
      await trip.save();
    }
    // notify via socket
    const io = req.app && req.app.get && req.app.get('io');
    if (io) io.to(`trip_${req.params.tripId}`).emit('request-accepted', { requestId: request.id, tripId: req.params.tripId });
    res.json({ ok: true, request });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// Reject a passenger request
router.post('/:tripId/requests/:requestId/reject', async (req, res) => {
  try {
    const request = await TripRequest.findById(req.params.requestId);
    if (!request) return res.status(404).json({ error: 'Request not found' });
    if (request.status !== 'WAITING') return res.status(400).json({ error: 'Request already processed' });
    request.status = 'REJECTED';
    await request.save();
    const io = req.app && req.app.get && req.app.get('io');
    if (io) io.to(`trip_${req.params.tripId}`).emit('request-rejected', { requestId: request.id, tripId: req.params.tripId });
    res.json({ ok: true, request });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

module.exports = router;
