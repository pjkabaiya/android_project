package matatu_system.A1.driver;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

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

public class DriverActivity extends AppCompatActivity {

    private EditText editPlate, editRoute, editSeats;
    private Button btnStartTrip, btnEndTrip, btnPlus, btnMinus;
    private View setupCard, tripCard, requestsCard, currentRoutesCard, mapCard;
    private TextView txtTripId, txtPlateDisplay, txtRouteDisplay, txtSeatsDisplay;
    private ListView currentRoutesList, requestsListView;
    private MapView driverMap;

    private String currentTripId;
    private int availableSeats = 14;
    private List<Trip> driverTrips;
    private List<TripRequest> pendingRequests;
    private ArrayAdapter<String> routesAdapter, requestsAdapter;

    private static final String DRIVER_ID = "driver_001";
    private LocationManager locationManager;
    private LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_driver);

        editPlate = findViewById(R.id.editPlateNumber);
        editRoute = findViewById(R.id.editRoute);
        editSeats = findViewById(R.id.editSeats);
        btnStartTrip = findViewById(R.id.btnStartTrip);
        btnEndTrip = findViewById(R.id.btnEndTrip);
        btnPlus = findViewById(R.id.btnPlusSeat);
        btnMinus = findViewById(R.id.btnMinusSeat);
        setupCard = findViewById(R.id.setupCard);
        tripCard = findViewById(R.id.tripCard);
        requestsCard = findViewById(R.id.requestsCard);
        currentRoutesCard = findViewById(R.id.currentRoutesCard);
        mapCard = findViewById(R.id.mapCard);
        currentRoutesList = findViewById(R.id.currentRoutesList);
        requestsListView = findViewById(R.id.requestsListView);
        driverMap = findViewById(R.id.driverMap);
        txtTripId = findViewById(R.id.txtTripId);
        txtPlateDisplay = findViewById(R.id.txtPlateDisplay);
        txtRouteDisplay = findViewById(R.id.txtRouteDisplay);
        txtSeatsDisplay = findViewById(R.id.txtSeatsDisplay);

        driverMap.setTileSource(TileSourceFactory.MAPNIK);
        driverMap.setMultiTouchControls(true);
        driverMap.getController().setZoom(13.0);
        driverMap.getController().setCenter(new GeoPoint(-1.286389, 36.817223));

        btnStartTrip.setOnClickListener(v -> startTrip());
        btnEndTrip.setOnClickListener(v -> endTrip());
        btnPlus.setOnClickListener(v -> changeSeats(1));
        btnMinus.setOnClickListener(v -> changeSeats(-1));

        SocketManager.establishConnection();
        listenForRequests();
        listenForVehicleLocation();

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override public void onLocationChanged(Location loc) {
                sendLocationUpdate(loc.getLatitude(), loc.getLongitude());
                updateDriverMapLocation(loc.getLatitude(), loc.getLongitude());
            }
            @Override public void onStatusChanged(String p, int i, Bundle b) {}
            @Override public void onProviderEnabled(String p) {}
            @Override public void onProviderDisabled(String p) {}
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        driverMap.onResume();
        loadDriverTrips();
    }

    @Override
    protected void onPause() {
        super.onPause();
        driverMap.onPause();
    }

    private void loadDriverTrips() {
        RetrofitClient.getApiService().searchTrips(null, DRIVER_ID).enqueue(new Callback<List<Trip>>() {
            @Override
            public void onResponse(Call<List<Trip>> call, Response<List<Trip>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    driverTrips = response.body();
                    showCurrentRoutes();
                }
            }

            @Override
            public void onFailure(Call<List<Trip>> call, Throwable t) {}
        });
    }

    private void showCurrentRoutes() {
        if (driverTrips == null || driverTrips.isEmpty()) {
            currentRoutesCard.setVisibility(View.GONE);
            return;
        }
        currentRoutesCard.setVisibility(View.VISIBLE);

        List<String> items = new ArrayList<>();
        for (Trip t : driverTrips) {
            items.add(t.getNumberPlate() + " - " + t.getRoute() + " (" + t.getAvailableSeats() + " seats)");
        }

        routesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
        currentRoutesList.setAdapter(routesAdapter);

        currentRoutesList.setOnItemClickListener((parent, view, position, id) -> {
            Trip trip = driverTrips.get(position);
            resumeTrip(trip);
        });
    }

    private void resumeTrip(Trip trip) {
        currentTripId = trip.getId();
        availableSeats = trip.getAvailableSeats();
        showTripActive(trip.getNumberPlate(), trip.getRoute());
        joinTripRoom();
        loadTripRequests();
        Toast.makeText(this, "Resumed trip: " + trip.getNumberPlate(), Toast.LENGTH_SHORT).show();
    }

    private void startTrip() {
        String plate = editPlate.getText().toString().trim();
        String route = editRoute.getText().toString().trim();
        String seatsStr = editSeats.getText().toString().trim();

        if (plate.isEmpty() || route.isEmpty()) {
            Toast.makeText(this, "Fill in plate and route", Toast.LENGTH_SHORT).show();
            return;
        }

        availableSeats = seatsStr.isEmpty() ? 14 : Integer.parseInt(seatsStr);

        Trip trip = new Trip(plate, route, availableSeats, DRIVER_ID);
        RetrofitClient.getApiService().createTrip(trip).enqueue(new Callback<Trip>() {
            @Override
            public void onResponse(Call<Trip> call, Response<Trip> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentTripId = response.body().getId();
                    showTripActive(plate, route);
                    joinTripRoom();
                    loadDriverTrips();
                    Toast.makeText(DriverActivity.this, "Trip started!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(DriverActivity.this, "Failed to start trip", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Trip> call, Throwable t) {
                Toast.makeText(DriverActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showTripActive(String plate, String route) {
        setupCard.setVisibility(View.GONE);
        tripCard.setVisibility(View.VISIBLE);
        requestsCard.setVisibility(View.VISIBLE);
        mapCard.setVisibility(View.VISIBLE);
        txtTripId.setText("Trip ID: " + currentTripId);
        txtPlateDisplay.setText("Plate: " + plate);
        txtRouteDisplay.setText("Route: " + route);
        updateSeatDisplay();
        loadTripRequests();
        startLocationUpdates();
    }

    private void joinTripRoom() {
        if (SocketManager.getSocket() != null) {
            try {
                JSONObject data = new JSONObject();
                data.put("tripId", currentTripId);
                SocketManager.getSocket().emit("driver-join", data);
            } catch (JSONException e) { e.printStackTrace(); }
        }
    }

    private void changeSeats(int delta) {
        availableSeats = Math.max(0, Math.min(60, availableSeats + delta));
        updateSeatDisplay();
        updateSeatsOnBackend();
    }

    private void updateSeatDisplay() {
        txtSeatsDisplay.setText("Available Seats: " + availableSeats);
    }

    private void updateSeatsOnBackend() {
        if (currentTripId == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("availableSeats", availableSeats);
        RetrofitClient.getApiService().updateTrip(currentTripId, updates).enqueue(new Callback<Trip>() {
            @Override
            public void onResponse(Call<Trip> call, Response<Trip> response) {}
            @Override
            public void onFailure(Call<Trip> call, Throwable t) {}
        });
    }

    private void endTrip() {
        if (currentTripId == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "COMPLETED");
        updates.put("availableSeats", availableSeats);
        RetrofitClient.getApiService().updateTrip(currentTripId, updates).enqueue(new Callback<Trip>() {
            @Override
            public void onResponse(Call<Trip> call, Response<Trip> response) {
                currentTripId = null;
                tripCard.setVisibility(View.GONE);
                requestsCard.setVisibility(View.GONE);
                mapCard.setVisibility(View.GONE);
                setupCard.setVisibility(View.VISIBLE);
                editPlate.setText("");
                editRoute.setText("");
                loadDriverTrips();
                stopLocationUpdates();
                Toast.makeText(DriverActivity.this, "Trip ended", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<Trip> call, Throwable t) {
                Toast.makeText(DriverActivity.this, "Error ending trip", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTripRequests() {
        if (currentTripId == null) return;
        RetrofitClient.getApiService().getTripRequests(currentTripId).enqueue(new Callback<List<TripRequest>>() {
            @Override
            public void onResponse(Call<List<TripRequest>> call, Response<List<TripRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    pendingRequests = response.body();
                    updateRequestsList();
                    for (TripRequest req : pendingRequests) {
                        if (req.getPassengerLat() != 0 || req.getPassengerLng() != 0) {
                            addPassengerMarker(req.getPassengerLat(), req.getPassengerLng(), req.getPickupPoint());
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<List<TripRequest>> call, Throwable t) {}
        });
    }

    private void updateRequestsList() {
        if (pendingRequests == null || pendingRequests.isEmpty()) {
            List<String> empty = new ArrayList<>();
            empty.add("Waiting for requests...");
            requestsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, empty);
            requestsListView.setAdapter(requestsAdapter);
            return;
        }

        List<String> items = new ArrayList<>();
        for (TripRequest req : pendingRequests) {
            items.add(req.getPassengerId() + " at " + req.getPickupPoint() + " (" + req.getStatus() + ")");
        }
        requestsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
        requestsListView.setAdapter(requestsAdapter);
    }

    private void listenForRequests() {
        if (SocketManager.getSocket() != null) {
            SocketManager.getSocket().on("reservation-update", args -> {
                if (args.length > 0) {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        String tripId = data.optString("tripId");
                        if (tripId.equals(currentTripId)) {
                            double pLat = data.optDouble("passengerLat", -1.286389);
                            double pLng = data.optDouble("passengerLng", 36.817223);
                            String pickup = data.optString("pickupPoint");
                            runOnUiThread(() -> {
                                loadTripRequests();
                                addPassengerMarker(pLat, pLng, pickup);
                                Toast.makeText(this, "New request at " + pickup, Toast.LENGTH_LONG).show();
                            });
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
            });
        }
    }

    private void listenForVehicleLocation() {
        if (SocketManager.getSocket() != null) {
            SocketManager.getSocket().on("vehicle-location", args -> {
                if (args.length > 0) {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        String tripId = data.optString("tripId");
                        if (tripId.equals(currentTripId)) {
                            double lat = data.getDouble("latitude");
                            double lng = data.getDouble("longitude");
                            runOnUiThread(() -> updateDriverMapLocation(lat, lng));
                        }
                    } catch (JSONException e) { e.printStackTrace(); }
                }
            });
        }
    }

    private void updateDriverMapLocation(double lat, double lng) {
        if (driverMap == null) return;
        GeoPoint pos = new GeoPoint(lat, lng);
        driverMap.getController().setCenter(pos);
        driverMap.getOverlays().clear();
        Marker marker = new Marker(driverMap);
        marker.setPosition(pos);
        marker.setTitle("My Location");
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        driverMap.getOverlays().add(marker);
        driverMap.invalidate();
    }

    private void addPassengerMarker(double lat, double lng, String pickup) {
        if (driverMap == null) return;
        GeoPoint pos = new GeoPoint(lat, lng);
        Marker marker = new Marker(driverMap);
        marker.setPosition(pos);
        marker.setTitle("Passenger at " + pickup);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(getDrawable(android.R.drawable.ic_menu_mylocation));
        driverMap.getOverlays().add(marker);
        driverMap.invalidate();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
        }
    }

    private void stopLocationUpdates() {
        locationManager.removeUpdates(locationListener);
    }

    private void sendLocationUpdate(double lat, double lng) {
        if (currentTripId == null || SocketManager.getSocket() == null) return;
        try {
            JSONObject payload = new JSONObject();
            payload.put("tripId", currentTripId);
            payload.put("latitude", lat);
            payload.put("longitude", lng);
            SocketManager.getSocket().emit("location-update", payload);
        } catch (JSONException e) { e.printStackTrace(); }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        SocketManager.releaseConnection();
    }
}
