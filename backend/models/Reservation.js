const mongoose = require('mongoose');

const ReservationSchema = new mongoose.Schema({
  passengerId: { type: String },
  vehicleId: { type: String },
  pickupPoint: String,
  destination: String,
  status: { type: String, enum: ['pending','accepted','rejected'], default: 'pending' }
}, { timestamps: true, toJSON: { virtuals: true, transform: (doc, ret) => { ret.id = ret._id; delete ret._id; delete ret.__v; } } });

module.exports = mongoose.model('Reservation', ReservationSchema);
