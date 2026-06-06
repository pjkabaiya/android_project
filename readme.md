Here is a **clean, final “school project specification prompt”** you can reuse to guide development, documentation, or even generate code step-by-step.

---

# 📌 FINAL SCHOOL PROJECT PROMPT

## Smart Matatu Route Tracking and Seat Reservation System (Android – Java)

Develop an Android application using **Java** that enables real-time tracking of matatus, seat availability monitoring, and seat reservation requests between passengers and drivers.

The system is designed for public transport efficiency and is NOT a payment platform. All payments are handled physically between passengers and drivers.

---

## 🧩 SYSTEM OVERVIEW

The application connects three user roles:

* **Passenger**
* **Driver**
* **Admin**

It enables:

* Real-time GPS tracking of vehicles
* Route-based vehicle discovery
* Seat availability updates
* Seat reservation requests (no in-app payments)
* Driver response to reservations
* Admin control of routes, drivers, and vehicles

---

## 🛠️ TECHNOLOGY STACK

### Mobile App

* Java (Android Studio)
* XML Layouts
* RecyclerView
* MVVM Architecture (ViewModel + LiveData)
* Retrofit (API communication)

### Backend

* Node.js + Express.js REST API
* MongoDB Atlas (Primary Database)
* Mongoose ODM

### Authentication

* Firebase Authentication (Email/Password)

### Maps & Location

* Google Maps SDK for Android
* Fused Location Provider API
* Google Directions API (for ETA calculations)

### Real-Time Updates

* Socket.IO (WebSockets for live GPS + seat updates)

---

## 👤 USER ROLES & FEATURES

---

## 1️⃣ PASSENGER MODULE

### Authentication

* Register / Login
* Logout

### Core Features

* Search destination or route
* View available matatus on route
* View live vehicle location on Google Maps
* View seat availability in real time
* View number plate and driver info
* View estimated arrival time (ETA)

### Seat Reservation

* Select pickup point
* Select destination
* Request seat reservation
* Receive status updates:

  * Pending
  * Accepted
  * Rejected

### Notifications

* Vehicle approaching pickup point
* Reservation accepted/rejected
* Vehicle arrival alert

---

## 2️⃣ DRIVER MODULE

### Authentication

* Login

### Trip Management

* Enter vehicle details:

  * Number plate
  * Route
  * Seat capacity
* Start trip (activates GPS tracking)
* End trip

### GPS Tracking

* Continuous location updates sent to backend
* Vehicle displayed on passenger map in real time

### Seat Management

Driver updates seat status:

* Occupied seats
* Vacant seats
* Reserved seats

### Reservation Handling

* Receive reservation requests
* Accept or reject requests
* View upcoming pickup points

Example:

```text id="driver_view"
Pickup: Point B → 1 Passenger  
Pickup: Point C → 2 Passengers
```

---

## 3️⃣ ADMIN MODULE

### Dashboard

* Total drivers
* Total passengers
* Active vehicles
* Active routes
* Active reservations

### Management

* Add/Edit/Delete routes
* Add/Edit/Delete drivers
* Assign drivers to vehicles
* Monitor live vehicle tracking

### Analytics

* Most used routes
* Peak travel times
* Vehicle utilization reports

---

## 🚍 CORE SYSTEM WORKFLOW

### Driver Flow

1. Login
2. Enter vehicle details
3. Start trip
4. Share live GPS location
5. Receive reservation requests
6. Update seat availability
7. End trip

---

### Passenger Flow

1. Login
2. Search route or destination
3. View available vehicles
4. View live tracking
5. Request seat reservation
6. Receive confirmation
7. Board vehicle and pay driver physically

---

## 🗄️ MONGODB DATABASE STRUCTURE

### Users Collection

```json
{
  "_id": "",
  "firebaseUid": "",
  "name": "",
  "email": "",
  "role": "passenger"
}
```

---

### Vehicles Collection

```json
{
  "_id": "",
  "driverId": "",
  "numberPlate": "",
  "routeId": "",
  "capacity": 14,
  "occupiedSeats": 0,
  "reservedSeats": 0,
  "availableSeats": 14,
  "status": "active"
}
```

---

### Routes Collection

```json
{
  "_id": "",
  "routeName": "CBD - Embakasi",
  "stops": ["A", "B", "C"],
  "fare": 50
}
```

---

### Reservations Collection

```json
{
  "_id": "",
  "passengerId": "",
  "vehicleId": "",
  "pickupPoint": "B",
  "destination": "C",
  "status": "pending",
  "createdAt": ""
}
```

---

### Location Collection

```json
{
  "_id": "",
  "vehicleId": "",
  "latitude": "",
  "longitude": "",
  "timestamp": ""
}
```

---

## 📍 GOOGLE MAPS FUNCTIONALITY

* Display live vehicle movement
* Show route paths
* Mark pickup and drop-off points
* Calculate ETA using Distance Matrix API

---

## ⚡ REAL-TIME FEATURES (Socket.IO)

* Live GPS updates from drivers
* Instant seat availability changes
* Reservation notifications
* Vehicle arrival alerts

---

## 🎯 MINIMUM VIABLE PRODUCT (MVP)

For school submission, ensure these features are working:

1. Firebase Authentication (role-based login)
2. Driver GPS live tracking
3. Passenger map view (Google Maps)
4. Vehicle route display
5. Seat availability updates
6. Reservation request system
7. Driver accept/reject reservations
8. MongoDB backend integration

---

## 🧠 PROJECT GOAL

The goal is to build a **real-time intelligent transport coordination system** that improves matatu efficiency by:

* Reducing passenger waiting time
* Improving seat utilization
* Allowing pre-arrival planning
* Enhancing route visibility

---


