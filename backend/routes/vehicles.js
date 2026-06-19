const express = require('express');
const router = express.Router();
const Vehicle = require('../models/Vehicle');

router.post('/', async (req, res) => {
  try {
    const doc = await Vehicle.create(req.body);
    res.json(doc);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

router.get('/', async (req, res) => {
  try {
    const docs = await Vehicle.find().populate('routeId driverId');
    res.json(docs);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

router.get('/:id', async (req, res) => {
  try {
    const doc = await Vehicle.findById(req.params.id).populate('routeId driverId');
    res.json(doc);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

module.exports = router;
