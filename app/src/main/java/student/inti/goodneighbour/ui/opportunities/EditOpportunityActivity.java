package student.inti.goodneighbour.ui.opportunities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.util.Log;
import android.graphics.Bitmap;



import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import student.inti.goodneighbour.R;
import student.inti.goodneighbour.models.Opportunity;
import student.inti.goodneighbour.utils.LocationUtils;
import student.inti.goodneighbour.utils.ImageUtils;
import student.inti.goodneighbour.utils.UIUtils;

public class EditOpportunityActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private Uri selectedImageUri;
    private String opportunityId;
    private String currentImageUrl;

    private ImageView opportunityImageView;
    private TextInputEditText titleEditText, descriptionEditText,
            dateEditText, timeEditText, locationEditText;
    private MaterialButton saveButton;
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

        opportunityId = getIntent().getStringExtra("opportunity_id");
        if (opportunityId == null) {
            Toast.makeText(this, "Error loading opportunity", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        selectedDateTime = Calendar.getInstance();

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Edit Opportunity");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize views
        initializeViews();
        setupListeners();
        loadOpportunityData();
    }

    private void initializeViews() {
        opportunityImageView = findViewById(R.id.opportunityImageView);
        titleEditText = findViewById(R.id.titleEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        dateEditText = findViewById(R.id.dateEditText);
        timeEditText = findViewById(R.id.timeEditText);
        locationEditText = findViewById(R.id.locationEditText);
        saveButton = findViewById(R.id.createButton);
        saveButton.setText("Save Changes"); // Change button text for edit mode
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
        saveButton.setOnClickListener(v -> validateAndUpdateOpportunity());
    }

    private void loadOpportunityData() {
        db.collection("opportunities").document(opportunityId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Opportunity opportunity = documentSnapshot.toObject(Opportunity.class);
                    if (opportunity != null) {
                        titleEditText.setText(opportunity.getTitle());
                        descriptionEditText.setText(opportunity.getDescription());
                        locationEditText.setText(opportunity.getLocation());

                        // Set date and time
                        selectedDateTime.setTime(opportunity.getDate());
                        updateDateDisplay();
                        timeEditText.setText(opportunity.getTime());

                        // Load image
                        currentImageUrl = opportunity.getImageUrl();
                        if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
                            ImageUtils.loadImage(currentImageUrl, new ImageUtils.OnCompleteListener<Bitmap>() {
                                @Override
                                public void onSuccess(Bitmap bitmap) {
                                    opportunityImageView.setImageBitmap(bitmap);
                                }

                                @Override
                                public void onFailure(String error) {
                                    Log.e("EditOpportunity", "Failed to load image: " + error);
                                    opportunityImageView.setImageResource(R.drawable.placeholder_image);
                                }
                            });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading opportunity: " + e.getMessage(),
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

    private void validateAndUpdateOpportunity() {
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

        saveButton.setEnabled(false);
        UIUtils.showLoading(this, "Updating opportunity...");

        // If new image is selected, upload it first
        if (selectedImageUri != null) {
            // Generate a unique ID for the image
            String imageId = UUID.randomUUID().toString();

            ImageUtils.uploadImage(
                    this,
                    selectedImageUri,
                    "opportunities",
                    imageId,
                    new ImageUtils.OnCompleteListener<String>() {
                        @Override
                        public void onSuccess(String imagePath) {
                            updateOpportunityInFirestore(imagePath);
                        }

                        @Override
                        public void onFailure(String error) {
                            saveButton.setEnabled(true);
                            Toast.makeText(EditOpportunityActivity.this,
                                    "Failed to upload image: " + error,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            updateOpportunityInFirestore(currentImageUrl);
        }
    }

    private void uploadImageAndUpdateOpportunity() {
        String imagePath = "opportunities/" + UUID.randomUUID().toString();
        StorageReference imageRef = storage.getReference().child(imagePath);

        imageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot ->
                        imageRef.getDownloadUrl()
                                .addOnSuccessListener(uri ->
                                        updateOpportunityInFirestore(uri.toString()))
                                .addOnFailureListener(e -> handleError(e.getMessage())))
                .addOnFailureListener(e -> handleError(e.getMessage()));
    }

    private void updateOpportunityInFirestore(String imageUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", titleEditText.getText().toString().trim());
        updates.put("description", descriptionEditText.getText().toString().trim());
        updates.put("location", locationEditText.getText().toString().trim());
        updates.put("date", selectedDateTime.getTime());
        updates.put("time", timeEditText.getText().toString().trim());
        if (imageUrl != null) {
            updates.put("imageUrl", imageUrl);
        }

        db.collection("opportunities").document(opportunityId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    UIUtils.hideLoading();
                    Toast.makeText(this, "Opportunity updated successfully!",
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> handleError(e.getMessage()));
    }

    private void handleError(String message) {
        UIUtils.hideLoading();
        saveButton.setEnabled(true);
        Toast.makeText(this, "Error: " + message, Toast.LENGTH_SHORT).show();
    }
}