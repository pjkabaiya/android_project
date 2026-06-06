const mongoose = require('mongoose');

const VehicleSchema = new mongoose.Schema({
  driverId: { type: mongoose.Schema.Types.ObjectId, ref: 'User' },
  numberPlate: String,
  routeId: { type: mongoose.Schema.Types.ObjectId, ref: 'Route' },
  capacity: Number,
  occupiedSeats: { type: Number, default: 0 },
  reservedSeats: { type: Number, default: 0 },
  availableSeats: { type: Number, default: 0 },
  status: { type: String, default: 'active' }
}, { timestamps: true });

module.exports = mongoose.model('Vehicle', VehicleSchema);
