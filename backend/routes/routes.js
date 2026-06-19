const express = require('express');
const router = express.Router();
const Route = require('../models/Route');

router.post('/', async (req, res) => {
  try {
    const doc = await Route.create(req.body);
    res.json(doc);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

router.get('/', async (req, res) => {
  try {
    const docs = await Route.find();
    res.json(docs);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

module.exports = router;
