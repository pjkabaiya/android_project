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

    private EditText editFrom, editTo;
    private Button btnSearch;
    private ListView vehicleList, activeRequestsList;
    private TextView txtResultsHeader, txtNoResults;
    private View activeRequestsCard;

    private List<Trip> activeTrips;

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
        btnSearch = findViewById(R.id.btnSearch);
        vehicleList = findViewById(R.id.vehicleList);
        txtResultsHeader = findViewById(R.id.txtResultsHeader);
        txtNoResults = findViewById(R.id.txtNoResults);
        activeRequestsCard = findViewById(R.id.activeRequestsCard);
        activeRequestsList = findViewById(R.id.activeRequestsList);

        btnSearch.setOnClickListener(v -> searchTrips());

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
                    if (activeTrips.size() == 1) {
                        goToMap(activeTrips.get(0));
                    } else {
                        showVehicleList();
                    }
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
                    if (activeTrips.size() == 1) {
                        goToMap(activeTrips.get(0));
                    } else {
                        showVehicleList();
                    }
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

    private void goToMap(Trip trip) {
        Intent intent = new Intent(PassengerDashboardActivity.this, MapViewActivity.class);
        intent.putExtra("tripId", trip.getId());
        intent.putExtra("isDriver", false);
        intent.putExtra("numberPlate", trip.getNumberPlate());
        intent.putExtra("route", trip.getRoute());
        intent.putExtra("selectPickupDirect", true);
        startActivity(intent);
    }

    private void showVehicleList() {
        if (activeTrips == null || activeTrips.isEmpty()) {
            showNoResults();
            return;
        }

        txtResultsHeader.setVisibility(View.VISIBLE);
        vehicleList.setVisibility(View.VISIBLE);
        txtNoResults.setVisibility(View.GONE);

        String[] items = new String[activeTrips.size()];
        for (int i = 0; i < activeTrips.size(); i++) {
            Trip trip = activeTrips.get(i);
            items[i] = trip.getNumberPlate() + "  |  " + trip.getRoute() + "  |  Seats: " + trip.getAvailableSeats();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_simple_text, items);
        vehicleList.setAdapter(adapter);

        vehicleList.setOnItemClickListener((parent, view, position, id) -> {
            Trip trip = activeTrips.get(position);
            Intent intent = new Intent(PassengerDashboardActivity.this, MapViewActivity.class);
            intent.putExtra("tripId", trip.getId());
            intent.putExtra("isDriver", false);
            intent.putExtra("numberPlate", trip.getNumberPlate());
            intent.putExtra("route", trip.getRoute());
            intent.putExtra("selectPickupDirect", true);
            startActivity(intent);
        });
    }

    private void showNoResults() {
        txtResultsHeader.setVisibility(View.GONE);
        vehicleList.setVisibility(View.GONE);
        txtNoResults.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SocketManager.releaseConnection();
    }
}
