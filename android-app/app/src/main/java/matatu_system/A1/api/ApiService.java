package matatu_system.A1.api;

import matatu_system.A1.models.User;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {
    @POST("users/register")
    Call<User> registerUser(@Body User user);

    @GET("users/{firebaseUid}")
    Call<User> getUserProfile(@Path("firebaseUid") String firebaseUid);
}
