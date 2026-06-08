package matatu_system.A1.models;

public class TripRequest {
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

    public void setPassengerLat(double passengerLat) { this.passengerLat = passengerLat; }
    public void setPassengerLng(double passengerLng) { this.passengerLng = passengerLng; }
}
