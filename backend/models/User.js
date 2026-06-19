const mongoose = require('mongoose');

const UserSchema = new mongoose.Schema({
  firebaseUid: { type: String, unique: true, sparse: true },
  name: { type: String },
  email: { type: String },
  password: { type: String },
  role: { type: String, enum: ['passenger','driver','admin'], default: 'passenger' },
  numberPlate: { type: String, default: '' },
  phone: { type: String, default: '' }
}, { timestamps: true, toJSON: { virtuals: true, transform: (doc, ret) => { delete ret.password; ret.id = ret._id; delete ret._id; delete ret.__v; } } });

module.exports = mongoose.model('User', UserSchema);
