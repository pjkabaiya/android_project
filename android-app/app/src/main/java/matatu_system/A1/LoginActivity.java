package matatu_system.A1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import matatu_system.A1.api.RetrofitClient;
import matatu_system.A1.driver.DriverActivity;
import matatu_system.A1.passenger.PassengerDashboardActivity;
import matatu_system.A1.utils.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_GOOGLE_SIGN_IN = 1001;

    private TextView emailEditText, passwordEditText, txtError, registerTextView;
    private Button loginButton, googleSignInButton;
    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        registerTextView = findViewById(R.id.registerTextView);
        txtError = findViewById(R.id.txtError);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        loginButton.setOnClickListener(v -> loginWithEmail());
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());
        registerTextView.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void loginWithEmail() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Fill in all fields");
            return;
        }

        setLoading(true);
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                setLoading(false);
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) fetchUserRoleAndRoute(user);
                } else {
                    String msg = task.getException() != null ? task.getException().getMessage() : "Login failed";
                    showError(msg);
                }
            });
    }

    private void signInWithGoogle() {
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            startActivityForResult(googleSignInClient.getSignInIntent(), RC_GOOGLE_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                showError("Google sign-in failed: " + e.getStatusCode());
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        setLoading(true);
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this, task -> {
                setLoading(false);
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        checkBackendAndRoute(user, account.getDisplayName(), account.getEmail());
                    }
                } else {
                    String msg = task.getException() != null ? task.getException().getMessage() : "Auth failed";
                    showError(msg);
                }
            });
    }

    private void checkBackendAndRoute(FirebaseUser user, String name, String email) {
        RetrofitClient.getApiService().getUserProfile(user.getUid()).enqueue(new Callback<matatu_system.A1.models.User>() {
            @Override
            public void onResponse(Call<matatu_system.A1.models.User> call, Response<matatu_system.A1.models.User> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getRole() != null) {
                    sessionManager.saveSession(user.getUid(), email, name, response.body().getRole());
                    routeToDashboard(response.body().getRole());
                } else {
                    Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                    intent.putExtra("prefill_email", email);
                    intent.putExtra("prefill_name", name);
                    intent.putExtra("firebaseUid", user.getUid());
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onFailure(Call<matatu_system.A1.models.User> call, Throwable t) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                intent.putExtra("prefill_email", email);
                intent.putExtra("prefill_name", name);
                intent.putExtra("firebaseUid", user.getUid());
                startActivity(intent);
                finish();
            }
        });
    }

    private void fetchUserRoleAndRoute(FirebaseUser user) {
        RetrofitClient.getApiService().getUserProfile(user.getUid()).enqueue(new Callback<matatu_system.A1.models.User>() {
            @Override
            public void onResponse(Call<matatu_system.A1.models.User> call, Response<matatu_system.A1.models.User> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getRole() != null) {
                    String role = response.body().getRole();
                    sessionManager.saveSession(user.getUid(), user.getEmail(), user.getDisplayName(), role);
                    routeToDashboard(role);
                } else {
                    showError("No role assigned. Please register again.");
                }
            }

            @Override
            public void onFailure(Call<matatu_system.A1.models.User> call, Throwable t) {
                showError("Could not connect to server");
            }
        });
    }

    private void routeToDashboard(String role) {
        if ("driver".equals(role)) {
            startActivity(new Intent(LoginActivity.this, DriverActivity.class));
        } else {
            startActivity(new Intent(LoginActivity.this, PassengerDashboardActivity.class));
        }
        finish();
    }

    private void showError(String msg) {
        txtError.setVisibility(TextView.VISIBLE);
        txtError.setText(msg);
    }

    private void setLoading(boolean loading) {
        loginButton.setEnabled(!loading);
        googleSignInButton.setEnabled(!loading);
        loginButton.setText(loading ? "Signing in..." : "Sign In");
    }
}
