package matatu_system.A1.map;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.Overlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import matatu_system.A1.R;
import matatu_system.A1.api.RetrofitClient;
import matatu_system.A1.models.Trip;
import matatu_system.A1.models.TripRequest;
import matatu_system.A1.utils.SocketManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapViewActivity extends AppCompatActivity {

    private MapView map;
    private TextView txtInfo, txtStatus, txtPassengerStatus, txtDistance, txtSpeed, txtEta;
    private Button btnWaypoint, btnSimulate, btnClear, btnSetPickup, btnRequestRide, btnUndo;
    private View bottomPanel, driverPanel, passengerPanel;
    private ListView requestList;

    private String tripId;
    private boolean isDriver;
    private boolean waypointMode;
    private boolean simulating;
    private boolean selectingPickup;

    private LocationManager locationManager;
    private final Handler simHandler = new Handler();
    private ArrayList<GeoPoint> waypoints = new ArrayList<>();
    private ArrayList<GeoPoint> routePoints = new ArrayList<>();
    private Polyline routePolyline;
    private Marker vehicleMarker;
    private int waypointIndex;
    private double currentLat, currentLng;
    private double simSpeedKph = 80;

    private List<TripRequest> pendingRequests = new ArrayList<>();
    private ArrayAdapter<String> requestAdapter;
    private Map<String, Marker> passengerMarkers = new HashMap<>();

    private static final double EARTH_RADIUS_KM = 6371;

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location loc) {
            sendLocationUpdate(loc.getLatitude(), loc.getLongitude());
        }
        @Override public void onStatusChanged(String p, int i, Bundle b) {}
        @Override public void onProviderEnabled(String p) {}
        @Override public void onProviderDisabled(String p) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_map_view);

        tripId = getIntent().getStringExtra("tripId");
        isDriver = getIntent().getBooleanExtra("isDriver", false);
        String plate = getIntent().getStringExtra("numberPlate");
        String route = getIntent().getStringExtra("route");
        boolean selectPickupDirect = getIntent().getBooleanExtra("selectPickupDirect", false);
        boolean createRouteDirect = getIntent().getBooleanExtra("createRouteDirect", false);

        map = findViewById(R.id.mapView);
        txtInfo = findViewById(R.id.txtMapInfo);
        txtStatus = findViewById(R.id.txtStatus);
        txtPassengerStatus = findViewById(R.id.txtPassengerStatus);
        txtDistance = findViewById(R.id.txtDistance);
        txtSpeed = findViewById(R.id.txtSpeed);
        txtEta = findViewById(R.id.txtEta);
        btnWaypoint = findViewById(R.id.btnAddWaypoint);
        btnSimulate = findViewById(R.id.btnSimulate);
        btnClear = findViewById(R.id.btnClearRoute);
        btnUndo = findViewById(R.id.btnUndoWaypoint);
        btnSetPickup = findViewById(R.id.btnSetPickup);
        btnRequestRide = findViewById(R.id.btnRequestRide);
        bottomPanel = findViewById(R.id.bottomPanel);
        driverPanel = findViewById(R.id.driverPanel);
        passengerPanel = findViewById(R.id.passengerPanel);
        requestList = findViewById(R.id.requestList);

        txtInfo.setText((isDriver ? "Driver" : "Passenger") + " - " + (plate != null ? plate : ""));
        
        // Enforce View vs Edit mode
        if (!isDriver) { 
            passengerPanel.setVisibility(View.VISIBLE); 
            driverPanel.setVisibility(View.GONE); 
            btnWaypoint.setVisibility(View.GONE);
            btnSimulate.setVisibility(View.GONE);
            btnClear.setVisibility(View.GONE);
            btnSetPickup.setVisibility(View.VISIBLE);
            if (btnRequestRide != null) btnRequestRide.setVisibility(View.VISIBLE);
            txtStatus.setText("Tracking vehicle...");
        } else {
            passengerPanel.setVisibility(View.GONE);
            driverPanel.setVisibility(View.VISIBLE);
            btnSetPickup.setVisibility(View.GONE);
            if (btnRequestRide != null) btnRequestRide.setVisibility(View.GONE);
            txtStatus.setText("Tap Set WP to plan route");
        }

        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(14.0);
        map.getController().setCenter(new GeoPoint(-1.286389, 36.817223));

        // Always add the event receiver at the start
        map.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override public boolean singleTapConfirmedHelper(GeoPoint p) { 
                if (isDriver && waypointMode) addWaypoint(p); 
                else if (!isDriver && selectingPickup) setPickupPoint(p);
                return true; 
            }
            @Override public boolean longPressHelper(GeoPoint p) { return false; }
        }));

        btnWaypoint.setOnClickListener(v -> toggleWaypointMode());
        btnSimulate.setOnClickListener(v -> toggleSimulation());
        btnClear.setOnClickListener(v -> clearRoute());
        if (btnUndo != null) {
            btnUndo.setOnClickListener(v -> undoLastWaypoint());
        }
        if (btnSetPickup != null) {
            btnSetPickup.setOnClickListener(v -> togglePickupSelection());
        }
        if (btnRequestRide != null) {
            btnRequestRide.setOnClickListener(v -> requestRideFromMap());
        }

        SocketManager.establishConnection();
        joinTripRoom();
        listenForUpdates();
        loadTripData();
        loadRequests();

        requestAdapter = new ArrayAdapter<>(this, R.layout.item_simple_text, new ArrayList<>());
        requestList.setAdapter(requestAdapter);
        requestList.setOnItemClickListener((parent, v, pos, id) -> {
            if (pos < pendingRequests.size()) acceptRequest(pendingRequests.get(pos));
        });

        if (isDriver) startGps();

        // Enable direct pickup selection for passengers
        if (selectPickupDirect && !isDriver) {
            selectingPickup = true;
            btnSetPickup.setText("Tap Map");
            txtStatus.setText("Tap map to set your pickup point");
        }

        // Enable direct route creation for drivers
        if (createRouteDirect && isDriver) {
            waypointMode = true;
            btnWaypoint.setText("Done WP");
            if (btnUndo != null) btnUndo.setVisibility(View.VISIBLE);
            if (btnSimulate != null) {
                btnSimulate.setText("Start Journey");
                btnSimulate.setVisibility(View.VISIBLE);
            }
            txtStatus.setText("Tap map to add route points");
        }
    }

    private void togglePickupSelection() {
        selectingPickup = !selectingPickup;
        btnSetPickup.setText(selectingPickup ? "Tap Map" : "Set Pickup");
        if (selectingPickup) txtStatus.setText("Tap map to set pickup point");
    }

    private void setPickupPoint(GeoPoint p) {
        currentLat = p.getLatitude();
        currentLng = p.getLongitude();
        selectingPickup = false;
        btnSetPickup.setText("Pickup Set");
        txtStatus.setText("Pickup point updated!");
        
        // Show name input dialog
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Enter Your Name");
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        input.setHint("Your name");
        builder.setView(input);
        
        builder.setPositiveButton("OK", (dialog, which) -> {
            String passengerName = input.getText().toString().trim();
            if (passengerName.isEmpty()) {
                passengerName = "Passenger";
            }
            
            // Add marker on map
            TripRequest dummy = new TripRequest(tripId, "PASSENGER", passengerName);
            dummy.setPassengerLat(currentLat);
            dummy.setPassengerLng(currentLng);
            dummy.setStatus("WAITING");
            addPassengerMarker(dummy);
            
            // Create the trip request
            createTripRequestWithName(passengerName);
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            selectingPickup = true;
            btnSetPickup.setText("Tap Map");
            txtStatus.setText("Tap map to set your pickup point");
            dialog.cancel();
        });
        
        builder.show();
    }

    private void createTripRequestWithName(String passengerName) {
        String passengerId = "passenger_" + System.currentTimeMillis();
        TripRequest req = new TripRequest(tripId, passengerId, passengerName);
        req.setPassengerLat(currentLat);
        req.setPassengerLng(currentLng);
        
        RetrofitClient.getApiService().createTripRequest(tripId, req).enqueue(new Callback<TripRequest>() {
            @Override
            public void onResponse(Call<TripRequest> call, Response<TripRequest> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MapViewActivity.this, "Ride requested with location!", Toast.LENGTH_LONG).show();
                    loadRequests();
                    
                    if (SocketManager.getSocket() != null) {
                        try {
                            JSONObject data = new JSONObject();
                            data.put("tripId", tripId);
                            data.put("pickupPoint", passengerName);
                            data.put("type", "passenger_request");
                            data.put("passengerLat", currentLat);
                            data.put("passengerLng", currentLng);
                            SocketManager.getSocket().emit("reservation-update", data);
                        } catch (JSONException e) { e.printStackTrace(); }
                    }
                } else {
                    Toast.makeText(MapViewActivity.this, "Request failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<TripRequest> call, Throwable t) {
                Toast.makeText(MapViewActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleWaypointMode() {
        waypointMode = !waypointMode;
        btnWaypoint.setText(waypointMode ? "Done WP" : "Set WP");
        if (btnUndo != null) btnUndo.setVisibility(waypointMode ? View.VISIBLE : View.GONE);
        if (waypointMode) {
            txtStatus.setText("Tap map to place waypoints");
        } else {
            if (waypoints.size() >= 2) {
                txtStatus.setText("Calculating road route...");
                fetchRoadRoute();
            } else {
                txtStatus.setText("Waypoints: " + waypoints.size());
            }
        }
    }

    private void undoLastWaypoint() {
        if (!waypoints.isEmpty()) {
            GeoPoint last = waypoints.remove(waypoints.size() - 1);
            
            // Remove its marker
            for (Overlay o : map.getOverlays()) {
                if (o instanceof Marker) {
                    Marker m = (Marker) o;
                    if (m.getPosition().equals(last) && m != vehicleMarker) {
                        map.getOverlays().remove(m);
                        break;
                    }
                }
            }
            
            txtStatus.setText("Waypoints: " + waypoints.size());
            drawRoute();
            sendRouteBroadcast();
        }
    }

    private void addWaypoint(GeoPoint p) {
        waypoints.add(p);
        txtStatus.setText("Waypoints: " + waypoints.size() + " (tap Done WP when finished)");
        drawRoute();
        Marker wpMarker = new Marker(map);
        wpMarker.setPosition(p);
        wpMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        wpMarker.setTitle("WP " + waypoints.size());
        wpMarker.setIcon(getDrawable(R.drawable.ic_waypoint));
        map.getOverlays().add(wpMarker);
        map.invalidate();
        
        // Broadcast waypoints in real-time
        sendRouteBroadcast();
    }

    private void drawRoute() {
        if (routePolyline != null) map.getOverlays().remove(routePolyline);
        ArrayList<GeoPoint> pts = (routePoints != null && !routePoints.isEmpty()) ? routePoints : waypoints;
        if (pts.size() < 2) return;
        
        // If this client is a passenger, remove any waypoint markers so route appears read-only
        if (!isDriver) {
            List<Overlay> toRemove = new ArrayList<>();
            for (Overlay o : map.getOverlays()) {
                if (o instanceof Marker && o != vehicleMarker) toRemove.add(o);
            }
            map.getOverlays().removeAll(toRemove);
        }

        routePolyline = new Polyline();
        routePolyline.setPoints(new ArrayList<>(pts));
        // Enforce blue color for shared route
        routePolyline.setColor(0xFF1565C0);
        routePolyline.setWidth(8f); // Slightly thicker for better visibility
        // Make route non-interactive for passengers
        routePolyline.setOnClickListener(null);

        // Add polyline below markers so vehicle/requests render above it
        map.getOverlays().add(0, routePolyline);
        map.invalidate();
    }

    private void fetchRoadRoute() {
        if (waypoints.size() < 2) return;
        new Thread(() -> {
            try {
                StringBuilder sb = new StringBuilder("https://routing.openstreetmap.de/routed-car/route/v1/driving/");
                for (int i = 0; i < waypoints.size(); i++) {
                    GeoPoint p = waypoints.get(i);
                    sb.append(p.getLongitude()).append(",").append(p.getLatitude());
                    if (i < waypoints.size() - 1) sb.append(";");
                }
                sb.append("?overview=full&geometries=geojson");

                java.net.URL url = new java.net.URL(sb.toString());
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "SmartMatatu/1.0 (" + getPackageName() + ")");

                int responseCode = conn.getResponseCode();
                java.io.InputStream in = (responseCode >= 200 && responseCode < 300) ? 
                    conn.getInputStream() : conn.getErrorStream();
                
                if (in == null) throw new Exception("OSRM Error: " + responseCode);

                java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\A");
                String result = s.hasNext() ? s.next() : "";

                if (responseCode >= 300) throw new Exception("OSRM Error: " + responseCode + " - " + result);

                JSONObject json = new JSONObject(result);
                JSONArray routes = json.getJSONArray("routes");
                if (routes.length() > 0) {
                    JSONObject route = routes.getJSONObject(0);
                    JSONObject geometry = route.getJSONObject("geometry");
                    JSONArray coordinates = geometry.getJSONArray("coordinates");

                    ArrayList<GeoPoint> newRoutePoints = new ArrayList<>();
                    List<List<Double>> pathForApi = new ArrayList<>();
                    for (int i = 0; i < coordinates.length(); i++) {
                        JSONArray coord = coordinates.getJSONArray(i);
                        double lat = coord.getDouble(1);
                        double lng = coord.getDouble(0);
                        newRoutePoints.add(new GeoPoint(lat, lng));
                        List<Double> point = new ArrayList<>();
                        point.add(lat); point.add(lng);
                        pathForApi.add(point);
                    }

                    runOnUiThread(() -> {
                        routePoints = newRoutePoints;
                        drawRoute();
                        txtStatus.setText("Road route created!");
                        if (isDriver) {
                            sendRouteBroadcast();
                            saveRouteToBackend(pathForApi);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Road snapping failed. Using straight lines.", Toast.LENGTH_SHORT).show();
                    routePoints.clear();
                    drawRoute();
                    if (isDriver) sendRouteBroadcast();
                });
            }
        }).start();
    }

    private void saveRouteToBackend(List<List<Double>> path) {
        if (tripId == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("routePath", path);
        RetrofitClient.getApiService().updateTrip(tripId, updates).enqueue(new Callback<Trip>() {
            @Override public void onResponse(Call<Trip> call, Response<Trip> response) {}
            @Override public void onFailure(Call<Trip> call, Throwable t) {}
        });
    }

    private void loadTripData() {
        if (tripId == null) return;
        RetrofitClient.getApiService().searchTrips(null, null).enqueue(new Callback<List<Trip>>() {
            @Override
            public void onResponse(Call<List<Trip>> call, Response<List<Trip>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Trip trip : response.body()) {
                        if (tripId.equals(trip.getId()) && trip.getRoutePath() != null) {
                            routePoints.clear();
                            for (Trip.RoutePoint point : trip.getRoutePath()) {
                                routePoints.add(new GeoPoint(point.lat, point.lng));
                            }
                            drawRoute();
                            break;
                        }
                    }
                }
            }
            @Override public void onFailure(Call<List<Trip>> call, Throwable t) {}
        });
    }

    private void sendRouteBroadcast() {
        if (tripId == null || SocketManager.getSocket() == null) return;
        ArrayList<GeoPoint> pts = (routePoints != null && !routePoints.isEmpty()) ? routePoints : waypoints;
        if (pts.isEmpty()) return;
        try {
            JSONArray pathArray = new JSONArray();
            for (GeoPoint p : pts) {
                JSONArray coord = new JSONArray();
                coord.put(p.getLatitude());
                coord.put(p.getLongitude());
                pathArray.put(coord);
            }
            JSONObject payload = new JSONObject();
            payload.put("tripId", tripId);
            payload.put("type", "route_broadcast");
            payload.put("routePath", pathArray);
            SocketManager.getSocket().emit("location-update", payload);
        } catch (JSONException e) { e.printStackTrace(); }
    }

    private void clearRoute() {
        if (!isDriver) return;
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Clear or Delete?")
            .setMessage("Do you want to clear the map route or completely delete this trip?")
            .setNeutralButton("Cancel", null)
            .setNegativeButton("Clear Map", (d, w) -> performClearRoute())
            .setPositiveButton("Delete Trip", (d, w) -> performDeleteTrip())
            .show();
    }

    private void performClearRoute() {
        if (simulating) stopSimulation();
        waypointMode = false;
        waypoints.clear();
        routePoints.clear();
        btnWaypoint.setText("Set WP");
        txtStatus.setText("Route cleared");
        
        List<Overlay> toRemove = new ArrayList<>();
        for (Overlay o : map.getOverlays()) {
            if (o instanceof Marker && o != vehicleMarker) toRemove.add(o);
            if (o instanceof Polyline) toRemove.add(o);
        }
        map.getOverlays().removeAll(toRemove);
        routePolyline = null;
        map.invalidate();
        
        sendRouteBroadcast();
        saveRouteToBackend(new ArrayList<>());
    }

    private void performDeleteTrip() {
        if (tripId == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "CANCELLED");
        RetrofitClient.getApiService().updateTrip(tripId, updates).enqueue(new Callback<Trip>() {
            @Override
            public void onResponse(Call<Trip> call, Response<Trip> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MapViewActivity.this, "Trip deleted", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            @Override
            public void onFailure(Call<Trip> call, Throwable t) {
                Toast.makeText(MapViewActivity.this, "Failed to delete", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleSimulation() {
        if (waypoints.size() < 2 && routePoints.isEmpty()) { 
            Toast.makeText(this, "Place at least 2 waypoints first", Toast.LENGTH_SHORT).show(); 
            return; 
        }
        if (simulating) stopSimulation();
        else startSimulation();
    }

    private void startSimulation() {
        ArrayList<GeoPoint> pts = (routePoints != null && !routePoints.isEmpty()) ? routePoints : waypoints;
        if (pts.isEmpty()) { Toast.makeText(this, "Place waypoints first", Toast.LENGTH_SHORT).show(); return; }
        simulating = true;
        waypointIndex = 0;
        currentLat = pts.get(0).getLatitude();
        currentLng = pts.get(0).getLongitude();
        btnSimulate.setText("Stop");
        txtStatus.setText("Simulating...");
        sendLocationUpdate(currentLat, currentLng);
        updateVehicleMarker(currentLat, currentLng);
        simHandler.post(simRunnable);
    }

    private void stopSimulation() {
        simulating = false;
        simHandler.removeCallbacks(simRunnable);
        btnSimulate.setText("Start");
        txtStatus.setText("Simulation stopped");
    }

    private final Runnable simRunnable = new Runnable() {
        @Override
        public void run() {
            ArrayList<GeoPoint> pts = (routePoints != null && !routePoints.isEmpty()) ? routePoints : waypoints;
            if (!simulating || waypointIndex >= pts.size() - 1) {
                if (waypointIndex >= pts.size() - 1 && simulating) {
                    txtStatus.setText("Route complete!");
                    stopSimulation();
                }
                return;
            }
            GeoPoint from = pts.get(waypointIndex);
            GeoPoint to = pts.get(waypointIndex + 1);
            if (from == null || to == null) return;

            double segDistKm = haversineKm(from.getLatitude(), from.getLongitude(), to.getLatitude(), to.getLongitude());
            double segSpeedKps = simSpeedKph / 3600.0;
            double timePerStepSec = 1.5;
            double stepDistKm = segSpeedKps * timePerStepSec;
            double fraction = stepDistKm / segDistKm;

            if (fraction >= 1.0) {
                waypointIndex++;
                currentLat = to.getLatitude();
                currentLng = to.getLongitude();
            } else {
                currentLat = from.getLatitude() + (to.getLatitude() - from.getLatitude()) * fraction;
                currentLng = from.getLongitude() + (to.getLongitude() - from.getLongitude()) * fraction;
            }

            sendLocationUpdate(currentLat, currentLng);
            updateVehicleMarker(currentLat, currentLng);
            updatePassengerEta();
            simHandler.postDelayed(this, (long)(timePerStepSec * 1000));
        }
    };

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private void updateVehicleMarker(double lat, double lng) {
        if (map == null) return;
        GeoPoint pos = new GeoPoint(lat, lng);
        map.getController().setCenter(pos);
        if (vehicleMarker == null) {
            vehicleMarker = new Marker(map);
            vehicleMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            vehicleMarker.setTitle(isDriver ? "My Vehicle" : "Matatu");
            vehicleMarker.setIcon(getDrawable(R.drawable.ic_matatu));
            map.getOverlays().add(vehicleMarker);
        }
        vehicleMarker.setPosition(pos);
        map.invalidate();
    }

    private void loadRequests() {
        if (tripId == null) return;
        RetrofitClient.getApiService().getTripRequests(tripId).enqueue(new Callback<List<TripRequest>>() {
            @Override
            public void onResponse(Call<List<TripRequest>> call, Response<List<TripRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    pendingRequests = response.body();
                    updateRequestList();
                    for (TripRequest req : pendingRequests) {
                        if (req.getPassengerLat() != 0 || req.getPassengerLng() != 0) {
                            addPassengerMarker(req);
                        }
                    }
                }
            }
            @Override
            public void onFailure(Call<List<TripRequest>> call, Throwable t) {}
        });
    }

    private void updateRequestList() {
        if (!isDriver) return;
        List<String> items = new ArrayList<>();
        for (TripRequest req : pendingRequests) {
            String status = req.getStatus() != null ? req.getStatus() : "WAITING";
            items.add(req.getPickupPoint() + " - " + status + " (tap to accept)");
        }
        if (items.isEmpty()) items.add("No requests yet");
        requestAdapter = new ArrayAdapter<>(this, R.layout.item_simple_text, items);
        requestList.setAdapter(requestAdapter);
    }

    private void acceptRequest(TripRequest req) {
        if (!req.getStatus().equals("WAITING")) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "ACCEPTED");
        RetrofitClient.getApiService().updateRequestStatus(req.getId(), updates).enqueue(new Callback<TripRequest>() {
            @Override
            public void onResponse(Call<TripRequest> call, Response<TripRequest> response) {
                if (response.isSuccessful()) {
                    req.setStatus("ACCEPTED");
                    updateRequestList();
                    Toast.makeText(MapViewActivity.this, "Request accepted!", Toast.LENGTH_SHORT).show();
                    sendAcceptEvent(req);
                    addPassengerMarker(req);
                }
            }
            @Override
            public void onFailure(Call<TripRequest> call, Throwable t) {
                Toast.makeText(MapViewActivity.this, "Accept failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendAcceptEvent(TripRequest req) {
        if (SocketManager.getSocket() == null) return;
        try {
            JSONObject data = new JSONObject();
            data.put("tripId", tripId);
            data.put("requestId", req.getId());
            data.put("status", "ACCEPTED");
            data.put("type", "request_accepted");
            SocketManager.getSocket().emit("reservation-update", data);
        } catch (JSONException e) { e.printStackTrace(); }
    }

    private void addPassengerMarker(TripRequest req) {
        if (map == null || (req.getPassengerLat() == 0 && req.getPassengerLng() == 0)) return;
        GeoPoint pos = new GeoPoint(req.getPassengerLat(), req.getPassengerLng());
        String key = req.getId();
        if (passengerMarkers.containsKey(key)) {
            map.getOverlays().remove(passengerMarkers.get(key));
        }
        Marker marker = new Marker(map);
        marker.setPosition(pos);
        marker.setTitle(req.getPickupPoint());
        marker.setSnippet(req.getStatus() != null ? req.getStatus() : "WAITING");
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        if ("ACCEPTED".equals(req.getStatus())) {
            marker.setIcon(getDrawable(R.drawable.ic_passenger_accepted));
        } else {
            marker.setIcon(getDrawable(R.drawable.ic_passenger_pin));
        }
        map.getOverlays().add(marker);
        passengerMarkers.put(key, marker);
        map.invalidate();
    }

    private void updatePassengerEta() {
        if (isDriver) return;
        for (TripRequest req : pendingRequests) {
            if ("ACCEPTED".equals(req.getStatus()) && (req.getPassengerLat() != 0 || req.getPassengerLng() != 0)) {
                double dist = haversineKm(currentLat, currentLng, req.getPassengerLat(), req.getPassengerLng());
                double etaMin = (dist / simSpeedKph) * 60;
                txtDistance.setText(String.format("%.1f km", dist));
                txtSpeed.setText(String.format("%.0f km/h", simSpeedKph));
                txtEta.setText(String.format("%.0f min", etaMin));
                txtPassengerStatus.setText("Driver accepted! Matatu is coming");
            }
        }
    }

    private void joinTripRoom() {
        if (SocketManager.getSocket() != null) {
            try {
                JSONObject data = new JSONObject();
                data.put("tripId", tripId);
                SocketManager.getSocket().emit("driver-join", data);
            } catch (JSONException e) { e.printStackTrace(); }
        }
    }

    private void listenForUpdates() {
        if (SocketManager.getSocket() == null) return;

        SocketManager.getSocket().on("vehicle-location", args -> {
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    if (data.optString("tripId").equals(tripId)) {
                        if ("route_broadcast".equals(data.optString("type")) && !isDriver) {
                            JSONArray pathArray = data.optJSONArray("routePath");
                            if (pathArray != null) {
                                runOnUiThread(() -> {
                                    routePoints.clear();
                                    for (int i = 0; i < pathArray.length(); i++) {
                                        JSONArray coord = pathArray.optJSONArray(i);
                                        if (coord != null) {
                                            routePoints.add(new GeoPoint(coord.optDouble(0), coord.optDouble(1)));
                                        }
                                    }
                                    drawRoute();
                                });
                            }
                        } else if (!isDriver) {
                            double lat = data.getDouble("latitude");
                            double lng = data.getDouble("longitude");
                            currentLat = lat; currentLng = lng;
                            runOnUiThread(() -> updateVehicleMarker(lat, lng));
                        }
                    }
                } catch (JSONException e) { e.printStackTrace(); }
            }
        });

        // Also listen for raw 'location-update' events (some relays use this name)
        SocketManager.getSocket().on("location-update", args -> {
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    if (data.optString("tripId").equals(tripId)) {
                        if ("route_broadcast".equals(data.optString("type")) && !isDriver) {
                            JSONArray pathArray = data.optJSONArray("routePath");
                            if (pathArray != null) {
                                runOnUiThread(() -> {
                                    routePoints.clear();
                                    for (int i = 0; i < pathArray.length(); i++) {
                                        JSONArray coord = pathArray.optJSONArray(i);
                                        if (coord != null) {
                                            routePoints.add(new GeoPoint(coord.optDouble(0), coord.optDouble(1)));
                                        }
                                    }
                                    drawRoute();
                                });
                            }
                        } else if (!isDriver && data.has("latitude") && data.has("longitude")) {
                            double lat = data.getDouble("latitude");
                            double lng = data.getDouble("longitude");
                            currentLat = lat; currentLng = lng;
                            runOnUiThread(() -> updateVehicleMarker(lat, lng));
                        }
                    }
                } catch (JSONException e) { e.printStackTrace(); }
            }
        });

        SocketManager.getSocket().on("reservation-update", args -> {
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    if (data.optString("tripId").equals(tripId)) {
                        String type = data.optString("type");
                        if ("request_accepted".equals(type) && !isDriver) {
                            runOnUiThread(() -> {
                                txtPassengerStatus.setText("Driver accepted! Matatu is coming");
                                loadRequests();
                            });
                        } else if (isDriver) {
                            runOnUiThread(() -> {
                                loadRequests();
                                Toast.makeText(this, "New request from " + data.optString("pickupPoint"), Toast.LENGTH_LONG).show();
                            });
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        });
    }

    private void startGps() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
        }
    }

    private void sendLocationUpdate(double lat, double lng) {
        if (tripId == null || SocketManager.getSocket() == null) return;
        try {
            JSONObject payload = new JSONObject();
            payload.put("tripId", tripId);
            payload.put("latitude", lat);
            payload.put("longitude", lng);
            SocketManager.getSocket().emit("location-update", payload);
        } catch (JSONException e) { e.printStackTrace(); }
    }

    private void requestRideFromMap() {
        if (currentLat == 0 || currentLng == 0) {
            Toast.makeText(this, "Please tap 'Set Pickup' and select a point on map", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String passengerId = "passenger_" + System.currentTimeMillis();
        TripRequest req = new TripRequest(tripId, passengerId, "Map Selection");
        req.setPassengerLat(currentLat);
        req.setPassengerLng(currentLng);
        
        RetrofitClient.getApiService().createTripRequest(tripId, req).enqueue(new Callback<TripRequest>() {
            @Override
            public void onResponse(Call<TripRequest> call, Response<TripRequest> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MapViewActivity.this, "Ride requested at map location!", Toast.LENGTH_LONG).show();
                    loadRequests();
                    
                    if (SocketManager.getSocket() != null) {
                        try {
                            JSONObject data = new JSONObject();
                            data.put("tripId", tripId);
                            data.put("pickupPoint", "Map Selection");
                            data.put("type", "passenger_request");
                            data.put("passengerLat", currentLat);
                            data.put("passengerLng", currentLng);
                            SocketManager.getSocket().emit("reservation-update", data);
                        } catch (JSONException e) { e.printStackTrace(); }
                    }
                }
            }
            @Override public void onFailure(Call<TripRequest> call, Throwable t) {}
        });
    }

    @Override protected void onResume() { super.onResume(); map.onResume(); }
    @Override protected void onPause() { super.onPause(); map.onPause(); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSimulation();
        if (locationManager != null) locationManager.removeUpdates(locationListener);
        SocketManager.releaseConnection();
    }
}
