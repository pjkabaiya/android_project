package matatu_system.A1.passenger;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import matatu_system.A1.R;
import matatu_system.A1.api.RetrofitClient;
import matatu_system.A1.models.Reservation;
import matatu_system.A1.utils.SocketManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PassengerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Map<String, Marker> vehicleMarkers = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        SocketManager.establishConnection();
        listenForLocationUpdates();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(marker -> {
            String vehicleId = (String) marker.getTag();
            if (vehicleId != null) {
                showReservationDialog(vehicleId);
            }
            return false;
        });

        LatLng nairobi = new LatLng(-1.286389, 36.817223);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nairobi, 12f));
    }

    private void showReservationDialog(String vehicleId) {
        new AlertDialog.Builder(this)
                .setTitle("Reserve Seat")
                .setMessage("Do you want to request a seat in matatu " + vehicleId + "?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    String passengerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    Reservation reservation = new Reservation(passengerId, vehicleId, "Point A", "Point B");
                    RetrofitClient.getApiService().requestReservation(reservation).enqueue(new Callback<Reservation>() {
                        @Override
                        public void onResponse(Call<Reservation> call, Response<Reservation> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(PassengerActivity.this, "Reservation request sent!", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Reservation> call, Throwable t) {
                            Toast.makeText(PassengerActivity.this, "Request failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void listenForLocationUpdates() {
        if (SocketManager.getSocket() != null) {
            // Updated to match backend "vehicle-location" broadcast event
            SocketManager.getSocket().on("vehicle-location", args -> {
                if (args.length > 0) {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        String vehicleId = data.getString("vehicleId");
                        double lat = data.getDouble("latitude");
                        double lng = data.getDouble("longitude");

                        runOnUiThread(() -> updateVehicleMarker(vehicleId, lat, lng));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void updateVehicleMarker(String vehicleId, double lat, double lng) {
        LatLng pos = new LatLng(lat, lng);
        if (vehicleMarkers.containsKey(vehicleId)) {
            vehicleMarkers.get(vehicleId).setPosition(pos);
        } else {
            Marker marker = mMap.addMarker(new MarkerOptions().position(pos).title("Matatu " + vehicleId));
            if (marker != null) {
                marker.setTag(vehicleId);
                vehicleMarkers.put(vehicleId, marker);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SocketManager.closeConnection();
    }
}
