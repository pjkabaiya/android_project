package matatu_system.A1.models;

public class Trip {
    private String id;
    private String numberPlate;
    private String route;
    private int availableSeats;
    private String status;
    private String driverId;

    public Trip() {}

    public Trip(String numberPlate, String route, int availableSeats, String driverId) {
        this.numberPlate = numberPlate;
        this.route = route;
        this.availableSeats = availableSeats;
        this.driverId = driverId;
        this.status = "ON_ROUTE";
    }

    public String getId() { return id; }
    public String getNumberPlate() { return numberPlate; }
    public String getRoute() { return route; }
    public int getAvailableSeats() { return availableSeats; }
    public String getStatus() { return status; }
    public String getDriverId() { return driverId; }

    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }
    public void setStatus(String status) { this.status = status; }
}
