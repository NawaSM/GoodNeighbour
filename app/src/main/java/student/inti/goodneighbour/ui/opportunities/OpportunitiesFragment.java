package student.inti.goodneighbour.ui.opportunities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import student.inti.goodneighbour.R;
import student.inti.goodneighbour.models.Opportunity;
import student.inti.goodneighbour.ui.auth.LoginActivity;
import student.inti.goodneighbour.utils.LocationUtils;

public class OpportunitiesFragment extends Fragment implements OpportunitiesAdapter.OnOpportunityClickListener {
    private String currentSearch = "";
    private String currentSortBy = "date";
    private boolean sortAscending = false;
    private Set<String> selectedLocations = new HashSet<>();
    private BottomSheetDialog filterDialog;
    private RecyclerView recyclerView;
    private OpportunitiesAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyView;
    private SwipeRefreshLayout swipeRefresh;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_opportunities, container, false);

        // Setup FAB
        view.findViewById(R.id.addOpportunityFab).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CreateOpportunityActivity.class);
            startActivity(intent);
        });

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Get current user ID
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            // Handle not authenticated state
            startActivity(new Intent(requireContext(), LoginActivity.class));
            requireActivity().finish();
            return view;
        }
        String currentUserId = mAuth.getCurrentUser().getUid();

        // Initialize views
        recyclerView = view.findViewById(R.id.opportunitiesRecyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyView = view.findViewById(R.id.emptyView);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);

        // Setup search
        EditText searchEditText = view.findViewById(R.id.searchEditText);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                currentSearch = s.toString();
                loadOpportunities();
            }
        });

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new OpportunitiesAdapter(this, currentUserId);
        recyclerView.setAdapter(adapter);

        // Setup SwipeRefreshLayout
        swipeRefresh.setOnRefreshListener(this::loadOpportunities);

        // Setup filter button
        view.findViewById(R.id.filterButton).setOnClickListener(v -> showFilterDialog());

        // Load initial data
        loadOpportunities();

        return view;
    }

    private void showFilterDialog() {
        if (filterDialog == null) {
            filterDialog = new BottomSheetDialog(requireContext());
            View view = getLayoutInflater().inflate(R.layout.layout_filter_opportunities, null);
            filterDialog.setContentView(view);

            RadioGroup sortGroup = view.findViewById(R.id.sortGroup);
            ChipGroup locationChipGroup = view.findViewById(R.id.locationChipGroup);

            // Setup sort
            sortGroup.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.sortDate) {
                    currentSortBy = "date";
                    sortAscending = false;
                } else if (checkedId == R.id.sortDateAsc) {
                    currentSortBy = "date";
                    sortAscending = true;
                } else if (checkedId == R.id.sortTitle) {
                    currentSortBy = "title";
                    sortAscending = true;
                }
                loadOpportunities();
            });

            // Load unique locations and create chips
            loadLocations(locationChipGroup);
        }
        filterDialog.show();
    }

    private void loadLocations(ChipGroup chipGroup) {
        // Get locations from LocationUtils
        List<String> locations = LocationUtils.getLocations();

        chipGroup.removeAllViews();
        for (String location : locations) {
            Chip chip = new Chip(requireContext());
            chip.setText(location);
            chip.setCheckable(true);
            chip.setChecked(selectedLocations.contains(location));
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedLocations.add(location);
                } else {
                    selectedLocations.remove(location);
                }
                loadOpportunities();
            });
            chipGroup.addView(chip);
        }
    }

    private void loadOpportunities() {
        if (!swipeRefresh.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);

        // Start with a simple query
        db.collection("opportunities")
                .whereEqualTo("status", "active")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Opportunity> opportunities = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Opportunity opportunity = document.toObject(Opportunity.class);
                            opportunity.setId(document.getId());
                            opportunities.add(opportunity);
                        }

                        // Sort in memory instead of in query
                        opportunities.sort((o1, o2) -> {
                            if (currentSortBy.equals("date")) {
                                return sortAscending ?
                                        o1.getDate().compareTo(o2.getDate()) :
                                        o2.getDate().compareTo(o1.getDate());
                            } else {
                                return o1.getTitle().compareToIgnoreCase(o2.getTitle());
                            }
                        });

                        // Apply search filter
                        if (!currentSearch.isEmpty()) {
                            String searchLower = currentSearch.toLowerCase();
                            opportunities = opportunities.stream()
                                    .filter(opp ->
                                            opp.getTitle().toLowerCase().contains(searchLower) ||
                                                    opp.getDescription().toLowerCase().contains(searchLower))
                                    .collect(Collectors.toList());
                        }

                        // Apply location filter
                        if (!selectedLocations.isEmpty()) {
                            opportunities = opportunities.stream()
                                    .filter(opp -> selectedLocations.contains(opp.getLocation()))
                                    .collect(Collectors.toList());
                        }

                        adapter.setOpportunities(opportunities);

                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);

                        if (opportunities.isEmpty()) {
                            emptyView.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            emptyView.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Toast.makeText(getContext(),
                                "Error loading opportunities: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                    }
                });
    }

    @Override
    public void onItemClick(Opportunity opportunity) {
        Intent intent = new Intent(requireActivity(), OpportunityDetailsActivity.class);
        intent.putExtra("opportunity_id", opportunity.getId());
        startActivity(intent);
    }

    @Override
    public void onApplyClick(Opportunity opportunity) {
        Intent intent = new Intent(requireActivity(), OpportunityDetailsActivity.class);
        intent.putExtra("opportunity_id", opportunity.getId());
        startActivity(intent);
    }

}