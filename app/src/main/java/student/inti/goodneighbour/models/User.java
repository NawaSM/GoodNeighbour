package student.inti.goodneighbour.models;

import com.google.firebase.Timestamp;

public class User {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String location;
    private String organizationName;
    private Timestamp organizationLastUpdated;
    private String profileImageUrl;
    private Timestamp createdAt;

    // Empty constructor needed for Firestore
    public User() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public Timestamp getOrganizationLastUpdated() { return organizationLastUpdated; }
    public void setOrganizationLastUpdated(Timestamp organizationLastUpdated) {
        this.organizationLastUpdated = organizationLastUpdated;
    }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    // Helper method to check if organization name can be updated
    public boolean canUpdateOrganization() {
        if (organizationLastUpdated == null) return true;

        long daysSinceLastUpdate = (Timestamp.now().getSeconds() -
                organizationLastUpdated.getSeconds()) / (60 * 60 * 24);
        return daysSinceLastUpdate >= 30;
    }
}