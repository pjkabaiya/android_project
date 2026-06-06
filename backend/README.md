# Smart Matatu Backend

Simple Node.js + Express backend with Socket.IO and MongoDB models to support the Smart Matatu Android app.

Quick start

1. Copy `.env.example` to `.env` and set `MONGO_URI`.
2. Install deps:

```bash
cd backend
npm install
```

3. Run:

```bash
npm run dev
```

Socket events

- `location-update` — driver emits: `{ vehicleId, latitude, longitude, timestamp }`
- `reservation-update` — reservation status events
