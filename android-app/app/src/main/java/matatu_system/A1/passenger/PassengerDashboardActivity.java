package matatu_system.A1.passenger;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import matatu_system.A1.R;

public class PassengerDashboardActivity extends AppCompatActivity {

    private EditText editPickup, editDestination;
    private Button btnFind;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger_dashboard);

        editPickup = findViewById(R.id.editPickup);
        editDestination = findViewById(R.id.editDestination);
        btnFind = findViewById(R.id.btnFindMatatu);

        btnFind.setOnClickListener(v -> {
            String pickup = editPickup.getText().toString().trim();
            String dest = editDestination.getText().toString().trim();

            if (pickup.isEmpty() || dest.isEmpty()) {
                Toast.makeText(this, "Please enter both points", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(PassengerDashboardActivity.this, PassengerActivity.class);
            intent.putExtra("PICKUP_POINT", pickup);
            intent.putExtra("DESTINATION_POINT", dest);
            startActivity(intent);
        });
    }
}
