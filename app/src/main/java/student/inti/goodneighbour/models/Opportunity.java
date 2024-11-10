package student.inti.goodneighbour.models;

import com.google.firebase.Timestamp;
import java.util.Date;

public class Opportunity {
    private String id;
    private String imageUrl;
    private String title;
    private String description;
    private String location;
    private Date date;
    private String time;
    private String organizationId;
    private String organizationName;
    private String status;
    private Date createdAt;

    // Empty constructor needed for Firestore
    public Opportunity() {}

    public Opportunity(String id, String title, String description, String location,
                       Date date, String time, String organizationId, String organizationName) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.date = date;
        this.time = time;
        this.organizationId = organizationId;
        this.organizationName = organizationName;
        this.status = "active";
        this.createdAt = new Date();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }

    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}