package matatu_system.A1.passenger;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import matatu_system.A1.R;
import matatu_system.A1.api.RetrofitClient;
import matatu_system.A1.models.Reservation;
import matatu_system.A1.utils.SocketManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PassengerActivity extends AppCompatActivity {

    private MapView mapView;
    private Map<String, Marker> vehicleMarkers = new HashMap<>();
    private Map<String, Polyline> vehicleRoutes = new HashMap<>();
    private CardView vehicleDetailCard;
    private TextView txtPlate, txtRoute, txtSeats;
    private String selectedVehicleId;
    private BottomSheetBehavior bottomSheetBehavior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_passenger);

        vehicleDetailCard = findViewById(R.id.vehicleDetailCard);
        txtPlate = findViewById(R.id.txtVehiclePlate);
        txtRoute = findViewById(R.id.txtRouteInfo);
        txtSeats = findViewById(R.id.txtSeatsAvailable);
        Button btnReserve = findViewById(R.id.btnReserveSeat);

        bottomSheetBehavior = BottomSheetBehavior.from(vehicleDetailCard);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        mapView = findViewById(R.id.map);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(13.0);
        mapView.getController().setCenter(new GeoPoint(-1.286389, 36.817223));

        btnReserve.setOnClickListener(v -> requestReservation());

        findViewById(R.id.btnSwitchToDriver).setOnClickListener(v -> {
            startActivity(new Intent(this, matatu_system.A1.driver.DriverActivity.class));
            finish();
        });

        SocketManager.establishConnection();
        listenForRealTimeUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SocketManager.closeConnection();
    }

    private void showVehicleDetails(String vehicleId) {
        selectedVehicleId = vehicleId;
        txtPlate.setText("Matatu: " + vehicleId);
        txtRoute.setText("Route: " + getIntent().getStringExtra("PICKUP_POINT") + " to " + getIntent().getStringExtra("DESTINATION_POINT"));

        vehicleDetailCard.setVisibility(View.VISIBLE);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void requestReservation() {
        if (selectedVehicleId == null) return;

        String passengerId = "test_passenger_" + (int)(Math.random() * 1000);
        String pickup = getIntent().getStringExtra("PICKUP_POINT");
        String dest = getIntent().getStringExtra("DESTINATION_POINT");

        if (SocketManager.getSocket() != null && SocketManager.getSocket().connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("passengerId", passengerId);
                data.put("vehicleId", selectedVehicleId);
                data.put("pickupPoint", pickup);
                data.put("destination", dest);
                SocketManager.getSocket().emit("reservation-update", data);
            } catch (JSONException e) { e.printStackTrace(); }
        }

        Reservation res = new Reservation(passengerId, selectedVehicleId, pickup, dest);
        RetrofitClient.getApiService().requestReservation(res).enqueue(new Callback<Reservation>() {
            @Override
            public void onResponse(Call<Reservation> call, Response<Reservation> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(PassengerActivity.this, "Booking request sent to Driver!", Toast.LENGTH_LONG).show();
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }
            }
            @Override
            public void onFailure(Call<Reservation> call, Throwable t) {
                Toast.makeText(PassengerActivity.this, "Booking update sent live.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void listenForRealTimeUpdates() {
        if (SocketManager.getSocket() != null) {
            SocketManager.getSocket().on("vehicle-location", args -> {
                if (args.length > 0) {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        String vId = data.getString("vehicleId");

                        if (data.has("type") && "route_broadcast".equals(data.getString("type"))) {
                            JSONArray pathArray = data.optJSONArray("routePath");
                            if (pathArray != null) {
                                runOnUiThread(() -> updateVehicleRoute(vId, pathArray));
                            }
                        } else {
                            double lat = data.getDouble("latitude");
                            double lng = data.getDouble("longitude");
                            runOnUiThread(() -> updateVehicleMarker(vId, lat, lng));
                        }
                    } catch (JSONException e) { e.printStackTrace(); }
                }
            });

            SocketManager.getSocket().on("reservation-update", args -> {
                if (args.length > 0) {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        if (data.has("type") && "seat_update".equals(data.getString("type"))) {
                            String vId = data.getString("vehicleId");
                            int available = data.getInt("availableSeats");
                            if (vId.equals(selectedVehicleId)) {
                                runOnUiThread(() -> txtSeats.setText("Seats Available: " + available));
                            }
                        }
                    } catch (JSONException e) { e.printStackTrace(); }
                }
            });
        }
    }

    private void updateVehicleRoute(String vId, JSONArray pathArray) {
        if (vehicleRoutes.containsKey(vId)) {
            mapView.getOverlayManager().remove(vehicleRoutes.get(vId));
        }
        ArrayList<GeoPoint> points = new ArrayList<>();
        for (int i = 0; i < pathArray.length(); i++) {
            JSONArray coord = pathArray.optJSONArray(i);
            if (coord != null && coord.length() >= 2) {
                points.add(new GeoPoint(coord.optDouble(0), coord.optDouble(1)));
            }
        }
        if (!points.isEmpty()) {
            Polyline polyline = new Polyline();
            polyline.setPoints(points);
            polyline.setColor(android.graphics.Color.parseColor("#1565C0"));
            polyline.setWidth(4f);
            mapView.getOverlayManager().add(polyline);
            vehicleRoutes.put(vId, polyline);
            mapView.invalidate();
        }
    }

    private void updateVehicleMarker(String vId, double lat, double lng) {
        GeoPoint pos = new GeoPoint(lat, lng);
        if (vehicleMarkers.containsKey(vId)) {
            vehicleMarkers.get(vId).setPosition(pos);
            mapView.invalidate();
        } else {
            Marker marker = new Marker(mapView);
            marker.setPosition(pos);
            marker.setTitle("Matatu " + vId);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setRelatedObject(vId);
            marker.setOnMarkerClickListener((m, mv) -> {
                String vid = (String) m.getRelatedObject();
                if (vid != null) showVehicleDetails(vid);
                return true;
            });
            mapView.getOverlays().add(marker);
            vehicleMarkers.put(vId, marker);
            mapView.invalidate();
        }
    }
}
