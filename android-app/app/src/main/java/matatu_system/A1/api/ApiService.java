package matatu_system.A1.api;

import matatu_system.A1.models.Reservation;
import matatu_system.A1.models.User;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {
    // These routes are at the root in your server.js
    @POST("users/register")
    Call<User> registerUser(@Body User user);

    @GET("users/{firebaseUid}")
    Call<User> getUserProfile(@Path("firebaseUid") String firebaseUid);

    // These routes are mounted under /api in your server.js
    @POST("api/reservations")
    Call<Reservation> requestReservation(@Body Reservation reservation);

    @PATCH("api/reservations/{id}/status")
    Call<Reservation> updateReservationStatus(@Path("id") String id, @Body String status);
}
