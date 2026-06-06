package matatu_system.A1.driver;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.json.JSONException;
import org.json.JSONObject;

import matatu_system.A1.R;
import matatu_system.A1.utils.SocketManager;

public class DriverActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean isTracking = false;
    private Button startTripButton, plusSeatButton, minusSeatButton;
    private TextView seatStatusText;
    private int occupiedSeats = 0;
    private final int MAX_SEATS = 14;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        startTripButton = findViewById(R.id.startTripButton);
        plusSeatButton = findViewById(R.id.plusSeatButton);
        minusSeatButton = findViewById(R.id.minusSeatButton);
        seatStatusText = findViewById(R.id.seatStatusText);

        startTripButton.setOnClickListener(v -> {
            if (isTracking) {
                stopTracking();
            } else {
                startTracking();
            }
        });

        plusSeatButton.setOnClickListener(v -> {
            if (occupiedSeats < MAX_SEATS) {
                occupiedSeats++;
                updateSeatUI();
                sendSeatUpdate();
            }
        });

        minusSeatButton.setOnClickListener(v -> {
            if (occupiedSeats > 0) {
                occupiedSeats--;
                updateSeatUI();
                sendSeatUpdate();
            }
        });

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    updateLocationOnServer(location);
                }
            }
        };

        updateSeatUI();
    }

    private void updateSeatUI() {
        seatStatusText.setText("Occupied Seats: " + occupiedSeats + " / " + MAX_SEATS);
    }

    private void sendSeatUpdate() {
        // Updated to match backend "reservation-update" event
        if (SocketManager.getSocket() != null && SocketManager.getSocket().connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("occupiedSeats", occupiedSeats);
                data.put("availableSeats", MAX_SEATS - occupiedSeats);
                data.put("vehicleId", "test_vehicle_id");
                data.put("type", "seat_update"); 
                SocketManager.getSocket().emit("reservation-update", data);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void startTracking() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        isTracking = true;
        startTripButton.setText("End Trip");
        SocketManager.establishConnection();
        Toast.makeText(this, "Trip started. Tracking enabled.", Toast.LENGTH_SHORT).show();
    }

    private void stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
        isTracking = false;
        startTripButton.setText("Start Trip");
        SocketManager.closeConnection();
        Toast.makeText(this, "Trip ended.", Toast.LENGTH_SHORT).show();
    }

    private void updateLocationOnServer(Location location) {
        // Updated to match backend "location-update" event
        if (SocketManager.getSocket() != null && SocketManager.getSocket().connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("latitude", location.getLatitude());
                data.put("longitude", location.getLongitude());
                data.put("vehicleId", "test_vehicle_id");
                data.put("timestamp", System.currentTimeMillis());
                SocketManager.getSocket().emit("location-update", data);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startTracking();
        }
    }
}
