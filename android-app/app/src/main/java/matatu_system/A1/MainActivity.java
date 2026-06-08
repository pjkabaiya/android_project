package matatu_system.A1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import matatu_system.A1.driver.DriverActivity;
import matatu_system.A1.passenger.PassengerDashboardActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_selector); // New layout for testing

        Button btnPassenger = findViewById(R.id.btnTestPassenger);
        Button btnDriver = findViewById(R.id.btnTestDriver);

        btnPassenger.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, PassengerDashboardActivity.class));
        });

        btnDriver.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, DriverActivity.class));
        });
    }
}
