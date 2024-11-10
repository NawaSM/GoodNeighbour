package student.inti.goodneighbour.ui.opportunities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import student.inti.goodneighbour.R;
import student.inti.goodneighbour.utils.LocationUtils;
import student.inti.goodneighbour.utils.ImageUtils;
import student.inti.goodneighbour.utils.UIUtils;

public class CreateOpportunityActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Uri selectedImageUri;

    private ImageView opportunityImageView;
    private TextInputEditText titleEditText, descriptionEditText,
            dateEditText, timeEditText, locationEditText;
    private MaterialButton createButton;
    private Calendar selectedDateTime;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    opportunityImageView.setImageURI(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_opportunity);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        selectedDateTime = Calendar.getInstance();

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize views
        initializeViews();
        setupListeners();

        // Check if user has organization name
        checkOrganizationStatus();
    }

    private void initializeViews() {
        opportunityImageView = findViewById(R.id.opportunityImageView);
        titleEditText = findViewById(R.id.titleEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        dateEditText = findViewById(R.id.dateEditText);
        timeEditText = findViewById(R.id.timeEditText);
        locationEditText = findViewById(R.id.locationEditText);
        createButton = findViewById(R.id.createButton);
    }

    private void setupListeners() {
        findViewById(R.id.changeImageButton).setOnClickListener(v ->
                pickMedia.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build())
        );

        dateEditText.setOnClickListener(v -> showDatePicker());
        timeEditText.setOnClickListener(v -> showTimePicker());
        locationEditText.setOnClickListener(v -> showLocationPicker());
        createButton.setOnClickListener(v -> validateAndCreateOpportunity());
    }

    private void checkOrganizationStatus() {
        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String orgName = documentSnapshot.getString("organizationName");
                    if (orgName == null || orgName.trim().isEmpty()) {
                        new MaterialAlertDialogBuilder(this)
                                .setTitle("Organization Required")
                                .setMessage("You need to set an organization name in your profile before creating opportunities.")
                                .setPositiveButton("OK", (dialog, which) -> finish())
                                .setCancelable(false)
                                .show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking organization status: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void showDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDateTime.set(Calendar.YEAR, year);
                    selectedDateTime.set(Calendar.MONTH, month);
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateDisplay();
                },
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH)
        );
        dialog.getDatePicker().setMinDate(System.currentTimeMillis());
        dialog.show();
    }

    private void showTimePicker() {
        new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedDateTime.set(Calendar.MINUTE, minute);
                    updateTimeDisplay();
                },
                selectedDateTime.get(Calendar.HOUR_OF_DAY),
                selectedDateTime.get(Calendar.MINUTE),
                false
        ).show();
    }

    private void showLocationPicker() {
        String[] locations = LocationUtils.getLocations().toArray(new String[0]);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Location")
                .setItems(locations, (dialog, which) ->
                        locationEditText.setText(locations[which]))
                .show();
    }

    private void updateDateDisplay() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        dateEditText.setText(dateFormat.format(selectedDateTime.getTime()));
    }

    private void updateTimeDisplay() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        timeEditText.setText(timeFormat.format(selectedDateTime.getTime()));
    }

    private void validateAndCreateOpportunity() {
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String location = locationEditText.getText().toString().trim();
        String date = dateEditText.getText().toString().trim();
        String time = timeEditText.getText().toString().trim();

        // Validate inputs
        if (title.isEmpty()) {
            titleEditText.setError("Title is required");
            return;
        }
        if (description.isEmpty()) {
            descriptionEditText.setError("Description is required");
            return;
        }
        if (location.isEmpty()) {
            locationEditText.setError("Location is required");
            return;
        }
        if (date.isEmpty()) {
            dateEditText.setError("Date is required");
            return;
        }
        if (time.isEmpty()) {
            timeEditText.setError("Time is required");
            return;
        }

        createButton.setEnabled(false);
        UIUtils.showLoading(this, "Creating opportunity...");

        if (selectedImageUri != null) {
            String imageId = UUID.randomUUID().toString();
            ImageUtils.uploadImage(
                    this,
                    selectedImageUri,
                    "opportunities",
                    imageId,
                    new ImageUtils.OnCompleteListener<String>() {
                        @Override
                        public void onSuccess(String imagePath) {
                            Log.d("CreateOpportunity", "Image uploaded successfully: " + imagePath);
                            createOpportunityInFirestore(imagePath);
                        }

                        @Override
                        public void onFailure(String error) {
                            UIUtils.hideLoading();
                            createButton.setEnabled(true);
                            Toast.makeText(CreateOpportunityActivity.this,
                                    "Failed to upload image: " + error,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            createOpportunityInFirestore(null);
        }
    }

    private void createOpportunityInFirestore(String imagePath) {
        String userId = mAuth.getCurrentUser().getUid();

        // Get organization name
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String organizationName = documentSnapshot.getString("organizationName");

                    Map<String, Object> opportunity = new HashMap<>();
                    opportunity.put("title", titleEditText.getText().toString().trim());
                    opportunity.put("description", descriptionEditText.getText().toString().trim());
                    opportunity.put("location", locationEditText.getText().toString().trim());
                    opportunity.put("date", selectedDateTime.getTime());
                    opportunity.put("time", timeEditText.getText().toString().trim());
                    opportunity.put("organizationId", userId);
                    opportunity.put("organizationName", organizationName);
                    opportunity.put("status", "active");
                    opportunity.put("createdAt", new Date());
                    if (imagePath != null) {
                        opportunity.put("imageUrl", imagePath);
                    }

                    db.collection("opportunities")
                            .add(opportunity)
                            .addOnSuccessListener(documentReference -> {
                                UIUtils.hideLoading();
                                Toast.makeText(this, "Opportunity created successfully!",
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> handleError(e.getMessage()));
                })
                .addOnFailureListener(e -> handleError(e.getMessage()));
    }

    private void handleError(String message) {
        UIUtils.hideLoading();
        createButton.setEnabled(true);
        Toast.makeText(this, "Error: " + message, Toast.LENGTH_SHORT).show();
    }
}