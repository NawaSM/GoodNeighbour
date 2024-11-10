package student.inti.goodneighbour.ui.profile;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.net.Uri;
import android.widget.ImageView;
import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.Timestamp;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import student.inti.goodneighbour.R;
import student.inti.goodneighbour.ui.auth.LoginActivity;
import student.inti.goodneighbour.ui.opportunities.ViewApplicationsActivity;
import student.inti.goodneighbour.ui.opportunities.ManageOpportunitiesActivity;
import student.inti.goodneighbour.utils.LocationUtils;
import student.inti.goodneighbour.utils.ImageUtils;
import student.inti.goodneighbour.utils.UIUtils;

public class ProfileFragment extends Fragment {
    private View rootView;
    private MaterialButton manageApplicationsButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private TextView totalApplicationsText;
    private TextView approvedApplicationsText;
    private TextView pendingApplicationsText;
    private TextView rejectedApplicationsText;
    private TextView createdOpportunitiesText;
    private TextView receivedApplicationsText;

    private TextInputEditText nameEditText, emailEditText, phoneEditText, locationEditText;
    private MaterialButton editProfileButton;
    private ProgressBar progressBar;
    private boolean isEditing = false;
    private ImageView profileImageView;
    private TextInputEditText organizationEditText;
    private Uri selectedProfileImageUri;
    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    selectedProfileImageUri = uri;
                    uploadProfileImage(uri);
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews(rootView);
        setupListeners(rootView);
        setupApplicationsManagement();

        // Load data only if user is authenticated
        if (mAuth.getCurrentUser() != null) {
            loadData();
        } else {
            // Handle not authenticated state
            redirectToLogin();
        }

        return rootView;
    }

    private void redirectToLogin() {
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void initializeViews(View view) {
        // Initialize TextInputEditTexts
        nameEditText = view.findViewById(R.id.nameEditText);
        emailEditText = view.findViewById(R.id.emailEditText);
        phoneEditText = view.findViewById(R.id.phoneEditText);
        locationEditText = view.findViewById(R.id.locationEditText);
        organizationEditText = view.findViewById(R.id.organizationEditText);
        profileImageView = view.findViewById(R.id.profileImageView);
        editProfileButton = view.findViewById(R.id.editProfileButton);
        progressBar = view.findViewById(R.id.progressBar);

        // Initialize TextViews with colored backgrounds
        totalApplicationsText = view.findViewById(R.id.totalApplicationsText);
        approvedApplicationsText = view.findViewById(R.id.approvedApplicationsText);
        pendingApplicationsText = view.findViewById(R.id.pendingApplicationsText);
        rejectedApplicationsText = view.findViewById(R.id.rejectedApplicationsText);
        createdOpportunitiesText = view.findViewById(R.id.createdOpportunitiesText);
        receivedApplicationsText = view.findViewById(R.id.receivedApplicationsText);

        // Set background tints for statistics
        totalApplicationsText.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.status_total)));
        approvedApplicationsText.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.status_approved)));
        pendingApplicationsText.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.status_pending)));
        rejectedApplicationsText.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.status_rejected)));

        // Set text color to white for better contrast
        int white = getResources().getColor(android.R.color.white);
        totalApplicationsText.setTextColor(white);
        approvedApplicationsText.setTextColor(white);
        pendingApplicationsText.setTextColor(white);
        rejectedApplicationsText.setTextColor(white);

        // Initialize buttons
        editProfileButton = view.findViewById(R.id.editProfileButton);
        manageApplicationsButton = view.findViewById(R.id.manageApplicationsButton);
        progressBar = view.findViewById(R.id.progressBar);

        // Set initial states
        setFieldsEditable(false);
    }

    private void loadData() {
        loadUserData();
        loadOrganizationStats();
        loadVolunteerStats();
    }

    private void setupListeners(View view) {
        editProfileButton.setOnClickListener(v -> toggleEditMode());

        locationEditText.setOnClickListener(v -> {
            if (isEditing) {
                showLocationPicker();
            }
        });

        // Makes the location field look clickable when in edit mode
        locationEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && isEditing) {
                showLocationPicker();
            }
        });

        // Add profile picture change listener
        view.findViewById(R.id.changePhotoButton).setOnClickListener(v -> {
            if (isEditing) {
                pickMedia.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build());
            } else {
                Toast.makeText(getContext(),
                        "Enable edit mode to change profile picture",
                        Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.manageOpportunitiesButton).setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ManageOpportunitiesActivity.class);
            startActivity(intent);
        });

        view.findViewById(R.id.logoutButton).setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Logout", (dialog, which) -> {
                        FirebaseAuth.getInstance().signOut();
                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void loadProfileImage(String imagePath) {
        ImageUtils.loadImage(imagePath, new ImageUtils.OnCompleteListener<Bitmap>() {
            @Override
            public void onSuccess(Bitmap bitmap) {
                if (isAdded() && profileImageView != null) {  // Check if Fragment is still attached
                    profileImageView.setImageBitmap(bitmap);
                }
            }

            @Override
            public void onFailure(String error) {
                if (isAdded()) {  // Check if Fragment is still attached
                    Log.e("ProfileFragment", "Failed to load profile image: " + error);
                    profileImageView.setImageResource(R.drawable.default_profile);
                    Toast.makeText(getContext(),
                            "Failed to load profile image",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void uploadProfileImage(Uri imageUri) {
        if (mAuth.getCurrentUser() == null) return;

        UIUtils.showLoading(requireContext(), "Uploading profile picture...");
        String userId = mAuth.getCurrentUser().getUid();

        ImageUtils.uploadImage(
                requireContext(),
                imageUri,
                "profile_pictures",
                userId,
                new ImageUtils.OnCompleteListener<String>() {
                    @Override
                    public void onSuccess(String imagePath) {
                        // Update Firestore with the image path
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("profileImageUrl", imagePath);

                        db.collection("users").document(userId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    UIUtils.hideLoading();
                                    Toast.makeText(getContext(),
                                            "Profile picture updated successfully",
                                            Toast.LENGTH_SHORT).show();
                                    loadProfileImage(imagePath);
                                })
                                .addOnFailureListener(e -> {
                                    UIUtils.hideLoading();
                                    Toast.makeText(getContext(),
                                            "Failed to update profile: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                    }

                    @Override
                    public void onFailure(String error) {
                        UIUtils.hideLoading();
                        Toast.makeText(getContext(),
                                "Failed to upload image: " + error,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void toggleEditMode() {
        isEditing = !isEditing;
        setFieldsEditable(isEditing);

        if (isEditing) {
            // Entering edit mode
            editProfileButton.setText("Save Changes");
            editProfileButton.setIconResource(R.drawable.ic_save);
            Toast.makeText(getContext(), "You can now edit your profile", Toast.LENGTH_SHORT).show();
        } else {
            // Exiting edit mode - save changes
            saveUserData();
            editProfileButton.setText("Edit Profile");
            editProfileButton.setIconResource(R.drawable.ic_edit);
        }
    }

    private void setFieldsEditable(boolean editable) {
        nameEditText.setEnabled(editable);
        phoneEditText.setEnabled(editable);

        // Update the location EditText appearance
        locationEditText.setEnabled(editable);
        locationEditText.setFocusable(false);  // false to prevent keyboard
        locationEditText.setClickable(editable);

        // Update the cursor visibility and background
        if (editable) {
            checkOrganizationUpdateEligibility();
        }

        // Email stays disabled as it shouldn't be editable
        emailEditText.setEnabled(false);
    }

    private void checkOrganizationUpdateEligibility() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Timestamp lastUpdated = documentSnapshot.getTimestamp("organizationLastUpdated");
                        String currentOrgName = documentSnapshot.getString("organizationName");

                        boolean canUpdate = true;
                        String message = null;

                        if (lastUpdated != null && currentOrgName != null && !currentOrgName.isEmpty()) {
                            // Calculate days since last update
                            long daysSinceUpdate = (Timestamp.now().getSeconds() -
                                    lastUpdated.getSeconds()) / (60 * 60 * 24);

                            if (daysSinceUpdate < 30) {
                                canUpdate = false;
                                message = "Organization name can only be changed once every 30 days. " +
                                        "Days remaining: " + (30 - daysSinceUpdate);
                            }
                        }

                        organizationEditText.setEnabled(canUpdate);
                        if (!canUpdate) {
                            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void showLocationPicker() {
        List<String> locations = LocationUtils.getLocations();
        String[] locationArray = locations.toArray(new String[0]);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Location")
                .setItems(locationArray, (dialog, which) -> {
                    locationEditText.setText(locationArray[which]);
                })
                .show();
    }

    private void saveUserData() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        String newOrgName = organizationEditText.getText().toString().trim();

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String currentOrgName = documentSnapshot.getString("organizationName");

                    // Create updates map
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("name", nameEditText.getText().toString().trim());
                    updates.put("phone", phoneEditText.getText().toString().trim());
                    updates.put("location", locationEditText.getText().toString().trim());

                    // Handle organization name update
                    if (!newOrgName.equals(currentOrgName)) {
                        updates.put("organizationName", newOrgName);
                        updates.put("organizationLastUpdated", Timestamp.now());
                    }

                    // Update Firestore
                    progressBar.setVisibility(View.VISIBLE);
                    db.collection("users").document(userId)
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Profile updated successfully",
                                        Toast.LENGTH_SHORT).show();
                                toggleEditMode(); // Exit edit mode
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(),
                                        "Error updating profile: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void loadUserData() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        progressBar.setVisibility(View.VISIBLE);

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        nameEditText.setText(documentSnapshot.getString("name"));
                        emailEditText.setText(documentSnapshot.getString("email"));
                        phoneEditText.setText(documentSnapshot.getString("phone"));
                        locationEditText.setText(documentSnapshot.getString("location"));
                        organizationEditText.setText(documentSnapshot.getString("organizationName"));

                        // Load profile picture using ImageUtils
                        String imagePath = documentSnapshot.getString("profileImageUrl");
                        if (imagePath != null && !imagePath.isEmpty()) {
                            loadProfileImage(imagePath);
                        } else {
                            profileImageView.setImageResource(R.drawable.default_profile);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(),
                            "Error loading profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void loadOrganizationStats() {
        if (mAuth.getCurrentUser() == null || !isAdded()) return;
        String userId = mAuth.getCurrentUser().getUid();

        // Load created opportunities count
        db.collection("opportunities")
                .whereEqualTo("organizationId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded() || createdOpportunitiesText == null) return;
                    int opportunityCount = queryDocumentSnapshots.size();
                    createdOpportunitiesText.setText("Created Opportunities: " + opportunityCount);
                    Log.d("ProfileStats", "Found " + opportunityCount + " opportunities");
                    createdOpportunitiesText.setText("Created Opportunities: " + opportunityCount);

                    // After getting opportunities, count all applications for these opportunities
                    List<String> opportunityIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        opportunityIds.add(doc.getId());
                        Log.d("ProfileStats", "Added opportunity ID: " + doc.getId());
                    }

                    if (!opportunityIds.isEmpty()) {
                        db.collection("applications")
                                .whereIn("opportunityId", opportunityIds)
                                .get()
                                .addOnSuccessListener(applicationSnapshots -> {
                                    if (!isAdded() || receivedApplicationsText == null) return;
                                    int applicationCount = applicationSnapshots.size();
                                    Log.d("ProfileStats", "Found " + applicationCount + " applications");
                                    receivedApplicationsText.setText("Applications Received: " + applicationCount);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("ProfileStats", "Error loading applications", e);
                                    if (isAdded()) {
                                        Toast.makeText(getContext(),
                                                "Error loading application count: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Log.d("ProfileStats", "No opportunities found");
                        if (receivedApplicationsText != null) {
                            receivedApplicationsText.setText("Applications Received: 0");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileStats", "Error loading opportunities", e);
                    if (isAdded()) {
                        Toast.makeText(getContext(),
                                "Error loading opportunities: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadVolunteerStats() {
        if (mAuth.getCurrentUser() == null || !isAdded()) return;
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("applications")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;
                    if (totalApplicationsText == null || approvedApplicationsText == null ||
                            pendingApplicationsText == null || rejectedApplicationsText == null) return;

                    int totalApplications = queryDocumentSnapshots.size();

                    // Count applications by status
                    long approvedApplications = queryDocumentSnapshots.getDocuments().stream()
                            .filter(doc -> "approved".equals(doc.getString("status")))
                            .count();

                    long pendingApplications = queryDocumentSnapshots.getDocuments().stream()
                            .filter(doc -> "pending".equals(doc.getString("status")))
                            .count();

                    long rejectedApplications = queryDocumentSnapshots.getDocuments().stream()
                            .filter(doc -> "rejected".equals(doc.getString("status")))
                            .count();

                    // Update UI
                    totalApplicationsText.setText("Total Applications: " + totalApplications);
                    approvedApplicationsText.setText("Approved: " + approvedApplications);
                    pendingApplicationsText.setText("Pending: " + pendingApplications);
                    rejectedApplicationsText.setText("Rejected: " + rejectedApplications);
                });
    }

    private void setupApplicationsManagement() {
        if (manageApplicationsButton == null || mAuth.getCurrentUser() == null) {
            return;
        }

        // Show/hide button based on organization status
        db.collection("users")
                .document(mAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isAdded()) return;  // Check if fragment is still attached

                    String orgName = documentSnapshot.getString("organizationName");
                    manageApplicationsButton.setVisibility(
                            orgName != null && !orgName.isEmpty() ? View.VISIBLE : View.GONE
                    );
                });

        // Set click listener
        manageApplicationsButton.setOnClickListener(v -> {
            if (!isAdded() || mAuth.getCurrentUser() == null) return;

            // Get current user's opportunities to check for applications
            db.collection("opportunities")
                    .whereEqualTo("organizationId", mAuth.getCurrentUser().getUid())
                    .get()
                    .addOnSuccessListener(opportunities -> {
                        if (!isAdded()) return;  // Check if fragment is still attached

                        if (opportunities.isEmpty()) {
                            Toast.makeText(getContext(),
                                    "No opportunities created yet",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Create intent to ViewApplicationsActivity
                        Intent intent = new Intent(getContext(), ViewApplicationsActivity.class);
                        startActivity(intent);
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded()) return;
                        Toast.makeText(getContext(),
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        });
    }


    private void handleLogout() {
        mAuth.signOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}