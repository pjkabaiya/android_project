package matatu_system.A1.driver;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import matatu_system.A1.R;
import matatu_system.A1.models.Reservation;

public class ReservationAdapter extends RecyclerView.Adapter<ReservationAdapter.ViewHolder> {

    private List<Reservation> reservations;
    private OnReservationAction listener;

    public interface OnReservationAction {
        void onAccept(Reservation res);
        void onReject(Reservation res);
    }

    public ReservationAdapter(List<Reservation> reservations, OnReservationAction listener) {
        this.reservations = reservations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reservation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Reservation res = reservations.get(position);
        holder.txtPassenger.setText("Passenger ID: " + res.getPassengerId());
        holder.txtDetails.setText("From: " + res.getPickupPoint() + " To: " + res.getDestination());
        
        holder.btnAccept.setOnClickListener(v -> listener.onAccept(res));
        holder.btnReject.setOnClickListener(v -> listener.onReject(res));
    }

    @Override
    public int getItemCount() {
        return reservations.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtPassenger, txtDetails;
        Button btnAccept, btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtPassenger = itemView.findViewById(R.id.txtPassengerId);
            txtDetails = itemView.findViewById(R.id.txtReservationDetails);
            btnAccept = itemView.findViewById(R.id.btnAcceptReservation);
            btnReject = itemView.findViewById(R.id.btnRejectReservation);
        }
    }
}
