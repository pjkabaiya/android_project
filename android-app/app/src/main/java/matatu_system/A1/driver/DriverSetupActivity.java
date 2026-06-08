package matatu_system.A1.driver;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import matatu_system.A1.R;

public class DriverSetupActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private EditText editPlate;
    private TextView txtStatus;
    private Button btnStart;
    private LatLng startPoint, endPoint;
    private Marker startMarker, endMarker;
    private Polyline routeLine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_setup);

        editPlate = findViewById(R.id.editPlateNumber);
        txtStatus = findViewById(R.id.txtRouteStatus);
        btnStart = findViewById(R.id.btnStartTrip);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.setupMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnStart.setOnClickListener(v -> {
            String plate = editPlate.getText().toString().trim();
            if (plate.isEmpty()) {
                Toast.makeText(this, "Enter Plate Number", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, DriverActivity.class);
            intent.putExtra("PLATE", plate);
            intent.putExtra("START_LAT", startPoint.latitude);
            intent.putExtra("START_LNG", startPoint.longitude);
            intent.putExtra("END_LAT", endPoint.latitude);
            intent.putExtra("END_LNG", endPoint.longitude);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        LatLng nairobi = new LatLng(-1.286389, 36.817223);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nairobi, 13f));

        mMap.setOnMapClickListener(latLng -> {
            if (startPoint == null) {
                startPoint = latLng;
                startMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Start Point"));
                txtStatus.setText("Start Set. Now tap for Destination.");
            } else if (endPoint == null) {
                endPoint = latLng;
                endMarker = mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title("Destination")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                
                // Draw route line
                routeLine = mMap.addPolyline(new PolylineOptions()
                        .add(startPoint, endPoint)
                        .width(10)
                        .color(android.graphics.Color.BLUE));
                
                txtStatus.setText("Route Set! Ready to go.");
                btnStart.setEnabled(true);
            } else {
                // Reset
                startPoint = null;
                endPoint = null;
                if (startMarker != null) startMarker.remove();
                if (endMarker != null) endMarker.remove();
                if (routeLine != null) routeLine.remove();
                btnStart.setEnabled(false);
                txtStatus.setText("Reset. Tap for Start Point.");
            }
        });
    }
}
