package matatu_system.A1;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import matatu_system.A1.api.RetrofitClient;
import matatu_system.A1.models.Trip;
import matatu_system.A1.models.TripRequest;
import matatu_system.A1.models.User;
import matatu_system.A1.utils.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private TextView txtName, txtEmail, txtRole, txtMemberSince, txtTrips, txtPassengers;
    private TextView txtRequests, txtAccepted, txtRejected, txtCancelled;
    private View driverStats, passengerStats;
    private EditText editName, editNumberPlate, editPhone;
    private MaterialButton btnSave;
    private View loadingOverlay;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sessionManager = new SessionManager(this);

        txtName = findViewById(R.id.txtName);
        txtEmail = findViewById(R.id.txtEmail);
        txtRole = findViewById(R.id.txtRole);
        txtMemberSince = findViewById(R.id.txtMemberSince);
        txtTrips = findViewById(R.id.txtTrips);
        txtPassengers = findViewById(R.id.txtPassengers);
        txtRequests = findViewById(R.id.txtRequests);
        txtAccepted = findViewById(R.id.txtAccepted);
        txtRejected = findViewById(R.id.txtRejected);
        txtCancelled = findViewById(R.id.txtCancelled);
        driverStats = findViewById(R.id.driverStats);
        passengerStats = findViewById(R.id.passengerStats);
        editName = findViewById(R.id.editName);
        editNumberPlate = findViewById(R.id.editNumberPlate);
        editPhone = findViewById(R.id.editPhone);
        btnSave = findViewById(R.id.btnSave);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        MaterialButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> saveProfile());

        loadProfile();
    }

    private void loadProfile() {
        loadingOverlay.setVisibility(View.VISIBLE);
        RetrofitClient.getApiService().getUserProfile(sessionManager.getUid()).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                loadingOverlay.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    displayUser(response.body());
                } else {
                    Toast.makeText(ProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                loadingOverlay.setVisibility(View.GONE);
                Toast.makeText(ProfileActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayUser(User user) {
        txtName.setText(user.getName() != null ? user.getName() : "-");
        txtEmail.setText(user.getEmail() != null ? user.getEmail() : "-");
        String role = user.getRole() != null ? user.getRole() : "passenger";
        txtRole.setText(role.substring(0, 1).toUpperCase() + role.substring(1));
        String date = user.getCreatedAt() != null ? user.getCreatedAt() : "";
        if (date.length() >= 10) date = date.substring(0, 10);
        txtMemberSince.setText(date.isEmpty() ? "-" : date);

        editName.setText(user.getName());
        editNumberPlate.setText(user.getNumberPlate() != null ? user.getNumberPlate() : "");
        editPhone.setText(user.getPhone() != null ? user.getPhone() : "");

        if ("driver".equals(role)) {
            driverStats.setVisibility(View.VISIBLE);
            passengerStats.setVisibility(View.GONE);
            loadDriverStats();
        } else {
            driverStats.setVisibility(View.GONE);
            passengerStats.setVisibility(View.VISIBLE);
            loadPassengerStats();
        }
    }

    private void loadDriverStats() {
        String uid = sessionManager.getUid();
        RetrofitClient.getApiService().searchTrips(null, uid).enqueue(new Callback<List<Trip>>() {
            @Override
            public void onResponse(Call<List<Trip>> call, Response<List<Trip>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    txtTrips.setText(String.valueOf(response.body().size()));
                }
            }
            @Override
            public void onFailure(Call<List<Trip>> call, Throwable t) {}
        });
        RetrofitClient.getApiService().getPassengerRequestsWithProcessed(uid, true).enqueue(new Callback<List<TripRequest>>() {
            @Override
            public void onResponse(Call<List<TripRequest>> call, Response<List<TripRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int accepted = 0;
                    for (TripRequest r : response.body()) {
                        if ("ACCEPTED".equals(r.getStatus())) accepted++;
                    }
                    txtPassengers.setText(String.valueOf(accepted));
                }
            }
            @Override
            public void onFailure(Call<List<TripRequest>> call, Throwable t) {}
        });
    }

    private void loadPassengerStats() {
        String uid = sessionManager.getUid();
        RetrofitClient.getApiService().getPassengerRequestsWithProcessed(uid, true).enqueue(new Callback<List<TripRequest>>() {
            @Override
            public void onResponse(Call<List<TripRequest>> call, Response<List<TripRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int total = response.body().size();
                    int acc = 0, rej = 0, can = 0;
                    for (TripRequest r : response.body()) {
                        String s = r.getStatus();
                        if ("ACCEPTED".equals(s)) acc++;
                        else if ("REJECTED".equals(s)) rej++;
                        else if ("CANCELLED".equals(s)) can++;
                    }
                    txtRequests.setText(String.valueOf(total));
                    txtAccepted.setText(String.valueOf(acc));
                    txtRejected.setText(String.valueOf(rej));
                    txtCancelled.setText(String.valueOf(can));
                }
            }
            @Override
            public void onFailure(Call<List<TripRequest>> call, Throwable t) {}
        });
    }

    private void saveProfile() {
        String name = editName.getText().toString().trim();
        String numberPlate = editNumberPlate.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("numberPlate", numberPlate);
        updates.put("phone", phone);

        RetrofitClient.getApiService().updateUserProfile(sessionManager.getUid(), updates).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                btnSave.setEnabled(true);
                btnSave.setText("Save Changes");
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(ProfileActivity.this, "Profile updated", Toast.LENGTH_SHORT).show();
                    displayUser(response.body());
                } else {
                    Toast.makeText(ProfileActivity.this, "Failed to save", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                btnSave.setEnabled(true);
                btnSave.setText("Save Changes");
                Toast.makeText(ProfileActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
