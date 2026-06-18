package matatu_system.A1.models;

import com.google.gson.annotations.SerializedName;

public class Reservation {
    @SerializedName("_id")
    private String id;
    private String passengerId;
    private String vehicleId;
    private String pickupPoint;
    private String destination;
    private String status; // pending, accepted, rejected

    public Reservation(String passengerId, String vehicleId, String pickupPoint, String destination) {
        this.passengerId = passengerId;
        this.vehicleId = vehicleId;
        this.pickupPoint = pickupPoint;
        this.destination = destination;
        this.status = "pending";
    }

    public String getId() { return id; }
    public String getPassengerId() { return passengerId; }
    public String getVehicleId() { return vehicleId; }
    public String getPickupPoint() { return pickupPoint; }
    public String getDestination() { return destination; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
