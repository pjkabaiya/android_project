const mongoose = require('mongoose');

const RouteSchema = new mongoose.Schema({
  routeName: String,
  stops: [String],
  fare: Number
}, { timestamps: true });

module.exports = mongoose.model('Route', RouteSchema);
