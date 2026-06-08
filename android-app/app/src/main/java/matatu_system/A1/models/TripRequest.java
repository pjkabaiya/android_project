package matatu_system.A1.models;

public class TripRequest {
    private String id;
    private String tripId;
    private String passengerId;
    private String pickupPoint;
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
    public String getStatus() { return status; }
}
