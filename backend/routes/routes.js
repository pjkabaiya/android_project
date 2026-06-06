const express = require('express');
const router = express.Router();
const Route = require('../models/Route');

router.post('/', async (req, res) => {
  const doc = await Route.create(req.body);
  res.json(doc);
});

router.get('/', async (req, res) => {
  const docs = await Route.find();
  res.json(docs);
});

module.exports = router;
