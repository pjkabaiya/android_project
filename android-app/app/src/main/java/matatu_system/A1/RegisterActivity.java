package matatu_system.A1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import matatu_system.A1.api.RetrofitClient;
import matatu_system.A1.driver.DriverActivity;
import matatu_system.A1.passenger.PassengerDashboardActivity;
import matatu_system.A1.models.User;
import matatu_system.A1.utils.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameEditText, emailEditText, passwordEditText;
    private TextView txtError;
    private MaterialCardView cardPassenger, cardDriver;
    private LinearLayout circlePassenger, circleDriver;
    private Button registerButton;
    private String selectedRole = "passenger";
    private FirebaseAuth mAuth;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);

        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        txtError = findViewById(R.id.txtError);
        registerButton = findViewById(R.id.registerButton);
        cardPassenger = findViewById(R.id.cardPassenger);
        cardDriver = findViewById(R.id.cardDriver);
        circlePassenger = findViewById(R.id.circlePassenger);
        circleDriver = findViewById(R.id.circleDriver);
        TextView loginTextView = findViewById(R.id.loginTextView);

        String prefillEmail = getIntent().getStringExtra("prefill_email");
        String prefillName = getIntent().getStringExtra("prefill_name");
        String firebaseUid = getIntent().getStringExtra("firebaseUid");

        if (prefillEmail != null) emailEditText.setText(prefillEmail);
        if (prefillName != null) nameEditText.setText(prefillName);

        if (firebaseUid != null) {
            passwordEditText.setVisibility(android.view.View.GONE);
            registerButton.setText("Complete Registration");
        }

        updateRoleSelection();

        cardPassenger.setOnClickListener(v -> {
            selectedRole = "passenger";
            updateRoleSelection();
        });

        cardDriver.setOnClickListener(v -> {
            selectedRole = "driver";
            updateRoleSelection();
        });

        String finalFirebaseUid = firebaseUid;
        registerButton.setOnClickListener(v -> {
            String name = nameEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty()) {
                showError("Fill in name and email");
                return;
            }

            if (finalFirebaseUid != null) {
                registerOnBackend(finalFirebaseUid, name, email, selectedRole);
            } else {
                if (password.isEmpty()) {
                    showError("Enter a password");
                    return;
                }
                registerWithFirebase(name, email, password, selectedRole);
            }
        });

        loginTextView.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void updateRoleSelection() {
        boolean isPassenger = "passenger".equals(selectedRole);
        applyCardStyle(cardPassenger, circlePassenger, isPassenger);
        applyCardStyle(cardDriver, circleDriver, !isPassenger);
    }

    private void applyCardStyle(MaterialCardView card, LinearLayout circle, boolean selected) {
        if (selected) {
            card.setCardBackgroundColor(0xFFFFF8E1);
            card.setStrokeWidth(2);
            card.setStrokeColor(0xFFFFC107);
            circle.setBackgroundResource(R.drawable.circle_yellow);
            for (int i = 0; i < ((LinearLayout) card.getChildAt(0)).getChildCount(); i++) {
                android.view.View v = ((LinearLayout) card.getChildAt(0)).getChildAt(i);
                if (v instanceof TextView) {
                    ((TextView) v).setTextColor(0xFF212121);
                }
            }
        } else {
            card.setCardBackgroundColor(0xFFFFFFFF);
            card.setStrokeWidth(0);
            circle.setBackgroundResource(R.drawable.circle_gray);
            for (int i = 0; i < ((LinearLayout) card.getChildAt(0)).getChildCount(); i++) {
                android.view.View v = ((LinearLayout) card.getChildAt(0)).getChildAt(i);
                if (v instanceof TextView) {
                    ((TextView) v).setTextColor(0xFF757575);
                }
            }
        }
    }

    private void registerWithFirebase(String name, String email, String password, String role) {
        registerButton.setEnabled(false);
        registerButton.setText("Creating account...");

        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        registerOnBackend(firebaseUser.getUid(), name, email, role);
                    }
                } else {
                    registerButton.setEnabled(true);
                    registerButton.setText("Create Account");
                    String msg = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                    showError(msg);
                }
            });
    }

    private void registerOnBackend(String uid, String name, String email, String role) {
        User user = new User(uid, name, email, role);
        RetrofitClient.getApiService().registerUser(user).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                registerButton.setEnabled(true);
                registerButton.setText("Create Account");
                if (response.isSuccessful()) {
                    sessionManager.saveSession(uid, email, name, role);
                    Toast.makeText(RegisterActivity.this, "Welcome " + name + "!", Toast.LENGTH_SHORT).show();
                    routeToDashboard(role);
                } else {
                    showError("Backend sync failed");
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                registerButton.setEnabled(true);
                registerButton.setText("Create Account");
                showError("Error: " + t.getMessage());
            }
        });
    }

    private void routeToDashboard(String role) {
        if ("driver".equals(role)) {
            startActivity(new Intent(RegisterActivity.this, DriverActivity.class));
        } else {
            startActivity(new Intent(RegisterActivity.this, PassengerDashboardActivity.class));
        }
        finish();
    }

    private void showError(String msg) {
        txtError.setVisibility(android.view.View.VISIBLE);
        txtError.setText(msg);
    }
}
