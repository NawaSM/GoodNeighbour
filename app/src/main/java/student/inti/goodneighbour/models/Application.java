package student.inti.goodneighbour.models;

import com.google.firebase.Timestamp;
import java.util.Date;

public class Application {
    private String id;
    private String userId;
    private String userName;
    private String userEmail;
    private String userPhone;
    private String userImageUrl;
    private String opportunityId;
    private String motivation;
    private String status;
    private Date createdAt;

    // Empty constructor needed for Firestore
    public Application() {}

    public Application(String userId, String userName, String opportunityId,
                       String motivation) {
        this.userId = userId;
        this.userName = userName;
        this.opportunityId = opportunityId;
        this.motivation = motivation;
        this.status = "pending";
        this.createdAt = new Date();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getOpportunityId() { return opportunityId; }
    public void setOpportunityId(String opportunityId) { this.opportunityId = opportunityId; }

    public String getMotivation() { return motivation; }
    public void setMotivation(String motivation) { this.motivation = motivation; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }

    public String getUserImageUrl() { return userImageUrl; }
    public void setUserImageUrl(String userImageUrl) { this.userImageUrl = userImageUrl; }
}