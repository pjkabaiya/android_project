package matatu_system.A1.passenger;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import matatu_system.A1.R;
import matatu_system.A1.api.RetrofitClient;
import matatu_system.A1.map.MapViewActivity;
import matatu_system.A1.models.Trip;
import matatu_system.A1.models.TripRequest;
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
    private List<TripRequest> myRequests = new ArrayList<>();
    private Map<String, Trip> requestTripMap = new HashMap<>();
    private RequestAdapter requestAdapter;

    private String getPassengerId() {
        SharedPreferences prefs = getSharedPreferences("matatu_prefs", MODE_PRIVATE);
        String id = prefs.getString("passengerId", null);
        if (id == null) {
            id = "passenger_" + System.currentTimeMillis();
            prefs.edit().putString("passengerId", id).apply();
        }
        return id;
    }
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

        requestAdapter = new RequestAdapter();
        activeRequestsList.setAdapter(requestAdapter);

        btnSearch.setOnClickListener(v -> searchTrips());

        activeRequestsList.setOnItemClickListener((parent, view, position, id) -> {
            TripRequest req = myRequests.get(position);
            Intent intent = new Intent(PassengerDashboardActivity.this, MapViewActivity.class);
            intent.putExtra("tripId", req.getTripId());
            intent.putExtra("isDriver", false);
            intent.putExtra("numberPlate", "");
            intent.putExtra("route", "");
            startActivity(intent);
        });

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
        RetrofitClient.getApiService().getPassengerRequestsWithProcessed(getPassengerId(), true).enqueue(new Callback<List<TripRequest>>() {
            @Override
            public void onResponse(Call<List<TripRequest>> call, Response<List<TripRequest>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    myRequests = response.body();
                    requestTripMap.clear();
                    for (TripRequest req : myRequests) {
                        loadTripForRequest(req);
                    }
                } else {
                    myRequests = new ArrayList<>();
                    activeRequestsCard.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<List<TripRequest>> call, Throwable t) {
                activeRequestsCard.setVisibility(View.GONE);
            }
        });
    }

    private void loadTripForRequest(TripRequest req) {
        RetrofitClient.getApiService().getTrip(req.getTripId()).enqueue(new Callback<Trip>() {
            @Override
            public void onResponse(Call<Trip> call, Response<Trip> response) {
                if (response.isSuccessful() && response.body() != null) {
                    requestTripMap.put(req.getId(), response.body());
                    requestAdapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(Call<Trip> call, Throwable t) {}
        });
    }

    private void cancelRequest(TripRequest req, int position) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "CANCELLED");
        RetrofitClient.getApiService().updateRequestStatus(req.getId(), updates).enqueue(new Callback<TripRequest>() {
            @Override
            public void onResponse(Call<TripRequest> call, Response<TripRequest> response) {
                if (response.isSuccessful()) {
                    myRequests.remove(position);
                    requestAdapter.notifyDataSetChanged();
                    if (myRequests.isEmpty()) activeRequestsCard.setVisibility(View.GONE);
                    Toast.makeText(PassengerDashboardActivity.this, "Request cancelled", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<TripRequest> call, Throwable t) {
                Toast.makeText(PassengerDashboardActivity.this, "Failed to cancel", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class RequestAdapter extends BaseAdapter {
        @Override
        public int getCount() { return myRequests.size(); }

        @Override
        public TripRequest getItem(int position) { return myRequests.get(position); }

        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_request, parent, false);
            }

            TripRequest req = myRequests.get(position);
            Trip trip = requestTripMap.get(req.getId());

            TextView tvTitle = convertView.findViewById(R.id.tvTitle);
            TextView tvSubtitle = convertView.findViewById(R.id.tvSubtitle);
            TextView tvStatus = convertView.findViewById(R.id.tvStatus);
            ImageButton btnDelete = convertView.findViewById(R.id.btnDeleteRequest);

            if (trip != null) {
                tvTitle.setText(trip.getNumberPlate() + "  |  " + trip.getRoute());
            } else {
                tvTitle.setText("Loading trip info...");
            }

            tvSubtitle.setText("Pickup: " + (req.getPickupPoint() != null ? req.getPickupPoint() : "set on map"));

            String status = req.getStatus() != null ? req.getStatus() : "WAITING";
            tvStatus.setText(status);

            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(24);
            switch (status) {
                case "WAITING":
                    bg.setColor(0xFFFFC107);
                    break;
                case "ACCEPTED":
                    bg.setColor(0xFF43A047);
                    break;
                case "REJECTED":
                    bg.setColor(0xFFD32F2F);
                    break;
                default:
                    bg.setColor(0xFF757575);
            }
            tvStatus.setBackground(bg);

            btnDelete.setOnClickListener(v -> cancelRequest(req, position));

            return convertView;
        }
    }

    private void searchTrips() {
        String from = editFrom.getText().toString().trim();
        String to = editTo.getText().toString().trim();

        if (from.isEmpty() || to.isEmpty()) {
            Toast.makeText(this, "Enter both locations", Toast.LENGTH_SHORT).show();
            return;
        }

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
}
