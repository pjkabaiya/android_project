const mongoose = require('mongoose');

const TripRequestSchema = new mongoose.Schema({
  tripId: { type: mongoose.Schema.Types.ObjectId, ref: 'Trip' },
  passengerId: { type: String },
  pickupPoint: { type: String },
  status: { type: String, enum: ['WAITING', 'ACCEPTED', 'REJECTED'], default: 'WAITING' }
}, { timestamps: true, toJSON: { virtuals: true, transform: (doc, ret) => { ret.id = ret._id; delete ret._id; delete ret.__v; } } });

module.exports = mongoose.model('TripRequest', TripRequestSchema);
