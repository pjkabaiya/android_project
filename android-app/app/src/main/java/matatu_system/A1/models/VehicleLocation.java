package matatu_system.A1.models;

public class VehicleLocation {
    private String vehicleId;
    private double latitude;
    private double longitude;
    private long timestamp;

    public VehicleLocation(String vehicleId, double latitude, double longitude, long timestamp) {
        this.vehicleId = vehicleId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }

    public String getVehicleId() { return vehicleId; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public long getTimestamp() { return timestamp; }
}
