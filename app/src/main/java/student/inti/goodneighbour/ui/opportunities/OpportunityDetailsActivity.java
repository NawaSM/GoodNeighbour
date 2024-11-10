package student.inti.goodneighbour.ui.opportunities;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import student.inti.goodneighbour.R;
import student.inti.goodneighbour.models.Opportunity;

public class OpportunityDetailsActivity extends AppCompatActivity {
    private static final String DATE_FORMAT = "MMMM dd, yyyy 'at' hh:mm a";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Opportunity opportunity;
    private String opportunityId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opportunity_details);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get opportunity ID from intent
        opportunityId = getIntent().getStringExtra("opportunity_id");
        if (opportunityId == null) {
            Toast.makeText(this, "Error loading opportunity details", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Load opportunity details
        loadOpportunityDetails(opportunityId);

        loadOpportunityDetails(opportunityId);
        checkExistingApplication();

        // Setup apply button
        MaterialButton applyButton = findViewById(R.id.applyButton);
        applyButton.setOnClickListener(v -> showApplyDialog());
    }

    private void loadOpportunityDetails(String opportunityId) {
        db.collection("opportunities").document(opportunityId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    opportunity = documentSnapshot.toObject(Opportunity.class);
                    if (opportunity != null) {
                        displayOpportunityDetails();
                    } else {
                        Toast.makeText(this, "Opportunity not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading opportunity: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void displayOpportunityDetails() {
        // Set activity title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(opportunity.getTitle());
        }

        ImageView imageView = findViewById(R.id.opportunityImage);
        if (opportunity.getImageUrl() != null && !opportunity.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(opportunity.getImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .centerCrop()
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.placeholder_image);
        }

        // Set opportunity details
        ((TextView) findViewById(R.id.opportunityTitle)).setText(opportunity.getTitle());
        ((TextView) findViewById(R.id.organizationName)).setText(opportunity.getOrganizationName());
        ((TextView) findViewById(R.id.descriptionText)).setText(opportunity.getDescription());
        ((TextView) findViewById(R.id.locationValue)).setText(opportunity.getLocation());

        // Format and set date/time
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        String dateTime = sdf.format(opportunity.getDate());
        ((TextView) findViewById(R.id.dateTimeValue)).setText(dateTime);
    }

    private void showApplyDialog() {
        // Check if user is logged in
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login to apply", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_apply);

        TextInputEditText motivationEditText = dialog.findViewById(R.id.motivationEditText);
        MaterialButton cancelButton = dialog.findViewById(R.id.cancelButton);
        MaterialButton submitButton = dialog.findViewById(R.id.submitButton);

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        submitButton.setOnClickListener(v -> {
            String motivation = motivationEditText.getText().toString().trim();
            if (motivation.isEmpty()) {
                motivationEditText.setError("Please enter your motivation");
                return;
            }
            submitApplication(motivation, dialog);
        });

        dialog.show();
    }

    private void submitApplication(String motivation, Dialog dialog) {
        String userId = mAuth.getCurrentUser().getUid();

        // Get user details before creating application
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String profileImagePath = documentSnapshot.getString("profileImageUrl");
                    Log.d("OpportunityDetails", "User profile image path: " + profileImagePath);

                    Map<String, Object> application = new HashMap<>();
                    application.put("userId", userId);
                    application.put("userName", documentSnapshot.getString("name"));
                    application.put("userEmail", documentSnapshot.getString("email"));
                    application.put("userPhone", documentSnapshot.getString("phone"));
                    application.put("userImageUrl", profileImagePath);
                    application.put("opportunityId", opportunityId);
                    application.put("motivation", motivation);
                    application.put("status", "pending");
                    application.put("createdAt", new Date());

                    db.collection("applications")
                            .add(application)
                            .addOnSuccessListener(documentReference -> {
                                Log.d("OpportunityDetails", "Application created with profile image: " +
                                        profileImagePath);
                                Toast.makeText(this, "Application submitted successfully!",
                                        Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("OpportunityDetails", "Failed to submit application", e);
                                Toast.makeText(this, "Failed to submit application: " +
                                        e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("OpportunityDetails", "Failed to get user details", e);
                    Toast.makeText(this, "Failed to submit application: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void checkExistingApplication() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        db.collection("applications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("opportunityId", opportunityId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    MaterialButton applyButton = findViewById(R.id.applyButton);
                    if (!queryDocumentSnapshots.isEmpty()) {
                        applyButton.setText("Already Applied");
                        applyButton.setEnabled(false);
                    }
                });
    }
}
