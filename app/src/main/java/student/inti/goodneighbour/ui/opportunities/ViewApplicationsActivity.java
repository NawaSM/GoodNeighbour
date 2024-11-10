package student.inti.goodneighbour.ui.opportunities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import student.inti.goodneighbour.R;
import student.inti.goodneighbour.models.Application;

public class ViewApplicationsActivity extends AppCompatActivity
        implements ApplicationsAdapter.OnApplicationActionListener {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private ApplicationsAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private TextView emptyView;
    private View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_applications);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        recyclerView = findViewById(R.id.applicationsRecyclerView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        emptyView = findViewById(R.id.emptyView);
        progressBar = findViewById(R.id.progressBar);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ApplicationsAdapter(this);
        recyclerView.setAdapter(adapter);

        // Setup SwipeRefreshLayout
        swipeRefresh.setOnRefreshListener(this::loadApplications);

        // Load initial data
        loadApplications();
    }

    private void loadApplications() {
        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        if (!swipeRefresh.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);

        // First get all opportunities for this organization
        db.collection("opportunities")
                .whereEqualTo("organizationId", userId)
                .get()
                .addOnSuccessListener(opportunityDocs -> {
                    List<String> opportunityIds = new ArrayList<>();
                    for (DocumentSnapshot doc : opportunityDocs) {
                        opportunityIds.add(doc.getId());
                    }

                    if (opportunityIds.isEmpty()) {
                        handleEmptyState();
                        return;
                    }

                    List<List<String>> batches = new ArrayList<>();
                    for (int i = 0; i < opportunityIds.size(); i += 10) {
                        batches.add(opportunityIds.subList(i,
                                Math.min(i + 10, opportunityIds.size())));
                    }

                    List<Application> allApplications = new ArrayList<>();
                    AtomicInteger completedQueries = new AtomicInteger(0);

                    for (List<String> batch : batches) {
                        db.collection("applications")
                                .whereIn("opportunityId", batch)
                                .orderBy("createdAt", Query.Direction.DESCENDING)
                                .get()
                                .addOnSuccessListener(applicationDocs -> {
                                    for (DocumentSnapshot doc : applicationDocs) {
                                        Application application = doc.toObject(Application.class);
                                        if (application != null) {
                                            application.setId(doc.getId());
                                            allApplications.add(application);
                                        }
                                    }

                                    // Check if all queries are complete
                                    if (completedQueries.incrementAndGet() == batches.size()) {
                                        updateUI(allApplications);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    handleError("Error loading applications: " + e.getMessage());
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    handleError("Error loading opportunities: " + e.getMessage());
                });
    }

    private void handleEmptyState() {
        progressBar.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
        emptyView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    private void updateUI(List<Application> applications) {
        progressBar.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);

        if (applications.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.setApplications(applications);
        }
    }

    private void handleError(String errorMessage) {
        progressBar.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onApprove(Application application) {
        updateApplicationStatus(application, "approved");
    }

    @Override
    public void onReject(Application application) {
        updateApplicationStatus(application, "rejected");
    }

    private void updateApplicationStatus(Application application, String status) {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("applications")
                .document(application.getId())
                .update("status", status)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "Application " + status,
                            Toast.LENGTH_SHORT).show();
                    loadApplications();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "Error updating application: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}