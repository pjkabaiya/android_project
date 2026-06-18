package matatu_system.A1.driver;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import matatu_system.A1.R;
import matatu_system.A1.api.RetrofitClient;
import matatu_system.A1.map.MapViewActivity;
import matatu_system.A1.models.Trip;
import matatu_system.A1.models.TripRequest;
import matatu_system.A1.utils.SocketManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverActivity extends AppCompatActivity {

    private EditText editPlate, editRoute, editSeats;
    private Button btnStartTrip, btnEndTrip, btnPlus, btnMinus;
    private View setupCard, tripCard, requestsCard, currentRoutesCard;
    private TextView txtTripId, txtPlateDisplay, txtRouteDisplay, txtSeatsDisplay;
    private ListView currentRoutesList, requestsListView;

    private String currentTripId;
    private String currentPlate;
    private String currentRoute;
    private int availableSeats = 14;
    private List<Trip> driverTrips;
    private List<TripRequest> pendingRequests = new ArrayList<>();
    private ArrayAdapter<String> routesAdapter, requestsAdapter;

    private static final String DRIVER_ID = "driver_001";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        currentRoutesList = findViewById(R.id.currentRoutesList);
        requestsListView = findViewById(R.id.requestsListView);
        txtTripId = findViewById(R.id.txtTripId);
        txtPlateDisplay = findViewById(R.id.txtPlateDisplay);
        txtRouteDisplay = findViewById(R.id.txtRouteDisplay);
        txtSeatsDisplay = findViewById(R.id.txtSeatsDisplay);

        btnStartTrip.setOnClickListener(v -> startTrip());
        btnEndTrip.setOnClickListener(v -> endTrip());
        btnPlus.setOnClickListener(v -> changeSeats(1));
        btnMinus.setOnClickListener(v -> changeSeats(-1));

        SocketManager.establishConnection();
        listenForRequests();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDriverTrips();
    }

    private void loadDriverTrips() {
        RetrofitClient.getApiService().searchTrips(null, DRIVER_ID).enqueue(new Callback<List<Trip>>() {
            @Override
            public void onResponse(Call<List<Trip>> call, Response<List<Trip>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    driverTrips = response.body();
                    showCurrentRoutes();
                } else {
                    Toast.makeText(DriverActivity.this, "Failed to load trips: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Trip>> call, Throwable t) {
                Toast.makeText(DriverActivity.this, "Error loading trips: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCurrentRoutes() {
        if (driverTrips == null || driverTrips.isEmpty()) {
            currentRoutesCard.setVisibility(View.GONE);
            return;
        }
        currentRoutesCard.setVisibility(View.VISIBLE);

        List<String> items = new ArrayList<>();
        for (Trip trip : driverTrips) {
            items.add(trip.getNumberPlate() + " - " + trip.getRoute() + " (" + trip.getAvailableSeats() + " seats)");
        }

        routesAdapter = new ArrayAdapter<>(this, R.layout.item_simple_text, items);
        currentRoutesList.setAdapter(routesAdapter);

        currentRoutesList.setOnItemClickListener((parent, view, position, id) -> {
            Trip trip = driverTrips.get(position);
            
            String[] options = {"Open Map (View Route)", "Trip Details (Passengers)"};
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Option")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Open Map
                        Intent intent = new Intent(DriverActivity.this, MapViewActivity.class);
                        intent.putExtra("tripId", trip.getId());
                        intent.putExtra("isDriver", true);
                        intent.putExtra("numberPlate", trip.getNumberPlate());
                        intent.putExtra("route", trip.getRoute());
                        startActivity(intent);
                    } else {
                        // Trip Details
                        showTripDetailsDialog(trip);
                    }
                })
                .show();
        });

        currentRoutesList.setOnItemLongClickListener((parent, view, position, id) -> {
            Trip trip = driverTrips.get(position);
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Trip")
                .setMessage("Are you sure you want to delete this trip (" + trip.getNumberPlate() + ")?")
                .setPositiveButton("Delete", (dialog, which) -> deleteTrip(trip.getId()))
                .setNegativeButton("Cancel", null)
                .show();
            return true;
        });
    }

    private void deleteTrip(String tripId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "CANCELLED");
        RetrofitClient.getApiService().updateTrip(tripId, updates).enqueue(new Callback<Trip>() {
            @Override
            public void onResponse(Call<Trip> call, Response<Trip> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(DriverActivity.this, "Trip deleted", Toast.LENGTH_SHORT).show();
                    loadDriverTrips();
                }
            }
            @Override
            public void onFailure(Call<Trip> call, Throwable t) {
                Toast.makeText(DriverActivity.this, "Failed to delete", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showTripDetailsDialog(Trip trip) {
        RetrofitClient.getApiService().getTripRequests(trip.getId()).enqueue(new Callback<List<TripRequest>>() {
            @Override
            public void onResponse(Call<List<TripRequest>> call, Response<List<TripRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<TripRequest> reqs = response.body();
                    StringBuilder sb = new StringBuilder();
                    sb.append("Status: ").append(trip.getStatus()).append("\n");
                    sb.append("Route: ").append(trip.getRoute()).append("\n");
                    sb.append("Plate: ").append(trip.getNumberPlate()).append("\n\n");
                    sb.append("Passengers:\n");
                    
                    if (reqs.isEmpty()) {
                        sb.append("- No passengers yet");
                    } else {
                        for (TripRequest req : reqs) {
                            sb.append("- ").append(req.getPickupPoint())
                              .append(" (").append(req.getStatus()).append(")\n");
                        }
                    }

                    new androidx.appcompat.app.AlertDialog.Builder(DriverActivity.this)
                        .setTitle("Trip Details")
                        .setMessage(sb.toString())
                        .setPositiveButton("Close", null)
                        .setNeutralButton("View on Map", (d, w) -> {
                            Intent intent = new Intent(DriverActivity.this, MapViewActivity.class);
                            intent.putExtra("tripId", trip.getId());
                            intent.putExtra("isDriver", true);
                            intent.putExtra("numberPlate", trip.getNumberPlate());
                            intent.putExtra("route", trip.getRoute());
                            startActivity(intent);
                        })
                        .show();
                } else {
                    Toast.makeText(DriverActivity.this, "Failed to load passenger details", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<List<TripRequest>> call, Throwable t) {
                Toast.makeText(DriverActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
                    String tripId = response.body().getId();

                    // Launch map to create the route visually
                    Intent intent = new Intent(DriverActivity.this, MapViewActivity.class);
                    intent.putExtra("tripId", tripId);
                    intent.putExtra("isDriver", true);
                    intent.putExtra("numberPlate", plate);
                    intent.putExtra("route", route);
                    intent.putExtra("availableSeats", availableSeats);
                    intent.putExtra("createRouteDirect", true);
                    startActivity(intent);

                    // Refresh trips list
                    loadDriverTrips();
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

    private void showTripActive() {
        setupCard.setVisibility(View.GONE);
        tripCard.setVisibility(View.VISIBLE);
        requestsCard.setVisibility(View.VISIBLE);
        txtTripId.setText("Trip ID: " + currentTripId);
        txtPlateDisplay.setText("Plate: " + currentPlate);
        txtRouteDisplay.setText("Route: " + currentRoute);
        updateSeatDisplay();
        loadTripRequests();
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
                setupCard.setVisibility(View.VISIBLE);
                editPlate.setText("");
                editRoute.setText("");
                loadDriverTrips();
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
                } else {
                    Toast.makeText(DriverActivity.this, "Failed to load requests", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<TripRequest>> call, Throwable t) {
                Toast.makeText(DriverActivity.this, "Error loading requests: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRequestsList() {
        if (pendingRequests == null || pendingRequests.isEmpty()) {
            List<String> empty = new ArrayList<>();
            empty.add("Waiting for requests...");
            requestsAdapter = new ArrayAdapter<>(this, R.layout.item_simple_text, empty);
            requestsListView.setAdapter(requestsAdapter);
            return;
        }

        List<String> items = new ArrayList<>();
        for (TripRequest req : pendingRequests) {
            items.add(req.getPassengerId() + " at " + req.getPickupPoint() + " (" + req.getStatus() + ")");
        }
        requestsAdapter = new ArrayAdapter<>(this, R.layout.item_simple_text, items);
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
                            runOnUiThread(() -> {
                                loadTripRequests();
                                Toast.makeText(this, "New request at " + data.optString("pickupPoint"), Toast.LENGTH_LONG).show();
                            });
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SocketManager.releaseConnection();
    }
}
