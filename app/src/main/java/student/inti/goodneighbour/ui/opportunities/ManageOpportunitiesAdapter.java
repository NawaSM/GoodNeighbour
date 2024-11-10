package student.inti.goodneighbour.ui.opportunities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.graphics.Bitmap;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import student.inti.goodneighbour.R;
import student.inti.goodneighbour.models.Opportunity;
import student.inti.goodneighbour.utils.ImageUtils;

public class ManageOpportunitiesAdapter extends RecyclerView.Adapter<ManageOpportunitiesAdapter.OpportunityViewHolder> {
    private List<Opportunity> opportunities = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final OnOpportunityActionListener listener;

    public interface OnOpportunityActionListener {
        void onEditClick(Opportunity opportunity);
        void onDeleteClick(Opportunity opportunity);
        void onViewApplicationsClick(Opportunity opportunity);
    }

    public ManageOpportunitiesAdapter(OnOpportunityActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public OpportunityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_manage_opportunity, parent, false);
        return new OpportunityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OpportunityViewHolder holder, int position) {
        holder.bind(opportunities.get(position));
    }

    @Override
    public int getItemCount() {
        return opportunities.size();
    }

    public void setOpportunities(List<Opportunity> opportunities) {
        this.opportunities = opportunities;
        notifyDataSetChanged();
    }

    class OpportunityViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final TextView titleText;
        private final TextView dateText;
        private final TextView locationText;
        private final TextView applicationCountText;
        private final MaterialButton editButton;
        private final MaterialButton viewApplicationsButton;
        private final ImageButton archiveButton;
        private final ImageButton deleteButton;

        OpportunityViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.opportunityImage);
            titleText = itemView.findViewById(R.id.opportunityTitle);
            dateText = itemView.findViewById(R.id.opportunityDate);
            locationText = itemView.findViewById(R.id.opportunityLocation);
            applicationCountText = itemView.findViewById(R.id.applicationCount);
            editButton = itemView.findViewById(R.id.editButton);
            viewApplicationsButton = itemView.findViewById(R.id.viewApplicationsButton);
            archiveButton = itemView.findViewById(R.id.archiveButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }

        void bind(Opportunity opportunity) {
            titleText.setText(opportunity.getTitle());
            dateText.setText(String.format("%s at %s",
                    dateFormat.format(opportunity.getDate()),
                    opportunity.getTime()));
            locationText.setText(opportunity.getLocation());

            // Load image using ImageUtils
            String imagePath = opportunity.getImageUrl();
            if (imagePath != null && !imagePath.trim().isEmpty()) {
                ImageUtils.loadImage(imagePath, new ImageUtils.OnCompleteListener<Bitmap>() {
                    @Override
                    public void onSuccess(Bitmap bitmap) {
                        imageView.setImageBitmap(bitmap);
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e("ManageOpportunitiesAdapter", "Failed to load image: " + error);
                        imageView.setImageResource(R.drawable.placeholder_image);
                    }
                });
            } else {
                imageView.setImageResource(R.drawable.placeholder_image);
            }

            archiveButton.setImageResource(
                    "archived".equals(opportunity.getStatus()) ?
                            R.drawable.ic_unarchive : R.drawable.ic_archive
            );

            // Setup click listeners
            editButton.setOnClickListener(v -> listener.onEditClick(opportunity));
            deleteButton.setOnClickListener(v -> listener.onDeleteClick(opportunity));
            viewApplicationsButton.setOnClickListener(v -> listener.onViewApplicationsClick(opportunity));
            archiveButton.setOnClickListener(v -> {
                String newStatus = "archived".equals(opportunity.getStatus()) ? "active" : "archived";
                FirebaseFirestore.getInstance()
                        .collection("opportunities")
                        .document(opportunity.getId())
                        .update("status", newStatus)
                        .addOnSuccessListener(aVoid -> {
                            opportunity.setStatus(newStatus);
                            // Update UI
                            archiveButton.setImageResource(
                                    "archived".equals(newStatus) ?
                                            R.drawable.ic_unarchive : R.drawable.ic_archive
                            );
                            Toast.makeText(itemView.getContext(),
                                    "archived".equals(newStatus) ?
                                            "Opportunity archived" : "Opportunity unarchived",
                                    Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(itemView.getContext(),
                                        "Failed to update status: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show()
                        );
            });

            FirebaseFirestore.getInstance()
                    .collection("applications")
                    .whereEqualTo("opportunityId", opportunity.getId())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        int totalCount = queryDocumentSnapshots.size();

                        // Count by status
                        long pendingCount = queryDocumentSnapshots.getDocuments().stream()
                                .filter(doc -> "pending".equals(doc.getString("status")))
                                .count();

                        long approvedCount = queryDocumentSnapshots.getDocuments().stream()
                                .filter(doc -> "approved".equals(doc.getString("status")))
                                .count();

                        long rejectedCount = queryDocumentSnapshots.getDocuments().stream()
                                .filter(doc -> "rejected".equals(doc.getString("status")))
                                .count();

                        // Display counts
                        String countText = String.format("Applications: %d total (%d pending, %d approved, %d rejected)",
                                totalCount, pendingCount, approvedCount, rejectedCount);
                        applicationCountText.setText(countText);
                    })
                    .addOnFailureListener(e ->
                            applicationCountText.setText("Applications: Unable to load count"));
        }
    }
}