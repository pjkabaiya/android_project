const express = require('express');
const router = express.Router();
const Vehicle = require('../models/Vehicle');

router.post('/', async (req, res) => {
  const doc = await Vehicle.create(req.body);
  res.json(doc);
});

router.get('/', async (req, res) => {
  const docs = await Vehicle.find().populate('routeId driverId');
  res.json(docs);
});

router.get('/:id', async (req, res) => {
  const doc = await Vehicle.findById(req.params.id).populate('routeId driverId');
  res.json(doc);
});

module.exports = router;
