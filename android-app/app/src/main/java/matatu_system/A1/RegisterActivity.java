package matatu_system.A1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
    private RadioGroup roleRadioGroup;
    private Button registerButton;
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
        roleRadioGroup = findViewById(R.id.roleRadioGroup);
        registerButton = findViewById(R.id.registerButton);

        String prefillEmail = getIntent().getStringExtra("prefill_email");
        String prefillName = getIntent().getStringExtra("prefill_name");
        String firebaseUid = getIntent().getStringExtra("firebaseUid");

        if (prefillEmail != null) emailEditText.setText(prefillEmail);
        if (prefillName != null) nameEditText.setText(prefillName);

        if (firebaseUid != null) {
            passwordEditText.setVisibility(android.view.View.GONE);
            registerButton.setText("Complete Registration");
        }

        String finalFirebaseUid = firebaseUid;
        registerButton.setOnClickListener(v -> {
            String name = nameEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            int selectedId = roleRadioGroup.getCheckedRadioButtonId();
            RadioButton selectedRoleButton = findViewById(selectedId);
            String role = selectedRoleButton != null ? selectedRoleButton.getText().toString().toLowerCase() : "passenger";

            if (name.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Fill in name and email", Toast.LENGTH_SHORT).show();
                return;
            }

            if (finalFirebaseUid != null) {
                registerOnBackend(finalFirebaseUid, name, email, role);
            } else {
                if (password.isEmpty()) {
                    Toast.makeText(this, "Enter a password", Toast.LENGTH_SHORT).show();
                    return;
                }
                registerWithFirebase(name, email, password, role);
            }
        });
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
                    registerButton.setText("Register");
                    String msg = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
            });
    }

    private void registerOnBackend(String uid, String name, String email, String role) {
        User user = new User(uid, name, email, role);
        RetrofitClient.getApiService().registerUser(user).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                registerButton.setEnabled(true);
                registerButton.setText("Register");
                if (response.isSuccessful()) {
                    sessionManager.saveSession(uid, email, name, role);
                    Toast.makeText(RegisterActivity.this, "Welcome " + name + "!", Toast.LENGTH_SHORT).show();
                    routeToDashboard(role);
                } else {
                    Toast.makeText(RegisterActivity.this, "Backend sync failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                registerButton.setEnabled(true);
                registerButton.setText("Register");
                Toast.makeText(RegisterActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
}
