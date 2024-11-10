package student.inti.goodneighbour.ui.applications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import student.inti.goodneighbour.R;
import student.inti.goodneighbour.models.Application;

public class MyApplicationsAdapter extends RecyclerView.Adapter<MyApplicationsAdapter.ApplicationViewHolder> {
    private List<Application> applications = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a",
            Locale.getDefault());

    @NonNull
    @Override
    public ApplicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_application, parent, false);
        return new ApplicationViewHolder(view, dateFormat);
    }

    @Override
    public void onBindViewHolder(@NonNull ApplicationViewHolder holder, int position) {
        holder.bind(applications.get(position));
    }

    @Override
    public int getItemCount() {
        return applications.size();
    }

    public void setApplications(List<Application> applications) {
        this.applications = applications;
        notifyDataSetChanged();
    }

    class ApplicationViewHolder extends RecyclerView.ViewHolder {
        private final TextView opportunityTitle;
        private final TextView organizationName;
        private final TextView applicationDate;
        private final TextView statusText;
        private final TextView motivationText;
        private final SimpleDateFormat dateFormat;

        ApplicationViewHolder(View itemView, SimpleDateFormat dateFormat) {
            super(itemView);
            this.dateFormat = dateFormat;
            opportunityTitle = itemView.findViewById(R.id.opportunityTitle);
            organizationName = itemView.findViewById(R.id.organizationName);
            applicationDate = itemView.findViewById(R.id.applicationDate);
            statusText = itemView.findViewById(R.id.statusText);
            motivationText = itemView.findViewById(R.id.motivationText);
        }

        void bind(Application application) {
            // Set opportunity details (will be loaded separately)
            opportunityTitle.setText("Loading...");
            organizationName.setText("");

            // Set application details
            applicationDate.setText(dateFormat.format(application.getCreatedAt()));
            motivationText.setText(application.getMotivation());

            // Set status with appropriate color
            statusText.setText(application.getStatus().toUpperCase());
            int colorResId;
            switch (application.getStatus()) {
                case "approved":
                    colorResId = R.color.status_approved;
                    break;
                case "rejected":
                    colorResId = R.color.status_rejected;
                    break;
                default:
                    colorResId = R.color.status_pending;
                    break;
            }
            statusText.setBackgroundTintList(itemView.getContext().getColorStateList(colorResId));

            // Load opportunity details
            FirebaseFirestore.getInstance()
                    .collection("opportunities")
                    .document(application.getOpportunityId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            opportunityTitle.setText(documentSnapshot.getString("title"));
                            organizationName.setText(documentSnapshot.getString("organizationName"));
                        }
                    });
        }
    }
}