package matatu_system.A1.map;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
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

import java.util.List;

import matatu_system.A1.R;
import matatu_system.A1.api.RetrofitClient;
import matatu_system.A1.models.TripRequest;
import matatu_system.A1.utils.SocketManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapViewActivity extends AppCompatActivity {

    private MapView map;
    private TextView txtInfo;
    private Marker vehicleMarker;
    private String tripId;
    private boolean isDriver;
    private LocationManager locationManager;

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location loc) {
            sendLocationUpdate(loc.getLatitude(), loc.getLongitude());
            updateVehicleMarker(loc.getLatitude(), loc.getLongitude());
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

        map = findViewById(R.id.mapView);
        txtInfo = findViewById(R.id.txtMapInfo);

        String label = (isDriver ? "Driver" : "Passenger");
        txtInfo.setText(label + " - " + (plate != null ? plate : "") + " " + (route != null ? route : ""));

        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);
        map.getController().setCenter(new GeoPoint(-1.286389, 36.817223));

        SocketManager.establishConnection();
        joinTripRoom();
        listenForUpdates();

        if (isDriver) {
            startGps();
            loadPassengerMarkers();
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
                    String tid = data.optString("tripId");
                    if (tid.equals(tripId)) {
                        double lat = data.getDouble("latitude");
                        double lng = data.getDouble("longitude");
                        runOnUiThread(() -> updateVehicleMarker(lat, lng));
                    }
                } catch (JSONException e) { e.printStackTrace(); }
            }
        });

        if (isDriver) {
            SocketManager.getSocket().on("reservation-update", args -> {
                if (args.length > 0) {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        if (data.optString("tripId").equals(tripId)) {
                            double pLat = data.optDouble("passengerLat", -1.286389);
                            double pLng = data.optDouble("passengerLng", 36.817223);
                            runOnUiThread(() -> {
                                addPassengerMarker(pLat, pLng, data.optString("pickupPoint"));
                                loadPassengerMarkers();
                            });
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
            });
        }
    }

    private void startGps() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
        } else {
            Toast.makeText(this, "Location permission needed for GPS", Toast.LENGTH_LONG).show();
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

    private void loadPassengerMarkers() {
        if (tripId == null) return;
        RetrofitClient.getApiService().getTripRequests(tripId).enqueue(new Callback<List<TripRequest>>() {
            @Override
            public void onResponse(Call<List<TripRequest>> call, Response<List<TripRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (TripRequest req : response.body()) {
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

    private void updateVehicleMarker(double lat, double lng) {
        if (map == null) return;
        GeoPoint pos = new GeoPoint(lat, lng);
        map.getController().setCenter(pos);
        if (vehicleMarker == null) {
            vehicleMarker = new Marker(map);
            vehicleMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            vehicleMarker.setTitle(isDriver ? "My Location" : "Matatu");
            map.getOverlays().add(vehicleMarker);
        }
        vehicleMarker.setPosition(pos);
        map.invalidate();
    }

    private void addPassengerMarker(double lat, double lng, String pickup) {
        if (map == null) return;
        GeoPoint pos = new GeoPoint(lat, lng);
        Marker marker = new Marker(map);
        marker.setPosition(pos);
        marker.setTitle("Passenger: " + pickup);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(getDrawable(android.R.drawable.ic_menu_mylocation));
        map.getOverlays().add(marker);
        map.invalidate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null) locationManager.removeUpdates(locationListener);
        SocketManager.releaseConnection();
    }
}
