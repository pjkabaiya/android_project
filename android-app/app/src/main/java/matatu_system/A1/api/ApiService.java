package matatu_system.A1.api;

import java.util.List;
import java.util.Map;

import matatu_system.A1.models.Trip;
import matatu_system.A1.models.TripRequest;
import matatu_system.A1.models.User;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @POST("users/register")
    Call<User> registerUser(@Body User user);

    @GET("users/{firebaseUid}")
    Call<User> getUserProfile(@Path("firebaseUid") String firebaseUid);

    @POST("api/trips")
    Call<Trip> createTrip(@Body Trip trip);

    @GET("api/trips")
    Call<List<Trip>> searchTrips(@Query("route") String route, @Query("driverId") String driverId);

    @GET("api/trips/requests")
    Call<List<TripRequest>> getPassengerRequests(@Query("passengerId") String passengerId);

    @GET("api/trips/requests")
    Call<List<TripRequest>> getPassengerRequestsWithProcessed(@Query("passengerId") String passengerId, @Query("includeProcessed") boolean includeProcessed);

    @GET("api/trips/{id}")
    Call<Trip> getTrip(@Path("id") String id);

    @PATCH("api/trips/{id}")
    Call<Trip> updateTrip(@Path("id") String id, @Body Map<String, Object> updates);

    @GET("api/trips/{id}/requests")
    Call<List<TripRequest>> getTripRequests(@Path("id") String tripId);

    @POST("api/trips/{id}/requests")
    Call<TripRequest> createTripRequest(@Path("id") String tripId, @Body TripRequest request);

    @PATCH("api/trips/requests/{id}")
    Call<TripRequest> updateRequestStatus(@Path("id") String requestId, @Body Map<String, Object> updates);
}
