package matatu_system.A1.models;

import com.google.gson.annotations.SerializedName;

public class TripRequest {
    @SerializedName(value = "_id", alternate = {"id"})
    private String id;
    private String tripId;
    private String passengerId;
    private String pickupPoint;
    private double passengerLat;
    private double passengerLng;
    private String status;

    public TripRequest() {}

    public TripRequest(String tripId, String passengerId, String pickupPoint) {
        this.tripId = tripId;
        this.passengerId = passengerId;
        this.pickupPoint = pickupPoint;
        this.status = "WAITING";
    }

    public String getId() { return id; }
    public String getTripId() { return tripId; }
    public String getPassengerId() { return passengerId; }
    public String getPickupPoint() { return pickupPoint; }
    public double getPassengerLat() { return passengerLat; }
    public double getPassengerLng() { return passengerLng; }
    public String getStatus() { return status; }

    public void setId(String id) { this.id = id; }
    public void setTripId(String tripId) { this.tripId = tripId; }
    public void setPassengerId(String passengerId) { this.passengerId = passengerId; }
    public void setPickupPoint(String pickupPoint) { this.pickupPoint = pickupPoint; }
    public void setPassengerLat(double passengerLat) { this.passengerLat = passengerLat; }
    public void setPassengerLng(double passengerLng) { this.passengerLng = passengerLng; }
    public void setStatus(String status) { this.status = status; }
}
