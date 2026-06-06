const mongoose = require('mongoose');

const UserSchema = new mongoose.Schema({
  firebaseUid: { type: String, required: true, unique: true },
  name: { type: String },
  email: { type: String },
  role: { type: String, enum: ['passenger','driver','admin'], default: 'passenger' }
}, { timestamps: true });

module.exports = mongoose.model('User', UserSchema);
