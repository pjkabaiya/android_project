const mongoose = require('mongoose');

const TripSchema = new mongoose.Schema({
  numberPlate: { type: String, required: true },
  route: { type: String, required: true },
  // Optional detailed route geometry (array of { lat, lng }) persisted by driver
  routePath: [{
    lat: { type: Number },
    lng: { type: Number }
  }],
  // Optional encoded polyline for compact transfer
  routeEncoded: { type: String },
  availableSeats: { type: Number, default: 14 },
  status: { type: String, enum: ['ON_ROUTE', 'COMPLETED', 'CANCELLED'], default: 'ON_ROUTE' },
  driverId: { type: String },
  currentLocation: {
    lat: { type: Number },
    lng: { type: Number }
  }
}, { timestamps: true, toJSON: { virtuals: true, transform: (doc, ret) => { ret.id = ret._id; delete ret._id; delete ret.__v; } } });

module.exports = mongoose.model('Trip', TripSchema);
