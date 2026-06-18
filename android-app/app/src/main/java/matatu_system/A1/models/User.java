package matatu_system.A1.models;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("_id")
    private String id;
    private String firebaseUid;
    private String name;
    private String email;
    private String role; // passenger, driver, admin

    public User() {}

    public User(String firebaseUid, String name, String email, String role) {
        this.firebaseUid = firebaseUid;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFirebaseUid() { return firebaseUid; }
    public void setFirebaseUid(String firebaseUid) { this.firebaseUid = firebaseUid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
