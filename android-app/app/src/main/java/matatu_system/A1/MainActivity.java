package matatu_system.A1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import matatu_system.A1.driver.DriverActivity;
import matatu_system.A1.passenger.PassengerDashboardActivity;
import matatu_system.A1.utils.SessionManager;

public class MainActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        if (sessionManager.isLoggedIn()) {
            String role = sessionManager.getRole();
            if ("driver".equals(role)) {
                startActivity(new Intent(this, DriverActivity.class));
            } else {
                startActivity(new Intent(this, PassengerDashboardActivity.class));
            }
            finish();
        } else {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }
}
