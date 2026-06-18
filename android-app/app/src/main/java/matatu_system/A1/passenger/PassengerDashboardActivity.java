package matatu_system.A1.passenger;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
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

import java.util.ArrayList;
import java.util.List;

import matatu_system.A1.R;
import matatu_system.A1.api.RetrofitClient;
import matatu_system.A1.map.MapViewActivity;
import matatu_system.A1.models.Trip;
import matatu_system.A1.models.TripRequest;
import matatu_system.A1.utils.SocketManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PassengerDashboardActivity extends AppCompatActivity {

    private EditText editFrom, editTo, editPickupPoint;
    private Button btnSearch, btnRequest;
    private ListView vehicleList, activeRequestsList;
    private TextView txtResultsHeader, txtNoResults, txtSelectedVehicle;
    private View requestCard, activeRequestsCard;

    private List<Trip> activeTrips;
    private String selectedTripId;
    private String currentPlate;
    private String currentRoute;

    private static final String PASSENGER_ID = "passenger_" + System.currentTimeMillis();
    private LocationManager passengerLocationManager;
    private double currentLat = -1.286389;
    private double currentLng = 36.817223;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger_dashboard);

        editFrom = findViewById(R.id.editFrom);
        editTo = findViewById(R.id.editTo);
        editPickupPoint = findViewById(R.id.editPickupPoint);
        btnSearch = findViewById(R.id.btnSearch);
        btnRequest = findViewById(R.id.btnRequestRide);
        vehicleList = findViewById(R.id.vehicleList);
        txtResultsHeader = findViewById(R.id.txtResultsHeader);
        txtNoResults = findViewById(R.id.txtNoResults);
        txtSelectedVehicle = findViewById(R.id.txtSelectedVehicle);
        requestCard = findViewById(R.id.requestCard);
        activeRequestsCard = findViewById(R.id.activeRequestsCard);
        activeRequestsList = findViewById(R.id.activeRequestsList);

        btnSearch.setOnClickListener(v -> searchTrips());
        btnRequest.setOnClickListener(v -> requestRide());

        SocketManager.establishConnection();

        passengerLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location loc = passengerLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc != null) { currentLat = loc.getLatitude(); currentLng = loc.getLongitude(); }
            passengerLocationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, location -> {
                currentLat = location.getLatitude();
                currentLng = location.getLongitude();
            }, null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadActiveRequests();
    }

    private void loadActiveRequests() {
        RetrofitClient.getApiService().getPassengerRequests(PASSENGER_ID).enqueue(new Callback<List<TripRequest>>() {
            @Override
            public void onResponse(Call<List<TripRequest>> call, Response<List<TripRequest>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    showActiveRequests(response.body());
                } else {
                    activeRequestsCard.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<List<TripRequest>> call, Throwable t) {
                activeRequestsCard.setVisibility(View.GONE);
            }
        });
    }

    private void showActiveRequests(List<TripRequest> requests) {
        activeRequestsCard.setVisibility(View.VISIBLE);
        List<String> items = new ArrayList<>();
        for (TripRequest req : requests) {
            items.add("Pickup: " + req.getPickupPoint() + " (" + req.getStatus() + ")");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_simple_text, items);
        activeRequestsList.setAdapter(adapter);

        activeRequestsList.setOnItemClickListener((parent, view, position, id) -> {
            TripRequest req = requests.get(position);
            Intent intent = new Intent(PassengerDashboardActivity.this, MapViewActivity.class);
            intent.putExtra("tripId", req.getTripId());
            intent.putExtra("isDriver", false);
            intent.putExtra("numberPlate", ""); // We'd need to fetch this or pass it in TripRequest
            intent.putExtra("route", "");
            startActivity(intent);
        });
    }

    private void searchTrips() {
        String from = editFrom.getText().toString().trim();
        String to = editTo.getText().toString().trim();

        if (from.isEmpty() || to.isEmpty()) {
            Toast.makeText(this, "Enter both locations", Toast.LENGTH_SHORT).show();
            return;
        }

        // Try searching for the combined route first
        String route = from + " " + to;
        RetrofitClient.getApiService().searchTrips(route, null).enqueue(new Callback<List<Trip>>() {
            @Override
            public void onResponse(Call<List<Trip>> call, Response<List<Trip>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    activeTrips = response.body();
                    showVehicleList();
                } else {
                    // If combined search fails, try searching just the "to" destination
                    searchByDestination(to);
                }
            }

            @Override
            public void onFailure(Call<List<Trip>> call, Throwable t) {
                searchByDestination(to);
            }
        });
    }

    private void searchByDestination(String to) {
        RetrofitClient.getApiService().searchTrips(to, null).enqueue(new Callback<List<Trip>>() {
            @Override
            public void onResponse(Call<List<Trip>> call, Response<List<Trip>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    activeTrips = response.body();
                    showVehicleList();
                } else {
                    showNoResults();
                }
            }
            @Override
            public void onFailure(Call<List<Trip>> call, Throwable t) {
                showNoResults();
            }
        });
    }

    private void showVehicleList() {
        txtResultsHeader.setVisibility(View.VISIBLE);
        vehicleList.setVisibility(View.VISIBLE);
        txtNoResults.setVisibility(View.GONE);
        requestCard.setVisibility(View.GONE);

        String[] items = new String[activeTrips.size()];
        for (int i = 0; i < activeTrips.size(); i++) {
            Trip t = activeTrips.get(i);
            items[i] = t.getNumberPlate() + "  |  " + t.getRoute() + "  |  Seats: " + t.getAvailableSeats();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_simple_text, items);
        vehicleList.setAdapter(adapter);

        vehicleList.setOnItemClickListener((parent, view, position, id) -> {
            Trip trip = activeTrips.get(position);
            selectedTripId = trip.getId();
            currentPlate = trip.getNumberPlate();
            currentRoute = trip.getRoute();
            txtSelectedVehicle.setText("Vehicle: " + trip.getNumberPlate() + " (" + trip.getRoute() + ")");
            requestCard.setVisibility(View.VISIBLE);
        });
    }

    private void showNoResults() {
        txtResultsHeader.setVisibility(View.GONE);
        vehicleList.setVisibility(View.GONE);
        txtNoResults.setVisibility(View.VISIBLE);
        requestCard.setVisibility(View.GONE);
        selectedTripId = null;
    }

    private void requestRide() {
        if (selectedTripId == null) {
            Toast.makeText(this, "Please select a vehicle first", Toast.LENGTH_SHORT).show();
            return;
        }
        String pickup = editPickupPoint.getText().toString().trim();
        if (pickup.isEmpty()) {
            Toast.makeText(this, "Enter your pickup location", Toast.LENGTH_SHORT).show();
            return;
        }

        TripRequest req = new TripRequest(selectedTripId, PASSENGER_ID, pickup);
        req.setPassengerLat(currentLat);
        req.setPassengerLng(currentLng);
        RetrofitClient.getApiService().createTripRequest(selectedTripId, req).enqueue(new Callback<TripRequest>() {
            @Override
            public void onResponse(Call<TripRequest> call, Response<TripRequest> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(PassengerDashboardActivity.this, "Ride requested!", Toast.LENGTH_LONG).show();
                    requestCard.setVisibility(View.GONE);
                    loadActiveRequests();
                    sendSocketRequest(pickup);
                } else {
                    Toast.makeText(PassengerDashboardActivity.this, "Request failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<TripRequest> call, Throwable t) {
                Toast.makeText(PassengerDashboardActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendSocketRequest(String pickup) {
        if (SocketManager.getSocket() != null && SocketManager.getSocket().connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("tripId", selectedTripId);
                data.put("pickupPoint", pickup);
                data.put("type", "passenger_request");
                data.put("passengerLat", currentLat);
                data.put("passengerLng", currentLng);
                SocketManager.getSocket().emit("reservation-update", data);
            } catch (JSONException e) { e.printStackTrace(); }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SocketManager.releaseConnection();
    }
}
