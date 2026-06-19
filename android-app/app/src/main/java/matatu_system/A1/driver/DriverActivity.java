package matatu_system.A1.driver;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.android.material.button.MaterialButton;

import matatu_system.A1.R;
import matatu_system.A1.api.RetrofitClient;
import matatu_system.A1.map.MapViewActivity;
import matatu_system.A1.models.Trip;
import matatu_system.A1.models.TripRequest;
import matatu_system.A1.utils.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverActivity extends AppCompatActivity {

    private EditText editPlate, editRoute, editSeats;
    private Button btnStartTrip;
    private View setupCard, currentRoutesCard;
    private ListView currentRoutesList;

    private List<Trip> driverTrips;
    private ArrayAdapter<String> routesAdapter;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver);

        editPlate = findViewById(R.id.editPlateNumber);
        editRoute = findViewById(R.id.editRoute);
        editSeats = findViewById(R.id.editSeats);
        btnStartTrip = findViewById(R.id.btnStartTrip);
        setupCard = findViewById(R.id.setupCard);
        currentRoutesCard = findViewById(R.id.currentRoutesCard);
        currentRoutesList = findViewById(R.id.currentRoutesList);

        btnStartTrip.setOnClickListener(v -> startTrip());

        sessionManager = new SessionManager(this);
        MaterialButton btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> sessionManager.logout(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDriverTrips();
    }

    private void loadDriverTrips() {
        RetrofitClient.getApiService().searchTrips(null, sessionManager.getUid()).enqueue(new Callback<List<Trip>>() {
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
                        Intent intent = new Intent(DriverActivity.this, MapViewActivity.class);
                        intent.putExtra("tripId", trip.getId());
                        intent.putExtra("isDriver", true);
                        intent.putExtra("numberPlate", trip.getNumberPlate());
                        intent.putExtra("route", trip.getRoute());
                        startActivity(intent);
                    } else {
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
        EditText input = new EditText(this);
        input.setHint("Reason for cancellation");
        input.setPadding(48, 16, 48, 16);
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Cancel Trip")
            .setMessage("Passengers will be notified of the reason.")
            .setView(input)
            .setPositiveButton("Cancel Trip", (d, w) -> {
                String reason = input.getText().toString().trim();
                if (reason.isEmpty()) reason = "Cancelled by driver";
                Map<String, Object> body = new HashMap<>();
                body.put("reason", reason);
                RetrofitClient.getApiService().cancelTrip(tripId, body).enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(DriverActivity.this, "Trip cancelled", Toast.LENGTH_SHORT).show();
                            loadDriverTrips();
                        } else {
                            Toast.makeText(DriverActivity.this, "Failed to cancel", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        Toast.makeText(DriverActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Back", null)
            .show();
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

        int availableSeats = seatsStr.isEmpty() ? 14 : Integer.parseInt(seatsStr);

        Trip trip = new Trip(plate, route, availableSeats, sessionManager.getUid());
        RetrofitClient.getApiService().createTrip(trip).enqueue(new Callback<Trip>() {
            @Override
            public void onResponse(Call<Trip> call, Response<Trip> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String tripId = response.body().getId();

                    Intent intent = new Intent(DriverActivity.this, MapViewActivity.class);
                    intent.putExtra("tripId", tripId);
                    intent.putExtra("isDriver", true);
                    intent.putExtra("numberPlate", plate);
                    intent.putExtra("route", route);
                    intent.putExtra("availableSeats", availableSeats);
                    intent.putExtra("createRouteDirect", true);
                    startActivity(intent);

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
}
