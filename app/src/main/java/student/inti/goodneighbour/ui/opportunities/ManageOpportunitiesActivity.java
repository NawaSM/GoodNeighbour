package student.inti.goodneighbour.ui.opportunities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import student.inti.goodneighbour.R;
import student.inti.goodneighbour.models.Opportunity;
import student.inti.goodneighbour.utils.LocationUtils;

public class ManageOpportunitiesActivity extends AppCompatActivity implements ManageOpportunitiesAdapter.OnOpportunityActionListener {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private ManageOpportunitiesAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private TextView emptyView;
    private View progressBar;
    private EditText searchEditText;
    private List<Opportunity> allOpportunities = new ArrayList<>();
    private String currentSearch = "";
    private String currentSortMethod = "dateDesc"; // default sort
    private Set<String> selectedLocations = new HashSet<>();
    private String selectedStatus = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_opportunities);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize views
        recyclerView = findViewById(R.id.opportunitiesRecyclerView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        emptyView = findViewById(R.id.emptyView);
        progressBar = findViewById(R.id.progressBar);
        searchEditText = findViewById(R.id.searchEditText);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ManageOpportunitiesAdapter(this);
        recyclerView.setAdapter(adapter);

        // Setup SwipeRefreshLayout
        swipeRefresh.setOnRefreshListener(this::loadOpportunities);

        // Setup search
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                currentSearch = s.toString();
                filterAndSortOpportunities();
            }
        });

        // Setup filter button
        findViewById(R.id.filterButton).setOnClickListener(v -> showFilterAndSortDialog());

        // Load initial data
        loadOpportunities();
    }

    private void loadOpportunities() {
        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        Log.d("ManageOpps", "Loading opportunities for user: " + userId);

        if (!swipeRefresh.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);

        db.collection("opportunities")
                .whereEqualTo("organizationId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allOpportunities.clear();
                    Log.d("ManageOpps", "Retrieved " + queryDocumentSnapshots.size() + " opportunities");
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Opportunity opportunity = document.toObject(Opportunity.class);
                        opportunity.setId(document.getId());
                        allOpportunities.add(opportunity);
                        Log.d("ManageOpps", "Added opportunity: " + opportunity.getTitle());
                    }
                    filterAndSortOpportunities();
                })
                .addOnFailureListener(e -> {
                    Log.e("ManageOpps", "Error loading opportunities: " + e.getMessage());
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, "Error loading opportunities: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void showFilterAndSortDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_filter_opportunities, null);
        RadioGroup sortGroup = view.findViewById(R.id.sortGroup);
        ChipGroup locationChipGroup = view.findViewById(R.id.locationChipGroup);
        RadioGroup statusGroup = view.findViewById(R.id.statusGroup);

        // Set current sort method
        switch (currentSortMethod) {
            case "dateDesc":
                sortGroup.check(R.id.sortDateDesc);
                break;
            case "dateAsc":
                sortGroup.check(R.id.sortDateAsc);
                break;
            case "title":
                sortGroup.check(R.id.sortTitle);
                break;
            case "applications":
                sortGroup.check(R.id.sortApplications);
                break;
        }

        // Setup location chips
        List<String> locations = LocationUtils.getLocations();
        for (String location : locations) {
            Chip chip = new Chip(this);
            chip.setText(location);
            chip.setCheckable(true);
            chip.setChecked(selectedLocations.contains(location));
            locationChipGroup.addView(chip);
        }

        // Set current status
        switch (selectedStatus) {
            case "all":
                statusGroup.check(R.id.statusAll);
                break;
            case "active":
                statusGroup.check(R.id.statusActive);
                break;
            case "closed":
                statusGroup.check(R.id.statusClosed);
                break;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Filter & Sort Opportunities")
                .setView(view)
                .setPositiveButton("Apply", (dialog, which) -> {
                    // Get sort method
                    int sortId = sortGroup.getCheckedRadioButtonId();
                    if (sortId == R.id.sortDateDesc) currentSortMethod = "dateDesc";
                    else if (sortId == R.id.sortDateAsc) currentSortMethod = "dateAsc";
                    else if (sortId == R.id.sortTitle) currentSortMethod = "title";
                    else if (sortId == R.id.sortApplications) currentSortMethod = "applications";

                    // Get selected locations
                    selectedLocations.clear();
                    for (int i = 0; i < locationChipGroup.getChildCount(); i++) {
                        Chip chip = (Chip) locationChipGroup.getChildAt(i);
                        if (chip.isChecked()) {
                            selectedLocations.add(chip.getText().toString());
                        }
                    }

                    // Get selected status
                    int statusId = statusGroup.getCheckedRadioButtonId();
                    if (statusId == R.id.statusAll) selectedStatus = "all";
                    else if (statusId == R.id.statusActive) selectedStatus = "active";
                    else if (statusId == R.id.statusClosed) selectedStatus = "closed";

                    filterAndSortOpportunities();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void filterAndSortOpportunities() {
        List<Opportunity> filteredList = new ArrayList<>(allOpportunities);
        Log.d("ManageOpps", "Filtering " + allOpportunities.size() + " opportunities");

        // Apply search filter
        if (!currentSearch.isEmpty()) {
            String searchLower = currentSearch.toLowerCase();
            filteredList = filteredList.stream()
                    .filter(opp -> opp.getTitle().toLowerCase().contains(searchLower) ||
                            opp.getDescription().toLowerCase().contains(searchLower))
                    .collect(Collectors.toList());
        }

        // Apply location filter
        if (!selectedLocations.isEmpty()) {
            filteredList = filteredList.stream()
                    .filter(opp -> selectedLocations.contains(opp.getLocation()))
                    .collect(Collectors.toList());
        }

        // Apply status filter
        if (!selectedStatus.equals("all")) {
            filteredList = filteredList.stream()
                    .filter(opp -> opp.getStatus().equals(selectedStatus))
                    .collect(Collectors.toList());
        }

        // Apply sorting
        switch (currentSortMethod) {
            case "dateDesc":
                filteredList.sort((o1, o2) -> o2.getDate().compareTo(o1.getDate()));
                break;
            case "dateAsc":
                filteredList.sort((o1, o2) -> o1.getDate().compareTo(o2.getDate()));
                break;
            case "title":
                filteredList.sort((o1, o2) -> o1.getTitle().compareToIgnoreCase(o2.getTitle()));
                break;
            case "applications":
                break;
        }

        // Update UI
        adapter.setOpportunities(filteredList);
        Log.d("ManageOpps", "Setting " + filteredList.size() + " opportunities to adapter");
        progressBar.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);

        if (filteredList.isEmpty()) {
            Log.d("ManageOpps", "No opportunities to display");
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            Log.d("ManageOpps", "Showing opportunities list");
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onEditClick(Opportunity opportunity) {
        Intent intent = new Intent(this, EditOpportunityActivity.class);
        intent.putExtra("opportunity_id", opportunity.getId());
        startActivity(intent);
    }

    @Override
    public void onViewApplicationsClick(Opportunity opportunity) {
        Intent intent = new Intent(this, ViewApplicationsActivity.class);
        intent.putExtra("opportunity_id", opportunity.getId());
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(Opportunity opportunity) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Opportunity")
                .setMessage("Are you sure you want to delete this opportunity? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    db.collection("opportunities").document(opportunity.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "Opportunity deleted successfully",
                                        Toast.LENGTH_SHORT).show();
                                loadOpportunities();
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "Error deleting opportunity: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}