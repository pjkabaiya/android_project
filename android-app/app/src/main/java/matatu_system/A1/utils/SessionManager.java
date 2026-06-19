package matatu_system.A1.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import matatu_system.A1.LoginActivity;

public class SessionManager {
    private static final String PREFS_NAME = "matatu_session";
    private static final String KEY_LOGGED_IN = "isLoggedIn";
    private static final String KEY_UID = "firebaseUid";
    private static final String KEY_ROLE = "role";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_NAME = "name";

    private SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(String uid, String email, String name, String role) {
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putString(KEY_UID, uid)
            .putString(KEY_EMAIL, email)
            .putString(KEY_NAME, name)
            .putString(KEY_ROLE, role)
            .apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    public String getUid() { return prefs.getString(KEY_UID, null); }
    public String getEmail() { return prefs.getString(KEY_EMAIL, null); }
    public String getName() { return prefs.getString(KEY_NAME, null); }
    public String getRole() { return prefs.getString(KEY_ROLE, null); }

    public boolean isDriver() { return "driver".equals(getRole()); }
    public boolean isPassenger() { return "passenger".equals(getRole()); }

    public void clearSession() {
        prefs.edit().clear().apply();
    }

    public void logout(Context context) {
        clearSession();
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
