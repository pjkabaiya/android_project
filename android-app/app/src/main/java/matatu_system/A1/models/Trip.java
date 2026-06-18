package matatu_system.A1.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Trip {
    @SerializedName(value = "id", alternate = {"_id"})
    private String id;
    private String numberPlate;
    private String route;
    private int availableSeats;
    private String status;
    private String driverId;
    private List<RoutePoint> routePath;

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
    public List<RoutePoint> getRoutePath() { return routePath; }

    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }
    public void setStatus(String status) { this.status = status; }
    public void setRoutePath(List<RoutePoint> routePath) { this.routePath = routePath; }

    public static class RoutePoint {
        public double lat;
        public double lng;

        public RoutePoint() {}
        public RoutePoint(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }
    }
}
