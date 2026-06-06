package matatu_system.A1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import matatu_system.A1.api.RetrofitClient;
import matatu_system.A1.driver.DriverActivity;
import matatu_system.A1.models.User;
import matatu_system.A1.passenger.PassengerActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private TextView statusText;
    private LinearLayout fallbackLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = findViewById(R.id.loadingProgress);
        statusText = findViewById(R.id.statusText);
        fallbackLayout = findViewById(R.id.fallbackLayout);

        Button btnPassenger = findViewById(R.id.btnPassengerFallback);
        Button btnDriver = findViewById(R.id.btnDriverFallback);

        btnPassenger.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, PassengerActivity.class));
            finish();
        });

        btnDriver.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, DriverActivity.class));
            finish();
        });

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        } else {
            fetchUserRole(currentUser.getUid());
        }
    }

    private void fetchUserRole(String uid) {
        RetrofitClient.getApiService().getUserProfile(uid).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String role = response.body().getRole();
                    if (role != null) {
                        role = role.toLowerCase();
                    }
                    
                    if ("driver".equals(role)) {
                        startActivity(new Intent(MainActivity.this, DriverActivity.class));
                        finish();
                    } else if ("passenger".equals(role)) {
                        startActivity(new Intent(MainActivity.this, PassengerActivity.class));
                        finish();
                    } else {
                        showFallback("Unknown role: " + role);
                    }
                } else {
                    showFallback("Backend profile not found. (Error " + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                showFallback("Network error: " + t.getMessage());
            }
        });
    }

    private void showFallback(String message) {
        progressBar.setVisibility(View.GONE);
        statusText.setText(message);
        fallbackLayout.setVisibility(View.VISIBLE);
    }
}
