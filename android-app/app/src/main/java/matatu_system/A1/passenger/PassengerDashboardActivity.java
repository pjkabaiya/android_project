package matatu_system.A1.passenger;

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

import java.util.List;

import matatu_system.A1.R;
import matatu_system.A1.api.RetrofitClient;
import matatu_system.A1.models.Trip;
import matatu_system.A1.models.TripRequest;
import matatu_system.A1.utils.SocketManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PassengerDashboardActivity extends AppCompatActivity {

    private EditText editFrom, editTo, editPickupPoint;
    private Button btnSearch, btnRequest;
    private ListView vehicleList;
    private TextView txtResultsHeader, txtNoResults, txtSelectedVehicle;
    private View requestCard;

    private List<Trip> activeTrips;
    private String selectedTripId;

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

        btnSearch.setOnClickListener(v -> searchTrips());
        btnRequest.setOnClickListener(v -> requestRide());

        SocketManager.establishConnection();
    }

    private void searchTrips() {
        String from = editFrom.getText().toString().trim();
        String to = editTo.getText().toString().trim();

        if (from.isEmpty() || to.isEmpty()) {
            Toast.makeText(this, "Enter both locations", Toast.LENGTH_SHORT).show();
            return;
        }

        String route = from + " " + to;
        RetrofitClient.getApiService().searchTrips(route).enqueue(new Callback<List<Trip>>() {
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
                Toast.makeText(PassengerDashboardActivity.this, "Search failed", Toast.LENGTH_SHORT).show();
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

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
        vehicleList.setAdapter(adapter);

        vehicleList.setOnItemClickListener((parent, view, position, id) -> {
            Trip trip = activeTrips.get(position);
            selectedTripId = trip.getId();
            txtSelectedVehicle.setText("Vehicle: " + trip.getNumberPlate() + " (" + trip.getRoute() + ")");
            requestCard.setVisibility(View.VISIBLE);
        });
    }

    private void showNoResults() {
        txtResultsHeader.setVisibility(View.GONE);
        vehicleList.setVisibility(View.GONE);
        txtNoResults.setVisibility(View.VISIBLE);
        requestCard.setVisibility(View.GONE);
    }

    private void requestRide() {
        if (selectedTripId == null) return;
        String pickup = editPickupPoint.getText().toString().trim();
        if (pickup.isEmpty()) {
            Toast.makeText(this, "Enter your pickup location", Toast.LENGTH_SHORT).show();
            return;
        }

        TripRequest req = new TripRequest(selectedTripId, "passenger_" + System.currentTimeMillis(), pickup);
        RetrofitClient.getApiService().createTripRequest(selectedTripId, req).enqueue(new Callback<TripRequest>() {
            @Override
            public void onResponse(Call<TripRequest> call, Response<TripRequest> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(PassengerDashboardActivity.this, "Ride requested!", Toast.LENGTH_LONG).show();
                    requestCard.setVisibility(View.GONE);
                    sendSocketRequest(pickup);
                }
            }

            @Override
            public void onFailure(Call<TripRequest> call, Throwable t) {
                Toast.makeText(PassengerDashboardActivity.this, "Request failed", Toast.LENGTH_SHORT).show();
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
                SocketManager.getSocket().emit("reservation-update", data);
            } catch (JSONException e) { e.printStackTrace(); }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SocketManager.closeConnection();
    }
}
